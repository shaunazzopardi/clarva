package fsm.date;

import fsm.Event;
import fsm.State;
import fsm.Transition;
import fsm.date.events.DateEvent;
import fsm.date.events.MethodCall;

import java.util.*;
import java.util.function.Predicate;

public class SubsetDate extends DateFSM{

	public DateFSM parent;

	public SubsetDate(DateFSM parent){
		super(parent);
		this.parent = parent;
	}
	//this constructor keeps also all transitions on channel and clock events
	public SubsetDate(DateFSM parent, List<DateEvent> alphabetToKeep){
		super(parent);

		this.parent = parent;

		this.alphabet.clear();
		for(Event<DateLabel> event : parent.alphabet){
			if(!(event.label.event instanceof MethodCall)){
				this.alphabet.add(event);
			}
			else if(alphabetToKeep.contains(event.label.event)){
				this.alphabet.add(event);
			}
		}

		for(String label : this.stateHoareTripleMethod.keySet()){
			State<String,DateLabel> state = this.labelToState.get(label);
			List<String> toKeep = new ArrayList<String>();
			for(String method : this.stateHoareTripleMethod.get(state.label)){
				for(DateEvent dateEvent : alphabetToKeep) {
					if (dateEvent.getClass().equals(MethodCall.class)) {
						MethodCall methodCall = (MethodCall) dateEvent;

 						String methodClass = method.split("\\.")[0];
						String methodName = method.split("\\.")[1];

						if (methodCall.name.equals(methodName)
								&& methodCall.objectType.equals(methodClass)) {
							toKeep.add(method);
						}
					}
				}
			}
			this.stateHoareTripleMethod.get(state.label).retainAll(toKeep);
		}

		//this.alphabet = alphabetToKeep;

		if(alphabet.size() == 0){
			this.transitions.clear();
			this.states.clear();
			this.neverFails = true;
			return;
		}

		//remove any transitions that have action that is to be removed
		Set<Transition<String, DateLabel>> transitionsToRemove = new HashSet<>();
		for(Transition<String, DateLabel> transition : this.transitions){
			if(!alphabet.contains(transition.event)){
				//this also removes references to the transition from inside the source and destination
				transition.remove();
				transitionsToRemove.add(transition);
			}
		}

		this.transitions.removeAll(transitionsToRemove);

		this.reachabilityReduction();

	}

	public SubsetDate(DateFSM parent, Set<Event<DateLabel>> alphabetToKeep){
		super(parent);
		this.alphabet = alphabetToKeep;
		this.parent = parent;

		if(alphabet.size() == 0
				|| alphabet.size() == parent.alphabet.size()){
			this.transitions.clear();
			this.states.clear();
			this.neverFails = true;
			return;
		}

		Set<Transition<String, DateLabel>> toRemove = new HashSet<Transition<String, DateLabel>>();
		//remove any transitions that have action that is to be removed
		for(Transition<String, DateLabel> transition : this.transitions){
			if(!alphabet.contains(transition.event)){
				//this also removes references to the transition from inside the source and destination
				transition.remove();
				toRemove.add(transition);
			}
		}

		this.transitions.removeAll(toRemove);

		for(State<String,DateLabel> state : this.states){
			List<String> toRemoveMethods = new ArrayList<String>();
			for(String method : this.stateHoareTripleMethod.get(state.label)){
				for(Event<DateLabel> event : alphabetToKeep){
					if(event.label.event instanceof MethodCall){
						MethodCall methodCall = (MethodCall) event.label.event;

						String methodClass = method.split(".")[0];
						String methodName = method.split(".")[1];

						if(methodCall.name.equals(methodName)
								&& methodCall.objectType.equals(methodClass)){
							toRemoveMethods.add(method);
						}
					}
				}
			}
			this.stateHoareTripleMethod.get(state.label).removeAll(toRemoveMethods);
		}

		this.reachabilityReduction();

	}

	public SubsetDate(DateFSM parent, final Collection<Transition<String,DateLabel>> transitionsToKeep, Map<String,Set<String>> methodsPossibleAtStates){
		this(parent, transitionsToKeep, methodsPossibleAtStates, true);
	}

	public SubsetDate(DateFSM parent, final Collection<Transition<String,DateLabel>> transitionsToKeep, Map<String,Set<String>> methodsPossibleAtStates, Boolean keepUnreachable){
		super(parent);
		//this.alphabet = alphabetToKeep;
		this.parent = parent;

//		if(transitionsToKeep.size() == 0
//				|| transitionsToKeep.size() == parent.alphabet.size()){
//
//			return;
//		}

		for(Transition<String, DateLabel> transition : this.transitions){
			if(!transitionsToKeep.contains(transition)){
				//this also removes references to the transition from inside the source and destination
				transition.remove();
			}
		}

		this.transitions.removeIf(new Predicate<Transition<String, DateLabel>>() {
			@Override
			public boolean test(Transition<String, DateLabel> t) {
				return !transitionsToKeep.contains(t);
			}
		});

		for(String label : this.stateHoareTripleMethod.keySet()){
			this.stateHoareTripleMethod.get(label).retainAll(methodsPossibleAtStates.get(label));
		}

		if(keepUnreachable) {
			this.reachabilityReduction();
		}

	}

	@Override
	public void retainOnly(Set<String> labelsToKeep){
	    super.retainOnly(labelsToKeep);

        List<String> keysToRemove = new ArrayList<>();
        for(String key : this.stateHoareTripleMethod.keySet()) {
            if(!labelsToKeep.contains(key)) {
                keysToRemove.add(key);
            }
        }

        keysToRemove.forEach(key -> this.stateHoareTripleMethod.remove(key));

    }

	public State<String, DateLabel> addStateAndContinuation(State<String, DateLabel> state){
		State<String, DateLabel> thisState;
		if(!this.states.contains(state)){
			thisState = getOrAddState(state.label);
		}
		else{
			thisState = this.labelToState.get(state.label);
		}

		for(Event<DateLabel> event : state.outgoingTransitions.keySet()){
			for(String label : state.outgoingTransitions.get(event)){
				if(this.labelToState.keySet().contains(label)){
					thisState.addOutgoingTransition(event, this.labelToState.get(label));
				}
				else{
					State<String, DateLabel> thisS = addStateAndContinuation(state.parent.getOrAddState(label));
					thisState.addOutgoingTransition(event, thisS);
				}
			}
		}

		for(Event<DateLabel> event : state.incomingTransitions.keySet()){
			for(String label : state.incomingTransitions.get(event)){
				if(this.labelToState.keySet().contains(label)){
					thisState.addIncomingTransition(event, this.labelToState.get(label));
				}
				else{
					State<String, DateLabel> thisS = addStateAndContinuation(state.parent.getOrAddState(label));
					thisState.addIncomingTransition(event, thisS);
				}

			}
		}

		return thisState;
	}

	public void add(fsm.date.SubsetDate otherDate) {
		add(otherDate, true);
	}

	public void add(fsm.date.SubsetDate otherDate, boolean reductions){
		//create a date that contains all the transitions of this date and the other date

		if(reductions) {
			otherDate.reachabilityReduction();
			this.reachabilityReduction();
		}

		for(State<String,DateLabel> s : otherDate.states){
			this.addStateAndContinuation(s);

			if(otherDate.finalStates.contains(s)){
				this.addFinalState(s);
			}

			if(otherDate.badStates.contains(s)){
				this.addBadState(s);
			}

			if(otherDate.acceptingStates.contains(s)){
				this.addAcceptingState(s);
			}

			if(otherDate.initial.contains(s)){
				this.addInitialState(s);
			}
		}

		this.transitions.addAll(otherDate.transitions);
		
		for(String label : this.stateHoareTripleMethod.keySet()){
			if(otherDate.stateHoareTripleMethod.containsKey(label)){
				this.stateHoareTripleMethod.get(label).addAll(otherDate.stateHoareTripleMethod.get(label));
			}
		}
		
		this.neverFails = otherDate.neverFails && this.neverFails;
		
	}
	
	public void remove(fsm.date.SubsetDate otherDate){
		//create a date that contains all the transitions of this date and the other date
		
		otherDate.reachabilityReduction();
		this.reachabilityReduction();
		
		for(Transition<String,DateLabel> t : this.transitions){
			if(!otherDate.transitions.contains(t)){
				t.remove();
				this.transitions.remove(t);
			}
		}		
	}
	
	public String toString(){
		return super.toString();
	}
	
//	public String toString(){
//		if(neverFails || this.transitions.size() == 0) return "FSM never violates!";
//		
//		String representation = "";
//		
//		representation += "STATES{\n";
//
//		representation += "BAD{\n";
//		for(State<String,DateLabel> state : this.badStates){
//			
//			String hoareTripleMethods = "";
//			if(this.stateHoareTripleMethod.get(state.label) != null){
//				hoareTripleMethods = String.join(",", this.stateHoareTripleMethod.get(state.label));  
//			}
//			representation += state.label + "{" + hoareTripleMethods + "}\n";
//		}
//		representation += "\n}";
//		
//
//		representation += "STARTING{\n";
//		for(State<String,DateLabel> state : this.initial){                                                                  
//			String hoareTripleMethods = "";                                      
//			if(this.stateHoareTripleMethod.get(state.label) != null){            
//				hoareTripleMethods = String.join(",", this.stateHoareTripleMethod.get(state.label));  
//			}                                                                    
//			representation += state.label + "{" + hoareTripleMethods + "}\n";    		
//		}
//		representation += "\n}";
//		representation += "NORMAL{\n";
//		Set<State<String,DateLabel>> normalStates = new HashSet<State<String,DateLabel>>();
//		normalStates.addAll(states);
//		normalStates.removeAll(initial);
//		normalStates.removeAll(badStates);
//		normalStates.removeAll(acceptingStates);
//		
//		for(State<String,DateLabel> state : normalStates){
//			                                                                     
//			String hoareTripleMethods = "";                                      
//			if(this.stateHoareTripleMethod.get(state.label) != null){            
//				hoareTripleMethods = String.join(",", this.stateHoareTripleMethod.get(state.label));  
//			}                                                                    
//			representation += state.label + "{" + hoareTripleMethods + "}\n";    
//		}
//		representation += "\n}";
//		representation += "ACCEPTING{\n";
//		for(State<String,DateLabel> state : this.acceptingStates){                                                               
//			String hoareTripleMethods = "";                                      
//			if(this.stateHoareTripleMethod.get(state.label) != null){            
//				hoareTripleMethods = String.join(",", this.stateHoareTripleMethod.get(state.label));  
//			}                                                                    
//			representation += state.label + "{" + hoareTripleMethods + "}\n";    		}
//		representation += "\n}";
//
//		representation += "\n}\n";
//
//		representation += "TRANSITIONS{\n";
//		for(Transition<String,DateLabel> t : this.transitions){
//			representation += t.source + " -> " + t.destination + " [" + t.dateEvent.label.toString() + "]\n";
//		}
//		representation += "\n}";
//		
//		return representation;
//	}
}
