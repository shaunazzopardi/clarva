package clarva.java.factanalysis;

import clarva.analysis.facts.Fact;

import java.util.Set;

public class JavaFact implements Fact {
    String declaration;
    String assertion;

    @Override
    public boolean compatible(Set<Fact> others) {
        return false;
    }

    @Override
    public boolean incompatible(Set<Fact> others) {
        return false;
    }
}
