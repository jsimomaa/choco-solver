/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.constraints.ternary;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.*;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.setDataStructures.iterable.IntIterableBitSet;

/**
 * X % Y = Z
 * A propagator for the constraint Z = X % Y where X, Y and Z are integer, possibly negative, variables
 * The filtering algorithm both supports bounded and enumerated integer variables
 *
 * @author Arthur Godet
 * @since 29/03/2019
 */
public class PropModXYZ extends Propagator<IntVar> {
    private final IntVar x;
    private final IntVar y;
    private final IntVar z;
    private IntIterableBitSet usedValues;
    private final boolean allEnnums;
    private static final int THRESHOLD = 10_000;

    public PropModXYZ(IntVar x, IntVar y, IntVar z) {
        super(new IntVar[]{x, y, z}, PropagatorPriority.TERNARY, false);
        this.x = x;
        this.y = y;
        this.z = z;
        this.allEnnums = x.hasEnumeratedDomain() && y.hasEnumeratedDomain() && z.hasEnumeratedDomain();
        if (z.hasEnumeratedDomain()) {
            usedValues = new IntIterableBitSet();
            usedValues.setOffset(z.getLB());
        }
    }

    @Override
    public int getPropagationConditions(int vIdx) {
        return IntEventType.boundAndInst();
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        y.removeValue(0, this);
        int zlb, zub;
        int xlb = x.getLB();
        int xub = x.getUB();
        int ylb = y.getLB();
        int yub = y.getUB();
        if (xlb >= 0) {
            zlb = 0;
            zub = Math.min(xub, Math.max(Math.abs(y.getLB()), Math.abs(y.getUB())) - 1);
        } else if (xub < 0) {
            zub = 0;
            zlb = Math.max(xlb, -Math.max(Math.abs(y.getLB()), Math.abs(y.getUB())) + 1);
        } else {
            zlb = Math.max(xlb, Math.min(ylb, Math.min(-ylb, -yub)) + 1);
            zub = Math.min(xub, Math.max(yub, Math.max(-ylb, -yub)) - 1);

        }
        z.updateBounds(zlb, zub, this);

        long card = (long) x.getDomainSize() * y.getDomainSize();
        if (allEnnums && card <= THRESHOLD) {
            propagateEnumerated();
        } else {
            propagateBounded();
        }
    }

    private void propagateEnumerated() throws ContradictionException {
        usedValues.clear();
        for (int vx = x.getLB(); vx <= x.getUB(); vx = x.nextValue(vx)) {
            boolean toRemove = true;
            for (int vy = y.getLB(); vy <= y.getUB(); vy = y.nextValue(vy)) {
                if (vy != 0 && z.contains(vx % vy)) {
                    usedValues.add(vx % vy);
                    toRemove = false;
                }
            }
            if (toRemove) {
                x.removeValue(vx, this);
            }
        }
        z.removeAllValuesBut(usedValues, this);
        for (int vy = y.getLB(); vy <= y.getUB(); vy = y.nextValue(vy)) {
            if (!containsOneDivid(x, vy, z)) {
                y.removeValue(vy, this);
            }
        }
    }

    private void propagateBounded() throws ContradictionException {
        boolean hasChange = true;
        while (hasChange) {
            hasChange = false;
            // filter bounds for X
            while (!containsOneDivid(x.getLB(), y, z)) {
                hasChange |= x.updateLowerBound(x.getLB() + 1, this);
            }
            while (!containsOneDivid(x.getUB(), y, z)) {
                hasChange |= x.updateUpperBound(x.getUB() - 1, this);
            }
            // filter bounds for Z
            while (!containsOneDividLB(x, y, z.getLB())) {
                hasChange |= z.updateLowerBound(z.getLB() + 1, this);
            }
            while (!containsOneDividUB(x, y, z.getUB())) {
                hasChange |= z.updateUpperBound(z.getUB() - 1, this);
            }
            // filter bounds for Y
            while (y.getLB() == 0 || !containsOneDivid(x, y.getLB(), z)) {
                hasChange |= y.updateLowerBound(y.getLB() + 1, this);
            }
            while (y.getUB() == 0 || !containsOneDivid(x, y.getUB(), z)) {
                hasChange |= y.updateUpperBound(y.getUB() - 1, this);
            }
        }
    }

    private static boolean containsOneDivid(int v, IntVar Y, IntVar Z) {
        for (int vy = Y.getLB(); vy <= Y.getUB(); vy = Y.nextValue(vy)) {
            if (vy != 0) {
                for (int vz = Z.getLB(); vz <= Z.getUB(); vz = Z.nextValue(vz)) {
                    if (v % vy == vz) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean containsOneDivid(IntVar X, int v, IntVar Z) {
        if (v != 0) {
            for (int vx = X.getLB(); vx <= X.getUB(); vx = X.nextValue(vx)) {
                for (int vz = Z.getLB(); vz <= Z.getUB(); vz = Z.nextValue(vz)) {
                    if (vx % v == vz) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean containsOneDividLB(IntVar X, IntVar Y, int v) {
        for (int vy = Y.getLB(); vy <= Y.getUB(); vy = Y.nextValue(vy)) {
            if (vy == 0) continue;
            for (int vx = X.getLB(); vx <= X.getUB(); vx = X.nextValue(vx)) {
                if (vx % vy == v) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean containsOneDividUB(IntVar X, IntVar Y, int v) {
        for (int vy = Y.getUB(); vy >= Y.getLB(); vy = Y.previousValue(vy)) {
            if (vy == 0) continue;
            for (int vx = X.getUB(); vx >= X.getLB(); vx = X.previousValue(vx)) {
                if (vx % vy == v) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public ESat isEntailed() {
        if (x.isInstantiated() && y.isInstantiated() && z.isInstantiated()) {
            return y.getValue() != 0 && x.getValue() % y.getValue() == z.getValue() ? ESat.TRUE : ESat.FALSE;
        }
        return ESat.UNDEFINED;
    }

    @Override
    public String toString() {
        return x.getName() + " % " + y.getName() + " = " + z.getName();
    }

}
