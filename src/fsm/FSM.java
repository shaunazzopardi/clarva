package fsm;

import com.google.common.collect.Sets;
import fsm.helper.Pair;

import java.util.*;

public class FSM<T, S> {

    public Set<State<T, S>> states;
    public Set<State<T, S>> initial;
    public Set<Transition<T, S>> transitions;

    public Set<State<T, S>> finalStates;

    public Set<Event<S>> alphabet;

    public Map<T, State<T, S>> labelToState;

    public HashSet<fsm.FSM<T, S>> internalFSMs;

    public boolean neverFails;

    public FSM() {
        states = new HashSet<State<T, S>>();
        transitions = new HashSet<Transition<T, S>>();
        finalStates = new HashSet<State<T, S>>();
        initial = new HashSet<State<T, S>>();
        internalFSMs = new HashSet<fsm.FSM<T, S>>();
        alphabet = new HashSet<Event<S>>();

        labelToState = new HashMap<T, State<T, S>>();

        this.neverFails = true;
    }

    public FSM(fsm.FSM<T, S> fsm) {
        states = new HashSet<State<T, S>>();
        transitions = new HashSet<Transition<T, S>>();
        finalStates = new HashSet<State<T, S>>();
        initial = new HashSet<State<T, S>>();
        internalFSMs = new HashSet<fsm.FSM<T, S>>();
        alphabet = new HashSet<Event<S>>();

        labelToState = new HashMap<T, State<T, S>>();

        for (Transition<T, S> transition : fsm.transitions) {
            this.addTransition(new State<T, S>(transition.source, null, this), transition.event, new State<T, S>(transition.destination, null, this));
        }

        for (State<T, S> state : fsm.initial) {
            this.addInitialState(new State<T, S>(state, null, this));
        }

        for (State<T, S> state : fsm.finalStates) {
            this.addFinalState(new State<T, S>(state, null, this));
        }

        this.neverFails = fsm.neverFails;
    }

    public FSM(Set<State<T, S>> states, Set<Transition<T, S>> transitions,
               Set<State<T, S>> finalStates, Set<State<T, S>> initial) {
        this.states = states;
        this.transitions = transitions;
        this.finalStates = finalStates;
        this.initial = initial;

        labelToState = new HashMap<T, State<T, S>>();
    }

    public static <T, S> fsm.FSM<T, S> deepCopy(fsm.FSM<T, S> fsm) {
        return new fsm.FSM<T, S>(fsm);
    }

    public void addTransition(State<T, S> source, Event<S> action, State<T, S> destination) {
        transitions.add(new Transition<T, S>(getOrAddState(source), this.getOrAddState(destination), action));
        alphabet.add(action);
    }

    public void addTransition(Transition<T, S> transition) {
        this.addTransition(transition.source, transition.event, transition.destination);
    }

    public void addTransition(T source, Event<S> action, T destination) {
        transitions.add(new Transition<T, S>(getOrAddState(source), getOrAddState(destination), action));
        alphabet.add(action);
    }


    //to avoid creating new state objectd
    //possibly useless
//	public State<T,S> stateWithLabels(T label){
//
//		if(labelToState.get(label) == null){
//
//			for(State<T,S> state : states){
//				if(state.state.equals(label)){
//					labelToState.put(label, state);
//					return state;
//				}
//			}
//
//			State<T,S> newState = new State<T,S>(label, null, this);
//			this.states.add(newState);
//			labelToState.put(label, newState);
//
//			return newState;
//		}
//		else{
//			return labelToState.get(label);
//		}
//
//
//	}
//

    public State<T, S> getOrAddState(T label) {
        if (this.labelToState.keySet().contains(label)) {
            return this.labelToState.get(label);
        } else {
            State<T, S> state = new State<T, S>(label, null, this);
            this.states.add(state);
            labelToState.put(label, state);
            return state;
        }
    }

    public State<T, S> getOrAddState(State<T, S> state) {
        State<T, S> thisState;
        if (!this.states.contains(state)) {
            thisState = getOrAddState(state.label);
        } else {
            thisState = this.labelToState.get(state.label);
        }

        for (Event<S> event : state.outgoingTransitions.keySet()) {
            for (T label : state.outgoingTransitions.get(event)) {
              //  State<T, S> s = this.getOrAddState(label);
                if (this.labelToState.keySet().contains(label)) {
                    thisState.addOutgoingTransition(event, labelToState.get(label));
                }
            }
        }

        for (Event<S> event : state.incomingTransitions.keySet()) {
            for (T label : state.incomingTransitions.get(event)) {
              //  State<T, S> s = this.getOrAddState(label);
                if (this.labelToState.keySet().contains(label)) {
                    thisState.addIncomingTransition(event, labelToState.get(label));
                }
            }
        }

        return thisState;
    }

    public void addInitialState(State<T, S> state) {
        this.initial.add(this.getOrAddState(state));
    }

    public void addFinalState(State<T, S> state) {
        this.finalStates.add(this.getOrAddState(state));
    }

    public void addInitialState(T label) {
        this.initial.add(this.getOrAddState(label));
    }

    public void addFinalState(T label) {
        this.finalStates.add(this.getOrAddState(label));
    }

    //computes the states the property may be in after a trace of events
    public Set<State<T, S>> statesAfterActions(List<Event<S>> trace) {
        List<Event<S>> actions = new ArrayList<Event<S>>(trace);

        Set<State<T, S>> currentStates = new HashSet<State<T, S>>();

        currentStates.addAll(initial);

        while (actions.size() > 0) {
            Event<S> action = actions.get(0);

            Set<State<T, S>> prevStates = new HashSet<State<T, S>>();
            prevStates.addAll(currentStates);

            currentStates.clear();

            for (State<T, S> state : prevStates) {
                currentStates.addAll((Set<State<T, S>>) (state.outgoingTransitions.get(action)));
            }

            actions.remove(0);
        }

        return currentStates;
    }

    //checks if sequence of events always end up in a final state
    public boolean traceAlwaysEnds(List<Event<S>> actions) {
        Set<State<T, S>> endStates = this.statesAfterActions(actions);

        if (finalStates.containsAll(endStates)) {
            return true;
        } else {
            return false;
        }
    }

    //returns set of transition objects that would be taken given a certain sequence of events
    public Set<Transition<T, S>> transitionsUsedWithTrace(List<Event<S>> trace) {
        List<Event<S>> actions = new ArrayList<Event<S>>(trace);

        Set<Transition<T, S>> transitionsUsed = new HashSet<Transition<T, S>>();

        Set<State<T, S>> currentStates = new HashSet<State<T, S>>();

        currentStates.addAll(initial);

        while (actions.size() > 0) {
            Event<S> action = actions.get(0);

            Set<State<T, S>> prevStates = new HashSet<State<T, S>>();
            prevStates.addAll(currentStates);

            currentStates.clear();

            for (State<T, S> state : prevStates) {
                for (State<T, S> nextState : (Set<State<T, S>>) state.outgoingTransitions.get(action)) {
                    currentStates.add(nextState);
                    transitionsUsed.add(new Transition<T, S>(state, nextState, action));
                }
            }

            actions.remove(0);
        }

        if (Collections.disjoint(finalStates, currentStates)) {
            transitionsUsed.clear();
        }

        return transitionsUsed;
    }


    public void simplify(Set<Transition<T, S>> transitionsToKeep) {
        Set<Transition<T, S>> transitionsToRemove = new HashSet<Transition<T, S>>(transitions);
        transitionsToRemove.removeAll(transitionsToKeep);

        for (Transition<T, S> toRemove : transitionsToRemove) {
            if (transitions.remove(toRemove)) {
                Set<State<T, S>> statesOutgoingAfterAction = (Set<State<T, S>>) toRemove.source.outgoingTransitions.get(toRemove.event);

                statesOutgoingAfterAction.remove(toRemove.event);

                if (statesOutgoingAfterAction.size() == 0) {
                    toRemove.source.outgoingTransitions.remove(toRemove.event);
                }

                Set<State<T, S>> statesIncomingBeforeAction = (Set<State<T, S>>) toRemove.destination.incomingTransitions.get(toRemove.event);

                statesIncomingBeforeAction.remove(toRemove.event);

                if (statesIncomingBeforeAction.size() == 0) {
                    toRemove.destination.incomingTransitions.remove(toRemove.event);
                }
            }
        }
    }

    public void removeStatesNotReachableFromInitialState() {
        Set<State<T, S>> statesReachableFromInitialState = new HashSet<>();

        Set<State<T, S>> current = new HashSet<>();
        current.addAll(initial);

        while (current.size() != 0) {
            statesReachableFromInitialState.addAll(current);
            Set<State<T, S>> next = new HashSet<>();

            for (State state : current) {
                for (Object event : state.outgoingTransitions.keySet()) {
                    Set<T> outgoingStatesLabels = (Set<T>) state.outgoingTransitions.get(event);
                    outgoingStatesLabels.forEach(label -> next.add((State<T, S>) state.parent.labelToState.get(label)));
                  //  next.addAll(states);
                }
            }

            next.removeAll(statesReachableFromInitialState);
            current = next;
        }

//        this.states = statesReachableFromInitialState;

        Set<T> stateLabelsReachableFromInitialState = new HashSet<>();
        statesReachableFromInitialState.forEach(s -> stateLabelsReachableFromInitialState.add(s.label));

        retainOnly(stateLabelsReachableFromInitialState);

        if (Sets.intersection(states, finalStates).size() != 0) {
            neverFails = false;
        } else {
            neverFails = true;
        }
    }

    public void retainOnly(Set<T> labelsToKeep){
        for(T label : labelsToKeep){
            if(!labelsToKeep.contains(label)) {
                this.labelToState.remove(label);
            }
        }

        Set<State<T,S>> statesToRemove = new HashSet<>();
        for(State<T,S> state: states){
            if(!labelsToKeep.contains(state.label)){
                statesToRemove.add(state);
            }
        }

        this.states.removeAll(statesToRemove);
        this.initial.removeAll(statesToRemove);
        this.finalStates.removeAll(statesToRemove);

        for(State<T,S> state : states){
            for(Event<S> event : state.incomingTransitions.keySet()){
                Set<T> stateLabelsToKeep = new HashSet<>();

                for(T label : state.incomingTransitions.get(event)){
                    if(labelsToKeep.contains(label)){
                        stateLabelsToKeep.add(label);
                    }
                }

                state.incomingTransitions.put(event, stateLabelsToKeep);
            }
            for(Event<S> event : state.outgoingTransitions.keySet()){
                Set<T> stateLabelsToKeep = new HashSet<>();

                for(T label : state.outgoingTransitions.get(event)){
                    if(labelsToKeep.contains(label)){
                        stateLabelsToKeep.add(label);
                    }
                }

                state.outgoingTransitions.put(event, stateLabelsToKeep);
            }
        }

        Set<Transition<T,S>> toRemove = new HashSet<>();

        for(Transition<T, S> transition : transitions){
            if(!labelsToKeep.contains(transition.source.label)
                    || !labelsToKeep.contains(transition.destination.label)){
                toRemove.add(transition);
            }
        }

        transitions.removeAll(toRemove);
    }

    //	public void removeStatesNotReachableFromInitialState(){
//		//remove all non-initial states that have no incoming transitions
//		//(thus remove all outgoing transitions of these states)
//		//continue until no more such states can be removed
//		//check if these is still a final state
//		//if not then automaton never fails
//		//else
//		//  remove all non-final, non-initial states that have no outgoing transitions
//
//		boolean changedFSM = true;
//		Set<State<T,S>> statesToRemove = new HashSet<State<T,S>>();
//
//		while(changedFSM){
//			changedFSM = false;
//			for(State<T,S> state : states){
//				//if the state is not an initial state and it doesn t have any incoming transitions
//				if(!initial.contains(state)
//						&& state.incomingTransitions.entrySet().size() == 0){
//
//					changedFSM = true;
//					statesToRemove.add(state);
//					for(Event<S> action : (Collection<Event<S>>) state.outgoingTransitions.keySet()){
//						Set<State<T,S>> outgoingStates = state.outgoingTransitions.get(action);
//
//						for(State<T,S> outgoingState : outgoingStates){
//							Set<State<T,S>> statesIncomingBeforeAction = (Set<State<T,S>>) outgoingState.incomingTransitions.get(action);
//
//							statesIncomingBeforeAction.remove(action);
//
//							if(statesIncomingBeforeAction.size() == 0){
//								outgoingState.incomingTransitions.remove(action);
//							}
//						}
//					}
//				}
//				else if(initial.contains(state)
//							&& state.outgoingTransitions.size() == 0){
//
//					changedFSM = true;
//					statesToRemove.add(state);
//					for(Event<S> action : (Collection<Event<S>>) state.outgoingTransitions.keySet()){
//						Set<State<T,S>> outgoingStates = state.outgoingTransitions.get(action);
//
//						for(State<T,S> outgoingState : outgoingStates){
//							Set<State<T,S>> statesIncomingBeforeAction = (Set<State<T,S>>) outgoingState.incomingTransitions.get(action);
//
//							statesIncomingBeforeAction.remove(action);
//
//							if(statesIncomingBeforeAction.size() == 0){
//								outgoingState.incomingTransitions.remove(action);
//							}
//						}
//					}
//				}
//				//this removes states from which a final state is not reachable
////				else if(!finalStates.contains(state)
////						 && state.outgoingTransitions.entrySet().size() == 0){
////					statesToRemove.add(state);
////					changedFSM = true;
////
////					for(Event<S> action : (Collection<Event<S>>) state.incomingTransitions.keySet()){
////						Set<State<T,S>> incomingStates = state.incomingTransitions.get(action);
////
////						for(State<T,S> incomingState : incomingStates){
////							Set<State<T,S>> statesOutgoingAfterAction = (Set<State<T,S>>) incomingState.outgoingTransitions.get(action);
////
////							statesOutgoingAfterAction.remove(action);
////
////							if(statesOutgoingAfterAction.size() == 0){
////								incomingState.incomingTransitions.remove(action);
////							}
////						}
////					}
////				}
//			}
//
//			this.states.removeAll(statesToRemove);
//			this.finalStates.removeAll(statesToRemove);
//			this.initial.removeAll(statesToRemove);
//		}
//
//		if(this.transitions.size() == 0){
//			this.clear();
//		}
//
//		//if(//Collections.disjoint(states, finalStates) ||
//			//	 Collections.disjoint(states, initial)){
//		if(initial.size() == 0){
//			neverFails = true;
//		}
//		else{
//			neverFails = false;
//		}
//	}
//
    public void clear() {
        this.initial.clear();
        this.states.clear();
        this.initial.clear();
        this.finalStates.clear();

        this.neverFails = true;
    }

//	public void removeStatesFromWhichFinalStatesAreNotReachable(){
//		//remove all non-initial states that have no incoming transitions
//		//(thus remove all outgoing transitions of these states)
//		//continue until no more such states can be removed
//		//check if these is still a final state
//		//if not then automaton never fails
//		//else
//		//  remove all non-final, non-initial states that have no outgoing transitions
//
//		boolean changedFSM = true;
//		Set<State<T,S>> statesToRemove = new HashSet<State<T,S>>();
//
//		while(changedFSM){
//			changedFSM = false;
//			for(State<T,S> state : states){
////				if(!initial.contains(state)
////						&& state.incomingTransitions.entrySet().size() == 0){
////
////					changedFSM = true;
////					statesToRemove.add(state);
////					for(Event<S> action : (Collection<Event<S>>) state.outgoingTransitions.keySet()){
////						Set<State<T,S>> outgoingStates = state.outgoingTransitions.get(action);
////
////						for(State<T,S> outgoingState : outgoingStates){
////							Set<State<T,S>> statesIncomingBeforeAction = (Set<State<T,S>>) outgoingState.incomingTransitions.get(action);
////
////							statesIncomingBeforeAction.remove(action);
////
////							if(statesIncomingBeforeAction.size() == 0){
////								outgoingState.incomingTransitions.remove(action);
////							}
////						}
////					}
////				}
////				else
//				if(!finalStates.contains(state)
//						 && state.outgoingTransitions.entrySet().size() == 0){
//					statesToRemove.add(state);
//					changedFSM = true;
//
//					for(Event<S> action : (Collection<Event<S>>) state.incomingTransitions.keySet()){
//						Set<T> incomingStatesLabels = state.incomingTransitions.get(action);
//
//						for(T incomingStateLabel : incomingStatesLabels){
//							State<T,S> incomingState = getOrAddState(incomingStateLabel);
//
//						    Set<T> statesOutgoingAfterAction = (Set<T>) incomingState.outgoingTransitions.get(action);
//
//							statesOutgoingAfterAction.remove(action);
//
//							if(statesOutgoingAfterAction.size() == 0){
//								incomingState.incomingTransitions.remove(action);
//							}
//						}
//					}
//				}
//			}
//
//			this.states.removeAll(statesToRemove);
//		}
//
//
//		if(Collections.disjoint(states, finalStates)
//				|| Collections.disjoint(states, initial)){
//			neverFails = true;
//		}
//		else{
//			neverFails = false;
//		}
//	}

    public fsm.FSM<T, S> total() {
        fsm.FSM<T, S> totalFSM = deepCopy(this);

        State<T, S> sinkState = new State<T, S>((T) null, null, totalFSM);

        for (State<T, S> state : totalFSM.states) {
            Set<Event<S>> actionsForSink = new HashSet<Event<S>>(totalFSM.alphabet);
            actionsForSink.removeAll(state.outgoingTransitions.entrySet());

            for (Event<S> action : actionsForSink) {
                state.addOutgoingTransition(action, sinkState);
            }
        }

        return totalFSM;
    }

    public DeterministicFSM<T, S> complement() {
        DeterministicFSM<T, S> dFSM = new DeterministicFSM<T, S>(this);
        dFSM = dFSM.total();

        Set<State<Set<State<T, S>>, S>> nonFinalStates = new HashSet<State<Set<State<T, S>>, S>>(dFSM.states);

        nonFinalStates.removeAll(dFSM.finalStates);

        dFSM.finalStates = nonFinalStates;

        return dFSM;
    }

    public static <Q, R, T, S> fsm.FSM<Pair<Q, T>, Pair<R, S>> union(fsm.FSM<Q, R> fsm1, fsm.FSM<T, S> fsm2) {

        fsm.FSM<Pair<Q, T>, Pair<R, S>> union = new fsm.FSM<Pair<Q, T>, Pair<R, S>>();

        for (State<Q, R> initial1 : fsm1.initial) {
            for (State<T, S> initial2 : fsm2.initial) {

                State<Pair<Q, T>, Pair<R, S>> initialState = new State<Pair<Q, T>, Pair<R, S>>(new Pair<Q, T>(initial1.label, initial2.label), null, union);
                union.addInitialState(initialState);

                Set<State<Pair<Q, T>, Pair<R, S>>> currentStates = new HashSet<State<Pair<Q, T>, Pair<R, S>>>();
                currentStates.add(initialState);

                boolean addedSomething = true;

                while (addedSomething) {
                    //Set<State<Pair<Q,T>,Pair<R,S>>> prevStates = new HashSet<State<Pair<Q,T>,Pair<R,S>>>(currentStates);

                    currentStates.clear();

                    addedSomething = false;

                    for (State<Pair<Q, T>, Pair<R, S>> state : currentStates) {
                        State<Q, R> firstState = fsm1.labelToState.get(state.label.first);
                        State<T, S> secondState = fsm2.labelToState.get(state.label.second);

                        Set<Event<R>> firstOutgoingEntryValues = firstState.outgoingTransitions.keySet();
                        Set<Event<S>> secondOutgoingEntryValues = secondState.outgoingTransitions.keySet();

                        //Will the below work? Who knows.
                        Set<Event> synchronousActions = new HashSet<Event>();

                        if (initial1.label.getClass().asSubclass(initial2.label.getClass()) != null
                                || initial2.label.getClass().asSubclass(initial1.label.getClass()) != null) {
                            synchronousActions.addAll(firstOutgoingEntryValues);
                            synchronousActions.retainAll(secondOutgoingEntryValues);

                            firstOutgoingEntryValues.removeAll(synchronousActions);
                            secondOutgoingEntryValues.removeAll(synchronousActions);
                        }

                        for (Event<R> action : firstOutgoingEntryValues) {
                            for (Q nextFirstStateLabel : firstState.outgoingTransitions.get(action)) {
                                Pair<Q, T> nextState = new Pair<Q, T>(nextFirstStateLabel, secondState.label);
                                Event<Pair<R, S>> newAction = new Event<Pair<R, S>>(new Pair<R, S>(action.label, null));

                                union.addTransition(state.label, newAction, nextState);
                                addedSomething = true;
                                if (fsm1.finalStates.contains(nextState.first)
                                        || fsm2.finalStates.contains(nextState.second)) {
                                    union.addFinalState(union.labelToState.get(nextState));
                                }
                            }
                        }

                        for (Event<S> action : secondOutgoingEntryValues) {
                            for (T nextSecondStateLabel : secondState.outgoingTransitions.get(action)) {
                                Pair<Q, T> nextState = new Pair<Q, T>(firstState.label, nextSecondStateLabel);

                                Event<Pair<R, S>> newAction = new Event<Pair<R, S>>(new Pair<R, S>(null, action.label));

                                union.addTransition(state.label, newAction, nextState);
                                addedSomething = true;
                                if (fsm1.finalStates.contains(nextState.first)
                                        || fsm2.finalStates.contains(nextState.second)) {
                                    union.addFinalState(union.labelToState.get(nextState));
                                }
                            }
                        }

                        for (Event action : synchronousActions) {
                            for (Q nextFirstStateLabel : firstState.outgoingTransitions.get(action)) {
                                for (T nextSecondStateLabel : secondState.outgoingTransitions.get(action)) {

                                    Pair<Q, T> nextState = new Pair<Q, T>(nextFirstStateLabel, nextSecondStateLabel);
                                    Event<Pair<R, S>> newAction = new Event<Pair<R, S>>(new Pair<R, S>((R) action.label, (S) action.label));

                                    union.addTransition(state.label, newAction, nextState);
                                    addedSomething = true;
                                    if (fsm1.finalStates.contains(nextState.first)
                                            || fsm2.finalStates.contains(nextState.second)) {
                                        union.addFinalState(union.labelToState.get(nextState));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return union;
    }

    public static <Q, R, T, S> DeterministicFSM<Pair<Set<State<Q, R>>, Set<State<T, S>>>, Pair<R, S>> intersection(fsm.FSM<Q, R> fsm1, fsm.FSM<T, S> fsm2) {
        return (union(fsm1.complement(), fsm2.complement())).complement();
    }

    public fsm.FSM<T, S> withLoopingStates(Set<Event<S>> loopingActions) {
        fsm.FSM<T, S> loopingFSM = deepCopy(this);

        for (State<T, S> s : loopingFSM.states) {
            for (Event<S> action : loopingActions) {
                loopingFSM.addTransition(s, action, s);
            }
        }

        return loopingFSM;
    }

    public String toString() {
        //if(neverFails) return "FSM never violates!";

        String representation = "";

        for (Transition<T, S> t : this.transitions) {
            representation += t.toString() + "\n";
        }

        return representation;
    }

    public void removeUnusedEvents() {
        Set<Event<S>> toKeep = new HashSet<Event<S>>();

        for (Transition<T, S> t : this.transitions) {
            toKeep.add(t.event);
        }

        this.alphabet.retainAll(toKeep);
    }

//	public boolean containsCycle(){
//		Set<FSM<T,S>> currentFSMs = new HashSet<FSM<T,S>>();
//		currentFSMs.add(this);
//		
//		while(currentFSMs.size() > 0){
//			for(FSM<T,S> fsm : currentFSMs){
//				if(!Collections.disjoint(currentFSMs, fsm.internalFSMs)){
//					return false;
//				}
//				else{
//					
//				}
//			}
//		}
//	}
////	
////	public FSM linearized(boolean copyFSM){
//		FSM<T,S> linearizedFSM;
//		
//		if(copyFSM){
//			linearizedFSM = new FSM<T,S>(this);
//		}
//		else{
//			linearizedFSM = this;
//		}
//		
//		for(State<T,S> state : linearizedFSM.states){
//			if(state.fsm != null){
//				FSM<T,S> subFSM = state.fsm;//.linearized(copyFSM);
//				
//				Set<State<T,S>> statesToAdd = subFSM.states;
//				
//				
//				
//				for(State<T,S> initial : subFSM.initial){
//					for(Action<S> incomingAction : state.incomingTransitions.keySet()){
//						for(State<T,S> pred : state.incomingTransitions.get(incomingAction)){
//							State<T,S>
//						}
//					}
//				}
//				
//			}
//		}
//	}

//	We're taking care of these in the reachability analysis
//	see: http://www.cs.um.edu.mt/gordon.pace/Research/Software/Relic/Transformations/FSA/remove-useless.html
//
//	public FSM removeUselessStates(FSM fsm){
//		Set<State> usefulStates = new HashSet<State>(fsm.finalStates);
//		
//		boolean addedSomething = true;
//		
//		while(addedSomething){
//			addedSomething = false;
//			
//			for(State usefulState : usefulStates){
//				for(Action action : (Set<Action>) usefulState.incomingTransitions.keySet()){
//					
//				}
//			}
//		}
//	}

//	public void removeTransition(Transition<T,S> transition){
//		this.transitions.remove(transition);
//
//		Set<State<T,S>> outgoingStates = transition.source.outgoingTransitions.get(transition.action);
//		
//		if(outgoingStates != null){
//			outgoingStates.remove(transition.destination);
//			if(outgoingStates.size() == 0){
//				transition.source.outgoingTransitions.remove(transition.action);
//			}
//		}
//
//		Set<State<T,S>> incomingStates = transition.destination.incomingTransitions.get(transition.action);
//		
//		if(incomingStates != null){
//			incomingStates.remove(transition.source);
//			if(incomingStates.size() == 0){
//				transition.destination.incomingTransitions.remove(transition.action);
//			}
//		}
//	}

//	public FSM<T,S> skipStates(Set<State<T,S>> toSkip, boolean copyFSM){
//		FSM<T,S> reducedFSM;
//		
//		if(copyFSM){
//			reducedFSM = deepCopy(this);
//		}
//		else reducedFSM = this;
//	
//		
//		if(reducedFSM.neverFails
//			|| toSkip.containsAll(reducedFSM.initial)
//			|| toSkip.containsAll(reducedFSM.finalStates)){
//			reducedFSM.neverFails = true;
//			return reducedFSM;
//		}
//		
//		
//		for(State<T,S> s : toSkip){
//		//	Set<State<T,S>> outgoingStates = new HashSet<State<T,S>>();
//			
//			for(Set<State<T,S>> stateSet : s.outgoingTransitions.values()){
//		//		outgoingStates.addAll(stateSet);
//				for(State<T,S> succ : stateSet){
//					for(Event<S> a : s.incomingTransitions.keySet()){
//						for(State<T,S> pred : s.incomingTransitions.get(s)){
//							reducedFSM.addTransition(pred, a, succ);
//						}
//					}
//				}
//			}
//			
//			reducedFSM.states.remove(s);
//			reducedFSM.initial.remove(s);
//			reducedFSM.finalStates.remove(s);
//			
//		}
//		
//		for(Transition<T,S> t : reducedFSM.transitions){
//			if(toSkip.contains(t.source)
//					|| toSkip.contains(t.destination)){
//				reducedFSM.removeTransition(t);
//			}
//		}
//		
//		return reducedFSM;
	}
