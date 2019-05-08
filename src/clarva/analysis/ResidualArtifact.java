package clarva.analysis;

import clarva.analysis.cfg.CFGEvent;
import fsm.Event;
import fsm.Transition;
import fsm.date.DateLabel;
import fsm.date.SubsetDate;

import java.util.Map;
import java.util.Set;

public class ResidualArtifact<T extends CFGEvent>{
    public CFGEvent shadow;
    public SubsetDate fullDate;

    public Map<Event<T>, Set<Transition<String, DateLabel>>> eventsAssociatedWithTransitions;

    public ResidualArtifact(CFGEvent shadow, SubsetDate fullDate,
                            Map<Event<T>, Set<Transition<String, DateLabel>>> eventsAssociatedWithTransitions){
        this.shadow = shadow;
        this.fullDate = fullDate;
        this.eventsAssociatedWithTransitions = eventsAssociatedWithTransitions;
    }
}