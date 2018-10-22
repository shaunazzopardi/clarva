package clarva.analysis.cfg;

import fsm.date.events.DateEvent;

public abstract class CFGEvent {
    public DateEvent dateEvent;
    public boolean epsilon = false;

    public CFGEvent(){
    }

    public CFGEvent(DateEvent dateEvent){
        this.dateEvent = dateEvent;
    }
}
