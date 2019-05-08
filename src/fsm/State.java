package fsm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class State<T, S> {

    public T label;

    public Map<Event<S>, Set<T>> outgoingTransitions;
    public Map<Event<S>, Set<T>> incomingTransitions;
    public FSM<T, S> parent;
    //Encapsulated FSM
    private FSM<T, S> internalFSM;

//	public State(T label){
//		this.state = label;
//		this.outgoingTransitions = new HashMap<Action<S>, Set<State<T,S>>>();
//		this.incomingTransitions = new HashMap<Action<S>, Set<State<T,S>>>();
//	}

    public State(T label, FSM<T, S> internalFSM, FSM<T, S> parent) {
        this.label = label;
        this.outgoingTransitions = new HashMap<Event<S>, Set<T>>();
        this.incomingTransitions = new HashMap<Event<S>, Set<T>>();
        this.internalFSM = internalFSM;
        this.parent = parent;
    }

    public State(fsm.State<T, S> s, FSM<T, S> internalFSM, FSM<T, S> parent) {
        this.label = s.label;
        this.outgoingTransitions = new HashMap<Event<S>, Set<T>>();
        this.incomingTransitions = new HashMap<Event<S>, Set<T>>();
        this.internalFSM = internalFSM;
        this.parent = parent;

        for (Event<S> event : s.outgoingTransitions.keySet()) {
            for (T stateLabel : s.outgoingTransitions.get(event)) {
                this.addOutgoingTransition(event, parent.getOrAddState(stateLabel));
            }
        }

        for (Event<S> event : s.incomingTransitions.keySet()) {
            for (T stateLabel : s.incomingTransitions.get(event)) {
                this.addIncomingTransition(event, parent.getOrAddState(stateLabel));
            }
        }

    }

    public String toString() {
        return label.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass().equals(fsm.State.class)) {
            if (((fsm.State) obj).label.equals(this.label)) {
                return true;
            } else return false;
        } else return false;
    }


    @Override
    public int hashCode() {
        if (label == null) return 0;
        else {
            int hash = label.hashCode();
            if (hash >= 0) return hash + 1;
            else return hash;
        }
    }

    public void removeOutgoingTransition(Event<S> action, fsm.State<T, S> state) {
        if ((outgoingTransitions.containsKey(action)
                && outgoingTransitions.get(action).contains(state.label))) {

            outgoingTransitions.get(action).remove(state.label);
        }
    }

    public void removeIncomoingTransition(Event<S> action, fsm.State<T, S> state) {
        if ((incomingTransitions.containsKey(action)
                && incomingTransitions.get(action).contains(state.label))) {

            incomingTransitions.get(action).remove(state.label);
        }
    }

    public void addOutgoingTransition(Event<S> action, fsm.State<T, S> state) {
        if (!(outgoingTransitions.containsKey(action)
                && outgoingTransitions.get(action).contains(state.label))) {

            Set<T> destinationLabels = outgoingTransitions.get(action);
            if (destinationLabels == null)
                destinationLabels = new HashSet<T>();

            destinationLabels.add(state.label);

            outgoingTransitions.put(action, destinationLabels);
        }
    }

    public void addOutgoingTransition(Event<S> action, Set<fsm.State<T, S>> states) {
        Set<T> destinationLabels;
        if (!(outgoingTransitions.containsKey(action))) {
            destinationLabels = new HashSet<T>();
        } else {
            destinationLabels = outgoingTransitions.get(action);
        }

        for (fsm.State<T, S> state : states) {
            destinationLabels.add(state.label);
        }

        outgoingTransitions.put(action, destinationLabels);
    }

//	public void addOutgoingTransitions(Map<Event<S>, Set<State<T,S>>> transitions){
//		outgoingTransitions.putAll(transitions);
//	}

    public void addIncomingTransition(Event<S> action, fsm.State<T, S> state) {
        if (!(incomingTransitions.containsKey(action)
                && incomingTransitions.get(action).contains(state.label))) {

            Set<T> destinationLabels = incomingTransitions.get(action);
            if (destinationLabels == null)
                destinationLabels = new HashSet<T>();

            destinationLabels.add(state.label);

            incomingTransitions.put(action, destinationLabels);
        }
    }

    public void addIncomingTransition(Event<S> action, Set<fsm.State<T, S>> states) {
        Set<T> destinationLabels;

        if (!(incomingTransitions.containsKey(action))) {
            destinationLabels = new HashSet<T>();
        } else {
            destinationLabels = incomingTransitions.get(action);
        }

        for (fsm.State<T, S> state : states) {
            destinationLabels.add(state.label);
        }

        incomingTransitions.put(action, destinationLabels);
    }

    //
    public FSM<T, S> getInternalFSM() {
        return internalFSM;
    }

    //	public void addIncomingTransitions(Map<Event<S>,Set<State<T,S>>> transitions){
//		incomingTransitions.putAll(transitions);
//	}
//
    public void setInternalFSM(FSM<T, S> internalFSM) {
        this.internalFSM = internalFSM;
        this.parent.internalFSMs.add(internalFSM);
    }

}
