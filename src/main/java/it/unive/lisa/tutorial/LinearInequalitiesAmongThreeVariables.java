package it.unive.lisa.tutorial;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.util.representation.StructuredRepresentation;

import java.util.function.Predicate;

public class LinearInequalitiesAmongThreeVariables implements ValueDomain<LinearInequalitiesAmongThreeVariables> {
    @Override
    public boolean lessOrEqual(LinearInequalitiesAmongThreeVariables other) throws SemanticException {
        return false;
    }

    @Override
    public LinearInequalitiesAmongThreeVariables lub(LinearInequalitiesAmongThreeVariables other) throws SemanticException {
        return null;
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
}
