package fsm;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Set;

public class Transition<T, S> {

    public State<T, S> source;
    public State<T, S> destination;

    public Event<S> event;

//	public boolean keep;

	/*public Transition(T source, T destination, Action action){
		this.source = new State<T>(source);
		this.destination = new State<T>(destination);
		this.action = action;
		
		this.source.addOutgoingTransition(action, this.destination);
	}*/

    public Transition(State<T, S> source, State<T, S> destination, Event<S> action) {
        this.source = source;
        this.destination = destination;
        this.event = action;

        source.addOutgoingTransition(action, destination);
        destination.addIncomingTransition(action, source);
    }

    //safe remove
    public boolean remove() {
        Set<T> nextStates = this.source.outgoingTransitions.get(event);

        nextStates.remove(destination.label);

        if (nextStates.size() == 0) {
            this.source.outgoingTransitions.remove(event);
        } else {
            this.source.outgoingTransitions.put(event, nextStates);
        }

        Set<T> previousStates = this.destination.incomingTransitions.get(event);

        previousStates.remove(source.label);

        if (previousStates.size() == 0) {
            this.destination.incomingTransitions.remove(event);
        } else {
            this.destination.incomingTransitions.put(event, previousStates);
        }

        return true;
    }

    public String toString() {
        return "\"" + source + "\"" + " -> \"" + destination + "\" [label=\"" + event + "\"]";
    }

    public boolean equals(Object obj) {
        if (obj.getClass().equals(this.getClass())) {
            fsm.Transition<T, S> transition = (fsm.Transition<T, S>) obj;

            if (this.source.equals(transition.source)
                    && this.event.equals(transition.event)
                    && this.destination.equals(transition.destination)) {
                return true;
            } else return false;
        } else return false;
    }

    public int hashCode() {
        HashCodeBuilder hcb = new HashCodeBuilder(17, 37);
        hcb.append(this.source.label);
        hcb.append(this.destination.label);
        if (this.event.label != null) hcb.append(this.event.label);

        return hcb.toHashCode();
    }

}
