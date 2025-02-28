/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.constraints.reification;

import org.chocosolver.sat.Reason;
import org.chocosolver.solver.constraints.*;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.util.ESat;

/**
 * Implication propagator
 * <p>
 * <br/>
 *
 * @author Jean-Guillaume Fages
 * @since 02/2013
 */
@Explained
public class PropImplied extends Propagator<Variable> {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    // boolean variable of the reification
    private final BoolVar bVar;
    // constraint to apply if bVar = true
    private final Constraint trueCons;
    // constraint in charge of the reification process (constraint of this propagator)
    private ImpliedConstraint reifCons;

    //***********************************************************************************
    // CONSTRUCTION
    //***********************************************************************************

    public PropImplied(Variable[] allVars, Constraint consIfBoolTrue) {
        super(allVars, computePriority(consIfBoolTrue), false);
        this.bVar = (BoolVar) vars[0];
        this.trueCons = consIfBoolTrue;
    }

    public void setReifCons(ImpliedConstraint reifCons) {
        assert this.reifCons == null : "cannot change the ReificationConstraint of a PropReif";
        this.reifCons = reifCons;
    }

    private static PropagatorPriority computePriority(Constraint consIfBoolTrue) {
        int p = consIfBoolTrue.computeMaxPriority().priority;
        return PropagatorPriority.get(Math.max(p, PropagatorPriority.TERNARY.priority));
    }

    //***********************************************************************************
    // METHODS
    //***********************************************************************************

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        if (bVar.isInstantiated()) {
            setPassive();
            if (bVar.getBooleanValue() == ESat.TRUE) {
                reifCons.activate(0);
            }
        } else {
            ESat sat = trueCons.isSatisfied();
            if (sat == ESat.FALSE) {
                bVar.setToFalse(this, lcg() ? Propagator.reason(bVar, vars) : Reason.undef());
                setPassive();
            }
        }
    }

    @Override
    public ESat isEntailed() {
        if (bVar.isInstantiated()) {
            if (bVar.getValue() == 1) {
                return trueCons.isSatisfied();
            } else {
                return ESat.TRUE;
            }
        }
        return ESat.UNDEFINED;
    }

    @Override
    public String toString() {
        return bVar.toString() + "=>" + trueCons.toString();
    }

}
