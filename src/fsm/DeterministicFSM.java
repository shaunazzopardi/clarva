package fsm;

import java.util.*;
import java.util.Map.Entry;

public class DeterministicFSM<T, S> extends FSM<Set<State<T, S>>, S> {

//	public Set<State<Set<State<T,S>>>> states;
//	public State<Set<State<T,S>>> initial;
//	public Set<Transition> transitions;

    public State<Set<State<T, S>>, S> initialState;

//	public Set<State<Set<State<T,S>>>> finalStates;

//	public Set<Action> alphabet;

    public DeterministicFSM() {
        super();
    }

    public DeterministicFSM(fsm.DeterministicFSM<T, S> fsm) {
        super(fsm);

//		initialState = this.labelToState.get(fsm.initialState.label);
//		initial.add(initialState);
//	//	states.add(initialState);
//
//		for(Transition<Set<State<T,S>>,S> transition : fsm.transitions){
//			this.addTransition(transition.source, transition.action, transition.destination);
//		}
//
//
//		for(State<Set<State<T,S>>,S> state : fsm.finalStates){
//			State<Set<State<T,S>>,S> finalState = this.labelToState.get(state.label);
//			finalStates.add(finalState);
//		}
    }

    public DeterministicFSM(FSM<T, S> fsm) {

        super();

        Set<State<T, S>> originalSet = fsm.states;

        Set<State<T, S>> initialLabels = new HashSet<State<T, S>>();

        initialState = new State<Set<State<T, S>>, S>(fsm.initial, null, this);
        initial.add(initialState);

        this.resolveOutgoingTransitionsOf(initialState);

        //	finalStates = new HashSet<State<Set<State<T,S>>>>();

        //fsm.finalStates);

        for (State<Set<State<T, S>>, S> st : states) {
            if (Collections.disjoint(fsm.finalStates, st.label)) {
                finalStates.add(st);
            }
        }
    }

    private static <T> Set<Set<T>> powerSet(Set<T> originalSet) {
        Set<Set<T>> sets = new HashSet<Set<T>>();
        if (originalSet.isEmpty()) {
            sets.add(new HashSet<T>());
            return sets;
        }
        List<T> list = new ArrayList<T>(originalSet);
        T head = list.get(0);
        Set<T> rest = new HashSet<T>(list.subList(1, list.size()));
        for (Set<T> set : powerSet(rest)) {
            Set<T> newSet = new HashSet<T>();
            newSet.add(head);
            newSet.addAll(set);
            sets.add(newSet);
            sets.add(set);
        }
        return sets;
    }

/*	private Set<State<Set<T>>> statesPowerSet(Set<State<T,S>> originalSet) {

		Set<Set<State<T,S>>> statesPowerSet = powerSet(originalSet);

		Set<State<Set<T>>> states = new HashSet<State<Set<T>>>();

		Map<State<Set<T>>, Map<Action, Set<State<T,S>>>> stateToOutgoingTransitions = new HashMap<State<Set<T>>, Map<Action, Set<State<T,S>>>>();

		HashMap<Set<State<T,S>>, State<Set<T>>> setToStatesMap = new HashMap<Set<State<T,S>>, State<Set<T>>>();

		for(Set<State<T,S>> setOfStates : statesPowerSet){

			State<Set<T>> newState;
			Map<Action, Set<State<T,S>>> outgoingTransitions = new HashMap<Action, Set<State<T,S>>>();

			Set<T> ts = new HashSet<T>();
			for(State<T,S> state : setOfStates){
				ts.add(state.state);

				outgoingTransitions.putAll(state.outgoingTransitions);
				//transitions?
			}

			newState = new State<Set<T>>(ts);

			stateToOutgoingTransitions.put(newState, outgoingTransitions);

			states.add(newState);

			setToStatesMap.put(setOfStates, newState);
		}

		for(Set<State<T,S>> setOfStates : statesPowerSet){
			State sourceState = setToStatesMap.get(setOfStates);


			Map<Action, Set<State<Set<T>>>> newOutgoingTs = new HashMap<Action, Set<State<Set<T>>>>();
			Map<Action, Set<State<T,S>>> outgoingTs = stateToOutgoingTransitions.get(setToStatesMap.get(setOfStates));

			for(Entry<Action, Set<State<T,S>>> entry : outgoingTs.entrySet()){

				State<Set<T>> destinationState = setToStatesMap.get(entry.getValue());

				HashSet<State<Set<T>>> destinationStateSet = new HashSet<State<Set<T>>>();

				destinationStateSet.add(setToStatesMap.get(entry.getValue()));

				newOutgoingTs.put(entry.getKey(), destinationStateSet);

				transitions.add(new Transition(sourceState, destinationState, entry.getKey()));
			}

			setToStatesMap.get(setOfStates).addOutgoingTransitions(newOutgoingTs);
		}

		return states;
	}*/

    public State<Set<State<T, S>>, S> stateWithLabelsInStateSet(Set<State<T, S>> labels, Set<State<Set<State<T, S>>, S>> states) {
        for (State<Set<State<T, S>>, S> state : states) {
            if (state.label.equals(labels)) {
                return state;
            }
        }

        State<Set<State<T, S>>, S> newState = new State<Set<State<T, S>>, S>(labels, null, this);
        //states.add(newState);
        return newState;
    }

    private boolean actuallyContains(Set<State<Set<State<T, S>>, S>> states, State<Set<State<T, S>>, S> state) {
        for (State<Set<State<T, S>>, S> st : states) {
            if (st.equals(state)) {
                return true;
            }
        }

        return false;
    }

    private void resolveOutgoingTransitionsOf(State<Set<State<T, S>>, S> state) {

        //Set<Set<State<T,S>>> statesPowerSet = powerSet(originalSet);

        if (!actuallyContains(states, state)) {
            this.states.add(state);
        } else {
            return;
        }

        for (State<T, S> oldState : state.label) {

            for (Entry<Event<S>, Set<T>> entry : oldState.outgoingTransitions.entrySet()) {
                Set<State<T, S>> oldStatesLabels = new HashSet<>();

                for (T label : entry.getValue()) {
                    oldStatesLabels.add(oldState.parent.labelToState.get(label));
                }

                State<Set<State<T, S>>, S> newDest = this.stateWithLabelsInStateSet(oldStatesLabels, states);

                state.addOutgoingTransition(entry.getKey(), newDest);

                transitions.add(new Transition<Set<State<T, S>>, S>(state, newDest, entry.getKey()));

                resolveOutgoingTransitionsOf(newDest);
            }
        }
    }

    public fsm.DeterministicFSM<Set<State<T, S>>, S> reverse() {

        Map<State<Set<State<T, S>>, S>, State<Set<State<T, S>>, S>> oldToNew = new HashMap<State<Set<State<T, S>>, S>, State<Set<State<T, S>>, S>>();

        Set<Transition<Set<State<T, S>>, S>> newTransitions = new HashSet<Transition<Set<State<T, S>>, S>>();

        Set<State<Set<State<T, S>>, S>> newStates = new HashSet<State<Set<State<T, S>>, S>>();

        for (State<Set<State<T, S>>, S> state : states) {
            State<Set<State<T, S>>, S> newState = new State<Set<State<T, S>>, S>(state.label, null, this);

            newStates.add(newState);

            oldToNew.put(state, newState);
        }

        for (State<Set<State<T, S>>, S> state : states) {

//			Map<Event<S>, Set<State<Set<State<T,S>>,S>>> revOutgoingTransitions = state.outgoingTransitions;

            State<Set<State<T, S>>, S> newState = oldToNew.get(state);

            for (Entry<Event<S>, Set<Set<State<T, S>>>> entry : state.outgoingTransitions.entrySet()) {
                for (Set<State<T, S>> destOldStateLabel : entry.getValue()) {

                    State<Set<State<T, S>>, S> destOldState = state.parent.labelToState.get(destOldStateLabel);
                    State<Set<State<T, S>>, S> sourceNewState = oldToNew.get(destOldState);

                    sourceNewState.addOutgoingTransition(entry.getKey(), newState);

                    newTransitions.add(new Transition<Set<State<T, S>>, S>(sourceNewState, newState, entry.getKey()));
                }
            }
        }

        Set<State<Set<State<T, S>>, S>> newInitial = new HashSet<State<Set<State<T, S>>, S>>();

        for (State<Set<State<T, S>>, S> finalSt : finalStates) {
            newInitial.add(oldToNew.get(finalSt));
        }

        Set<State<Set<State<T, S>>, S>> newFinal = new HashSet<State<Set<State<T, S>>, S>>();

        newFinal.add(this.initialState);

        FSM<Set<State<T, S>>, S> reversed = new FSM<Set<State<T, S>>, S>(newStates, newTransitions, newFinal, newInitial);

        return new fsm.DeterministicFSM<Set<State<T, S>>, S>(reversed);
    }

    public fsm.DeterministicFSM<T, S> total() {
        fsm.DeterministicFSM<T, S> totalFSM = new fsm.DeterministicFSM<T, S>(this);

        State<Set<State<T, S>>, S> sinkState = new State<Set<State<T, S>>, S>((Set<State<T, S>>) null, null, totalFSM);

        for (State<Set<State<T, S>>, S> state : totalFSM.states) {
            Set<Event<S>> actionsForSink = new HashSet<Event<S>>(totalFSM.alphabet);
            actionsForSink.removeAll(state.outgoingTransitions.entrySet());

            for (Event<S> action : actionsForSink) {
                state.addOutgoingTransition(action, sinkState);
            }
        }

        return totalFSM;
    }
}
