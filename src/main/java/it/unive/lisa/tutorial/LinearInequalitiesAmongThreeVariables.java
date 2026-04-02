package it.unive.lisa.tutorial;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.lattices.InverseSetLattice;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.util.representation.StructuredRepresentation;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class LinearInequalitiesAmongThreeVariables
    extends FunctionalLattice<LinearInequalitiesAmongThreeVariables, Identifier, LinearInequalitiesAmongThreeVariables.SetOfIdentifiers>
    implements ValueDomain<LinearInequalitiesAmongThreeVariables> {

    public LinearInequalitiesAmongThreeVariables(SetOfIdentifiers lattice, Map<Identifier, SetOfIdentifiers> function) {
        super(lattice, function);
    }

    @Override
    public SetOfIdentifiers stateOfUnknown(Identifier key) {
        return new SetOfIdentifiers(Collections.emptySet(), true);
    }

    @Override
    public LinearInequalitiesAmongThreeVariables mk(SetOfIdentifiers lattice, Map<Identifier, SetOfIdentifiers> function) {
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
    public LinearInequalitiesAmongThreeVariables assume(ValueExpression expression, ProgramPoint src, ProgramPoint dest, SemanticOracle oracle) throws SemanticException {
        return null;
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

    public static class PairIdentifiers {
        Identifier left;
        Identifier right;

    }

    public static class SetOfIdentifiers extends InverseSetLattice<SetOfIdentifiers, Identifier> {
        /**
         * Builds the lattice.
         *
         * @param elements the elements that are contained in the lattice
         * @param isTop    whether or not this is the top or bottom element of the
         *                 lattice, valid only if the set of elements is empty
         */
        public SetOfIdentifiers(Set<Identifier> elements, boolean isTop) {
            super(elements, isTop);
        }

        @Override
        public SetOfIdentifiers mk(Set<Identifier> set) {
            return new SetOfIdentifiers(set, set.isEmpty());
        }

        @Override
        public SetOfIdentifiers top() {
            return this.mk(Collections.emptySet());
        }

        @Override
        public SetOfIdentifiers bottom() {
            return new SetOfIdentifiers(Collections.emptySet(), false);
        }
    }
}
