/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.variables.impl;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.solver.variables.impl.scheduler.BoolEvtScheduler;
import org.chocosolver.util.ESat;
import org.chocosolver.util.iterators.EvtScheduler;

/**
 * A constant view specific to boolean variable
 * <p/>
 * Based on "Views and Iterators for Generic Constraint Implementations",
 * C. Schulte and G. Tack
 *
 * @author Charles Prud'homme
 * @since 04/02/11
 */
public class FixedBoolVarImpl extends FixedIntVarImpl implements BoolVar {

    private BoolVar not;

    public FixedBoolVarImpl(String name, int constant, Model model) {
        super(name, constant, model);
        assert constant == 0 || constant == 1 : "FixedBoolVarImpl value should be taken in {0,1}";
    }

    @Override
    public int getTypeAndKind() {
        return Variable.BOOL | Variable.CSTE;
    }

    @Override
    protected EvtScheduler createScheduler() {
        return new BoolEvtScheduler();
    }

    @Override
    public ESat getBooleanValue() {
        return ESat.eval(constante == 1);
    }

    @Override
    public BoolVar not() {
        if (!hasNot()) {
            not = model.boolNotView(this);
            not._setNot(this);
        }
        return not;
    }

    @Override
    public void _setNot(BoolVar not) {
        this.not = not;
    }

    @Override
    public boolean isLit() {
        return true;
    }

    @Override
    public boolean hasNot() {
        return not != null;
    }

    @Override
    public boolean isNot() {
        return constante == 0;
    }

    @Override
    public void setNot(boolean isNot) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return name + " = " + constante;
    }

}
