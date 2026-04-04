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
import it.unive.lisa.util.representation.StructuredRepresentation;
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
        return null;
    }

    @Override
    public LinearInequalitiesAmongThreeVariables bottom() {
        return null;
    }

    @Override
    public boolean lessOrEqual(LinearInequalitiesAmongThreeVariables other) throws SemanticException {
        return false;
    }

    @Override
    public LinearInequalitiesAmongThreeVariables lub(LinearInequalitiesAmongThreeVariables other) throws SemanticException {
        return null;
    }

    @Override
    public LinearInequalitiesAmongThreeVariables assign(Identifier id, ValueExpression expression, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        return null;
    }

    @Override
    public LinearInequalitiesAmongThreeVariables smallStepSemantics(ValueExpression expression, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        return null;
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
        return false;
    }

    @Override
    public LinearInequalitiesAmongThreeVariables forgetIdentifier(Identifier id) throws SemanticException {
        return null;
    }

    @Override
    public LinearInequalitiesAmongThreeVariables forgetIdentifiersIf(Predicate<Identifier> test) throws SemanticException {
        return null;
    }

    @Override
    public Satisfiability satisfies(ValueExpression expression, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        return null;
    }

    @Override
    public LinearInequalitiesAmongThreeVariables pushScope(ScopeToken token) throws SemanticException {
        return this;
    }

    @Override
    public LinearInequalitiesAmongThreeVariables popScope(ScopeToken token) throws SemanticException {
        return this;
    }

    @Override
    public StructuredRepresentation representation() {
        return null;
    }

    public record PairIdentifiers(Identifier first, Identifier second) {

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;

            if (!(o instanceof PairIdentifiers other))
                return false;

            return first.equals(other.first) && second.equals(other.second);
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
