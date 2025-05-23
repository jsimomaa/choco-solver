/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver;

import gnu.trove.map.hash.TIntObjectHashMap;
import org.chocosolver.memory.IEnvironment;
import org.chocosolver.sat.MiniSat;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.nary.cnf.SatConstraint;
import org.chocosolver.solver.constraints.real.IbexHandler;
import org.chocosolver.solver.constraints.unary.BooleanConstraint;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.exception.SolverException;
import org.chocosolver.solver.objective.IObjectiveManager;
import org.chocosolver.solver.objective.ObjectiveFactory;
import org.chocosolver.solver.propagation.PropagationEngine;
import org.chocosolver.solver.variables.*;
import org.chocosolver.util.tools.ArrayUtils;
import org.chocosolver.util.tools.VariableUtils;
import org.ehcache.sizeof.SizeOf;
import org.ehcache.sizeof.filters.SizeOfFilter;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * The <code>Model</code> is the header component of Constraint Programming.
 * It embeds the list of <code>Variable</code> (and their <code>Domain</code>), the <code>Constraint</code>'s network,
 * and a <code>IPropagationEngine</code> to pilot the propagation.<br/>
 * <code>Model</code> includes a <code>AbstractSearchLoop</code> to guide the search loop: applying decisions and propagating,
 * running backups and rollbacks and storing solutions.
 *
 * @author Xavier Lorca
 * @author Charles Prud'homme
 * @author Jean-Guillaume Fages
 * @author Arnaud Malapert
 * @version 0.01, june 2010
 * @see org.chocosolver.solver.variables.Variable
 * @see org.chocosolver.solver.constraints.Constraint
 * @since 0.01
 */
public final class Model implements IModel {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////// PRIVATE FIELDS /////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static boolean MAXIMIZE = true;
    public static boolean MINIMIZE = false;

    /**
     * Name of internal hook dedicated to store declared {@link org.chocosolver.solver.variables.Task}.
     */
    public static final String TASK_SET_HOOK_NAME = "H_TASKSET";
    public static final String MINISAT_HOOK_NAME = "H_MINISAT";
    public static final String IBEX_HOOK_NAME = "H_IBEX";

    /**
     * Settings to use with this solver
     */
    private final Settings settings;

    /**
     * A map to cache constants (considered as fixed variables)
     */
    private final TIntObjectHashMap<IntVar> cachedConstants;

    /**
     * Variables of the model
     */
    private Variable[] vars;

    /**
     * Index of the last added variable
     */
    private int vIdx;

    /**
     * Store the number of declared {@link IntVar}, including {@link BoolVar}.
     */
    private int nbIntVar;
    /**
     * Store the number of declared {@link BoolVar}.
     */
    private int nbBoolVar;
    /**
     * Store the number of declared {@link SetVar}.
     */
    private int nbSetVar;
    /**
     * Store the number of declared {@link RealVar}.
     */
    private int nbRealVar;
    /**
     * Constraints of the model
     */
    private Constraint[] cstrs;

    /**
     * Index of the last added constraint
     */
    private int cIdx;

    /**
     * Groups of variables
     */
    private List<Group<?>> groups;

    /**
     * Environment, based of the search tree (trailing or copying)
     */
    private final IEnvironment environment;

    /**
     * Resolver of the model, controls propagation and search
     */
    private final Solver solver;

    /**
     * Variable to optimize, possibly null.
     */
    private Variable objective;

    /**
     * Precision to consider when optimizing a RealVariable
     */
    private double precision = 0.0001D;

    /**
     * Model name
     */
    private String name;

    /**
     * Stores this model's creation time -- in nanoseconds
     */
    private final long creationTime;

    /**
     * Counter used to set ids to variables and propagators
     */
    private int id = 1;

    /**
     * Counter used to name variables created internally
     */
    private int nameId = 1;

    /**
     * Enable attaching hooks to a model.
     */
    private final Map<String, Object> hooks;

    /**
     * Resolution policy (sat/min/max)
     */
    private ResolutionPolicy policy = ResolutionPolicy.SATISFACTION;

    /**
     * A seed for randomness
     */
    private long seed = 0L;

    private ModelAnalyser modelAnalyser = null;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////// CONSTRUCTORS ///////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Creates a Model object to formulate a decision problem by declaring variables and posting constraints.
     * The model is named <code>name</code> and is set up with paramaters defined in <code>settings</code>.
     *
     * @param name        The name of the model (for logging purpose)
     * @param settings    settings to use
     */
    public Model(String name, Settings settings) {
        this.name = name;
        this.vars = new Variable[32];
        this.vIdx = 0;
        this.cstrs = new Constraint[32];
        this.cIdx = 0;
        this.environment = settings.getEnvironmentSupplier().get();
        this.creationTime = System.nanoTime();
        this.cachedConstants = new TIntObjectHashMap<>(16, 1.5f, Integer.MAX_VALUE);
        this.objective = null;
        this.hooks = new HashMap<>();
        this.settings = settings;
        this.solver = settings.initSolver(this);
        // to make sure MiniSat.C_Undef is not null, call it once
        this.hooks.put("C_Undef", MiniSat.C_Undef);
        this.hooks.clear();
        this.groups = new ArrayList<>();
    }

    /**
     * Creates a Model object to formulate a decision problem by declaring variables and posting constraints.
     * The model is named <code>name</code> and uses the default (trailing) backtracking environment.
     *
     * @param name The name of the model (for logging purpose)
     * @see Model#Model(String, Settings)
     */
    public Model(String name) {
        this(name, Settings.init());
    }

    /**
     * Creates a Model object to formulate a decision problem by declaring variables and posting constraints.
     * The model is uses the default (trailing) backtracking environment.
     *
     * @param settings settings to use
     * @see Model#Model(String, Settings)
     */
    public Model(Settings settings) {
        this("Model-" + nextModelNum(), settings);
    }

    /**
     * Creates a Model object to formulate a decision problem by declaring variables and posting constraints.
     * The model uses the default (trailing) backtracking environment.
     *
     * @see Model#Model(String)
     */
    public Model() {
        this("Model-" + nextModelNum());
    }

    /**
     * For autonumbering anonymous models.
     */
    private static int modelInitNumber;

    /**
     * @return next model's number, for anonymous models.
     */
    private static synchronized int nextModelNum() {
        return modelInitNumber++;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////// GETTERS ////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Get the creation time (in milliseconds) of the model (to estimate modeling duration)
     *
     * @return the time (in ms) of the creation of the model
     */
    public long getCreationTime() {
        return creationTime;
    }

    /**
     * Get the resolution policy of the model
     *
     * @return the resolution policy of the model
     * @see ResolutionPolicy
     */
    public ResolutionPolicy getResolutionPolicy() {
        return policy;
    }

    /**
     * Get the map of constant IntVar the have default names to avoid creating multiple identical constants.
     * Should not be called by the user.
     *
     * @return the map of constant IntVar having default names.
     */
    public TIntObjectHashMap<IntVar> getCachedConstants() {
        return cachedConstants;
    }

    /**
     * Returns the unique and internal propagation and search object to solve this model.
     *
     * @return the unique and internal <code>Resolver</code> object.
     */
    public Solver getSolver() {
        return solver;
    }

    /**
     * Returns the array of <code>Variable</code> objects declared in this <code>Model</code>.
     *
     * @return array of all variables in this model
     */
    public Variable[] getVars() {
        return Arrays.copyOf(vars, vIdx);
    }

    /**
     * Returns a sequential {@code Stream} with this model's variables as its source.
     *
     * @return a sequential {@code Stream} over this model's variables
     */
    public Stream<Variable> streamVars() {
        Spliterator<Variable> it =
                Spliterators.spliterator(vars, 0, vIdx,
                        Spliterator.DISTINCT | Spliterator.NONNULL
                                | Spliterator.CONCURRENT | Spliterator.SIZED);
        return StreamSupport.stream(it, true);
    }

    /**
     * Returns the number of variables involved in <code>this</code>.
     *
     * @return number of variables in this model
     */
    public int getNbVars() {
        return vIdx;
    }

    /**
     * Returns the i<sup>th</sup> variable within the array of variables defined in <code>this</code>.
     *
     * @param i index of the variable to return.
     * @return the i<sup>th</sup> variable of this model
     */
    public Variable getVar(int i) {
        return vars[i];
    }

    /**
     * Returns the number of {@link IntVar} of the model involved in <code>this</code>,
     * <b>excluding</b> {@link BoolVar} if <i>includeBoolVar</i>=<i>false</i>.
     * It also counts FIXED variables and VIEWS, if any.
     *
     * @param includeBoolVar indicates whether or not to include {@link BoolVar}
     * @return the number of {@link IntVar} of the model involved in <code>this</code>
     */
    public int getNbIntVar(boolean includeBoolVar) {
        return nbIntVar + (includeBoolVar ? nbBoolVar : 0);
    }

    /**
     * Iterate over the variable of <code>this</code> and build an array that contains all the {@link IntVar} of the model.
     * <b>excludes</b> {@link BoolVar} if includeBoolVar=false.
     * It also contains FIXED variables and VIEWS, if any.
     *
     * @param includeBoolVar indicates whether or not to include {@link BoolVar}
     * @return array of {@link IntVar} in <code>this</code> model
     */
    public IntVar[] retrieveIntVars(boolean includeBoolVar) {
        int size = getNbIntVar(includeBoolVar);
        IntVar[] ivars = new IntVar[size];
        int k = 0;
        for (int i = 0; i < vIdx; i++) {
            int kind = (vars[i].getTypeAndKind() & Variable.KIND);
            if (kind == Variable.INT || (includeBoolVar && kind == Variable.BOOL)) {
                ivars[k++] = (IntVar) vars[i];
            }
        }
        assert k == size;
        return ivars;
    }

    /**
     * Returns the number of {@link BoolVar} of the model involved in <code>this</code>,
     * It also counts FIXED variables and VIEWS, if any.
     *
     * @return the number of {@link BoolVar} of the model involved in <code>this</code>
     */
    public int getNbBoolVar() {
        return nbBoolVar;
    }

    /**
     * Iterate over the variable of <code>this</code> and build an array that contains the {@link BoolVar} only.
     * It also contains FIXED variables and VIEWS, if any.
     *
     * @return array of {@link BoolVar} in <code>this</code> model
     */
    public BoolVar[] retrieveBoolVars() {
        int size = getNbBoolVar();
        BoolVar[] bvars = new BoolVar[size];
        int k = 0;
        for (int i = 0; i < vIdx; i++) {
            if ((vars[i].getTypeAndKind() & Variable.KIND) == Variable.BOOL) {
                bvars[k++] = (BoolVar) vars[i];
            }
        }
        assert k == size;
        return bvars;
    }

    /**
     * Returns the number of {@link SetVar} of the model involved in <code>this</code>,
     * It also counts FIXED variables and VIEWS, if any.
     *
     * @return the number of {@link SetVar} of the model involved in <code>this</code>
     */
    public int getNbSetVar() {
        return nbSetVar;
    }

    /**
     * Iterate over the variable of <code>this</code> and build an array that contains the {@link SetVar} only.
     * It also contains FIXED variables and VIEWS, if any.
     *
     * @return array of SetVars in <code>this</code> model
     */
    public SetVar[] retrieveSetVars() {
        int size = getNbSetVar();
        SetVar[] svars = new SetVar[size];
        int k = 0;
        for (int i = 0; i < vIdx; i++) {
            if ((vars[i].getTypeAndKind() & Variable.KIND) == Variable.SET) {
                svars[k++] = (SetVar) vars[i];
            }
        }
        assert k == size;
        return svars;
    }

    /**
     * Returns the number of {@link RealVar} of the model involved in <code>this</code>,
     * It also counts FIXED variables and VIEWS, if any.
     *
     * @return the number of {@link RealVar} of the model involved in <code>this</code>
     */
    public int getNbRealVar() {
        return nbRealVar;
    }

    /**
     * Iterate over the variable of <code>this</code> and build an array that contains the {@link RealVar} only.
     * It also contains FIXED variables and VIEWS, if any.
     *
     * @return array of {@link RealVar} in <code>this</code> model
     */
    public RealVar[] retrieveRealVars() {
        int size = getNbRealVar();
        RealVar[] rvars = new RealVar[size];
        int k = 0;
        for (int i = 0; i < vIdx; i++) {
            if ((vars[i].getTypeAndKind() & Variable.KIND) == Variable.REAL) {
                rvars[k++] = (RealVar) vars[i];
            }
        }
        assert k == size;
        return rvars;
    }

    /**
     * Returns the array of <code>Constraint</code> objects posted in this <code>Model</code>.
     *
     * @return array of posted constraints
     */
    public Constraint[] getCstrs() {
        return Arrays.copyOf(cstrs, cIdx);
    }

    /**
     * Return the number of constraints posted in <code>this</code>.
     *
     * @return number of posted constraints.
     */
    public int getNbCstrs() {
        return cIdx;
    }

    /**
     * Returns a sequential {@code Stream} with this model's constraints as its source.
     *
     * @return a sequential {@code Stream} over this model's constraints
     */
    public Stream<Constraint> streamCstrs() {
        Spliterator<Constraint> it =
                Spliterators.spliterator(cstrs, 0, cIdx,
                        Spliterator.DISTINCT | Spliterator.NONNULL
                                | Spliterator.CONCURRENT | Spliterator.SIZED);
        return StreamSupport.stream(it, true);
    }

    /**
     * Return the name of <code>this</code> model.
     *
     * @return this model's name
     */
    public String getName() {
        return name;
    }

    /**
     * Return the backtracking environment of <code>this</code> model.
     *
     * @return the backtracking environment of this model
     */
    public IEnvironment getEnvironment() {
        return environment;
    }

    /**
     * Return the (possibly null) objective variable
     *
     * @return a variable (null for satisfaction problems)
     */
    public Variable getObjective() {
        return objective;
    }

    /**
     * In case of real variable(s) to optimize, a precision is required.
     *
     * @return the precision used
     */
    public double getPrecision() {
        return precision;
    }

    /**
     * Returns the object associated with the named <code>hookName</code>
     *
     * @param hookName the name of the hook to return
     * @return the object associated to the name <code>hookName</code>
     */
    public Object getHook(String hookName) {
        return hooks.get(hookName);
    }

    /**
     * Returns the object associated with the named <code>hookName</code>
     *
     * @param hookName     the name of the hook to return
     * @param defaultValue the default mapping of the key
     * @return the object associated to the name <code>hookName</code>,
     * or defaultValue if this map contains no mapping for the key
     */
    public Object getHookOrDefault(String hookName, Object defaultValue) {
        return hooks.getOrDefault(hookName, defaultValue);
    }

    /**
     * Returns the map containing declared hooks.
     * This map is mutable.
     *
     * @return the map of hooks.
     */
    protected Map<String, Object> getHooks() {
        return hooks;
    }

    /**
     * Add a group of variables to the model.
     * A group is a set of variables that are related to each other or have a specific semantic.
     * A name should be associated to a group, to help identifying it.
     *
     * @param g   a group of variables
     * @param <V> the type of variables in the group
     */
    public <V extends Variable> void addGroup(Group<V> g) {
        groups.add(g);
    }

    /**
     * Add a group of variables to the model.
     * A group is a set of variables that are related to each other or have a specific semantic.
     * A name should be associated to a group, to help identifying it.
     *
     * @param name the name of the group
     * @param vars the variables in the group
     * @param <V>  the type of variables in the group
     */
    @SafeVarargs
    public final <V extends Variable> void addAsGroup(String name, V... vars) {
        addGroup(new Group<>(name, vars));
    }

    /**
     * Get the groups of variables in the model.
     * A group is a set of variables that are related to each other or have a specific semantic.
     *
     * @return an unmodifiable list that contains all the groups of variables in the model.
     * If no group is declared, an empty list is returned.
     */
    public List<Group<?>> getGroups() {
        return Collections.unmodifiableList(groups);
    }

    /**
     * The basic "true" constraint, which is always satisfied
     *
     * @return a "true" constraint
     */
    public BooleanConstraint trueConstraint() {
        return new BooleanConstraint(this, true);
    }

    /**
     * The basic "false" constraint, which is always violated
     *
     * @return a "false" constraint
     */
    public BooleanConstraint falseConstraint() {
        return new BooleanConstraint(this, false);
    }

    /**
     * Create a VOID constraint that cannot be reified --  for LCG mainly
     * @return a void constraint
     * todo: find a more elegant way to do that
     */
    public Constraint voidConstraint() {
        return new Constraint("void") {
            @Override
            public void reifyWith(BoolVar bool) {
                throw new SolverException("Cannot reify a void constraint");
            }
        };
    }


    /**
     * Returns the unique constraint embedding a minisat model.
     * A call to this method will create and post the constraint if it does not exist already.
     *
     * @return the minisat constraint
     */
    public SatConstraint getMinisat() {
        if(solver.isLCG()){
            throw new UnsupportedOperationException("MiniSat is not supported with LCG");
        }
        if (getHook(MINISAT_HOOK_NAME) == null) {
            SatConstraint minisat = new SatConstraint(this);
            minisat.post();
            addHook(MINISAT_HOOK_NAME, minisat);
        }
        return (SatConstraint) getHook(MINISAT_HOOK_NAME);
    }

    /**
     * Unpost minisat constraint from model, if any.
     */
    public void removeMinisat() {
        if (getHook(MINISAT_HOOK_NAME) != null) {
            SatConstraint minisat = (SatConstraint) getHook(MINISAT_HOOK_NAME);
            unpost(minisat);
            removeHook(MINISAT_HOOK_NAME);
        }
    }


    /**
     * Return a constraint embedding an instance of Ibex (continuous solver).
     * A call to this method will create and post the constraint if it does not exist already.
     *
     * @return the Ibex constraint
     */
    public IbexHandler getIbexHandler() {
        if (getHook(IBEX_HOOK_NAME) == null) {
            IbexHandler ibexHnadler = new IbexHandler();
            ibexHnadler.setPreserveRounding(settings.getIbexRestoreRounding());
            addHook(IBEX_HOOK_NAME, ibexHnadler);
        }
        return (IbexHandler) getHook(IBEX_HOOK_NAME);
    }

    /**
     * Return the current settings for the solver
     *
     * @return a {@link Settings}
     */
    public Settings getSettings() {
        return this.settings;
    }

    /**
     * Return an analyser for the Model
     *
     * @return a {@link ModelAnalyser}
     */
    public ModelAnalyser getModelAnalyser() {
        if (this.modelAnalyser == null) {
            this.modelAnalyser = new ModelAnalyser(this);
        }
        return this.modelAnalyser;
    }

    /**
     * Returns an estimation of the current memory footprint of this.
     * If an error occurs during the estimation (related to SizeOf), -1 is returned.
     *
     * @return the total size in bytes for this model
     * @implNote this is based on : <a href="https://github.com/ehcache/sizeof">SizeOf</a>
     */
    public long getEstimatedMemory() {
        long size = -1;
        try {
            SizeOf sizeOf = SizeOf.newInstance(new SizeOfFilter() {
                        @Override
                        public Collection<Field> filterFields(Class<?> klazz, Collection<Field> fields) {
                            return fields;
                        }

                        @Override
                        public boolean filterClass(Class<?> klazz) {
                            return !klazz.getName().contains("Lambda");
                        }
                    });
            size = sizeOf.deepSizeOf(this);
        } catch (UnsupportedOperationException ignored) {
            // do nothing
        }
        return size;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////// SETTERS ////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Defines the variable to optimize (maximize or minimize)
     * By default, each solution forces either :
     * <ul>
     * <li> for {@link Model#MAXIMIZE}: to increase by one {@link IntVar} (or {@link #precision} for {@link RealVar}) the objective lower bound, or</li>
     * <li> for {@link Model#MINIMIZE}:  to decrease by one {@link IntVar} (or {@link #precision} for {@link RealVar}) the objective upper bound.</li>
     * </ul>
     *
     * @param maximize  whether to maximize (true) or minimize (false) the objective
     * @param objective variable to optimize
     * @see IObjectiveManager#setStrictDynamicCut()
     * @see IObjectiveManager#setWalkingDynamicCut()
     * @see IObjectiveManager#setCutComputer(Function)
     */
    public void setObjective(boolean maximize, Variable objective) {
        if (objective == null) {
            throw new SolverException("Cannot set objective to null");
        } else {
            this.policy = maximize ? ResolutionPolicy.MAXIMIZE : ResolutionPolicy.MINIMIZE;
            this.objective = objective;
            if ((objective.getTypeAndKind() & Variable.KIND) == Variable.REAL) {
                getSolver().setObjectiveManager(
                        ObjectiveFactory.makeObjectiveManager((RealVar) objective, policy, precision)
                );
            } else {
                getSolver().setObjectiveManager(
                        ObjectiveFactory.makeObjectiveManager((IntVar) objective, policy)
                );
            }
        }
    }

    /**
     * Removes any objective and set problem to a satisfaction problem
     */
    public void clearObjective() {
        this.objective = null;
        this.policy = ResolutionPolicy.SATISFACTION;
        getSolver().setObjectiveManager(ObjectiveFactory.SAT());
    }

    /**
     * In case of real variable to optimize, a precision is required.
     *
     * @param p the precision (default is 0.0001D)
     */
    public void setPrecision(double p) {
        this.precision = p;
    }

    /**
     * Sets the seed used for random number generator using a single
     * {@code long} seed.
     *
     * @param seed the initial seed
     * @see java.util.Random#setSeed(long)
     */
    public void setSeed(long seed) {
        this.seed = seed;
    }

    /**
     * Gets the seed used random number generator.
     *
     * @return the seed
     */
    public long getSeed() {
        return this.seed;
    }

    /**
     * Adds the <code>hookObject</code> to store in this model, associated with the name <code>hookName</code>.
     * A hook is a simple map "hookName" <-> hookObject.
     *
     * @param hookName   name of the hook
     * @param hookObject hook to store
     */
    public void addHook(String hookName, Object hookObject) {
        this.hooks.put(hookName, hookObject);
    }

    /**
     * Removes the hook named <code>hookName</code>
     *
     * @param hookName name of the hookObject to remove
     */
    public void removeHook(String hookName) {
        this.hooks.remove(hookName);
    }

    /**
     * Empties the hooks attached to this model.
     */
    public void removeAllHooks() {
        this.hooks.clear();
    }

    /**
     * Changes the name of this model to be equal to the argument <code>name</code>.
     *
     * @param name the new name of this model.
     */
    public void setName(String name) {
        this.name = name;
        this.getSolver().getMeasures().setModelName(name);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////         RELATED TO VAR              ////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Link a variable to <code>this</code>. This is executed AUTOMATICALLY in variable constructor,
     * so no checked are done on multiple occurrences of the very same variable.
     * Should not be called by the user.
     *
     * @param variable a newly created variable, not already added
     */
    public void associates(Variable variable) {
        if (vIdx == vars.length) {
            vars = Arrays.copyOf(vars, ArrayUtils.newBoundedSize(vars.length, vars.length * 2));
        }
        vars[vIdx++] = variable;
        switch ((variable.getTypeAndKind() & Variable.KIND)) {
            case Variable.INT:
                nbIntVar++;
                break;
            case Variable.BOOL:
                nbBoolVar++;
                break;
            case Variable.SET:
                nbSetVar++;
                break;
            case Variable.REAL:
                nbRealVar++;
                break;
        }
    }

    /**
     * Unlink the variable from <code>this</code>.
     * Should not be called by the user.
     *
     * @param variable variable to un-associate
     */
    public void unassociates(Variable variable) {
        if (variable.getNbProps() > 0) {
            throw new SolverException("Try to remove a variable (" + variable.getName() + ")which is still involved in at least one constraint");
        }
        // to check if the variable is in the model (debug purpose only
        int idx = Arrays.binarySearch(vars, 0, vIdx, variable, Comparator.comparingInt(Identity::getId));
        System.arraycopy(vars, idx + 1, vars, idx + 1 - 1, vIdx - (idx + 1));
        vars[--vIdx] = null;
        switch ((variable.getTypeAndKind() & Variable.KIND)) {
            case Variable.INT:
                nbIntVar--;
                break;
            case Variable.BOOL:
                nbBoolVar--;
                break;
            case Variable.SET:
                nbSetVar--;
                break;
            case Variable.REAL:
                nbRealVar--;
                break;
        }
    }

    /**
     * Get a free single-use id to identify a new variable.
     * Should not be called by the user.
     *
     * @return a free id to use
     */
    public int nextId() {
        return id++;
    }

    /**
     * Get a free single-use name id to identify a variable created internally.
     * Should not be called by the user.
     *
     * @return a free id to use
     */
    public int nextNameId() {
        return nameId++;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////     RELATED TO CSTR DECLARATION     ////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Posts constraints <code>cs</code> permanently in the constraints network of <code>this</code>:
     * - add them to the data structure,
     * - set the fixed idx,
     * - checks for restrictions
     *
     * @param cs Constraints
     * @throws SolverException if the constraint is posted twice, posted although reified or reified twice.
     */
    public void post(Constraint... cs) throws SolverException {
        if (cs != null) {
            _post(true, cs);
        }
    }

    /**
     * Add constraints to the model.
     *
     * @param permanent specify whether the constraints are added permanently (if set to true) or temporary (ie, should be removed on backtrack)
     * @param cs        list of constraints
     * @throws SolverException if a constraint is posted twice, posted although reified or reified twice.
     */
    private void _post(boolean permanent, Constraint... cs) throws SolverException {
        PropagationEngine engine = getSolver().getEngine();
        // check if the resolution already started -> if true, dynamic addition
        boolean dynAdd = engine.isInitialized();
        // then prepare storage of the constraints
        if (cIdx + cs.length >= cstrs.length) {
            int nsize = cstrs.length;
            while (cIdx + cs.length >= nsize) {
                nsize *= 3 / 2 + 1;
            }
            cstrs = Arrays.copyOf(cstrs, nsize);
        }
        // specific behavior for dynamic addition and/or reified constraints
        for (Constraint c : cs) {
            for (Propagator<?> p : c.getPropagators()) {
                if (p.isPassive()) {
                    throw new SolverException("Try to add a constraint with a passive propagator");
                }
                p.getConstraint().checkNewStatus(Constraint.Status.POSTED);
                p.linkVariables();
            }
            if (dynAdd) {
                engine.dynamicAddition(permanent, c.getPropagators());
            }
            c.declareAs(Constraint.Status.POSTED, cIdx);
            cstrs[cIdx++] = c;
        }
    }

    /**
     * Posts constraints <code>cs</code> temporary, that is, they will be unposted upon backtrack.
     * <p>
     * The unpost instruction is defined by an {@link org.chocosolver.memory.structure.IOperation}
     * saved in the {@link IEnvironment}
     *
     * @param cs a set of constraints to add
     * @throws ContradictionException if the addition of constraints <code>cs</code> detects inconsistency.
     * @throws SolverException        if a constraint is posted twice, posted although reified or reified twice.
     */
    public void postTemp(Constraint... cs) throws ContradictionException {
        if (cs != null) {
            for (Constraint c : cs) {
                this.getEnvironment().save(() -> unpost(c));
                _post(false, c);
                if (!getSolver().getEngine().isInitialized()) {
                    throw new SolverException("Try to post a temporary constraint while the resolution has not begun.\n" +
                            "A call to Model.post(Constraint) is more appropriate.");
                } else {
                    for (Propagator<?> p : c.getPropagators()) {
                        getSolver().getEngine().execute(p);
                    }
                }
            }
        }
    }

    /**
     * Remove permanently the constraint <code>c</code> from the constraint network.
     *
     * @param constraints the constraints to remove
     * @throws SolverException if a constraint is unknown from the model
     */
    public void unpost(Constraint... constraints) throws SolverException {
        if (constraints != null) {
            for (Constraint c : constraints) {
                // 1. look for the constraint c
                if (c.getStatus() != Constraint.Status.POSTED) {
                    throw new SolverException("The constraint " + c + " was not posted to the model and cannot be unposted");
                }
                int idx = c.getCidxInModel();
                c.declareAs(Constraint.Status.FREE, -1);
                c.ignore();
                // 2. remove it from the network
                Constraint cm = cstrs[--cIdx];
                if (idx < cIdx) {
                    cstrs[idx] = cm;
                    cstrs[idx].declareAs(Constraint.Status.FREE, -1); // needed, to avoid throwing an exception
                    cstrs[idx].declareAs(Constraint.Status.POSTED, idx);
                }
                cstrs[cIdx] = null;
                // 3. check if the resolution already started -> if true, dynamic deletion
                PropagationEngine engine = getSolver().getEngine();
                if (engine.isInitialized()) {
                    engine.dynamicDeletion(c.getPropagators());
                }
                // 4. remove the propagators of the constraint from its variables
                for (Propagator<?> prop : c.getPropagators()) {
                    prop.unlinkVariables();
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////// RELATED TO I/O ////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Return a string describing the CSP defined in <code>this</code> model.
     */
    @Override
    public String toString() {
        StringBuilder st = new StringBuilder(256);
        st.append(String.format("\n Model[%s]\n", name));
        st.append(String.format("\n[ %d vars -- %d cstrs -- %d lits -- %d clauses ]\n",
                vIdx, cIdx,
                getSolver().getSat() == null ? 0 : getSolver().getSat().nVars(),
                getSolver().getSat() == null ? 0 : getSolver().getSat().nClauses()));
        st.append(policy.name().toLowerCase()).append(" ");
        if (objective != null) {
            st.append(objective.getName()).append(" ");
        }
        st.append(" : ").append(getSolver().isFeasible().name().toLowerCase()).append("\n");
        st.append("== variables ==\n");
        for (int v = 0; v < vIdx; v++) {
            st.append(vars[v].toString()).append('\n');
        }
        st.append("== constraints ==\n");
        for (int c = 0; c < cIdx; c++) {
            st.append(cstrs[c].toString()).append('\n');
        }
        return st.toString();
    }

    /**
     * Display for each variable involved in the model, its number of occurrences.
     */
    public void displayVariableOccurrences() {
        Map<String, Integer> l = new HashMap<>();
        int cnt = 0;
        for (Variable v : this.getVars()) {
            cnt++;
            if (v.isAConstant()) {
                l.compute("constants", (n, k) -> k == null ? 1 : k + 1);
            } else {
                if (VariableUtils.isView(v)) {
                    l.compute("views", (n, k) -> k == null ? 1 : k + 1);
                } else if (VariableUtils.isInt(v)) {
                    IntVar iv = v.asIntVar();
                    l.compute(String.format("[%d,%d]", iv.getLB(), iv.getUB()), (n, k) -> k == null ? 1 : k + 1);
                } else {
                    l.compute(v.getClass().getSimpleName(), (n, k) -> k == null ? 1 : k + 1);
                }
            }
        }
        if (getSolver().getSat() != null && getSolver().getSat().nClauses() > 0) {
            l.put("lits", getSolver().getSat().nVars());
        }
        solver.log().bold().printf("== %d variables ==%n", cnt);
        l.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(e ->
                        solver.log().printf("\t%s #%d\n", e.getKey(), e.getValue())
                );
    }

    /**
     * Display for each propagator involved in the model, its number of occurrences.
     */
    public void displayPropagatorOccurrences() {
        Map<String, Integer> l = new HashMap<>();
        int cnt = 0;
        for (Constraint c : this.getCstrs()) {
            for (Propagator<?> p : c.getPropagators()) {
                l.compute(p.getClass().getSimpleName(), (n, k) -> k == null ? 1 : k + 1);
                cnt++;
            }
        }
        if (getSolver().getSat() != null && getSolver().getSat().nClauses() > 0) {
            l.put("clauses", getSolver().getSat().nClauses());
        }
        solver.log().bold().printf("== %d propagators ==%n", cnt);
        l.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(e ->
                        solver.log().printf("\t%s #%d\n", e.getKey(), e.getValue())
                );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////// RELATED TO MODELING FACTORIES /////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public Model ref() {
        return this;
    }
}
