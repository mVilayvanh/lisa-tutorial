package it.unive.lisa.tutorial;

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
 * Domaine d'intervalles circulaires sur les entiers signés 32 bits.
 *
 * Un élément [low, high] représente :
 *   - si low <= high : { low, ..., high }
 *   - si low > high  : { low, ..., MAX } ∪ { MIN, ..., high }
 *                      (franchissement du bord modulaire, ex: MAX+1 = MIN)
 *
 * TOP    = l'ensemble de tous les entiers 32 bits (flag isTop)
 * BOTTOM = ensemble vide (flag isBottom)
 *
 * La taille d'un intervalle circulaire est toujours < 2^32.
 * Toutes les opérations arithmétiques sont modulaires: un dépassement
 * de Integer.MAX_VALUE revient à Integer.MIN_VALUE, exactement
 * comme le font les opérations Java sur int.
 */
public class IntervalsWithOverflow
    implements BaseNonRelationalValueDomain<IntervalsWithOverflow> {

    private static final int MIN = Integer.MIN_VALUE;
    private static final int MAX = Integer.MAX_VALUE;

    public static final IntervalsWithOverflow TOP =
        new IntervalsWithOverflow(0, 0, false, true);
    public static final IntervalsWithOverflow BOTTOM =
        new IntervalsWithOverflow(0, 0, true, false);

    private final int low;
    private final int high;
    private final boolean isBottom;
    private final boolean isTop;

    /** Constructeur par défaut : TOP (requis par LiSA). */
    public IntervalsWithOverflow() {
        this(0, 0, false, true);
    }

    /** Intervalle normal ou circulaire [low, high]. */
    public IntervalsWithOverflow(int low, int high) {
        this(low, high, false, false);
    }

    private IntervalsWithOverflow(int low, int high, boolean isBottom, boolean isTop) {
        this.low = low;
        this.high = high;
        this.isBottom = isBottom;
        this.isTop = isTop;
    }

    /**
     * Un intervalle est circulaire si low > high.
     * Il représente [low..MAX] ∪ [MIN..high].
     */
    public boolean isWrapped() {
        return !isBottom && !isTop && low > high;
    }

    private boolean isSingleton() {
        return !isBottom && !isTop && low == high;
    }

    /** Taille de l'intervalle sur le cercle Z/2^32Z. */
    private static long circularSize(IntervalsWithOverflow i) {
        if (i.isTop)    return 1L << 32;
        if (i.isBottom) return 0L;
        if (!i.isWrapped())
            return (long) i.high - i.low + 1L;
        // [low..MAX] ∪ [MIN..high]
        return ((long) MAX - i.low + 1L) + ((long) i.high - MIN + 1L);
    }

    /**
     * Décompose en segments linéaires sur la droite réelle.
     * Non circulaire → 1 segment. Circulaire → 2 segments.
     */
    private static int[][] toSegments(IntervalsWithOverflow i) {
        if (!i.isWrapped())
            return new int[][] {{ i.low, i.high }};
        return new int[][] {{ i.low, MAX }, { MIN, i.high }};
    }

    /** Vérifie si la valeur concrète v appartient à l'intervalle. */
    public boolean contains(int v) {
        if (isBottom) return false;
        if (isTop)    return true;
        if (!isWrapped()) return low <= v && v <= high;
        return v >= low || v <= high; // circulaire
    }

    @Override public IntervalsWithOverflow top()    { return TOP; }
    @Override public IntervalsWithOverflow bottom() { return BOTTOM; }
    @Override public boolean isTop()    { return isTop; }
    @Override public boolean isBottom() { return isBottom; }

    @Override
    public StructuredRepresentation representation() {
        if (isBottom) return Lattice.bottomRepresentation();
        if (isTop)    return Lattice.topRepresentation();
        if (isWrapped())
            return new StringRepresentation("[" + low + ", " + high + "]ₒ");
        return new StringRepresentation("[" + low + ", " + high + "]");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IntervalsWithOverflow other)) return false;
        return low == other.low && high == other.high
            && isBottom == other.isBottom && isTop == other.isTop;
    }

    @Override
    public int hashCode() {
        return Objects.hash(low, high, isBottom, isTop);
    }

    @Override
    public boolean lessOrEqualAux(IntervalsWithOverflow other) throws SemanticException {
        if (!other.contains(this.low) || !other.contains(this.high))
            return false;
        if (this.isWrapped())
            return other.contains(MIN) && other.contains(MAX);
        return true;
    }

    @Override
    public IntervalsWithOverflow lubAux(IntervalsWithOverflow other) throws SemanticException {
        if (this.equals(other))      return this;
        if (other.lessOrEqual(this)) return this;
        if (this.lessOrEqual(other)) return other;

        long[] pts = {
            toUnsigned(this.low),  toUnsigned(this.high),
            toUnsigned(other.low), toUnsigned(other.high)
        };
        java.util.Arrays.sort(pts);

        long bestGap = Long.MIN_VALUE;
        int gapIdx = 0;
        for (int i = 0; i < 4; i++) {
            long cur  = pts[i];
            long next = pts[(i + 1) % 4];
            long gap  = (i < 3) ? next - cur : (1L << 32) - cur + next;
            if (gap > bestGap) { bestGap = gap; gapIdx = i; }
        }

        int newLow  = fromUnsigned(pts[(gapIdx + 1) % 4]);
        int newHigh = fromUnsigned(pts[gapIdx]);

        IntervalsWithOverflow result = new IntervalsWithOverflow(newLow, newHigh);
        if (circularSize(result) >= (1L << 32)) return TOP;
        return result;
    }

    @Override
    public IntervalsWithOverflow glbAux(IntervalsWithOverflow other) throws SemanticException {
        if (this.equals(other))      return this;
        if (this.lessOrEqual(other)) return this;
        if (other.lessOrEqual(this)) return other;

        int[][] segs1 = toSegments(this);
        int[][] segs2 = toSegments(other);

        int resLow1 = 0, resHigh1 = -1;
        int resLow2 = 0, resHigh2 = -1;
        int count = 0;

        for (int[] s1 : segs1) {
            for (int[] s2 : segs2) {
                int l = Math.max(s1[0], s2[0]);
                int h = Math.min(s1[1], s2[1]);
                if (l <= h) {
                    if (count == 0) { resLow1 = l; resHigh1 = h; }
                    else            { resLow2 = l; resHigh2 = h; }
                    count++;
                }
            }
        }

        if (count == 0) return BOTTOM;
        if (count == 1) return new IntervalsWithOverflow(resLow1, resHigh1);

        int circLow  = (resLow1 > resLow2)   ? resLow1  : resLow2;
        int circHigh = (resHigh1 < resHigh2) ? resHigh1 : resHigh2;
        return new IntervalsWithOverflow(circLow, circHigh);
    }

    @Override
    public IntervalsWithOverflow wideningAux(IntervalsWithOverflow other) throws SemanticException {
        if (other.lessOrEqual(this)) return this;

        if (this.isWrapped() && other.isWrapped()) {
            // Pour [low..MAX] ∪ [MIN..high] :
            //   - le segment haut déborde par la gauche si other.low < this.low → élargir vers MIN
            //   - le segment bas déborde par la droite si other.high > this.high → élargir vers MAX
            int newLow  = (other.low  < this.low)  ? MIN : this.low;
            int newHigh = (other.high > this.high) ? MAX : this.high;
            if (newLow == MIN && newHigh == MAX) return TOP;
            return new IntervalsWithOverflow(newLow, newHigh);
        }

        if (this.isWrapped() || other.isWrapped()) return TOP;

        // Cas standard non circulaire (inchangé)
        int newLow  = (other.low  < this.low)  ? MIN : this.low;
        int newHigh = (other.high > this.high) ? MAX : this.high;
        if (newLow == MIN && newHigh == MAX) return TOP;
        return new IntervalsWithOverflow(newLow, newHigh);
    }

    @Override
    public IntervalsWithOverflow evalNonNullConstant(
        Constant constant, ProgramPoint pp, SemanticOracle oracle) {
        if (constant.getValue() instanceof Integer v)
            return new IntervalsWithOverflow(v, v);
        return TOP;
    }

    @Override
    public IntervalsWithOverflow evalUnaryExpression(
        UnaryOperator operator, IntervalsWithOverflow arg,
        ProgramPoint pp, SemanticOracle oracle) {

        if (operator instanceof NumericNegation) {
            // -MIN = MIN (overflow), donc si MIN ∈ arg → TOP conservatif
            if (arg.contains(MIN)) return TOP;
            return new IntervalsWithOverflow(-arg.high, -arg.low);
        }
        return TOP;
    }

    @Override
    public IntervalsWithOverflow evalBinaryExpression(
        BinaryOperator operator,
        IntervalsWithOverflow left, IntervalsWithOverflow right,
        ProgramPoint pp, SemanticOracle oracle) {

        if (operator instanceof AdditionOperator)       return add(left, right);
        if (operator instanceof SubtractionOperator)    return sub(left, right);
        if (operator instanceof MultiplicationOperator) return mul(left, right);
        if (operator instanceof DivisionOperator)       return div(left, right);
        return TOP;
    }

    /**
     * [a,b] + [c,d] = [a+c, b+d] (mod 2^32).
     *
     * La taille du résultat est size(l) + size(r) - 1 (les bornes se chevauchent
     * d'une valeur). Si elle atteint 2^32, l'intervalle couvre tout le cercle → TOP.
     * L'addition Java sur int est déjà modulaire : l'overflow est intentionnel.
     */
    private static IntervalsWithOverflow add(IntervalsWithOverflow l, IntervalsWithOverflow r) {
        // size(l) + size(r) - 1 >= 2^32  ⟺  le résultat couvre tout Z/2^32Z
        if (circularSize(l) + circularSize(r) - 1 >= (1L << 32)) return TOP;
        // Overflow Java intentionnel : arithmétique modulaire mod 2^32
        return new IntervalsWithOverflow(l.low + r.low, l.high + r.high);
    }

    /**
     * [a,b] - [c,d] = [a-d, b-c] (mod 2^32).
     *
     * Même raisonnement que l'addition : la soustraction par un intervalle de
     * taille k élargit le résultat de k-1 valeurs supplémentaires.
     */
    private static IntervalsWithOverflow sub(IntervalsWithOverflow l, IntervalsWithOverflow r) {
        if (circularSize(l) + circularSize(r) - 1 >= (1L << 32)) return TOP;
        // [a,b] - [c,d] = [a-d, b-c]  (overflow Java intentionnel)
        return new IntervalsWithOverflow(l.low - r.high, l.high - r.low);
    }

    /**
     * Multiplication : précise uniquement pour intervalles non circulaires
     * sans overflow long → int. TOP sinon.
     */
    private static IntervalsWithOverflow mul(IntervalsWithOverflow l, IntervalsWithOverflow r) {
        if (l.isWrapped() || r.isWrapped()) return TOP;

        long[] cands = {
            (long) l.low * r.low,  (long) l.low * r.high,
            (long) l.high * r.low, (long) l.high * r.high
        };
        long min = cands[0], max = cands[0];
        for (long c : cands) { min = Math.min(min, c); max = Math.max(max, c); }

        if (min < MIN || max > MAX) return TOP;
        return new IntervalsWithOverflow((int) min, (int) max);
    }

    /**
     * Division entière : précise pour intervalles non circulaires
     * sans diviseur zéro et sans le cas MIN/-1. TOP sinon.
     */
    private static IntervalsWithOverflow div(IntervalsWithOverflow l, IntervalsWithOverflow r) {
        if (r.contains(0) || l.isWrapped() || r.isWrapped()) return TOP;
        if (l.contains(MIN) && r.contains(-1)) return TOP; // MIN / -1 overflow

        int[] cands = {
            l.low / r.low,  l.low / r.high,
            l.high / r.low, l.high / r.high
        };
        int min = cands[0], max = cands[0];
        for (int c : cands) { min = Math.min(min, c); max = Math.max(max, c); }
        return new IntervalsWithOverflow(min, max);
    }

    @Override
    public Satisfiability satisfiesBinaryExpression(
        BinaryOperator operator,
        IntervalsWithOverflow left, IntervalsWithOverflow right,
        ProgramPoint pp, SemanticOracle oracle) {

        try {
            if (operator == ComparisonEq.INSTANCE) {
                if (left.glb(right).isBottom()) return Satisfiability.NOT_SATISFIED;
                if (left.isSingleton() && right.isSingleton() && left.low == right.low)
                    return Satisfiability.SATISFIED;
                return Satisfiability.UNKNOWN;
            }

            if (operator == ComparisonNe.INSTANCE) {
                if (left.glb(right).isBottom()) return Satisfiability.SATISFIED;
                if (left.isSingleton() && right.isSingleton() && left.low == right.low)
                    return Satisfiability.NOT_SATISFIED;
                return Satisfiability.UNKNOWN;
            }

            if (!left.isWrapped() && !right.isWrapped()) {
                if (operator == ComparisonLe.INSTANCE) {
                    if (left.high <= right.low)  return Satisfiability.SATISFIED;
                    if (left.low  >  right.high) return Satisfiability.NOT_SATISFIED;
                }
                if (operator == ComparisonLt.INSTANCE) {
                    if (left.high <  right.low)  return Satisfiability.SATISFIED;
                    if (left.low  >= right.high) return Satisfiability.NOT_SATISFIED;
                }
                if (operator == ComparisonGe.INSTANCE) {
                    if (left.low  >= right.high) return Satisfiability.SATISFIED;
                    if (left.high <  right.low)  return Satisfiability.NOT_SATISFIED;
                }
                if (operator == ComparisonGt.INSTANCE) {
                    if (left.low  >  right.high) return Satisfiability.SATISFIED;
                    if (left.high <= right.low)  return Satisfiability.NOT_SATISFIED;
                }
            }
        } catch (SemanticException e) { /* conservatif */ }

        return Satisfiability.UNKNOWN;
    }

    @Override
    public ValueEnvironment<IntervalsWithOverflow> assumeBinaryExpression(
        ValueEnvironment<IntervalsWithOverflow> environment,
        BinaryOperator operator,
        ValueExpression left, ValueExpression right,
        ProgramPoint src, ProgramPoint dest,
        SemanticOracle oracle) throws SemanticException {

        Identifier id;
        IntervalsWithOverflow eval;
        boolean idIsLeft;

        if (left instanceof Identifier lid) {
            id = lid;
            eval = eval(right, environment, src, oracle);
            idIsLeft = true;
        } else if (right instanceof Identifier rid) {
            id = rid;
            eval = eval(left, environment, src, oracle);
            idIsLeft = false;
        } else {
            return environment;
        }

        if (eval.isBottom()) return environment.bottom();

        IntervalsWithOverflow current = environment.getState(id);
        if (current.isBottom()) return environment.bottom();

        IntervalsWithOverflow refined = refine(current, operator, eval, idIsLeft);
        if (refined == null)    return environment;
        if (refined.isBottom()) return environment.bottom();

        return environment.putState(id, refined);
    }

    private IntervalsWithOverflow refine(
        IntervalsWithOverflow current,
        BinaryOperator op,
        IntervalsWithOverflow eval,
        boolean idIsLeft) throws SemanticException {

        if (op == ComparisonEq.INSTANCE)
            return current.glb(eval);

        if (op == ComparisonNe.INSTANCE) {
            if (current.isSingleton() && eval.isSingleton() && current.low == eval.low)
                return BOTTOM;
            return null;
        }

        if (op == ComparisonGe.INSTANCE)
            return refine(current, ComparisonLe.INSTANCE, eval, !idIsLeft);
        if (op == ComparisonGt.INSTANCE)
            return refine(current, ComparisonLt.INSTANCE, eval, !idIsLeft);

        if (eval.isWrapped()) return null;

        if (op == ComparisonLe.INSTANCE) {
            IntervalsWithOverflow bound = idIsLeft
                ? new IntervalsWithOverflow(MIN, eval.high)
                : new IntervalsWithOverflow(eval.low, MAX);
            return current.glb(bound);
        }

        if (op == ComparisonLt.INSTANCE) {
            if (idIsLeft) {
                if (eval.low == MIN) return BOTTOM;
                return current.glb(new IntervalsWithOverflow(MIN, eval.low - 1));
            } else {
                if (eval.high == MAX) return BOTTOM;
                return current.glb(new IntervalsWithOverflow(eval.high + 1, MAX));
            }
        }

        return null;
    }

    /** Convertit un int signé en long non signé pour trier sur le cercle. */
    private static long toUnsigned(int x) {
        return Integer.toUnsignedLong(x);
    }

    /** Reconvertit un long non signé en int signé. */
    private static int fromUnsigned(long x) {
        return (int) x;
    }
}