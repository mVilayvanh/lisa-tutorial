package it.unive.lisa.tutorial;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

/**
 * Wrapped interval domain over 32-bit signed integers.
 *
 * <p>
 * This domain models machine integers with modular arithmetic (Java int semantics).
 * An element [low, high] is interpreted as:
 * </p>
 * <ul>
 *   <li>a standard interval if low <= high</li>
 *   <li>a wrapped interval crossing the modular boundary if low > high</li>
 * </ul>
 *
 * <p>
 * Example: [Integer.MAX_VALUE - 1, Integer.MIN_VALUE + 2] is a valid wrapped interval.
 * </p>
 *
 * <p>
 * This is a sound but intentionally simple implementation: when an arithmetic result
 * cannot be represented precisely by a single wrapped interval, the domain safely
 * over-approximates it, possibly returning TOP.
 * </p>
 */
public class IntervalsWithOverflow implements BaseNonRelationalValueDomain<IntervalsWithOverflow> {

    public static final IntervalsWithOverflow TOP =
            new IntervalsWithOverflow(0, -1, false, true);

    public static final IntervalsWithOverflow BOTTOM =
            new IntervalsWithOverflow(0, 0, true, false);

    public static final IntervalsWithOverflow ZERO =
            new IntervalsWithOverflow(0, 0, false, false);

    private final int low;
    private final int high;
    private final boolean isBottom;
    private final boolean isTop;

    public IntervalsWithOverflow() {
        this(0, -1, false, true);
    }

    public IntervalsWithOverflow(int low, int high) {
        this(low, high, false, false);
    }

    private IntervalsWithOverflow(int low, int high, boolean isBottom, boolean isTop) {
        this.low = low;
        this.high = high;
        this.isBottom = isBottom;
        this.isTop = isTop;
    }

    public int getLow() {
        return low;
    }

    public int getHigh() {
        return high;
    }

    public boolean isWrapped() {
        return !isBottom && !isTop && low > high;
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
        return isTop;
    }

    @Override
    public boolean isBottom() {
        return isBottom;
    }

    @Override
    public StructuredRepresentation representation() {
        if (isBottom)
            return Lattice.bottomRepresentation();
        if (isTop)
            return Lattice.topRepresentation();
        return new StringRepresentation("[" + low + ", " + high + "]");
    }

    @Override
    public int hashCode() {
        return Objects.hash(low, high, isBottom, isTop);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof IntervalsWithOverflow))
            return false;
        IntervalsWithOverflow other = (IntervalsWithOverflow) o;
        return low == other.low
                && high == other.high
                && isBottom == other.isBottom
                && isTop == other.isTop;
    }

    /**
     * Returns true if the interval contains the concrete value v.
     */
    public boolean contains(int v) {
        if (isBottom)
            return false;
        if (isTop)
            return true;
        if (!isWrapped())
            return low <= v && v <= high;
        return v >= low || v <= high;
    }

    /**
     * Returns the concrete segments representing this interval on the standard signed line.
     *
     * <p>
     * A non-wrapped interval is returned as one segment.
     * A wrapped interval [low, high] is returned as two segments:
     * [low, Integer.MAX_VALUE] and [Integer.MIN_VALUE, high].
     * </p>
     */
    private List<Segment> toSegments() {
        List<Segment> result = new ArrayList<>();
        if (isBottom)
            return result;
        if (isTop) {
            result.add(new Segment(Integer.MIN_VALUE, Integer.MAX_VALUE));
            return result;
        }

        if (!isWrapped()) {
            result.add(new Segment(low, high));
        } else {
            result.add(new Segment(low, Integer.MAX_VALUE));
            result.add(new Segment(Integer.MIN_VALUE, high));
        }
        return result;
    }

    @Override
    public boolean lessOrEqualAux(IntervalsWithOverflow other) throws SemanticException {
        if (this.isBottom)
            return true;
        if (other.isTop)
            return true;
        if (other.isBottom)
            return this.isBottom;
        if (this.isTop)
            return other.isTop;

        for (Segment s : this.toSegments()) {
            for (long v : s.endpointsAsLongs()) {
                if (!other.contains((int) v))
                    return false;
            }
        }

        if (!this.isWrapped()) {
            return other.contains(this.low) && other.contains(this.high);
        }

        return other.contains(this.low)
                && other.contains(this.high)
                && other.contains(Integer.MIN_VALUE)
                && other.contains(Integer.MAX_VALUE);
    }

    @Override
    public IntervalsWithOverflow lubAux(IntervalsWithOverflow other) throws SemanticException {
        if (this.isBottom)
            return other;
        if (other.isBottom)
            return this;
        if (this.isTop || other.isTop)
            return TOP;
        if (this.equals(other))
            return this;

        if (other.lessOrEqual(this))
            return this;

        if (this.lessOrEqual(other))
            return other;

        List<Integer> points = new ArrayList<>();
        addBoundaryPoints(points, this);
        addBoundaryPoints(points, other);

        return smallestCoveringInterval(points);
    }

    @Override
    public IntervalsWithOverflow glbAux(IntervalsWithOverflow other) throws SemanticException {
        if (this.isBottom || other.isBottom)
            return BOTTOM;
        if (this.isTop)
            return other;
        if (other.isTop)
            return this;

        List<Segment> intersections = new ArrayList<>();
        for (Segment s1 : this.toSegments()) {
            for (Segment s2 : other.toSegments()) {
                Segment inter = s1.intersection(s2);
                if (inter != null)
                    intersections.add(inter);
            }
        }

        if (intersections.isEmpty())
            return BOTTOM;

        List<Segment> merged = mergeSegments(intersections);

        if (merged.size() == 1) {
            Segment s = merged.get(0);
            return new IntervalsWithOverflow(s.low, s.high);
        }

        if (merged.size() == 2) {
            Segment first = merged.get(0);
            Segment second = merged.get(1);

            if (first.low == Integer.MIN_VALUE && second.high == Integer.MAX_VALUE) {
                return new IntervalsWithOverflow(second.low, first.high);
            }
        }

        return TOP;
    }

    @Override
    public IntervalsWithOverflow wideningAux(IntervalsWithOverflow other) throws SemanticException {
        if (this.isBottom)
            return other;
        if (other.isBottom)
            return this;
        if (this.isTop || other.isTop)
            return TOP;

        if (other.lessOrEqual(this))
            return this;

        if (this.equals(other))
            return this;

        // Conservative fallback for wrapped intervals
        if (this.isWrapped() || other.isWrapped())
            return TOP;

        int newLow = this.low;
        int newHigh = this.high;

        if (other.low < this.low)
            newLow = Integer.MIN_VALUE;

        if (other.high > this.high) {
            long growth = (long) other.high - this.high;

            // Small progressive growth is kept precise
            if (growth <= 1L)
                newHigh = other.high;
            else
                newHigh = Integer.MAX_VALUE;
        }

        if (newLow == Integer.MIN_VALUE && newHigh == Integer.MAX_VALUE)
            return TOP;

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
        if (arg.isTop)
            return TOP;

        if (operator == NumericNegation.INSTANCE) {
            return mapUnary(arg, v -> -v);
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
        if (left.isTop || right.isTop)
            return TOP;

        if (operator instanceof AdditionOperator) {
            return mapBinary(left, right, (a, b) -> a + b);
        } else if (operator instanceof SubtractionOperator) {
            return mapBinary(left, right, (a, b) -> a - b);
        } else if (operator instanceof MultiplicationOperator) {
            return mapBinary(left, right, (a, b) -> a * b);
        } else if (operator instanceof DivisionOperator) {
            if (right.contains(0))
                return TOP;
            return mapBinaryDivision(left, right);
        }

        return TOP;
    }

    /**
     * Evaluates the satisfiability of comparison operators.
     *
     * <p>
     * This implementation is intentionally conservative. Equality/inequality are handled
     * through the intersection of operands. Ordering comparisons are handled precisely
     * only on non-wrapped intervals; wrapped cases safely return UNKNOWN.
     * </p>
     */
    @Override
    public Satisfiability satisfiesBinaryExpression(
            BinaryOperator operator,
            IntervalsWithOverflow left,
            IntervalsWithOverflow right,
            ProgramPoint pp,
            SemanticOracle oracle) {

        if (left.isBottom() || right.isBottom())
            return Satisfiability.NOT_SATISFIED;

        if (left.isTop() || right.isTop())
            return Satisfiability.UNKNOWN;

        if (operator == ComparisonEq.INSTANCE) {
            try {
                IntervalsWithOverflow inter = left.glb(right);
                if (inter.isBottom())
                    return Satisfiability.NOT_SATISFIED;

                if (left.isSingleton() && right.isSingleton()
                        && left.getLow() == right.getLow())
                    return Satisfiability.SATISFIED;

                return Satisfiability.UNKNOWN;
            } catch (SemanticException e) {
                return Satisfiability.UNKNOWN;
            }
        }

        if (operator == ComparisonNe.INSTANCE) {
            try {
                IntervalsWithOverflow inter = left.glb(right);
                if (inter.isBottom())
                    return Satisfiability.SATISFIED;

                if (left.isSingleton() && right.isSingleton()
                        && left.getLow() == right.getLow())
                    return Satisfiability.NOT_SATISFIED;

                return Satisfiability.UNKNOWN;
            } catch (SemanticException e) {
                return Satisfiability.UNKNOWN;
            }
        }

        if (operator == ComparisonGe.INSTANCE)
            return satisfiesBinaryExpression(ComparisonLe.INSTANCE, right, left, pp, oracle);

        if (operator == ComparisonGt.INSTANCE)
            return satisfiesBinaryExpression(ComparisonLt.INSTANCE, right, left, pp, oracle);

        if (operator == ComparisonLe.INSTANCE) {
            if (!left.isWrapped() && !right.isWrapped()) {
                if (left.getHigh() <= right.getLow())
                    return Satisfiability.SATISFIED;
                if (left.getLow() > right.getHigh())
                    return Satisfiability.NOT_SATISFIED;
            }
            return Satisfiability.UNKNOWN;
        }

        if (operator == ComparisonLt.INSTANCE) {
            if (!left.isWrapped() && !right.isWrapped()) {
                if (left.getHigh() < right.getLow())
                    return Satisfiability.SATISFIED;
                if (left.getLow() >= right.getHigh())
                    return Satisfiability.NOT_SATISFIED;
            }
            return Satisfiability.UNKNOWN;
        }

        return Satisfiability.UNKNOWN;
    }

    /**
     * Refines the environment after assuming a comparison on a variable.
     *
     * <p>
     * This refinement is conservative. Equality is handled through intersection, while
     * ordering comparisons are refined only against non-wrapped bounds. Wrapped bounds
     * safely produce no refinement.
     * </p>
     */
    @Override
    public ValueEnvironment<IntervalsWithOverflow> assumeBinaryExpression(
            ValueEnvironment<IntervalsWithOverflow> environment,
            BinaryOperator operator,
            ValueExpression left,
            ValueExpression right,
            ProgramPoint src,
            ProgramPoint dest,
            SemanticOracle oracle)
            throws SemanticException {

        Identifier id;
        IntervalsWithOverflow eval;
        boolean idIsLeft;

        if (left instanceof Identifier) {
            id = (Identifier) left;
            eval = eval(right, environment, src, oracle);
            idIsLeft = true;
        } else if (right instanceof Identifier) {
            id = (Identifier) right;
            eval = eval(left, environment, src, oracle);
            idIsLeft = false;
        } else {
            return environment;
        }

        IntervalsWithOverflow starting = environment.getState(id);

        if (starting.isBottom() || eval.isBottom())
            return environment.bottom();

        IntervalsWithOverflow update = refineByComparison(starting, operator, eval, idIsLeft);

        if (update == null)
            return environment;

        if (update.isBottom())
            return environment.bottom();

        return environment.putState(id, update);
    }

    /**
     * Applies a unary operator to representative endpoints of the interval and rebuilds
     * a sound wrapped interval approximation.
     */
    private static IntervalsWithOverflow mapUnary(
            IntervalsWithOverflow arg,
            IntUnaryModOperator op) {

        List<Integer> images = new ArrayList<>();
        for (int p : representativePoints(arg)) {
            images.add(op.apply(p));
        }
        return fromPoints(images);
    }

    /**
     * Applies a binary modular operator to representative endpoints of the operands and
     * rebuilds a sound wrapped interval approximation.
     */
    private static IntervalsWithOverflow mapBinary(
            IntervalsWithOverflow left,
            IntervalsWithOverflow right,
            IntBinaryModOperator op) {

        List<Integer> images = new ArrayList<>();
        for (int a : representativePoints(left)) {
            for (int b : representativePoints(right)) {
                images.add(op.apply(a, b));
            }
        }
        return fromPoints(images);
    }

    /**
     * Division is handled separately because Java modular division differs from +,-,*:
     * it throws away cases involving zero divisor, and MIN_VALUE / -1 overflows to MIN_VALUE.
     */
    private static IntervalsWithOverflow mapBinaryDivision(
            IntervalsWithOverflow left,
            IntervalsWithOverflow right) {

        List<Integer> images = new ArrayList<>();
        for (int a : representativePoints(left)) {
            for (int b : representativePoints(right)) {
                if (b == 0)
                    continue;
                images.add(a / b);
            }
        }

        if (images.isEmpty())
            return TOP;

        return fromPoints(images);
    }

    /**
     * Returns representative points used to build a sound coarse approximation.
     *
     * <p>
     * For a standard interval: endpoints.
     * For a wrapped interval: low, high, MIN_VALUE, MAX_VALUE.
     * </p>
     */
    private static List<Integer> representativePoints(IntervalsWithOverflow i) {
        List<Integer> pts = new ArrayList<>();
        if (i.isBottom)
            return pts;
        if (i.isTop) {
            pts.add(Integer.MIN_VALUE);
            pts.add(Integer.MAX_VALUE);
            pts.add(0);
            return pts;
        }

        pts.add(i.low);
        pts.add(i.high);

        if (i.isWrapped()) {
            pts.add(Integer.MIN_VALUE);
            pts.add(Integer.MAX_VALUE);
        }

        return dedup(pts);
    }

    /**
     * Builds the smallest standard interval or wrapped interval covering the given points.
     * If no single wrapped interval is clearly better than TOP, returns TOP.
     */
    private static IntervalsWithOverflow fromPoints(List<Integer> points) {
        points = dedup(points);

        if (points.isEmpty())
            return BOTTOM;
        if (points.size() == 1) {
            int v = points.get(0);
            return new IntervalsWithOverflow(v, v);
        }

        Segment hull = lineHull(points);

        int circularGapStart = 0;
        long bestGap = Long.MIN_VALUE;

        List<Long> ordered = new ArrayList<>();
        for (int p : points)
            ordered.add(unsignedKey(p));
        ordered.sort(Long::compare);

        for (int i = 0; i < ordered.size(); i++) {
            long cur = ordered.get(i);
            long next = ordered.get((i + 1) % ordered.size());
            long gap = (i + 1 < ordered.size())
                    ? next - cur
                    : (1L << 32) - cur + next;

            if (gap > bestGap) {
                bestGap = gap;
                circularGapStart = i;
            }
        }

        long start = ordered.get((circularGapStart + 1) % ordered.size());
        long end = ordered.get(circularGapStart);

        int wrappedLow = fromUnsignedKey(start);
        int wrappedHigh = fromUnsignedKey(end);

        IntervalsWithOverflow wrapped = new IntervalsWithOverflow(wrappedLow, wrappedHigh);
        IntervalsWithOverflow standard = new IntervalsWithOverflow(hull.low, hull.high);

        for (int p : points) {
            if (!wrapped.contains(p))
                return standard;
        }

        long stdSize = hull.size();
        long wrapSize = wrappedApproxSize(wrapped);

        if (wrapSize < stdSize)
            return wrapped;
        return standard;
    }

    /**
     * Adds the boundary points that characterize an interval.
     */
    private static void addBoundaryPoints(List<Integer> points, IntervalsWithOverflow i) {
        if (i.isBottom || i.isTop)
            return;

        points.add(i.low);
        points.add(i.high);

        if (i.isWrapped()) {
            points.add(Integer.MIN_VALUE);
            points.add(Integer.MAX_VALUE);
        }
    }

    /**
     * Builds the smallest wrapped/non-wrapped interval covering all given points on the
     * 32-bit modular circle.
     */
    private static IntervalsWithOverflow smallestCoveringInterval(List<Integer> points) {
        points = dedup(points);

        if (points.isEmpty())
            return BOTTOM;

        if (points.size() == 1) {
            int v = points.get(0);
            return new IntervalsWithOverflow(v, v);
        }

        List<Long> ordered = new ArrayList<>();
        for (int p : points)
            ordered.add(unsignedKey(p));
        ordered.sort(Long::compare);

        long bestGap = Long.MIN_VALUE;
        int gapIndex = -1;

        for (int i = 0; i < ordered.size(); i++) {
            long cur = ordered.get(i);
            long next = ordered.get((i + 1) % ordered.size());
            long gap = (i + 1 < ordered.size())
                    ? next - cur
                    : (1L << 32) - cur + next;

            if (gap > bestGap) {
                bestGap = gap;
                gapIndex = i;
            }
        }

        long start = ordered.get((gapIndex + 1) % ordered.size());
        long end = ordered.get(gapIndex);

        int newLow = fromUnsignedKey(start);
        int newHigh = fromUnsignedKey(end);

        return new IntervalsWithOverflow(newLow, newHigh);
    }

    private static long wrappedApproxSize(IntervalsWithOverflow i) {
        if (i.isTop)
            return 1L << 32;
        if (i.isBottom)
            return 0;
        if (!i.isWrapped())
            return (long) i.high - i.low + 1L;

        long highPart = (long) Integer.MAX_VALUE - i.low + 1L;
        long lowPart = (long) i.high - Integer.MIN_VALUE + 1L;
        return highPart + lowPart;
    }

    private static Segment lineHull(List<Integer> points) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int p : points) {
            min = Math.min(min, p);
            max = Math.max(max, p);
        }
        return new Segment(min, max);
    }

    private static List<Integer> dedup(List<Integer> input) {
        List<Integer> out = new ArrayList<>();
        for (int x : input) {
            if (!out.contains(x))
                out.add(x);
        }
        return out;
    }

    private static List<Segment> mergeSegments(List<Segment> segments) {
        if (segments.isEmpty())
            return segments;

        segments.sort((a, b) -> {
            int c = Integer.compare(a.low, b.low);
            if (c != 0)
                return c;
            return Integer.compare(a.high, b.high);
        });

        List<Segment> merged = new ArrayList<>();
        Segment cur = segments.get(0);

        for (int i = 1; i < segments.size(); i++) {
            Segment next = segments.get(i);
            if ((long) next.low <= (long) cur.high + 1L) {
                cur = new Segment(cur.low, Math.max(cur.high, next.high));
            } else {
                merged.add(cur);
                cur = next;
            }
        }

        merged.add(cur);
        return merged;
    }

    /**
     * Returns true if this abstract value is a singleton interval [v, v].
     */
    private boolean isSingleton() {
        return !isBottom && !isTop && low == high;
    }

    /**
     * Safely increments a machine integer without leaving the 32-bit signed range.
     */
    private static int safeIncrement(int x) {
        return x == Integer.MAX_VALUE ? Integer.MAX_VALUE : x + 1;
    }

    /**
     * Safely decrements a machine integer without leaving the 32-bit signed range.
     */
    private static int safeDecrement(int x) {
        return x == Integer.MIN_VALUE ? Integer.MIN_VALUE : x - 1;
    }

    /**
     * Computes a conservative refinement of {@code starting} after assuming a comparison
     * against {@code eval}.
     *
     * <p>
     * Refinement is performed only when it can be expressed safely with the current
     * wrapped-interval abstraction. Wrapped bounds are conservatively ignored.
     * </p>
     */
    private static IntervalsWithOverflow refineByComparison(
            IntervalsWithOverflow starting,
            BinaryOperator operator,
            IntervalsWithOverflow eval,
            boolean idIsLeft)
            throws SemanticException {

        if (starting.isTop() && eval.isTop())
            return null;

        if (operator == ComparisonEq.INSTANCE) {
            return starting.glb(eval);
        }

        if (operator == ComparisonNe.INSTANCE) {
            if (starting.isSingleton() && eval.isSingleton()
                    && starting.getLow() == eval.getLow())
                return BOTTOM;

            return null;
        }

        if (operator == ComparisonGe.INSTANCE) {
            return refineByComparison(starting, ComparisonLe.INSTANCE, eval, !idIsLeft);
        }

        if (operator == ComparisonGt.INSTANCE) {
            return refineByComparison(starting, ComparisonLt.INSTANCE, eval, !idIsLeft);
        }

        if (eval.isWrapped()) {
            return null;
        }

        if (operator == ComparisonLe.INSTANCE) {
            if (idIsLeft) {
                IntervalsWithOverflow bound =
                        new IntervalsWithOverflow(Integer.MIN_VALUE, eval.getHigh());
                return starting.glb(bound);
            } else {
                IntervalsWithOverflow bound =
                        new IntervalsWithOverflow(eval.getLow(), Integer.MAX_VALUE);
                return starting.glb(bound);
            }
        }

        if (operator == ComparisonLt.INSTANCE) {
            if (idIsLeft) {
                if (eval.getHigh() == Integer.MIN_VALUE)
                    return BOTTOM;

                IntervalsWithOverflow bound =
                        new IntervalsWithOverflow(Integer.MIN_VALUE, safeDecrement(eval.getHigh()));
                return starting.glb(bound);
            } else {
                if (eval.getLow() == Integer.MAX_VALUE)
                    return BOTTOM;

                IntervalsWithOverflow bound =
                        new IntervalsWithOverflow(safeIncrement(eval.getLow()), Integer.MAX_VALUE);
                return starting.glb(bound);
            }
        }

        return null;
    }

    private static long unsignedKey(int x) {
        return Integer.toUnsignedLong(x);
    }

    private static int fromUnsignedKey(long x) {
        return (int) x;
    }

    private interface IntUnaryModOperator {
        int apply(int x);
    }

    private interface IntBinaryModOperator {
        int apply(int x, int y);
    }

    private static final class Segment {
        private final int low;
        private final int high;

        private Segment(int low, int high) {
            this.low = low;
            this.high = high;
        }

        private Segment intersection(Segment other) {
            int l = Math.max(this.low, other.low);
            int h = Math.min(this.high, other.high);
            if (l > h)
                return null;
            return new Segment(l, h);
        }

        private List<Long> endpointsAsLongs() {
            List<Long> pts = new ArrayList<>();
            pts.add((long) low);
            pts.add((long) high);
            return pts;
        }

        private long size() {
            return (long) high - low + 1L;
        }
    }
}