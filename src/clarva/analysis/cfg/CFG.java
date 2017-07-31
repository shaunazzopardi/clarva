package clarva.analysis.cfg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import fsm.Event;
import fsm.FSM;
import fsm.State;
import fsm.Transition;
import soot.Unit;

public class CFG extends FSM<Integer, Shadow>{
	
	public ArrayList<Unit> units;
	
	public Map<Unit, State<Integer, Shadow>> labelToState;
	
	public CFG(){
		super();
		units = new ArrayList<Unit>();
		labelToState = new HashMap<Unit, State<Integer, Shadow>>();
		this.neverFails = false;
	}
	
	public CFG(CFG cfg){
		super(cfg);
		
		units = new ArrayList<Unit>(cfg.units);
		labelToState = new HashMap<Unit, State<Integer, Shadow>>();
		
		for(State<Integer, Shadow> state : this.states){
			labelToState.put(units.get(state.label), state);
		}
		
		this.neverFails = cfg.neverFails;
	}
	
	public State<Integer, Shadow> getOrAddState(Unit u){
		if(!units.contains(u)){
			this.units.add(u);
			State<Integer, Shadow> state = this.getOrAddState(units.size() - 1);
			labelToState.put(u, state);
			return state;
		}
		else{
			return this.labelToState.get(u);
		}
	}
	
	public void addInitialState(Unit u){
		if(!units.contains(u)){
			this.units.add(u);
		}
		
		this.addInitialState(units.size() - 1);
	}
	
	public void addFinalState(Unit u){
		if(!units.contains(u)){
			this.units.add(u);
		}
		
		this.addFinalState(units.size() - 1);
	}
	
	public void reduce(){
		
		//if the CFG is not hierarchical
		//i.e. if no state has some further interal computation
		if(this.internalFSMs.size() == 0){
			boolean allEpsilon = true;
			
			//check if there is an event that is not an epsilon event
			for(Event<Shadow> letter : this.alphabet){
				if(!letter.label.epsilon){
					allEpsilon = false;
				}
			}
			
			//if there is no event that is not an epsilon event
			//then remove all control-flow and return
			if(allEpsilon){
				this.states.clear();
				this.transitions.clear();
				this.finalStates.clear();
				this.states.addAll(this.initial);
				State<Integer,Shadow> onlyState = this.initial.iterator().next();
				
				onlyState.incomingTransitions.clear();
				onlyState.outgoingTransitions.clear();
				
				this.initial.clear();
				this.initial.add(onlyState);
				this.finalStates.add(onlyState);
				return;
			}
		}
		
		List<State<Integer, Shadow>> stateList = new ArrayList<State<Integer,Shadow>>(states);
		//for each state
		for(int i = 0; i < stateList.size(); i ++){
			State<Integer,Shadow> state = stateList.get(i);
			List<Event<Shadow>> outgoingEvents = new ArrayList<Event<Shadow>>(state.outgoingTransitions.keySet());
			List<Event<Shadow>> incomingEvents = new ArrayList<Event<Shadow>>(state.incomingTransitions.keySet());
			
			//if the state only has one outgoing transition
			//and one incoming transition exactly
			if(outgoingEvents.size() == 1
					&& incomingEvents.size() == 1){
				List<State<Integer,Shadow>> outgoingStates = 
						new ArrayList<State<Integer,Shadow>>(state.outgoingTransitions.get(outgoingEvents.get(0)));
				List<State<Integer,Shadow>> incomingStates = 
						new ArrayList<State<Integer,Shadow>>(state.incomingTransitions.get(incomingEvents.get(0)));
				
				//if the outgoing and incoming event are both epsilon events
				if(outgoingEvents.get(0).label.epsilon
						&& outgoingStates.size() == 1
						&& incomingEvents.get(0).label.epsilon
						&& incomingStates.size() == 1){
					State<Integer, Shadow> outgoingState = outgoingStates.get(0);
					State<Integer, Shadow> incomingState = incomingStates.get(0);
					
					
					//if the current state has no internal fsm or the outgoing state has no internal fsm
					//then (i) remove the current state from the incomingstates transition list
					//
					//     (ii) add the current states internal fsm to the outgoing states internal fsms
					//	   (iii) add the
					if((state.getInternalFSM() == null
							|| outgoingState.getInternalFSM() == null)){
						stateList.remove(state);
						
						if(incomingState.outgoingTransitions.get(incomingEvents.get(0)).size() == 1)
						{
							incomingState.outgoingTransitions.remove(incomingEvents.get(0));
						}
						else{
							incomingState.outgoingTransitions.get(incomingEvents.get(0)).remove(state);
						}
						
						if(outgoingState.incomingTransitions.get(outgoingEvents.get(0)).size() == 1)
						{
							outgoingState.incomingTransitions.remove(outgoingEvents.get(0));
						}
						else{
							outgoingState.incomingTransitions.get(outgoingEvents.get(0)).remove(state);
						}
						
						this.addTransition(incomingState, outgoingEvents.get(0), outgoingState);
						
						if(state.getInternalFSM() != null) outgoingState.setInternalFSM(state.getInternalFSM());
//						outgoingState.addOutgoingTransitions(state.outgoingTransitions);
//						outgoingState.addOutgoingTransitions(state.incomingTransitions);
						i--;
					}
					//else if state has no internal fsm
//					else if(state.getInternalFSM() == null
//							&& !(incomingState.getInternalFSM() == null)
//						    && (outgoingState.getInternalFSM() == null)){
//						stateList.remove(state);
//						
//						incomingState.outgoingTransitions.get(incomingEvents.get(0)).remove(state);
//						outgoingState.setInternalFSM(incomingState.getInternalFSM());
////						outgoingState.addOutgoingTransitions(incomingState.outgoingTransitions);
////						outgoingState.addOutgoingTransitions(incomingState.incomingTransitions);
//						i--;
//					}
//					else if(state.getInternalFSM() == null){
//						stateList.remove(state);
//						
//						if(incomingState.outgoingTransitions.get(incomingEvents.get(0)).size() == 1)
//						{
//							incomingState.outgoingTransitions.remove(incomingEvents.get(0));
//						}
//						else{
//							incomingState.outgoingTransitions.get(incomingEvents.get(0)).remove(state);
//						}
////						outgoingState.addOutgoingTransitions(incomingState.outgoingTransitions);
////						outgoingState.addOutgoingTransitions(incomingState.incomingTransitions);
//						i--;
//					}
				}
			}
		}
	
		this.states.retainAll(stateList);

//		Set<Transition<Integer,Shadow>> newTransitions = new HashSet<Transition<Integer,Shadow>>(this.transitions);
//		for(int i = 0; i < stateList.size(); i++){
//			State<Integer,Shadow> state = stateList.get(i);
//			if(this.states.contains(state)){
//				for(Entry<Event<Shadow>,Set<State<Integer,Shadow>>> entry : state.outgoingTransitions.entrySet()){
//					List<State<Integer,Shadow>> outgoingStates = new ArrayList<State<Integer,Shadow>>(entry.getValue());
//					for(int j = 0; j < outgoingStates.size(); j++){
//						State<Integer,Shadow> outgoingState = outgoingStates.get(j);
//						if(this.states.contains(outgoingState)){
//							newTransitions.add(new Transition<Integer, Shadow>(state, outgoingState, entry.getKey()));
//						}
//						else{
//							state.removeOutgoingTransition(entry.getKey(), outgoingState);
//							outgoingState.removeOutgoingTransition(entry.getKey(), state);
//						}
//					}
//				}
//			}
//		}
//		
//		this.transitions = newTransitions;
	}
}