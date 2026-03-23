package it.unive.lisa.tutorial;

import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

    // -------------------------
    // Lattice operations
    // -------------------------

    @Test
    public void testLub() throws SemanticException {
        IntervalsWithOverflow a = new IntervalsWithOverflow(1, 1);
        IntervalsWithOverflow b = new IntervalsWithOverflow(10, 10);

        assertEquals(new IntervalsWithOverflow(1, 10), a.lub(b));
    }

    @Test
    public void testLessOrEqual() throws SemanticException {
        IntervalsWithOverflow small = new IntervalsWithOverflow(3, 5);
        IntervalsWithOverflow big = new IntervalsWithOverflow(1, 10);

        assertTrue(small.lessOrEqual(big));
    }

    @Test
    public void testGlb() throws SemanticException {
        IntervalsWithOverflow a = new IntervalsWithOverflow(1, 5);
        IntervalsWithOverflow b = new IntervalsWithOverflow(3, 10);

        assertEquals(new IntervalsWithOverflow(3, 5), a.glb(b));
    }

    @Test
    public void testGlbDisjointGivesBottom() throws SemanticException {
        IntervalsWithOverflow a = new IntervalsWithOverflow(1, 2);
        IntervalsWithOverflow b = new IntervalsWithOverflow(5, 6);

        assertEquals(IntervalsWithOverflow.BOTTOM, a.glb(b));
    }

    @Test
    public void testWidening() throws SemanticException {
        IntervalsWithOverflow a = new IntervalsWithOverflow(1, 5);
        IntervalsWithOverflow b = new IntervalsWithOverflow(0, 10);

        assertEquals(IntervalsWithOverflow.TOP, a.widening(b));
    }

    // -------------------------
    // Wrapped interval behavior
    // -------------------------

    @Test
    public void testWrappedContains() {
        IntervalsWithOverflow wrapped = new IntervalsWithOverflow(10, 5);

        assertTrue(wrapped.isWrapped());
        assertTrue(wrapped.contains(10));
        assertTrue(wrapped.contains(Integer.MAX_VALUE));
        assertTrue(wrapped.contains(Integer.MIN_VALUE));
        assertTrue(wrapped.contains(0));
        assertTrue(wrapped.contains(5));
        assertFalse(wrapped.contains(7));
    }

    @Test
    public void testWrappedLessOrEqualTop() throws SemanticException {
        IntervalsWithOverflow wrapped = new IntervalsWithOverflow(10, 5);
        assertTrue(wrapped.lessOrEqual(IntervalsWithOverflow.TOP));
    }

    @Test
    public void testWrappedGlbStandard() throws SemanticException {
        IntervalsWithOverflow wrapped = new IntervalsWithOverflow(10, 5);
        IntervalsWithOverflow standard = new IntervalsWithOverflow(0, 3);

        assertEquals(new IntervalsWithOverflow(0, 3), wrapped.glb(standard));
    }

    @Test
    public void testWrappedLubWithContainedStandard() throws SemanticException {
        IntervalsWithOverflow wrapped = new IntervalsWithOverflow(10, 5);
        IntervalsWithOverflow standard = new IntervalsWithOverflow(0, 3);

        assertEquals(wrapped, wrapped.lub(standard));
    }

    // -------------------------
    // Arithmetic - normal cases
    // -------------------------

    @Test
    public void testAdditionNoOverflow() throws SemanticException {
        IntervalsWithOverflow a = new IntervalsWithOverflow(1, 3);
        IntervalsWithOverflow b = new IntervalsWithOverflow(2, 4);

        assertEquals(new IntervalsWithOverflow(3, 7),
                a.evalBinaryExpression(ADD, a, b, null, null));
    }

    @Test
    public void testSubtractionNoOverflow() throws SemanticException {
        IntervalsWithOverflow a = new IntervalsWithOverflow(5, 8);
        IntervalsWithOverflow b = new IntervalsWithOverflow(2, 3);

        assertEquals(new IntervalsWithOverflow(2, 6),
                a.evalBinaryExpression(SUB, a, b, null, null));
    }

    @Test
    public void testMultiplicationNoOverflow() throws SemanticException {
        IntervalsWithOverflow a = new IntervalsWithOverflow(2, 4);
        IntervalsWithOverflow b = new IntervalsWithOverflow(3, 5);

        assertEquals(new IntervalsWithOverflow(6, 20),
                a.evalBinaryExpression(MUL, a, b, null, null));
    }

    @Test
    public void testDivisionNoOverflow() throws SemanticException {
        IntervalsWithOverflow a = new IntervalsWithOverflow(8, 12);
        IntervalsWithOverflow b = new IntervalsWithOverflow(2, 3);

        assertEquals(new IntervalsWithOverflow(2, 6),
                a.evalBinaryExpression(DIV, a, b, null, null));
    }

    @Test
    public void testNegationNoOverflow() throws SemanticException {
        IntervalsWithOverflow a = new IntervalsWithOverflow(1, 3);

        assertEquals(new IntervalsWithOverflow(-3, -1),
                a.evalUnaryExpression(NumericNegation.INSTANCE, a, null, null));
    }

    // -------------------------
    // Overflow / modular cases
    // -------------------------

    @Test
    public void testAdditionOverflowWraps() throws SemanticException {
        IntervalsWithOverflow a = new IntervalsWithOverflow(Integer.MAX_VALUE, Integer.MAX_VALUE);
        IntervalsWithOverflow b = new IntervalsWithOverflow(1, 1);

        assertEquals(new IntervalsWithOverflow(Integer.MIN_VALUE, Integer.MIN_VALUE),
                a.evalBinaryExpression(ADD, a, b, null, null));
    }

    @Test
    public void testSubtractionOverflowWraps() throws SemanticException {
        IntervalsWithOverflow a = new IntervalsWithOverflow(Integer.MIN_VALUE, Integer.MIN_VALUE);
        IntervalsWithOverflow b = new IntervalsWithOverflow(1, 1);

        assertEquals(new IntervalsWithOverflow(Integer.MAX_VALUE, Integer.MAX_VALUE),
                a.evalBinaryExpression(SUB, a, b, null, null));
    }

    @Test
    public void testMultiplicationOverflowWraps() throws SemanticException {
        IntervalsWithOverflow a = new IntervalsWithOverflow(Integer.MAX_VALUE, Integer.MAX_VALUE);
        IntervalsWithOverflow b = new IntervalsWithOverflow(2, 2);

        assertEquals(new IntervalsWithOverflow(-2, -2),
                a.evalBinaryExpression(MUL, a, b, null, null));
    }

    @Test
    public void testNegationOverflowWraps() throws SemanticException {
        IntervalsWithOverflow a = new IntervalsWithOverflow(Integer.MIN_VALUE, Integer.MIN_VALUE);

        assertEquals(new IntervalsWithOverflow(Integer.MIN_VALUE, Integer.MIN_VALUE),
                a.evalUnaryExpression(NumericNegation.INSTANCE, a, null, null));
    }

    @Test
    public void testDivisionByZeroGivesTop() throws SemanticException {
        IntervalsWithOverflow a = new IntervalsWithOverflow(1, 10);
        IntervalsWithOverflow b = new IntervalsWithOverflow(-1, 1);

        assertEquals(IntervalsWithOverflow.TOP,
                a.evalBinaryExpression(DIV, a, b, null, null));
    }

    @Test
    public void testDivisionMinByMinusOneWraps() throws SemanticException {
        IntervalsWithOverflow a = new IntervalsWithOverflow(Integer.MIN_VALUE, Integer.MIN_VALUE);
        IntervalsWithOverflow b = new IntervalsWithOverflow(-1, -1);

        assertEquals(new IntervalsWithOverflow(Integer.MIN_VALUE, Integer.MIN_VALUE),
                a.evalBinaryExpression(DIV, a, b, null, null));
    }

    // -------------------------
    // Bottom behavior
    // -------------------------

    @Test
    public void testBottomLessOrEqualEverything() throws SemanticException {
        assertTrue(IntervalsWithOverflow.BOTTOM.lessOrEqual(
                new IntervalsWithOverflow(1, 2)));
    }

    @Test
    public void testOperationWithBottomGivesBottom() throws SemanticException {
        IntervalsWithOverflow a = new IntervalsWithOverflow(1, 2);

        assertEquals(IntervalsWithOverflow.BOTTOM,
                a.evalBinaryExpression(ADD, a, IntervalsWithOverflow.BOTTOM, null, null));
    }
}