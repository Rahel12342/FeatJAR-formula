package de.featjar.formula.analysis.value;

import de.featjar.base.computation.IComputation;
import de.featjar.base.tree.structure.ITree;
import de.featjar.formula.analysis.VariableMap;
import de.featjar.formula.analysis.bool.BooleanClause;

public class ComputeValueRepresentationOfClause extends AValueRepresentationComputation<BooleanClause, ValueClause> {
    public ComputeValueRepresentationOfClause(
            IComputation<BooleanClause> booleanRepresentation, IComputation<VariableMap> variableMap) {
        super(booleanRepresentation, variableMap);
    }

    @Override
    public ITree<IComputation<?>> cloneNode() {
        return new ComputeValueRepresentationOfClause(getInput(), getVariableMap());
    }
}
