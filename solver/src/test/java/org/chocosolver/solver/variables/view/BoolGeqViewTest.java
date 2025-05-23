/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.variables.view;

import org.chocosolver.solver.Cause;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Settings;
import org.chocosolver.solver.constraints.extension.TuplesFactory;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.view.bool.BoolGeqView;
import org.chocosolver.util.ESat;
import org.chocosolver.util.iterators.DisposableRangeIterator;
import org.chocosolver.util.iterators.DisposableValueIterator;
import org.chocosolver.util.tools.ArrayUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.stream.Stream;

/**
 * <p>
 * Project: choco-solver.
 *
 * @author Charles Prud'homme
 * @since 26/11/2018.
 */
public class BoolGeqViewTest {

    Model model;
    IntVar x;
    BoolVar b;

    @BeforeMethod(alwaysRun = true)
    public void before() {
        model = new Model();
        x = model.intVar("x", 0, 5);
        b = new BoolGeqView<>(x, 3);
    }

    @Test(groups = "1s")
    public void testMonitorDelta() {
    }

    @Test(groups = "1s", timeOut = 60000, expectedExceptions = ContradictionException.class)
    public void testUpdateInfeasBounds1() throws Exception {
        before();
        b.updateBounds(1, 0, Cause.Null);
    }


    @Test(groups = "1s", timeOut = 60000, expectedExceptions = ContradictionException.class)
    public void testUpdateInfeasBounds2() throws Exception {
        before();
        b.updateBounds(2, 1, Cause.Null);
    }


    @Test(groups = "1s", timeOut = 60000, expectedExceptions = ContradictionException.class)
    public void testUpdateInfeasBounds3() throws Exception {
        before();
        b.updateBounds(0, -1, Cause.Null);
    }

    @Test(groups = "1s", timeOut = 60000, expectedExceptions = ContradictionException.class)
    public void testUpdateInfeasBounds4() throws Exception {
        before();
        b.updateBounds(2, -1, Cause.Null);
    }

    @Test(groups = "1s")
    public void testGetBooleanValueU() {
        Assert.assertEquals(b.getBooleanValue(), ESat.UNDEFINED);
    }

    @Test(groups = "1s")
    public void testGetBooleanValueT() throws ContradictionException {
        Assert.assertTrue(x.updateLowerBound(3, Cause.Null));
        Assert.assertEquals(b.getBooleanValue(), ESat.TRUE);
        Assert.assertEquals(b.getValue(), 1);
    }

    @Test(groups = "1s")
    public void testGetBooleanValueF() throws ContradictionException {
        Assert.assertTrue(x.updateUpperBound(2, Cause.Null));
        Assert.assertEquals(b.getBooleanValue(), ESat.FALSE);
        Assert.assertEquals(b.getValue(), 0);
    }

    @Test(groups = "1s")
    public void testSetToTrue() throws ContradictionException {
        Assert.assertTrue(b.setToTrue(Cause.Null));
        Assert.assertTrue(x.getLB() >= 3);
    }

    @Test(groups = "1s")
    public void testSetToFalse() throws ContradictionException {
        Assert.assertTrue(b.setToFalse(Cause.Null));
        Assert.assertTrue(x.getUB() < 3);
    }

    @Test(groups = "1s")
    public void testDoInstantiateVar0() throws ContradictionException {
        Assert.assertTrue(b.instantiateTo(0, Cause.Null));
        Assert.assertTrue(x.getUB() < 3);
    }

    @Test(groups = "1s")
    public void testDoInstantiateVar1() throws ContradictionException {
        Assert.assertTrue(b.instantiateTo(1, Cause.Null));
        Assert.assertTrue(x.getLB() >= 3);
    }

    @Test(groups = "1s", expectedExceptions = ContradictionException.class)
    public void testDoInstantiateVar2() throws ContradictionException {
        Assert.assertFalse(b.instantiateTo(2, Cause.Null));
    }

    @Test(groups = "1s")
    public void testDoUpdateLowerBoundOfVar11() throws ContradictionException {
        Assert.assertFalse(b.updateLowerBound(-1, Cause.Null));
    }

    @Test(groups = "1s")
    public void testDoUpdateLowerBoundOfVar0() throws ContradictionException {
        Assert.assertFalse(b.updateLowerBound(0, Cause.Null));
        Assert.assertFalse(x.getLB() >= 3);
        Assert.assertTrue(3 <= x.getUB());
    }

    @Test(groups = "1s")
    public void testDoUpdateLowerBoundOfVar1() throws ContradictionException {
        Assert.assertTrue(b.updateLowerBound(1, Cause.Null));
        Assert.assertFalse(x.getLB() > 3);
        Assert.assertTrue(3 <= x.getUB());
    }

    @Test(groups = "1s", expectedExceptions = ContradictionException.class)
    public void testDoUpdateLowerBoundOfVar2() throws ContradictionException {
        Assert.assertFalse(b.updateLowerBound(2, Cause.Null));
    }

    @Test(groups = "1s", expectedExceptions = ContradictionException.class)
    public void testDoUpdateUpperBoundOfVar11() throws ContradictionException {
        Assert.assertFalse(b.updateUpperBound(-1, Cause.Null));
    }

    @Test(groups = "1s")
    public void testDoUpdateUpperBoundOfVar0() throws ContradictionException {
        Assert.assertTrue(b.updateUpperBound(0, Cause.Null));
        Assert.assertTrue(x.getUB() < 3);
        Assert.assertTrue(0 <= x.getLB());
    }

    @Test(groups = "1s")
    public void testDoUpdateUpperBoundOfVar1() throws ContradictionException {
        Assert.assertFalse(b.updateUpperBound(1, Cause.Null));
    }

    @Test(groups = "1s")
    public void testDoUpdateUpperBoundOfVar2() throws ContradictionException {
        Assert.assertFalse(b.updateUpperBound(2, Cause.Null));
    }

    @Test(groups = "1s")
    public void testDoRemoveValueFromVar11() throws ContradictionException {
        Assert.assertFalse(b.removeValue(-1, Cause.Null));
    }

    @Test(groups = "1s")
    public void testDoRemoveValueFromVar0() throws ContradictionException {
        Assert.assertTrue(b.removeValue(0, Cause.Null));
        Assert.assertTrue(x.getLB() >= 3);
        Assert.assertTrue(x.getUB() <= 5);
    }

    @Test(groups = "1s")
    public void testDoRemoveValueFromVar1() throws ContradictionException {
        Assert.assertTrue(b.removeValue(1, Cause.Null));
        Assert.assertTrue(x.getUB() < 3);
        Assert.assertTrue(0 <= x.getLB());
    }

    @Test(groups = "1s")
    public void testDoRemoveValueFromVar2() throws ContradictionException {
        Assert.assertFalse(b.removeValue(2, Cause.Null));
    }

    @Test(groups = "1s", expectedExceptions = ContradictionException.class)
    public void testDoRemoveIntervalFromVar11() throws Exception {
        Assert.assertFalse(b.removeInterval(-1, 2, Cause.Null));
    }

    @Test(groups = "1s", expectedExceptions = ContradictionException.class)
    public void testDoRemoveIntervalFromVar01() throws Exception {
        Assert.assertFalse(b.removeInterval(0, 1, Cause.Null));
    }

    @Test(groups = "1s")
    public void testDoRemoveIntervalFromVar10() throws Exception {
        Assert.assertTrue(b.removeInterval(-1, 0, Cause.Null));
        Assert.assertFalse(x.getLB() > 3);
        Assert.assertTrue(3 <= x.getUB());
    }

    @Test(groups = "1s")
    public void testDoRemoveIntervalFromVar12() throws Exception {
        Assert.assertTrue(b.removeInterval(1, 2, Cause.Null));
        Assert.assertTrue(x.getUB() < 3);
        Assert.assertTrue(0 <= x.getLB());
    }

    @Test(groups = "1s")
    public void testContains11() {
        Assert.assertFalse(b.contains(-1));
    }

    @Test(groups = "1s")
    public void testContains0() throws ContradictionException {
        Assert.assertTrue(b.contains(0));
        x.updateLowerBound(3, Cause.Null);
        Assert.assertFalse(b.contains(0));
    }

    @Test(groups = "1s")
    public void testContains1() throws ContradictionException {
        Assert.assertTrue(b.contains(1));
        x.updateUpperBound(2, Cause.Null);
        Assert.assertFalse(b.contains(1));
    }

    @Test(groups = "1s")
    public void testContains2() {
        Assert.assertFalse(b.contains(2));
    }

    @Test(groups = "1s")
    public void testIsInstantiatedTo0() throws ContradictionException {
        Assert.assertFalse(b.isInstantiatedTo(0));
        x.updateUpperBound(2, Cause.Null);
        Assert.assertTrue(b.isInstantiatedTo(0));
    }

    @Test(groups = "1s")
    public void testIsInstantiatedTo1() throws ContradictionException {
        Assert.assertFalse(b.isInstantiatedTo(1));
        x.updateLowerBound(3, Cause.Null);
        Assert.assertTrue(b.isInstantiatedTo(1));
    }

    @Test(groups = "1s")
    public void testGetLB0() throws Exception {
        Assert.assertEquals(b.getLB(), 0);
        x.updateLowerBound(3, Cause.Null);
        Assert.assertEquals(b.getLB(), 1);
    }

    @Test(groups = "1s")
    public void testGetLB1() throws Exception {
        Assert.assertEquals(b.getLB(), 0);
        x.updateUpperBound(2, Cause.Null);
        Assert.assertEquals(b.getLB(), 0);
    }

    @Test(groups = "1s")
    public void testGetUB0() throws ContradictionException {
        Assert.assertEquals(b.getUB(), 1);
        x.updateUpperBound(3, Cause.Null);
        Assert.assertEquals(b.getUB(), 1);
    }

    @Test(groups = "1s")
    public void testGetUB1() throws ContradictionException {
        Assert.assertEquals(b.getUB(), 1);
        x.updateUpperBound(2, Cause.Null);
        Assert.assertEquals(b.getUB(), 0);
    }

    @Test(groups = "1s")
    public void testNextValue1() {
        Assert.assertEquals(b.nextValue(-1), 0);
        Assert.assertEquals(b.nextValue(0), 1);
        Assert.assertEquals(b.nextValue(1), Integer.MAX_VALUE);
    }

    @Test(groups = "1s")
    public void testNextValue2() throws ContradictionException {
        x.updateLowerBound(3, Cause.Null);
        Assert.assertEquals(b.nextValue(-1), 1);
        Assert.assertEquals(b.nextValue(0), 1);
        Assert.assertEquals(b.nextValue(1), Integer.MAX_VALUE);
    }

    @Test(groups = "1s")
    public void testNextValue3() throws ContradictionException {
        x.updateUpperBound(2, Cause.Null);
        Assert.assertEquals(b.nextValue(-1), 0);
        Assert.assertEquals(b.nextValue(0), Integer.MAX_VALUE);
        Assert.assertEquals(b.nextValue(1), Integer.MAX_VALUE);
    }

    @Test(groups = "1s")
    public void testNextValueOut1() {
        Assert.assertEquals(b.nextValueOut(-2), -1);
        Assert.assertEquals(b.nextValueOut(-1), 2);
        Assert.assertEquals(b.nextValueOut(0), 2);
        Assert.assertEquals(b.nextValueOut(1), 2);
        Assert.assertEquals(b.nextValueOut(2), 3);
    }

    @Test(groups = "1s")
    public void testNextValueOut2() throws ContradictionException {
        x.updateLowerBound(3, Cause.Null);
        Assert.assertEquals(b.nextValueOut(-2), -1);
        Assert.assertEquals(b.nextValueOut(-1), 0);
        Assert.assertEquals(b.nextValueOut(0), 2);
        Assert.assertEquals(b.nextValueOut(1), 2);
        Assert.assertEquals(b.nextValueOut(2), 3);
    }

    @Test(groups = "1s")
    public void testNextValueOut3() throws ContradictionException {
        x.updateUpperBound(2, Cause.Null);
        Assert.assertEquals(b.nextValueOut(-2), -1);
        Assert.assertEquals(b.nextValueOut(-1), 1);
        Assert.assertEquals(b.nextValueOut(0), 1);
        Assert.assertEquals(b.nextValueOut(1), 2);
        Assert.assertEquals(b.nextValueOut(2), 3);
    }

    @Test(groups = "1s")
    public void testPreviousValue1() {
        Assert.assertEquals(b.previousValue(10), 1);
        Assert.assertEquals(b.previousValue(2), 1);
        Assert.assertEquals(b.previousValue(1), 0);
        Assert.assertEquals(b.previousValue(0), Integer.MIN_VALUE);
    }

    @Test(groups = "1s")
    public void testPreviousValue2() throws ContradictionException {
        x.updateLowerBound(3, Cause.Null);
        Assert.assertEquals(b.previousValue(10), 1);
        Assert.assertEquals(b.previousValue(2), 1);
        Assert.assertEquals(b.previousValue(1), Integer.MIN_VALUE);
        Assert.assertEquals(b.previousValue(0), Integer.MIN_VALUE);
    }

    @Test(groups = "1s")
    public void testPreviousValue3() throws ContradictionException {
        x.updateUpperBound(2, Cause.Null);
        Assert.assertEquals(b.previousValue(10), 0);
        Assert.assertEquals(b.previousValue(2), 0);
        Assert.assertEquals(b.previousValue(1), 0);
        Assert.assertEquals(b.previousValue(0), Integer.MIN_VALUE);
    }

    @Test(groups = "1s")
    public void testPreviousValueOut0() {
        Assert.assertEquals(b.previousValueOut(10), 9);
        Assert.assertEquals(b.previousValueOut(2), -1);
        Assert.assertEquals(b.previousValueOut(1), -1);
        Assert.assertEquals(b.previousValueOut(0), -1);
    }

    @Test(groups = "1s")
    public void testPreviousValueOut1() throws ContradictionException {
        x.updateLowerBound(3, Cause.Null);
        Assert.assertEquals(b.previousValueOut(10), 9);
        Assert.assertEquals(b.previousValueOut(2), 0);
        Assert.assertEquals(b.previousValueOut(1), 0);
        Assert.assertEquals(b.previousValueOut(0), -1);
    }

    @Test(groups = "1s")
    public void testPreviousValueOut2() throws ContradictionException {
        x.updateUpperBound(2, Cause.Null);
        Assert.assertEquals(b.previousValueOut(10), 9);
        Assert.assertEquals(b.previousValueOut(2), 1);
        Assert.assertEquals(b.previousValueOut(1), -1);
        Assert.assertEquals(b.previousValueOut(0), -1);
    }

    @Test(groups = "1s", timeOut = 60000)
    public void testGetValueIterator() {
        DisposableValueIterator vit = b.getValueIterator(true);
        Assert.assertTrue(vit.hasNext());
        Assert.assertEquals(0, vit.next());
        Assert.assertTrue(vit.hasNext());
        Assert.assertEquals(1, vit.next());
        Assert.assertFalse(vit.hasNext());
        vit.dispose();

        vit = b.getValueIterator(false);
        Assert.assertTrue(vit.hasPrevious());
        Assert.assertEquals(1, vit.previous());
        Assert.assertTrue(vit.hasPrevious());
        Assert.assertEquals(0, vit.previous());
        Assert.assertFalse(vit.hasPrevious());
        vit.dispose();
    }

    @Test(groups = "1s", timeOut = 60000)
    public void testGetRangeIterator() {
        DisposableRangeIterator rit = b.getRangeIterator(true);
        Assert.assertTrue(rit.hasNext());
        Assert.assertEquals(0, rit.min());
        Assert.assertEquals(1, rit.max());
        rit.next();
        Assert.assertFalse(rit.hasNext());

        rit = b.getRangeIterator(false);
        Assert.assertTrue(rit.hasPrevious());
        Assert.assertEquals(0, rit.min());
        Assert.assertEquals(1, rit.max());
        rit.previous();
        Assert.assertFalse(rit.hasPrevious());
    }

    @Test(groups = "1s", timeOut = 60000)
    public void test1() {
        BoolVar[] doms = new BoolVar[6];
        for (int i = 0; i < 6; i++) {
            doms[i] = model.isEq(x, i);
        }
        while (model.getSolver().solve()) {
//            System.out.printf("%s\n", x);
//            System.out.printf("%s\n", Arrays.toString(doms));

        }
    }

    @Test(groups = "1s", timeOut = 6000000)
    public void testAA23() {
        Model model = new Model();
        final IntVar[] xs = model.intVarArray("x", 5, 0, 5);
        final BoolVar[] bs = Stream.of(xs).map(x ->
                model.isGeq(x, 1)
        ).toArray(BoolVar[]::new);

        final IntVar count = model.intVar(0, 5);
        model.sum(bs, "=", count).post();
        Assert.assertEquals(model.getSolver().findAllSolutions().size(), 7776);
    }

    @Test(groups = "1s")
    public void testToTable() throws ContradictionException {
        for (int i = 0; i < 100; i++) {
            Model mod = new Model(Settings.init().setEnableViews(false));
            IntVar res = mod.intVar("r", 0, 1004);
            IntVar[] xs = mod.intVarArray(5, new int[]{2, 3, 4, 5});
            BoolVar[] vs = new BoolVar[5];
            for (int j = 0; j < 5; j++) {
                vs[j] = mod.isLeq(xs[j], 3);
                /*vs[j] = mod.boolVar();
                mod.reifyXltC(xs[j], 4, vs[j]);*/
            }
            int[] coeffs = new int[]{
                    1, 1, 1, 1, 1000
            };

            mod.table(ArrayUtils.append(vs, new IntVar[]{res}),
                    TuplesFactory.scalar(vs, coeffs, res, 1), "CT+").post();
            mod.getSolver().setSearch(Search.randomSearch(xs, i));
            while (mod.getSolver().solve()) {
                int c = 0;
                for (int j = 0; j < xs.length; j++) {
                    c += vs[j].getValue() * coeffs[j];
                }
                Assert.assertEquals(c, res.getValue());
            }
            Assert.assertEquals(mod.getSolver().getSolutionCount(), 1024);
        }
    }

}