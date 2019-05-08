package clarva.analysis.cfg;

import fsm.date.events.DateEvent;

public abstract class CFGEvent {
    public DateEvent dateEvent;
    //taking transition implies condition holds
    // on both incoming and outgoing states assuming
    //computing conditions has no effect on state
    public boolean epsilon = false;

    public CFGEvent() {
    }

    public CFGEvent(DateEvent dateEvent) {
        this.dateEvent = dateEvent;
    }
}
