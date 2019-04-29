package clarva.analysis.cfg;

import clarva.analysis.facts.Fact;

import java.util.*;

public class CFGState<St> {
    St statement;
    Set<Fact> invariants;

    public CFGState(){
    }

    public CFGState(St statement){
        this.statement = statement;
    }

    public void setInvariants(Set<Fact> invariants){
        this.invariants = invariants;
    }
}

