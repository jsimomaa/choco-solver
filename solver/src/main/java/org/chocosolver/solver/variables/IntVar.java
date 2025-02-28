/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.variables;

import org.chocosolver.sat.Reason;
import org.chocosolver.solver.ICause;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.expression.discrete.arithmetic.ArExpression;
import org.chocosolver.solver.variables.delta.IIntDeltaMonitor;
import org.chocosolver.solver.variables.events.IEventType;
import org.chocosolver.util.iterators.DisposableRangeIterator;
import org.chocosolver.util.iterators.DisposableValueIterator;
import org.chocosolver.util.objects.setDataStructures.iterable.IntIterableSet;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;


/**
 * Interface for integer variables. Provides every required services.
 * The domain is explicitly represented but is not (and should not be) accessible from outside.
 * <br/>
 * <p>
 * CPRU r544: remove default implementation
 *
 * @author Charles Prud'homme
 * @since 18 nov. 2010
 */
public interface IntVar extends ICause, Variable, Iterable<Integer>, ArExpression {

    /**
     * Provide a minimum value for integer variable lower bound.
     * Do not prevent from underflow, but may avoid it, somehow.
     */
    int MIN_INT_BOUND = Integer.MIN_VALUE / 100;

    /**
     * Provide a minimum value for integer variable lower bound.
     * Do not prevent from overflow, but may avoid it, somehow.
     */
    int MAX_INT_BOUND = Integer.MAX_VALUE / 100;
    int LR_NE = 0; // [x != v]
    int LR_EQ = 1; // [x = v]
    int LR_GE = 2; // [x >= v]
    int LR_LE = 3; // [x <= v]

    /**
     * Removes <code>value</code>from the domain of <code>this</code>. The instruction comes from <code>propagator</code>.
     * <ul>
     * <li>If <code>value</code> is out of the domain, nothing is done and the return value is <code>false</code>,</li>
     * <li>if removing <code>value</code> leads to a dead-end (domain wipe-out),
     * a <code>ContradictionException</code> is thrown,</li>
     * <li>otherwise, if removing <code>value</code> from the domain can be done safely,
     * the event type is created (the original event can be promoted) and observers are notified
     * and the return value is <code>true</code></li>
     * </ul>
     *
     * @param value value to remove from the domain (int)
     * @param cause removal releaser
     * @return true if the value has been removed, false otherwise
     * @throws ContradictionException if the domain become empty due to this action
     */
    default boolean removeValue(int value, ICause cause) throws ContradictionException {
        return removeValue(value, cause, cause.defaultReason(this));
    }

    /**
     * Removes <code>value</code>from the domain of <code>this</code>. The instruction comes from <code>propagator</code>.
     * <ul>
     * <li>If <code>value</code> is out of the domain, nothing is done and the return value is <code>false</code>,</li>
     * <li>if removing <code>value</code> leads to a dead-end (domain wipe-out),
     * a <code>ContradictionException</code> is thrown,</li>
     * <li>otherwise, if removing <code>value</code> from the domain can be done safely,
     * the event type is created (the original event can be promoted) and observers are notified
     * and the return value is <code>true</code></li>
     * </ul>
     *
     * @param value  value to remove from the domain (int)
     * @param cause  removal releaser
     * @param reason the reason why the value is removed
     * @return true if the value has been removed, false otherwise
     * @throws ContradictionException if the domain become empty due to this action
     */
    boolean removeValue(int value, ICause cause, Reason reason) throws ContradictionException;

    /**
     * Removes <code>value</code>from the domain of <code>this</code>. The instruction comes from <code>propagator</code>.
     * <p>
     * This method deals with <code>value</code> as <b>long</b>.
     * If such a long can be safely cast to an int, this falls back to regular case (int).
     * Otherwise, it can either trivially do nothing or fail.
     * </p>
     * <ul>
     * <li>If <code>value</code> is out of the domain, nothing is done and the return value is <code>false</code>,</li>
     * <li>if removing <code>value</code> leads to a dead-end (domain wipe-out),
     * a <code>ContradictionException</code> is thrown,</li>
     * <li>otherwise, if removing <code>value</code> from the domain can be done safely,
     * the event type is created (the original event can be promoted) and observers are notified
     * and the return value is <code>true</code></li>
     * </ul>
     *
     * @param value value to remove from the domain (int)
     * @param cause removal releaser
     * @return true if the value has been removed, false otherwise
     * @throws ContradictionException if the domain become empty due to this action
     */
    default boolean removeValue(long value, ICause cause) throws ContradictionException {
        if ((int) value != value) { // cannot be cast to an int
            return false;
        } else {
            return removeValue((int) value, cause);
        }
    }

    /**
     * Removes the value in <code>values</code>from the domain of <code>this</code>. The instruction comes from <code>propagator</code>.
     * <ul>
     * <li>If all values are out of the domain, nothing is done and the return value is <code>false</code>,</li>
     * <li>if removing a value leads to a dead-end (domain wipe-out),
     * a <code>ContradictionException</code> is thrown,</li>
     * <li>otherwise, if removing the <code>values</code> from the domain can be done safely,
     * the event type is created (the original event can be promoted) and observers are notified
     * and the return value is <code>true</code></li>
     * </ul>
     *
     * @param values set of ordered values to remove
     * @param cause  removal release
     * @param reason the reason why the values are removed
     * @return true if at least a value has been removed, false otherwise
     * @throws ContradictionException if the domain become empty due to this action
     */
    default boolean removeValues(IntIterableSet values, ICause cause, Reason reason) throws ContradictionException {
        assert cause != null;
        boolean hasChanged = false, fixpoint;
        int vlb, vub;
        do {
            int nlb = getLB();
            int nub = getUB();
            vlb = values.nextValue(nlb - 1);
            vub = values.previousValue(nub + 1);
            if (!hasChanged && (vlb > nub || vub < nlb)) {
                return false;
            }
            if (vlb == nlb) {
                // look for the new lb
                do {
                    nlb = nextValue(nlb);
                    vlb = values.nextValue(nlb - 1);
                } while (nlb < Integer.MAX_VALUE && nub < Integer.MAX_VALUE && vlb == nlb);
            }
            if (vub == nub) {
                // look for the new ub
                do {
                    nub = previousValue(nub);
                    vub = values.previousValue(nub + 1);
                } while (nlb > Integer.MIN_VALUE && nub > Integer.MIN_VALUE && vub == nub);
            }
            // the new bounds are now known, delegate to the right method
            fixpoint = updateBounds(nlb, nub, cause, reason);
            hasChanged |= fixpoint;
        } while (fixpoint);
        // now deal with holes
        if (hasEnumeratedDomain()) {
            int value = vlb;
            while (value <= vub) {
                hasChanged |= removeValue(value, cause, reason);
                value = values.nextValue(value);
            }
        }
        return hasChanged;
    }

    /**
     * Removes the value in <code>values</code>from the domain of <code>this</code>. The instruction comes from <code>propagator</code>.
     * <ul>
     * <li>If all values are out of the domain, nothing is done and the return value is <code>false</code>,</li>
     * <li>if removing a value leads to a dead-end (domain wipe-out),
     * a <code>ContradictionException</code> is thrown,</li>
     * <li>otherwise, if removing the <code>values</code> from the domain can be done safely,
     * the event type is created (the original event can be promoted) and observers are notified
     * and the return value is <code>true</code></li>
     * </ul>
     *
     * @param values set of ordered values to remove
     * @param cause  removal release
     * @return true if at least a value has been removed, false otherwise
     * @throws ContradictionException if the domain become empty due to this action
     */
    default boolean removeValues(IntIterableSet values, ICause cause) throws ContradictionException {
        return removeValues(values, cause, cause.defaultReason(this));
    }

    /**
     * Removes all values from the domain of <code>this</code> except those in <code>values</code>. The instruction comes from <code>propagator</code>.
     * <ul>
     * <li>If all values are out of the domain,
     * a <code>ContradictionException</code> is thrown,</li>
     * <li>if the domain is a subset of values,
     * nothing is done and the return value is <code>false</code>,</li>
     * <li>otherwise, if removing all values but <code>values</code> from the domain can be done safely,
     * the event type is created (the original event can be promoted) and observers are notified
     * and the return value is <code>true</code></li>
     * </ul>
     *
     * @param values set of ordered values to keep in the domain
     * @param cause  removal cause
     * @param reason the reason why the values are removed
     * @return true if a at least a value has been removed, false otherwise
     * @throws ContradictionException if the domain become empty due to this action
     */
    default boolean removeAllValuesBut(IntIterableSet values, ICause cause, Reason reason) throws ContradictionException {
        boolean hasChanged = false, fixpoint;
        int nlb, nub;
        do {
            int clb = getLB();
            int cub = getUB();
            nlb = values.nextValue(clb - 1);
            nub = values.previousValue(cub + 1);
            // the new bounds are now known, delegate to the right method
            fixpoint = updateBounds(nlb, nub, cause, reason);
            hasChanged |= fixpoint;
        } while (fixpoint);
        // now deal with holes
        int to = previousValue(nub);
        if (hasEnumeratedDomain()) {
            int value = nextValue(nlb);
            // iterate over the values in the domain, remove the ones that are not in values
            for (; value <= to; value = nextValue(value)) {
                if (!values.contains(value)) {
                    hasChanged |= removeValue(value, cause, reason);
                }
            }
        }
        return hasChanged;
    }

    /**
     * Removes all values from the domain of <code>this</code> except those in <code>values</code>. The instruction comes from <code>propagator</code>.
     * <ul>
     * <li>If all values are out of the domain,
     * a <code>ContradictionException</code> is thrown,</li>
     * <li>if the domain is a subset of values,
     * nothing is done and the return value is <code>false</code>,</li>
     * <li>otherwise, if removing all values but <code>values</code> from the domain can be done safely,
     * the event type is created (the original event can be promoted) and observers are notified
     * and the return value is <code>true</code></li>
     * </ul>
     *
     * @param values set of ordered values to keep in the domain
     * @param cause  removal release
     * @return true if a at least a value has been removed, false otherwise
     * @throws ContradictionException if the domain become empty due to this action
     */
    default boolean removeAllValuesBut(IntIterableSet values, ICause cause) throws ContradictionException {
        return removeAllValuesBut(values, cause, cause.defaultReason(this));
    }

    /**
     * Removes values between [<code>from, to</code>] from the domain of <code>this</code>. The instruction comes from <code>propagator</code>.
     * <ul>
     * <li>If union between values and the current domain is empty, nothing is done and the return value is <code>false</code>,</li>
     * <li>if removing a <code>value</code> leads to a dead-end (domain wipe-out),
     * a <code>ContradictionException</code> is thrown,</li>
     * <li>otherwise, if removing at least a <code>value</code> from the domain can be done safely,
     * the event type is created (the original event can be promoted) and observers are notified
     * and the return value is <code>true</code></li>
     * </ul>
     *
     * @param from  lower bound of the interval to remove (int)
     * @param to    upper bound of the interval to remove(int)
     * @param cause removal releaser
     * @return true if the value has been removed, false otherwise
     * @throws ContradictionException if the domain become empty due to this action
     */
    default boolean removeInterval(int from, int to, ICause cause) throws ContradictionException {
        assert cause != null;
        if (from <= getLB()) {
            return updateLowerBound(to + 1, cause);
        } else if (getUB() <= to) {
            return updateUpperBound(from - 1, cause);
        } else if (hasEnumeratedDomain()) {
            boolean done = false;
            for (int v = from; v <= to; v++) {
                done |= removeValue(v, cause);
            }
            return done;
        }
        return false;
    }

    /**
     * Instantiates the domain of <code>this</code> to <code>value</code>. The instruction comes from <code>propagator</code>.
     * <ul>
     * <li>If the domain of <code>this</code> is already instantiated to <code>value</code>,
     * nothing is done and the return value is <code>false</code>,</li>
     * <li>If the domain of <code>this</code> is already instantiated to another value,
     * then a <code>ContradictionException</code> is thrown,</li>
     * <li>Otherwise, the domain of <code>this</code> is restricted to <code>value</code> and the observers are notified
     * and the return value is <code>true</code>.</li>
     * </ul>
     *
     * @param value instantiation value (int)
     * @param cause instantiation releaser
     * @return true if the instantiation is done, false otherwise
     * @throws ContradictionException if the domain become empty due to this action
     */
    default boolean instantiateTo(int value, ICause cause) throws ContradictionException {
        return instantiateTo(value, cause, cause.defaultReason(this));
    }

    /**
     * Instantiates the domain of <code>this</code> to <code>value</code>. The instruction comes from <code>propagator</code>.
     * <ul>
     * <li>If the domain of <code>this</code> is already instantiated to <code>value</code>,
     * nothing is done and the return value is <code>false</code>,</li>
     * <li>If the domain of <code>this</code> is already instantiated to another value,
     * then a <code>ContradictionException</code> is thrown,</li>
     * <li>Otherwise, the domain of <code>this</code> is restricted to <code>value</code> and the observers are notified
     * and the return value is <code>true</code>.</li>
     * </ul>
     *
     * @param value  instantiation value (int)
     * @param cause  instantiation releaser
     * @param reason the reason why the variable is instantiated
     * @return true if the instantiation is done, false otherwise
     * @throws ContradictionException if the domain become empty due to this action
     */
    boolean instantiateTo(int value, ICause cause, Reason reason) throws ContradictionException;

    /**
     * Instantiates the domain of <code>this</code> to <code>value</code>. The instruction comes from <code>propagator</code>.
     * <p>
     * This method deals with <code>value</code> as <b>long</b>.
     * If such a long can be safely cast to an int, this falls back to regular case (int).
     * Otherwise, it can either trivially do nothing or fail.
     * </p>
     * <ul>
     * <li>If the domain of <code>this</code> is already instantiated to <code>value</code>,
     * nothing is done and the return value is <code>false</code>,</li>
     * <li>If the domain of <code>this</code> is already instantiated to another value,
     * then a <code>ContradictionException</code> is thrown,</li>
     * <li>Otherwise, the domain of <code>this</code> is restricted to <code>value</code> and the observers are notified
     * and the return value is <code>true</code>.</li>
     * </ul>
     *
     * @param value instantiation value (int)
     * @param cause instantiation releaser
     * @return true if the instantiation is done, false otherwise
     * @throws ContradictionException if the domain become empty due to this action
     */
    default boolean instantiateTo(long value, ICause cause) throws ContradictionException {
        if ((int) value != value) { // cannot be cast to an int
            return instantiateTo(value < getLB() ? getLB() - 1 : getUB() + 1, cause);
        } else {
            return instantiateTo((int) value, cause);
        }
    }

    /**
     * Updates the lower bound of the domain of <code>this</code> to <code>value</code>.
     * The instruction comes from <code>propagator</code>.
     * <ul>
     * <li>If <code>value</code> is smaller than the lower bound of the domain, nothing is done and the return value is <code>false</code>,</li>
     * <li>if updating the lower bound to <code>value</code> leads to a dead-end (domain wipe-out),
     * a <code>ContradictionException</code> is thrown,</li>
     * <li>otherwise, if updating the lower bound to <code>value</code> can be done safely,
     * the event type is created (the original event can be promoted) and observers are notified
     * and the return value is <code>true</code></li>
     * </ul>
     *
     * @param value new lower bound (included)
     * @param cause updating releaser
     * @return true if the lower bound has been updated, false otherwise
     * @throws ContradictionException if the domain become empty due to this action
     */
    default boolean updateLowerBound(int value, ICause cause) throws ContradictionException {
        return updateLowerBound(value, cause, cause.defaultReason(this));
    }

    /**
     * Updates the lower bound of the domain of <code>this</code> to <code>value</code>.
     * The instruction comes from <code>propagator</code>.
     * <ul>
     * <li>If <code>value</code> is smaller than the lower bound of the domain, nothing is done and the return value is <code>false</code>,</li>
     * <li>if updating the lower bound to <code>value</code> leads to a dead-end (domain wipe-out),
     * a <code>ContradictionException</code> is thrown,</li>
     * <li>otherwise, if updating the lower bound to <code>value</code> can be done safely,
     * the event type is created (the original event can be promoted) and observers are notified
     * and the return value is <code>true</code></li>
     * </ul>
     *
     * @param value  new lower bound (included)
     * @param cause  updating releaser
     * @param reason the reason why the lower bound is updated
     * @return true if the lower bound has been updated, false otherwise
     * @throws ContradictionException if the domain become empty due to this action
     */
    boolean updateLowerBound(int value, ICause cause, Reason reason) throws ContradictionException;

    /**
     * Updates the lower bound of the domain of <code>this</code> to <code>value</code>.
     * The instruction comes from <code>propagator</code>.
     * <p>
     * This method deals with <code>value</code> as <b>long</b>.
     * If such a long can be safely cast to an int, this falls back to regular case (int).
     * Otherwise, it can either trivially do nothing or fail.
     * </p>
     * <ul>
     * <li>If <code>value</code> is smaller than the lower bound of the domain, nothing is done and the return value is <code>false</code>,</li>
     * <li>if updating the lower bound to <code>value</code> leads to a dead-end (domain wipe-out),
     * a <code>ContradictionException</code> is thrown,</li>
     * <li>otherwise, if updating the lower bound to <code>value</code> can be done safely,
     * the event type is created (the original event can be promoted) and observers are notified
     * and the return value is <code>true</code></li>
     * </ul>
     *
     * @param value new lower bound (included)
     * @param cause updating releaser
     * @return true if the lower bound has been updated, false otherwise
     * @throws ContradictionException if the domain become empty due to this action
     */
    default boolean updateLowerBound(long value, ICause cause) throws ContradictionException {
        if ((int) value != value) { // cannot be cast to an int
            if (value < getLB()) {
                return false;
            } else { // then value >> getLB, this fails
                return updateLowerBound(getUB() + 1, cause);
            }
        } else {
            return updateLowerBound((int) value, cause);
        }
    }

    /**
     * Updates the upper bound of the domain of <code>this</code> to <code>value</code>.
     * The instruction comes from <code>propagator</code>.
     * <ul>
     * <li>If <code>value</code> is greater than the upper bound of the domain, nothing is done and the return value is <code>false</code>,</li>
     * <li>if updating the upper bound to <code>value</code> leads to a dead-end (domain wipe-out),
     * a <code>ContradictionException</code> is thrown,</li>
     * <li>otherwise, if updating the upper bound to <code>value</code> can be done safely,
     * the event type is created (the original event can be promoted) and observers are notified
     * and the return value is <code>true</code></li>
     * </ul>
     *
     * @param value new upper bound (included)
     * @param cause update releaser
     * @return true if the upper bound has been updated, false otherwise
     * @throws ContradictionException if the domain become empty due to this action
     */
    default boolean updateUpperBound(int value, ICause cause) throws ContradictionException {
        return updateUpperBound(value, cause, cause.defaultReason(this));
    }

    /**
     * Updates the upper bound of the domain of <code>this</code> to <code>value</code>.
     * The instruction comes from <code>propagator</code>.
     * <ul>
     * <li>If <code>value</code> is greater than the upper bound of the domain, nothing is done and the return value is <code>false</code>,</li>
     * <li>if updating the upper bound to <code>value</code> leads to a dead-end (domain wipe-out),
     * a <code>ContradictionException</code> is thrown,</li>
     * <li>otherwise, if updating the upper bound to <code>value</code> can be done safely,
     * the event type is created (the original event can be promoted) and observers are notified
     * and the return value is <code>true</code></li>
     * </ul>
     *
     * @param value  new upper bound (included)
     * @param cause  update releaser
     * @param reason the reason why the upper bound is updated
     * @return true if the upper bound has been updated, false otherwise
     * @throws ContradictionException if the domain become empty due to this action
     */
    boolean updateUpperBound(int value, ICause cause, Reason reason) throws ContradictionException;

    /**
     * Updates the upper bound of the domain of <code>this</code> to <code>value</code>.
     * The instruction comes from <code>propagator</code>.
     * <p>
     * This method deals with <code>value</code> as <b>long</b>.
     * If such a long can be safely cast to an int, this falls back to regular case (int).
     * Otherwise, it can either trivially do nothing or fail.
     * </p>
     * <ul>
     * <li>If <code>value</code> is greater than the upper bound of the domain, nothing is done and the return value is <code>false</code>,</li>
     * <li>if updating the upper bound to <code>value</code> leads to a dead-end (domain wipe-out),
     * a <code>ContradictionException</code> is thrown,</li>
     * <li>otherwise, if updating the upper bound to <code>value</code> can be done safely,
     * the event type is created (the original event can be promoted) and observers are notified
     * and the return value is <code>true</code></li>
     * </ul>
     *
     * @param value new upper bound (included)
     * @param cause update releaser
     * @return true if the upper bound has been updated, false otherwise
     * @throws ContradictionException if the domain become empty due to this action
     */
    default boolean updateUpperBound(long value, ICause cause) throws ContradictionException {
        if ((int) value != value) { // cannot be cast to an int
            if (value > getUB()) {
                return false;
            } else { // then value << getUB, this fails
                return updateUpperBound(getLB() - 1, cause);
            }
        } else {
            return updateUpperBound((int) value, cause);
        }
    }

    /**
     * Updates the lower bound and the upper bound of the domain of <code>this</code> to, resp. <code>lb</code> and <code>ub</code>.
     * The instruction comes from <code>propagator</code>.
     * <p>
     * <ul>
     * <li>If <code>lb</code> is smaller than the lower bound of the domain
     * and <code>ub</code> is greater than the upper bound of the domain,
     * <p>
     * nothing is done and the return value is <code>false</code>,</li>
     * <li>if updating the lower bound to <code>lb</code>, or updating the upper bound to <code>ub</code> leads to a dead-end (domain wipe-out),
     * or if <code>lb</code> is strictly greater than <code>ub</code>,
     * a <code>ContradictionException</code> is thrown,</li>
     * <li>otherwise, if updating the lower bound to <code>lb</code> and/or the upper bound to <code>ub</code>
     * can be done safely can be done safely,
     * the event type is created (the original event can be promoted) and observers are notified
     * and the return value is <code>true</code></li>
     * </ul>
     *
     * @param lb    new lower bound (included)
     * @param ub    new upper bound (included)
     * @param cause update releaser
     * @return true if the upper bound has been updated, false otherwise
     * @throws ContradictionException if the domain become empty due to this action
     */
    default boolean updateBounds(int lb, int ub, ICause cause) throws ContradictionException {
        return updateBounds(lb, ub, cause, cause.defaultReason(this));
    }

    /**
     * Updates the lower bound and the upper bound of the domain of <code>this</code> to, resp. <code>lb</code> and <code>ub</code>.
     * The instruction comes from <code>propagator</code>.
     * <p>
     * <ul>
     * <li>If <code>lb</code> is smaller than the lower bound of the domain
     * and <code>ub</code> is greater than the upper bound of the domain,
     * <p>
     * nothing is done and the return value is <code>false</code>,</li>
     * <li>if updating the lower bound to <code>lb</code>, or updating the upper bound to <code>ub</code> leads to a dead-end (domain wipe-out),
     * or if <code>lb</code> is strictly greater than <code>ub</code>,
     * a <code>ContradictionException</code> is thrown,</li>
     * <li>otherwise, if updating the lower bound to <code>lb</code> and/or the upper bound to <code>ub</code>
     * can be done safely can be done safely,
     * the event type is created (the original event can be promoted) and observers are notified
     * and the return value is <code>true</code></li>
     * </ul>
     *
     * @param lb     new lower bound (included)
     * @param ub     new upper bound (included)
     * @param cause  update cause
     * @param reason the reason why the bounds are updated
     * @return true if the upper bound has been updated, false otherwise
     * @throws ContradictionException if the domain become empty due to this action
     */
    default boolean updateBounds(int lb, int ub, ICause cause, Reason reason) throws ContradictionException {
        return updateLowerBound(lb, cause, reason) | updateUpperBound(ub, cause, reason);
    }

    /**
     * Checks if a value <code>v</code> belongs to the domain of <code>this</code>
     *
     * @param value int
     * @return <code>true</code> if the value belongs to the domain of <code>this</code>, <code>false</code> otherwise.
     */
    boolean contains(int value);

    /**
     * Checks wether <code>this</code> is instantiated to <code>val</code>
     *
     * @param value int
     * @return true if <code>this</code> is instantiated to <code>val</code>, false otherwise
     */
    boolean isInstantiatedTo(int value);

    /**
     * Retrieves the current value of the variable if instantiated
     *
     * @return the current value
     * @throws IllegalStateException when the variable is not instantiated
     */
    int getValue() throws IllegalStateException;

    /**
     * Retrieves the lower bound of the variable
     *
     * @return the lower bound
     */
    int getLB();

    /**
     * Retrieves the upper bound of the variable
     *
     * @return the upper bound
     */
    int getUB();


    /**
     * Returns the range of this domain, that is, the difference between the upper bound and the lower bound.
     *
     * @return the range of this domain
     */
    int getRange();

    /**
     * Returns the first value just after v in <code>this</code> which is <b>in</b> the domain.
     * If no such value exists, returns Integer.MAX_VALUE;
     * <p>
     * To iterate over the values in a <code>IntVar</code>,
     * use the following loop:
     * <p>
     * <pre>
     * int ub = iv.getUB();
     * for (int i = iv.getLB(); i <= ub; i = iv.nextValue(i)) {
     *     // operate on value i here
     * }</pre>
     *
     * @param v the value to start checking (exclusive)
     * @return the next value in the domain
     */
    int nextValue(int v);

    /**
     * Returns the first value just after v in <code>this</code> which is <b>out of</b> the domain.
     * If <i>v</i> is less than or equal to {@link #getLB()}-2, returns <i>v + 1</i>,
     * if <i>v</i> is greater than or equal to {@link #getUB()}, returns <i>v + 1</i>.
     *
     * @param v the value to start checking (exclusive)
     * @return the next value out of the domain
     */
    int nextValueOut(int v);

    /**
     * Returns the previous value just before v in <code>this</code>.
     * If no such value exists, returns Integer.MIN_VALUE;
     * <p>
     * To iterate over the values in a <code>IntVar</code>,
     * use the following loop:
     * <p>
     * <pre>
     * int lb = iv.getLB();
     * for (int i = iv.getUB(); i >= lb; i = iv.previousValue(i)) {
     *     // operate on value i here
     * }</pre>
     *
     * @param v the value to start checking (exclusive)
     * @return the previous value in the domain
     */
    int previousValue(int v);

    /**
     * Returns the first value just before v in <code>this</code> which is <b>out of</b> the domain.
     * If <i>v</i> is greater than or equal to {@link #getUB()}+2, returns <i>v - 1</i>,
     * if <i>v</i> is less than or equal to {@link #getLB()}, returns <i>v - 1</i>.
     *
     * @param v the value to start checking (exclusive)
     * @return the previous value out of the domain
     */
    int previousValueOut(int v);

    /**
     * Retrieves an iterator over values of <code>this</code>.
     * <p>
     * The values can be iterated in a bottom-up way or top-down way.
     * <p>
     * To bottom-up iterate over the values in a <code>IntVar</code>,
     * use the following loop:
     * <p>
     * <pre>
     * DisposableValueIterator vit = var.getValueIterator(true);
     * while(vit.hasNext()){
     *     int v = vit.next();
     *     // operate on value v here
     * }
     * vit.dispose();</pre>
     * <p>
     * <p>
     * To top-down iterate over the values in a <code>IntVar</code>,
     * use the following loop:
     *
     * <pre>
     * DisposableValueIterator vit = var.getValueIterator(false);
     * while(vit.hasPrevious()){
     *     int v = vit.previous();
     *     // operate on value v here
     * }
     * vit.dispose();</pre>
     *
     * <b>Using both previous and next can lead to unexpected behaviour.</b>
     *
     * @param bottomUp way to iterate over values. <code>true</code> means from lower bound to upper bound,
     *                 <code>false</code> means from upper bound to lower bound.
     * @return a disposable iterator over values of <code>this</code>.
     */
    DisposableValueIterator getValueIterator(boolean bottomUp);

    /**
     * Retrieves an iterator over ranges (or intervals) of <code>this</code>.
     * <p>
     * The ranges can be iterated in a bottom-up way or top-down way.
     * <p>
     * To bottom-up iterate over the values in a <code>IntVar</code>,
     * use the following loop:
     * <p>
     * <pre>
     * DisposableRangeIterator rit = var.getRangeIterator(true);
     * while (rit.hasNext()) {
     *     int from = rit.min();
     *     int to = rit.max();
     *     // operate on range [from,to] here
     *     rit.next();
     * }
     * rit.dispose();</pre>
     * <p>
     * To top-down iterate over the values in a <code>IntVar</code>,
     * use the following loop:
     *
     * <pre>
     * DisposableRangeIterator rit = var.getRangeIterator(false);
     * while (rit.hasPrevious()) {
     *     int from = rit.min();
     *     int to = rit.max();
     *     // operate on range [from,to] here
     *     rit.previous();
     * }
     * rit.dispose();</pre>
     *
     * <b>Using both previous and next can lead to unexpected behaviour.</b>
     *
     * @param bottomUp way to iterate over ranges. <code>true</code> means from lower bound to upper bound,
     *                 <code>false</code> means from upper bound to lower bound.
     * @return a disposable iterator over ranges of <code>this</code>.
     */
    DisposableRangeIterator getRangeIterator(boolean bottomUp);

    /**
     * Indicates wether (or not) <code>this</code> has an enumerated domain (represented in extension)
     * or not (only bounds)
     *
     * @return <code>true</code> if the domain is enumerated, <code>false</code> otherwise.
     */
    boolean hasEnumeratedDomain();

    /**
     * Allow to monitor removed values of <code>this</code>.
     *
     * @param propagator the cause that requires to monitor delta
     * @return a delta monitor
     */
    IIntDeltaMonitor monitorDelta(ICause propagator);


    /**
     * @return true iff the variable has a binary domain
     */
    boolean isBool();

    @Override
    default void forEachIntVar(Consumer<IntVar> action) {
        action.accept(this);
    }

    /**
     * @param evt original event
     * @return transforms the original event wrt this IntVar
     */
    default IEventType transformEvent(IEventType evt) {
        return evt;
    }


    @Override
    default IntVar intVar() {
        return this;
    }

    @Override
    default int getNoChild() {
        return 1;
    }

    @Override
    default boolean isExpressionLeaf() {
        return true;
    }

    default IntStream stream() {
        Spliterators.AbstractIntSpliterator it = new Spliterators.AbstractIntSpliterator(IntVar.this.getDomainSize(),
                Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.NONNULL) {
            final int[] val = {IntVar.this.getLB() - 1};

            @Override
            public boolean tryAdvance(IntConsumer action) {
                if ((val[0] = IntVar.this.nextValue(val[0])) < Integer.MAX_VALUE) {
                    action.accept(val[0]);
                    return true;
                }
                return false;
            }

        };
        return StreamSupport.intStream(it, false);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // FOR LAZY CLAUSE GENERATION ONLY
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Get the literal corresponding to the value v and the type t
     *
     * @param val the value
     * @param type the corresponding type (NE, EQ, GE or LE)
     * @return the literal
     */
    default int getLit(int val, int type) {
        throw new UnsupportedOperationException();
    }

    /**
     * Get the literal corresponding to [x != v]
     * @param v the value
     * @return the literal
     * @implNote a literal <code>l</code> can be negated with <code>MiniSat.neg(l)</code>
     */
    default int getNELit(int v) {
        return getLit(v, LR_NE);
    }

    /**
     * Get the literal corresponding to [x = v]
     * @param v the value
     * @return the literal
     * @implNote a literal <code>l</code> can be negated with <code>MiniSat.neg(l)</code>
     */
    default int getEQLit(int v) {
        return getLit(v, LR_EQ);
    }

    /**
     * Get the literal corresponding to [x >= v]
     * @param v the value
     * @return the literal
     * @implNote a literal <code>l</code> can be negated with <code>MiniSat.neg(l)</code>
     */
    default int getGELit(int v) {
        return getLit(v, LR_GE);
    }

    /**
     * Get the literal corresponding to [x <= v]
     * @param v the value
     * @return the literal
     * @implNote a literal <code>l</code> can be negated with <code>MiniSat.neg(l)</code>
     */
    default int getLELit(int v) {
        return getLit(v, LR_LE);
    }

    /**
     * @return the literal corresponding to current lower bound
     */
    default int getMinLit() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the literal corresponding to current upper bound
     */
    default int getMaxLit() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the literal corresponding to current instantiation value
     */
    default int getValLit() {
        throw new UnsupportedOperationException();
    }

}
