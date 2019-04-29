package clarva.java.factanalysis;

import clarva.analysis.facts.FactCreator;
import clarva.analysis.facts.Facts;
import soot.Unit;

public class JavaFactCreator implements FactCreator<Unit> {
    @Override
    public Facts extract(Unit unit) {
        return null;
    }
//    @Override
//    public Facts extract(Unit unit) {
//        if(unit.getClass().equals(Jre))
//    }
}
