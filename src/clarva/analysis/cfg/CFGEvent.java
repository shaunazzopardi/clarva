package clarva.analysis.cfg;

import clarva.java.JavaEvent;
import fsm.date.events.DateEvent;
import fsm.date.events.MethodCall;

public abstract class CFGEvent implements Cloneable {
    public DateEvent dateEvent;
    //taking transition implies condition holds
    // on both incoming and outgoing states assuming
    //computing conditions has no effect on state
    public boolean epsilon = false;

    public boolean outsideEvent = false;

    public CFGEvent() {
    }

    public CFGEvent(DateEvent dateEvent) {
        this.dateEvent = dateEvent;
    }

    public abstract <T extends CFGEvent> T abstractVersion();
}
