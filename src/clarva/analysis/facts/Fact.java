package clarva.analysis.facts;

import java.util.Set;

public interface Fact {
    //this and others =/=> false
    boolean compatible(Set<Fact> others);

    //this and others ==> false
    boolean incompatible(Set<Fact> others);
}
