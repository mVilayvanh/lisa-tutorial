package it.unive.lisa.tutorial;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.lattices.InverseSetLattice;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class LinearInequalitiesAmongThreeVariables
    extends FunctionalLattice<LinearInequalitiesAmongThreeVariables, Identifier, LinearInequalitiesAmongThreeVariables.SetOfPairIdentifiers>
    implements ValueDomain<LinearInequalitiesAmongThreeVariables> {

    public LinearInequalitiesAmongThreeVariables(SetOfPairIdentifiers lattice, Map<Identifier, SetOfPairIdentifiers> function) {
        super(lattice, function);
    }

    public LinearInequalitiesAmongThreeVariables() {
        super(new SetOfPairIdentifiers(Collections.emptySet(), true));
    }

    @Override
    public SetOfPairIdentifiers stateOfUnknown(Identifier key) {
        return new SetOfPairIdentifiers(Collections.emptySet(), true);
    }

    @Override
    public LinearInequalitiesAmongThreeVariables mk(SetOfPairIdentifiers lattice, Map<Identifier, SetOfPairIdentifiers> function) {
        return new LinearInequalitiesAmongThreeVariables(lattice, function);
    }

    @Override
    public LinearInequalitiesAmongThreeVariables top() {
        return new LinearInequalitiesAmongThreeVariables(lattice.top(), null);
    }

    @Override
    public LinearInequalitiesAmongThreeVariables bottom() {
        return new LinearInequalitiesAmongThreeVariables(lattice.bottom(), null);
    }

    @Override
    public boolean lessOrEqual(LinearInequalitiesAmongThreeVariables other) throws SemanticException {
        if (this.isBottom()) return true;
        if (other.isTop()) return true;
        if (this.isTop()) return other.isTop();
        if (other.isBottom()) return this.isBottom();
        for (Map.Entry<Identifier, SetOfPairIdentifiers> entry : this.function.entrySet()) {
            Identifier id = entry.getKey();
            SetOfPairIdentifiers thisVal = entry.getValue();
            SetOfPairIdentifiers otherVal = other.getState(id);
            if (!thisVal.lessOrEqual(otherVal))
                return false;
        }
        return true;
    }

    @Override
    public LinearInequalitiesAmongThreeVariables lub(
        LinearInequalitiesAmongThreeVariables other)
        throws SemanticException {
        if (this.isTop() || this.isBottom()) return this;
        if (this.isTop() || other.isBottom()) return this;
        if (other.isTop() || this.isBottom()) return other;
        LinearInequalitiesAmongThreeVariables result = this;
        Set<Identifier> allKeys = new HashSet<>();
        if (this.function != null) allKeys.addAll(this.function.keySet());
        if (other.function != null) allKeys.addAll(other.function.keySet());
        for (Identifier id : allKeys) {
            SetOfPairIdentifiers thisVal = this.getState(id);
            SetOfPairIdentifiers otherVal = other.getState(id);
            result = result.putState(id, thisVal.lub(otherVal));
        }
        return result;
    }

    @Override
    public LinearInequalitiesAmongThreeVariables assign(
        Identifier id,
        ValueExpression expression,
        ProgramPoint pp,
        SemanticOracle oracle)
        throws SemanticException {
        if (this.isTop() || this.isBottom()) return this;
        LinearInequalitiesAmongThreeVariables result = this;
        result = verifyIdAmongOtherKey(id, result);
        result = result.forgetIdentifier(id);
        if (expression instanceof BinaryExpression sum
            && sum.getOperator() instanceof AdditionOperator
            && sum.getLeft() instanceof Identifier y
            && sum.getRight() instanceof Identifier z) {

            PairIdentifiers pair = new PairIdentifiers(y, z);
            result = result.putState(id,
                new SetOfPairIdentifiers(Collections.singleton(pair), false));
        }

        return result;
    }

    @Override
    public LinearInequalitiesAmongThreeVariables smallStepSemantics(ValueExpression expression, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        return this;
    }

    @Override
    public LinearInequalitiesAmongThreeVariables assume(
        ValueExpression expression,
        ProgramPoint src,
        ProgramPoint dest,
        SemanticOracle oracle) throws SemanticException {

        LinearInequalitiesAmongThreeVariables ret = this;
        if (expression instanceof BinaryExpression cmp) {
            if (cmp.getOperator() instanceof ComparisonGe &&
                cmp.getLeft() instanceof Identifier left &&
                cmp.getRight() instanceof BinaryExpression sum &&
                sum.getOperator() instanceof AdditionOperator &&
                sum.getLeft() instanceof Identifier y &&
                sum.getRight() instanceof Identifier z) {
                PairIdentifiers pair = new PairIdentifiers(y, z);
                SetOfPairIdentifiers value = ret.getState(left);
                if (value.isTop())
                    value = new SetOfPairIdentifiers(Collections.singleton(pair), true);
                else {
                    Set<PairIdentifiers> val = new HashSet<>(value.elements());
                    val.add(pair);
                    value = new SetOfPairIdentifiers(val, false);
                }
                ret = ret.putState(left, value);
            }
        }
        return ret;
    }

    @Override
    public boolean knowsIdentifier(Identifier id) {
        return function.containsKey(id);
    }

    @Override
    public LinearInequalitiesAmongThreeVariables forgetIdentifier(Identifier id)
        throws SemanticException {
        if (this.isTop() || this.isBottom()) return this;
        LinearInequalitiesAmongThreeVariables ret = this;
        if (this.function.containsKey(id)) {
            ret = ret.putState(id, lattice.top());
        }
        ret = verifyIdAmongOtherKey(id, ret);
        return ret;
    }

    private LinearInequalitiesAmongThreeVariables verifyIdAmongOtherKey(Identifier id, LinearInequalitiesAmongThreeVariables ret) {
        for (Identifier key : this.function.keySet()) {
            if (key.equals(id)) continue;
            SetOfPairIdentifiers val = this.getState(key);
            if (!val.isTop() && !val.isBottom()) {
                Set<PairIdentifiers> filtered = new HashSet<>();
                for (PairIdentifiers p : val.elements()) {
                    if (!p.first().equals(id) && !p.second().equals(id))
                        filtered.add(p);
                }
                ret = ret.putState(key,
                    new SetOfPairIdentifiers(filtered, filtered.isEmpty()));
            }
        }
        return ret;
    }

    @Override
    public LinearInequalitiesAmongThreeVariables forgetIdentifiersIf(Predicate<Identifier> test) throws SemanticException {
        return this;
    }

    @Override
    public Satisfiability satisfies(ValueExpression expression, ProgramPoint pp, SemanticOracle oracle)
        throws SemanticException {
        if (this.isBottom()) return Satisfiability.BOTTOM;
        if (expression instanceof BinaryExpression cmp
            && cmp.getOperator() instanceof ComparisonGe
            && cmp.getLeft() instanceof Identifier x
            && cmp.getRight() instanceof BinaryExpression sum
            && sum.getOperator() instanceof AdditionOperator
            && sum.getLeft() instanceof Identifier y
            && sum.getRight() instanceof Identifier z) {

            SetOfPairIdentifiers pairs = this.getState(x);
            if (!pairs.isTop() && !pairs.isBottom()) {
                PairIdentifiers seeking = new PairIdentifiers(y, z);
                if (pairs.elements().contains(seeking))
                    return Satisfiability.SATISFIED;
            }
        }
        return Satisfiability.UNKNOWN;
    }

    @Override
    public LinearInequalitiesAmongThreeVariables pushScope(ScopeToken token) throws SemanticException {
        return this;
    }

    @Override
    public LinearInequalitiesAmongThreeVariables popScope(ScopeToken token) throws SemanticException {
        return this;
    }

    public record PairIdentifiers(Identifier first, Identifier second) {

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;

            if (!(o instanceof PairIdentifiers(Identifier first1, Identifier second1)))
                return false;

            return first.equals(first1) && second.equals(second1);
        }

        @Override
        public String toString() {
            return "(" + first + "," + second + ")";
        }
    }

    public static class SetOfPairIdentifiers
        extends InverseSetLattice<SetOfPairIdentifiers, PairIdentifiers> {

        public SetOfPairIdentifiers(Set<PairIdentifiers> elements, boolean isTop) {
            super(elements, isTop);
        }

        @Override
        public SetOfPairIdentifiers mk(Set<PairIdentifiers> set) {
            return new SetOfPairIdentifiers(set, set.isEmpty());
        }

        @Override
        public SetOfPairIdentifiers top() {
            return new SetOfPairIdentifiers(Collections.emptySet(), true);
        }

        @Override
        public SetOfPairIdentifiers bottom() {
            return new SetOfPairIdentifiers(Collections.emptySet(), false);
        }
    }
}
