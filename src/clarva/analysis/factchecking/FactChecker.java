package clarva.analysis.factchecking;

import clarva.analysis.facts.Fact;

import java.util.Set;

//interface to model checker
public abstract class FactChecker {
    //we then need a way to transform each fact into their needed declarations and the formula that needs to be checked
    public abstract Set<String> getDeclarations(Fact fact);

    public abstract Set<String> getFormulas(Fact fact);


    //OBEYS:
    //conjunction of facts =/=> false
    //conjunction of facts satisfiable
    //DESCR: Here connect to the model checker, get each declaration of each fact,
    //       and check that the respective formulas are satisfiable together
    public abstract boolean viable(Set<Fact> facts);
}
