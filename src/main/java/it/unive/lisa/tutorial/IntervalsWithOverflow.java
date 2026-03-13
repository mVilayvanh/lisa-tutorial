package it.unive.lisa.tutorial;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.util.representation.StructuredRepresentation;

public class IntervalsWithOverflow implements BaseNonRelationalValueDomain<IntervalsWithOverflow> {
    @Override
    public IntervalsWithOverflow lubAux(IntervalsWithOverflow other) throws SemanticException {
        return null;
    }

    @Override
    public boolean lessOrEqualAux(IntervalsWithOverflow other) throws SemanticException {
        return false;
    }

    @Override
    public IntervalsWithOverflow top() {
        return null;
    }

    @Override
    public IntervalsWithOverflow bottom() {
        return null;
    }

    @Override
    public StructuredRepresentation representation() {
        return null;
    }
}
