package it.unive.lisa.tutorial;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

import java.util.Objects;

public class IntervalsWithOverflow implements BaseNonRelationalValueDomain<IntervalsWithOverflow> {

    public static final IntervalsWithOverflow TOP =
            new IntervalsWithOverflow(Integer.MIN_VALUE, Integer.MAX_VALUE, false);

    public static final IntervalsWithOverflow BOTTOM =
            new IntervalsWithOverflow(0, 0, true);

    public static final IntervalsWithOverflow ZERO =
            new IntervalsWithOverflow(0, 0, false);

    private final int low;
    private final int high;
    private final boolean isBottom;

    public IntervalsWithOverflow() {
        this(Integer.MIN_VALUE, Integer.MAX_VALUE, false);
    }

    public IntervalsWithOverflow(int low, int high) {
        this(low, high, false);
    }

    private IntervalsWithOverflow(int low, int high, boolean isBottom) {
        this.low = low;
        this.high = high;
        this.isBottom = isBottom;
    }

    public boolean isCircular() {
        return !isBottom && low > high;
    }

    @Override
    public int hashCode() {
        return Objects.hash(low, high, isBottom);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof IntervalsWithOverflow))
            return false;
        IntervalsWithOverflow other = (IntervalsWithOverflow) o;
        return low == other.low && high == other.high && isBottom == other.isBottom;
    }

    @Override
    public IntervalsWithOverflow top() {
        return TOP;
    }

    @Override
    public IntervalsWithOverflow bottom() {
        return BOTTOM;
    }

    @Override
    public boolean isTop() {
        return !isBottom && low == Integer.MIN_VALUE && high == Integer.MAX_VALUE;
    }

    @Override
    public boolean isBottom() {
        return isBottom;
    }

    @Override
    public StructuredRepresentation representation() {
        if (isBottom)
            return Lattice.bottomRepresentation();
        return new StringRepresentation("[" + low + ", " + high + "]");
    }

    @Override
    public boolean lessOrEqualAux(IntervalsWithOverflow other) throws SemanticException {
        if (this.isBottom)
            return true;
        if (other.isBottom)
            return false;
        if (other.equals(TOP))
            return true;
        if (this.equals(other))
            return true;

        if (!this.isCircular() && !other.isCircular())
            return other.low <= this.low && this.high <= other.high;
        return false;
    }

    @Override
    public IntervalsWithOverflow lubAux(IntervalsWithOverflow other) throws SemanticException {
        if (this.isBottom)
            return other;
        if (other.isBottom)
            return this;

        if (this.equals(other))
            return this;

        if (this.isCircular() || other.isCircular())
            return TOP;

        int newLow = Math.min(this.low, other.low);
        int newHigh = Math.max(this.high, other.high);
        return new IntervalsWithOverflow(newLow, newHigh);
    }

    @Override
    public IntervalsWithOverflow glbAux(IntervalsWithOverflow other) throws SemanticException {
        if (this.isBottom || other.isBottom)
            return BOTTOM;

        if (!this.isCircular() && !other.isCircular()) {
            int newLow = Math.max(this.low, other.low);
            int newHigh = Math.min(this.high, other.high);
            if (newLow > newHigh)
                return BOTTOM;
            return new IntervalsWithOverflow(newLow, newHigh);
        }

        return BOTTOM;
    }

    @Override
    public IntervalsWithOverflow wideningAux(IntervalsWithOverflow other) throws SemanticException {
        if (this.isBottom)
            return other;
        if (other.isBottom)
            return this;

        if (this.isCircular() || other.isCircular())
            return TOP;

        int newLow = other.low < this.low ? Integer.MIN_VALUE : this.low;
        int newHigh = other.high > this.high ? Integer.MAX_VALUE : this.high;

        return new IntervalsWithOverflow(newLow, newHigh);
    }

    @Override
    public IntervalsWithOverflow evalNonNullConstant(
            Constant constant,
            ProgramPoint pp,
            SemanticOracle oracle) {

        if (constant.getValue() instanceof Integer) {
            int v = (Integer) constant.getValue();
            return new IntervalsWithOverflow(v, v);
        }

        return TOP;
    }

    @Override
    public IntervalsWithOverflow evalUnaryExpression(
            UnaryOperator operator,
            IntervalsWithOverflow arg,
            ProgramPoint pp,
            SemanticOracle oracle) {

        if (arg.isBottom)
            return BOTTOM;

        if (operator == NumericNegation.INSTANCE) {
            long newLow = -(long) arg.high;
            long newHigh = -(long) arg.low;

            if (!fitsInt(newLow) || !fitsInt(newHigh))
                return TOP;

            return new IntervalsWithOverflow((int) newLow, (int) newHigh);
        }

        return TOP;
    }

    @Override
    public IntervalsWithOverflow evalBinaryExpression(
            BinaryOperator operator,
            IntervalsWithOverflow left,
            IntervalsWithOverflow right,
            ProgramPoint pp,
            SemanticOracle oracle) {

        if (left.isBottom || right.isBottom)
            return BOTTOM;

        if (left.isCircular() || right.isCircular())
            return TOP;

        if (operator instanceof AdditionOperator) {
            long newLow = (long) left.low + right.low;
            long newHigh = (long) left.high + right.high;

            if (!fitsInt(newLow) || !fitsInt(newHigh))
                return TOP;

            return new IntervalsWithOverflow((int) newLow, (int) newHigh);
        } else if (operator instanceof SubtractionOperator) {
            long newLow = (long) left.low - right.high;
            long newHigh = (long) left.high - right.low;

            if (!fitsInt(newLow) || !fitsInt(newHigh))
                return TOP;

            return new IntervalsWithOverflow((int) newLow, (int) newHigh);
        } else if (operator instanceof MultiplicationOperator) {
            long p1 = (long) left.low * right.low;
            long p2 = (long) left.low * right.high;
            long p3 = (long) left.high * right.low;
            long p4 = (long) left.high * right.high;

            long min = Math.min(Math.min(p1, p2), Math.min(p3, p4));
            long max = Math.max(Math.max(p1, p2), Math.max(p3, p4));

            if (!fitsInt(min) || !fitsInt(max))
                return TOP;

            return new IntervalsWithOverflow((int) min, (int) max);
        } else if (operator instanceof DivisionOperator) {
            if (right.low <= 0 && 0 <= right.high)
                return TOP;

            long d1 = left.low / right.low;
            long d2 = left.low / right.high;
            long d3 = left.high / right.low;
            long d4 = left.high / right.high;

            long min = Math.min(Math.min(d1, d2), Math.min(d3, d4));
            long max = Math.max(Math.max(d1, d2), Math.max(d3, d4));

            if (!fitsInt(min) || !fitsInt(max))
                return TOP;

            return new IntervalsWithOverflow((int) min, (int) max);
        }

        return TOP;
    }

    private static boolean fitsInt(long value) {
        return value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE;
    }
}