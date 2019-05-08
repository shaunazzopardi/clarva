package clarva.analysis.factchecking;

import clarva.analysis.facts.Fact;

import java.util.Set;

public class Z3FactChecker extends FactChecker {
    @Override
    public Set<String> getDeclarations(Fact fact) {
        return null;
    }

    @Override
    public Set<String> getFormulas(Fact fact) {
        return null;
    }

    @Override
    public boolean viable(Set<Fact> facts) {
        return false;
    }
}
