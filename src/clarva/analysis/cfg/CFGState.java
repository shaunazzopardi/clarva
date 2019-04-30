package clarva.analysis.cfg;

import clarva.analysis.facts.Fact;
import com.sun.org.apache.xpath.internal.operations.Bool;

import java.util.*;

public class CFGState<St> {
    public St statement;
    Set<Fact> invariants;

    public CFGState(){
    }

    public CFGState(St statement){
        this.statement = statement;
    }

    public void setInvariants(Set<Fact> invariants){
        this.invariants = invariants;
    }

    @Override
    public boolean equals(Object cfgState){
        if(cfgState.getClass().equals(this.getClass())){
            if(((CFGState) cfgState).statement.equals(this.statement)) {
                return true;
            } else {
                return false;
            }
        } else{
            return false;
        }
    }

    @Override
    public int hashCode(){
        return statement.hashCode();
    }
}

