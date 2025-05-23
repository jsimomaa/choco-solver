/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2025, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.expression.discrete.arithmetic;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.expression.discrete.relational.BiReExpression;
import org.chocosolver.solver.expression.discrete.relational.NaReExpression;
import org.chocosolver.solver.expression.discrete.relational.ReExpression;
import org.chocosolver.solver.expression.discrete.relational.UnCReExpression;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.tools.MathUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.OptionalInt;

/**
 * arithmetic expression
 * <p>
 * Project: choco-solver.
 *
 * @author Charles Prud'homme
 * @since 28/04/2016.
 */
public interface ArExpression {

    /**
     * List of available operator for arithmetic expression
     */
    enum Operator {
        /**
         * negation operator
         */
        NEG {
            @Override
            int eval(int i1) {
                return -i1;
            }

            @Override
            int eval(int i1, int i2) {
                throw new UnsupportedOperationException();
            }

            @Override
            int identity() {
                return 0;
            }
        },
        /**
         * absolute operator
         */
        ABS {
            @Override
            int eval(int i1) {
                return Math.abs(i1);
            }

            @Override
            int eval(int i1, int i2) {
                throw new UnsupportedOperationException();
            }

            @Override
            int identity() {
                return 0;
            }
        },
        /**
         * addition operator
         */
        ADD {
            @Override
            int eval(int i1) {
                throw new UnsupportedOperationException();
            }

            @Override
            int eval(int i1, int i2) {
                return MathUtils.safeAdd(i1, i2);
            }

            @Override
            int identity() {
                return 0;
            }
        },
        /**
         * subtraction operator
         */
        SUB {
            @Override
            int eval(int i1) {
                throw new UnsupportedOperationException();
            }

            @Override
            int eval(int i1, int i2) {
                return MathUtils.safeSubstract(i1, i2);
            }

            @Override
            int identity() {
                return 0;
            }
        },
        /**
         * multiplication operator
         */
        MUL {
            @Override
            int eval(int i1) {
                throw new UnsupportedOperationException();
            }

            @Override
            int eval(int i1, int i2) {
                return MathUtils.safeMultiply(i1, i2);
            }

            @Override
            int identity() {
                return 1;
            }
        },
        /**
         * division operator
         */
        DIV {
            @Override
            int eval(int i1) {
                throw new UnsupportedOperationException();
            }

            @Override
            int eval(int i1, int i2) {
                if (i2 == 0) {
                    if (i1 > 0) {
                        return Integer.MAX_VALUE;
                    } else {
                        return Integer.MIN_VALUE;
                    }
                } else {
                    return i1 / i2;
                }
            }

            @Override
            int identity() {
                return 0;
            }
        },
        /**
         * modulo operator
         */
        MOD {
            @Override
            int eval(int i1) {
                throw new UnsupportedOperationException();
            }

            @Override
            int eval(int i1, int i2) {
                if (i2 == 0) {
                    if (i1 > 0) {
                        return Integer.MAX_VALUE;
                    } else {
                        return Integer.MIN_VALUE;
                    }
                } else {
                    return i1 % i2;
                }
            }

            @Override
            int identity() {
                return 0;
            }
        },
        /**
         * square operator
         */
        SQR {
            @Override
            int eval(int i1) {
                return MathUtils.safeMultiply(i1, i1);
            }

            @Override
            int eval(int i1, int i2) {
                throw new UnsupportedOperationException();
            }

            @Override
            int identity() {
                return 0;
            }
        },
        /**
         * power operator
         */
        POW {
            @Override
            int eval(int i1) {
                throw new UnsupportedOperationException();
            }

            @Override
            int eval(int i1, int i2) {
                return MathUtils.pow(i1, i2);
            }

            @Override
            int identity() {
                return 0;
            }
        },
        /**
         * min operator
         */
        MIN {
            @Override
            int eval(int i1) {
                throw new UnsupportedOperationException();
            }

            @Override
            int eval(int i1, int i2) {
                return Math.min(i1, i2);
            }

            @Override
            int identity() {
                return Integer.MAX_VALUE;
            }
        },
        /**
         * max operator
         */
        MAX {
            @Override
            int eval(int i1) {
                throw new UnsupportedOperationException();
            }

            @Override
            int eval(int i1, int i2) {
                return Math.max(i1, i2);
            }

            @Override
            int identity() {
                return Integer.MIN_VALUE;
            }
        },
        NOP {
            @Override
            int eval(int i1) {
                return 0;
            }

            @Override
            int eval(int i1, int i2) {
                return 0;
            }

            @Override
            int identity() {
                return 0;
            }
        };

        abstract int eval(int i1);

        abstract int eval(int i1, int i2);

        abstract int identity();
    }

    /**
     * A default empty array
     */
    ArExpression[] NO_CHILD = new ArExpression[0];

    /**
     * Return the associated model
     *
     * @return a Model object
     */
    Model getModel();

    /**
     * @return the arithmetic expression as an {@link IntVar}.
     * If necessary, it creates intermediary variable and posts intermediary constraints
     */
    IntVar intVar();

    /**
     * @return an {@link OptionalInt} which contains an {@code int} this expression is a primitive.
     */
    default OptionalInt primitive() {
        return OptionalInt.empty();
    }

    /**
     * @return <tt>true</tt> if this expression is a leaf, ie a variable, <tt>false</tt> otherwise
     */
    default boolean isExpressionLeaf() {
        return false;
    }

    /**
     * Extract the variables from this expression
     *
     * @param variables set of variables
     */
    default void extractVar(HashSet<IntVar> variables) {
        if (this.isExpressionLeaf()) {
            variables.add((IntVar) this);
        } else {
            for (ArExpression e : getExpressionChild()) {
                e.extractVar(variables);
            }
        }
    }

    /**
     * @param values int values to evaluate
     * @param map    mapping between variables of the topmost expression and position in <i>values</i>
     * @return an evaluation of this expression with a tuple
     */
    default int ieval(int[] values, Map<IntVar, Integer> map) {
        assert this instanceof IntVar;
        return values[map.get(this)];
    }

    /**
     * @return the child of this expression, or null if thid
     */
    default int getNoChild() {
        return 0;
    }

    /**
     * @return the child of this expression, or null if thid
     */
    default ArExpression[] getExpressionChild() {
        return NO_CHILD;
    }

    /**
     * @return return the expression "-x" where this is "x"
     */
    default ArExpression neg() {
        return new UnArExpression(ArExpression.Operator.NEG, this);
    }

    /**
     * @return return the expression "|x|" where this is "x"
     */
    default ArExpression abs() {
        return new UnArExpression(ArExpression.Operator.ABS, this);
    }

    /**
     * @param y an int
     * @return return the expression "x + y" where this is "x"
     */
    default ArExpression add(int y) {
        return new UnCArExpression(ArExpression.Operator.ADD, this, y);
    }

    /**
     * @param y an expression
     * @return return the expression "x + y" where this is "x"
     */
    default ArExpression add(ArExpression y) {
        if (y.primitive().isPresent()) {
            return add(y.primitive().getAsInt());
        }
        return new BiArExpression(ArExpression.Operator.ADD, this, y);
    }

    /**
     * @param y some expressions
     * @return return the expression "x + y_1 + y_2 + ..." where this is "x"
     */
    default ArExpression add(ArExpression... y) {
        return new NaArExpression(ArExpression.Operator.ADD, this, y);
    }

    /**
     * @param y an int
     * @return return the expression "x - y" where this is "x"
     */
    default ArExpression sub(int y) {
        return new UnCArExpression(ArExpression.Operator.SUB, this, y);
    }

    /**
     * @param y an expression
     * @return return the expression "x - y" where this is "x"
     */
    default ArExpression sub(ArExpression y) {
        if (y.primitive().isPresent()) {
            return sub(y.primitive().getAsInt());
        }
        return new BiArExpression(ArExpression.Operator.SUB, this, y);
    }


    /**
     * @param y an int
     * @return return the expression "x * y" where this is "x"
     */
    default ArExpression mul(int y) {
        return new UnCArExpression(ArExpression.Operator.MUL, this, y);
    }

    /**
     * @param y an expression
     * @return return the expression "x * y" where this is "x"
     */
    default ArExpression mul(ArExpression y) {
        if (y.primitive().isPresent()) {
            return mul(y.primitive().getAsInt());
        }
        return new BiArExpression(ArExpression.Operator.MUL, this, y);
    }

    /**
     * @param y some expressions
     * @return return the expression "x * y_1 * y_2 * ..." where this is "x"
     */
    default ArExpression mul(ArExpression... y) {
        return new NaArExpression(ArExpression.Operator.MUL, this, y);
    }

    /**
     * @param y an int
     * @return return the expression "x / y" where this is "x"
     */
    default ArExpression div(int y) {
        return new UnCArExpression(ArExpression.Operator.DIV, this, y);
    }

    /**
     * @param y an expression
     * @return return the expression "x / y" where this is "x"
     */
    default ArExpression div(ArExpression y) {
        if (y.primitive().isPresent()) {
            return div(y.primitive().getAsInt());
        }
        return new BiArExpression(ArExpression.Operator.DIV, this, y);
    }

    /**
     * @param y an int
     * @return return the expression "x % y" where this is "x"
     */
    default ArExpression mod(int y) {
        return new UnCArExpression(ArExpression.Operator.MOD, this, y);
    }

    /**
     * @param y an expression
     * @return return the expression "x % y" where this is "x"
     */
    default ArExpression mod(ArExpression y) {
        if (y.primitive().isPresent()) {
            return mod(y.primitive().getAsInt());
        }
        return new BiArExpression(ArExpression.Operator.MOD, this, y);
    }

    /**
     * @return return the expression "x^2" where this is "x"
     */
    default ArExpression sqr() {
        return new UnArExpression(ArExpression.Operator.SQR, this);
    }

    /**
     * @param y an int
     * @return return the expression "x + y" where this is "x"
     */
    default ArExpression pow(int y) {
        return new UnCArExpression(ArExpression.Operator.POW, this, y);
    }

    /**
     * @param y an expression
     * @return return the expression "x + y" where this is "x"
     */
    default ArExpression pow(ArExpression y) {
        if (y.primitive().isPresent()) {
            return pow(y.primitive().getAsInt());
        }
        return new BiArExpression(ArExpression.Operator.POW, this, y);
    }

    /**
     * @param y an int
     * @return return the expression "min(x, y)" where this is "x"
     */
    default ArExpression min(int y) {
        return new UnCArExpression(ArExpression.Operator.MIN, this, y);
    }

    /**
     * @param y an expression
     * @return return the expression "min(x, y)" where this is "x"
     */
    default ArExpression min(ArExpression y) {
        if (y.primitive().isPresent()) {
            return min(y.primitive().getAsInt());
        }
        return new BiArExpression(ArExpression.Operator.MIN, this, y);
    }

    /**
     * @param y some expressions
     * @return return the expression "min(x, y_1, y_2, ...)" where this is "x"
     */
    default ArExpression min(ArExpression... y) {
        return new NaArExpression(ArExpression.Operator.MIN, this, y);
    }

    /**
     * @param y an int
     * @return return the expression "max(x, y)" where this is "x"
     */
    default ArExpression max(int y) {
        return new UnCArExpression(ArExpression.Operator.MAX, this, y);
    }

    /**
     * @param y an expression
     * @return return the expression "max(x, y)" where this is "x"
     */
    default ArExpression max(ArExpression y) {
        if (y.primitive().isPresent()) {
            return max(y.primitive().getAsInt());
        }
        return new BiArExpression(ArExpression.Operator.MAX, this, y);
    }

    /**
     * @param y some expressions
     * @return return the expression "max(x, y_1, y_2, ...)" where this is "x"
     */
    default ArExpression max(ArExpression... y) {
        return new NaArExpression(ArExpression.Operator.MAX, this, y);
    }

    /**
     * @param y an int
     * @return return the expression "|x - y|" where this is "x"
     */
    default ArExpression dist(int y) {
        return this.sub(y).abs();
    }

    /**
     * @param y an expression
     * @return return the expression "|x - y|" where this is "x"
     */
    default ArExpression dist(ArExpression y) {
        return this.sub(y).abs();
    }

    /**
     * @param y an int
     * @return return the expression "x < y" where this is "x"
     */
    default ReExpression lt(int y) {
        return new UnCReExpression(ReExpression.Operator.LT, this, y);
    }

    /**
     * @param y an expression
     * @return return the expression "x < y" where this is "x"
     */
    default ReExpression lt(ArExpression y) {
        if (y.primitive().isPresent()) {
            return lt(y.primitive().getAsInt());
        }
        return new BiReExpression(ReExpression.Operator.LT, this, y);
    }

    /**
     * @param y an int
     * @return return the expression "x <= y" where this is "x"
     */
    default UnCReExpression le(int y) {
        return new UnCReExpression(ReExpression.Operator.LE, this, y);
    }

    /**
     * @param y an expression
     * @return return the expression "x <= y" where this is "x"
     */
    default ReExpression le(ArExpression y) {
        if (y.primitive().isPresent()) {
            return le(y.primitive().getAsInt());
        }
        return new BiReExpression(ReExpression.Operator.LE, this, y);
    }

    /**
     * @param y an ibt
     * @return return the expression "x > y" where this is "x"
     */
    default ReExpression gt(int y) {
        return new UnCReExpression(ReExpression.Operator.GT, this, y);
    }

    /**
     * @param y an expression
     * @return return the expression "x > y" where this is "x"
     */
    default ReExpression gt(ArExpression y) {
        if (y.primitive().isPresent()) {
            return gt(y.primitive().getAsInt());
        }
        return new BiReExpression(ReExpression.Operator.GT, this, y);
    }

    /**
     * @param y an int
     * @return return the expression "x >= y" where this is "x"
     */
    default ReExpression ge(int y) {
        return new UnCReExpression(ReExpression.Operator.GE, this, y);
    }

    /**
     * @param y an expression
     * @return return the expression "x >= y" where this is "x"
     */
    default ReExpression ge(ArExpression y) {
        if (y.primitive().isPresent()) {
            return ge(y.primitive().getAsInt());
        }
        return new BiReExpression(ReExpression.Operator.GE, this, y);
    }

    /**
     * @param y an int
     * @return return the expression "x =/= y" where this is "x"
     */
    default ReExpression ne(int y) {
        return new UnCReExpression(ReExpression.Operator.NE, this, y);
    }

    /**
     * @param y an expression
     * @return return the expression "x =/= y" where this is "x"
     */
    default ReExpression ne(ArExpression y) {
        if (y.primitive().isPresent()) {
            return ne(y.primitive().getAsInt());
        }
        return new BiReExpression(ReExpression.Operator.NE, this, y);
    }

    /**
     * @param y an int
     * @return return the expression "x = y" where this is "x"
     */
    default UnCReExpression eq(int y) {
        return new UnCReExpression(ReExpression.Operator.EQ, this, y);
    }

    /**
     * @param y an expression
     * @return return the expression "x = y" where this is "x"
     */
    default ReExpression eq(ArExpression y) {
        if (y.primitive().isPresent()) {
            return eq(y.primitive().getAsInt());
        }
        return new BiReExpression(ReExpression.Operator.EQ, this, y);
    }

    /**
     * @param ys some expressions
     * @return return the expression "x = y_1 = y_2 = ..." where this is "x"
     */
    default ReExpression eq(ArExpression... ys) {
        return new NaReExpression(ReExpression.Operator.EQ, this, ys);
    }

    /**
     * @param ys some ints
     * @return return the expression "(x = y_1) or (x = y_) or ..." where this is "x"
     */
    default ReExpression in(int... ys) {
        return new NaReExpression(ReExpression.Operator.IN, this, Arrays.stream(ys).mapToObj(y -> getModel().intVar(y)).toArray(IntVar[]::new));
    }

    /**
     * @param ys some expressions
     * @return return the expression "(x = y_1) or (x = y_) or ..." where this is "x"
     */
    default ReExpression in(ArExpression... ys) {
        return new NaReExpression(ReExpression.Operator.IN, this, ys);
    }

    /**
     * @param ys some ints
     * @return return the expression "(x = y_1) or (x = y_) or ..." where this is "x"
     */
    default ReExpression notin(int... ys) {
        return new NaReExpression(ReExpression.Operator.NIN, this, Arrays.stream(ys).mapToObj(y -> getModel().intVar(y)).toArray(IntVar[]::new));
    }

    /**
     * @param ys some expressions
     * @return return the expression "(x = y_1) or (x = y_) or ..." where this is "x"
     */
    default ReExpression notin(ArExpression... ys) {
        return new NaReExpression(ReExpression.Operator.NIN, this, ys);
    }

    class IntPrimitive implements ArExpression {

        final int v;
        final Model m;

        public IntPrimitive(int value, Model model) {
            this.v = value;
            this.m = model;
        }

        @Override
        public OptionalInt primitive() {
            return OptionalInt.of(v);
        }

        @Override
        public Model getModel() {
            return m;
        }

        @Override
        public IntVar intVar() {
            return m.intVar(v);
        }

        @Override
        public boolean isExpressionLeaf() {
            return true;
        }

        @Override
        public void extractVar(HashSet<IntVar> variables) {
            variables.add(this.intVar());
        }

        @Override
        public int ieval(int[] values, Map<IntVar, Integer> map) {
            return v;
        }

        @Override
        public String toString() {
            return Integer.toString(v);
        }
    }
}
