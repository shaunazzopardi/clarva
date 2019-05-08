package clarva.analysis.factchecking;

import clarva.analysis.cfg.CFG;
import clarva.analysis.facts.Fact;

public class AnnotateCFG<Statement, F extends Fact> {
    public void annotateCFG(CFG cfg) {
        //for each if statement
        //  tag its previous and next state by its condition as facts
        //  transition forward and tag states by condition until a statement is executed that can affect the condition
        //
    }

    public boolean canAffectFact(Statement st, Fact fact) {
        return true;
    }

    public Fact factify(Statement st) {
        return null;
    }
}
