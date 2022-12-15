package de.featjar.formula.analysis.value;

import de.featjar.base.data.Result;
import de.featjar.formula.analysis.Solution;
import de.featjar.formula.analysis.Solver;
import de.featjar.formula.analysis.bool.BooleanClause;
import de.featjar.formula.analysis.bool.BooleanSolution;
import de.featjar.formula.analysis.bool.VariableMap;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A (partial) value solution; that is, a conjunction of equalities.
 * Often holds output of an SMT {@link Solver}.
 *
 * @author Elias Kuiter
 */
public class ValueSolution extends ValueAssignment implements Solution<String> {
    public ValueSolution() {
    }

    public ValueSolution(LinkedHashMap<String, Object> variableValuePairs) {
        super(variableValuePairs);
    }

    public ValueSolution(ValueClause predicateClause) {
        this(new LinkedHashMap<>(predicateClause.variableValuePairs));
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    protected ValueSolution clone() {
        return toSolution();
    }

    @Override
    public Result<BooleanSolution> toBoolean(VariableMap variableMap) {
        return variableMap.toBoolean(this);
    }

    @Override
    public String toString() {
        return String.format("ValueSolution[%s]", print());
    }
}