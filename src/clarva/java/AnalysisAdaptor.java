package clarva.java;

import clarva.analysis.MethodsAnalysis;
import fsm.date.events.DateEvent;
import fsm.date.events.MethodCall;

import java.util.ArrayList;
import java.util.List;

public class AnalysisAdaptor {
    public static List<DateEvent> allEvents(MethodsAnalysis ma){
        List<DateEvent> usedActions = new ArrayList<>();

        for(MethodCall event : ma.dateEventToSootMethods.keySet()){
            if(ma.dateEventToSootMethods.get(event) != null
                    && ma.dateEventToSootMethods.get(event).size() != 0)
                usedActions.add(event);
        }

        return usedActions;
    }
}
