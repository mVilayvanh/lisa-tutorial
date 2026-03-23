package it.unive.lisa.tutorial;

import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;

public class IntervalsWithOverflowDomainTest {

    private static final class AddOp implements BinaryOperator, AdditionOperator {
        @Override
        public Set<Type> typeInference(TypeSystem types, Set<Type> left, Set<Type> right) {
            return Collections.emptySet();
        }

        @Override
        public String toString() {
            return "+";
        }
    }

    private static final class SubOp implements BinaryOperator, SubtractionOperator {
        @Override
        public Set<Type> typeInference(TypeSystem types, Set<Type> left, Set<Type> right) {
            return Collections.emptySet();
        }

        @Override
        public String toString() {
            return "-";
        }
    }

    private static final class MulOp implements BinaryOperator, MultiplicationOperator {
        @Override
        public Set<Type> typeInference(TypeSystem types, Set<Type> left, Set<Type> right) {
            return Collections.emptySet();
        }

        @Override
        public String toString() {
            return "*";
        }
    }

    private static final class DivOp implements BinaryOperator, DivisionOperator {
        @Override
        public Set<Type> typeInference(TypeSystem types, Set<Type> left, Set<Type> right) {
            return Collections.emptySet();
        }

        @Override
        public String toString() {
            return "/";
        }
    }

    private static final BinaryOperator ADD = new AddOp();
    private static final BinaryOperator SUB = new SubOp();
    private static final BinaryOperator MUL = new MulOp();
    private static final BinaryOperator DIV = new DivOp();

    @Test
    public void testLub() throws SemanticException {
        IntervalsWithOverflow a = new IntervalsWithOverflow(1, 1);
        IntervalsWithOverflow b = new IntervalsWithOverflow(10, 10);

        IntervalsWithOverflow res = a.lub(b);

        assertEquals(new IntervalsWithOverflow(1, 10), res);
    }

    @Test
    public void testLessOrEqual() throws SemanticException {
        IntervalsWithOverflow small = new IntervalsWithOverflow(3, 5);
        IntervalsWithOverflow big = new IntervalsWithOverflow(1, 10);

        assertTrue(small.lessOrEqual(big));
    }

    @Test
    public void testAdditionNoOverflow() throws SemanticException {
        IntervalsWithOverflow a = new IntervalsWithOverflow(1, 3);
        IntervalsWithOverflow b = new IntervalsWithOverflow(2, 4);

        IntervalsWithOverflow res = a.evalBinaryExpression(ADD, a, b, null, null);

        assertEquals(new IntervalsWithOverflow(3, 7), res);
    }

    @Test
    public void testSubtractionNoOverflow() throws SemanticException {
        IntervalsWithOverflow a = new IntervalsWithOverflow(5, 8);
        IntervalsWithOverflow b = new IntervalsWithOverflow(2, 3);

        IntervalsWithOverflow res = a.evalBinaryExpression(SUB, a, b, null, null);

        assertEquals(new IntervalsWithOverflow(2, 6), res);
    }

    @Test
    public void testNegationNoOverflow() throws SemanticException {
        IntervalsWithOverflow a = new IntervalsWithOverflow(1, 3);

        IntervalsWithOverflow res = a.evalUnaryExpression(NumericNegation.INSTANCE, a, null, null);

        assertEquals(new IntervalsWithOverflow(-3, -1), res);
    }

    @Test
    public void testAdditionOverflowGivesTop() throws SemanticException {
        IntervalsWithOverflow a = new IntervalsWithOverflow(Integer.MAX_VALUE, Integer.MAX_VALUE);
        IntervalsWithOverflow b = new IntervalsWithOverflow(1, 1);

        IntervalsWithOverflow res = a.evalBinaryExpression(ADD, a, b, null, null);

        assertEquals(IntervalsWithOverflow.TOP, res);
    }

    @Test
    public void testNegationOverflowGivesTop() throws SemanticException {
        IntervalsWithOverflow a = new IntervalsWithOverflow(Integer.MIN_VALUE, Integer.MIN_VALUE);

        IntervalsWithOverflow res = a.evalUnaryExpression(NumericNegation.INSTANCE, a, null, null);

        assertEquals(IntervalsWithOverflow.TOP, res);
    }

    @Test
    public void testMultiplicationOverflowGivesTop() throws SemanticException {
        IntervalsWithOverflow a = new IntervalsWithOverflow(Integer.MAX_VALUE, Integer.MAX_VALUE);
        IntervalsWithOverflow b = new IntervalsWithOverflow(2, 2);

        IntervalsWithOverflow res = a.evalBinaryExpression(MUL, a, b, null, null);

        assertEquals(IntervalsWithOverflow.TOP, res);
    }

    @Test
    public void testDivisionByZeroGivesTop() throws SemanticException {
        IntervalsWithOverflow a = new IntervalsWithOverflow(1, 10);
        IntervalsWithOverflow b = new IntervalsWithOverflow(-1, 1);

        IntervalsWithOverflow res = a.evalBinaryExpression(DIV, a, b, null, null);

        assertEquals(IntervalsWithOverflow.TOP, res);
    }
}