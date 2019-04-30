package clarva.analysis.cfg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fsm.Event;
import fsm.FSM;
import fsm.State;

public class CFG <St, Ev extends CFGEvent> extends FSM<Integer, Ev>{

	public ArrayList<CFGState<St>> statements;

	//why are we doing this?
    public Map<CFGState<St>, State<Integer, Ev>> labelToState;
    public Map<State<Integer, Ev>, CFGState<St>> stateToLabel;
    public Map<Integer, State<Integer, Ev>> integerToState;

	public CFG(){
		super();
        statements = new ArrayList<>();
		labelToState = new HashMap<>();
        stateToLabel = new HashMap<>();
		integerToState = new HashMap<>();
		this.neverFails = false;
	}

	public CFG(CFG cfg){
		super(cfg);

		statements = new ArrayList<>(cfg.statements);
		labelToState = new HashMap();

		for(State<Integer, Ev> state : this.states){
			labelToState.put(statements.get(state.label), state);
		}

		this.neverFails = cfg.neverFails;
	}

	public State<Integer, Ev> getOrAddState(CFGState<St> u){
		if(!statements.contains(u)){
			this.statements.add(u);
			State<Integer, Ev> state = this.getOrAddState(statements.size() - 1);
			labelToState.put(u, state);
            stateToLabel.put(state, u);
			integerToState.put(statements.size() - 1, state);
			return state;
		}
		else{
			return this.labelToState.get(u);
		}
	}

	public void addInitialState(CFGState<St> u){
		if(!statements.contains(u)){
			this.statements.add(u);
		}

		this.addInitialState(statements.size() - 1);
	}

	public void addFinalState(CFGState<St> u){
		if(!statements.contains(u)){
			this.statements.add(u);
		}
		
		this.addFinalState(statements.size() - 1);
	}
	//only use for control-flow analysis
	public void reduce(){
		
		//if the CFG is not hierarchical
		//i.e. if no state has some further interal computation
		if(this.internalFSMs.size() == 0){
			boolean allEpsilon = true;
			
			//check if there is an dateEvent that is not an epsilon dateEvent
			for(Event<Ev> letter : this.alphabet){
				if(!letter.label.epsilon){
					allEpsilon = false;
				}
			}
			
			//if there is no dateEvent that is not an epsilon dateEvent
			//then remove all control-flow and return
			if(allEpsilon){
				this.states.clear();
				this.transitions.clear();
				this.finalStates.clear();
				this.states.addAll(this.initial);
				State<Integer, Ev> onlyState = this.initial.iterator().next();
				
				onlyState.incomingTransitions.clear();
				onlyState.outgoingTransitions.clear();
				
				this.initial.clear();
				this.initial.add(onlyState);
				this.finalStates.add(onlyState);
				return;
			}
		}
		
		List<State<Integer, Ev>> stateList = new ArrayList<>(states);
		//for each state
		for(int i = 0; i < stateList.size(); i ++){
			State<Integer, Ev> state = stateList.get(i);
			List<Event<Ev>> outgoingEvents = new ArrayList<>(state.outgoingTransitions.keySet());
			List<Event<Ev>> incomingEvents = new ArrayList<>(state.incomingTransitions.keySet());
			
			//if the state only has one outgoing transition
			//and one incoming transition exactly
			if(outgoingEvents.size() == 1
					&& incomingEvents.size() == 1){
				List<Integer> outgoingStates =
						new ArrayList<Integer>(state.outgoingTransitions.get(outgoingEvents.get(0)));
				List<Integer> incomingStates =
						new ArrayList<Integer>(state.incomingTransitions.get(incomingEvents.get(0)));
				
				//if the outgoing and incoming dateEvent are both epsilon events
				if(outgoingEvents.get(0).label.epsilon
						&& outgoingStates.size() == 1
						&& incomingEvents.get(0).label.epsilon
						&& incomingStates.size() == 1){
					State<Integer, Ev> outgoingState = state.parent.labelToState.get(outgoingStates.get(0));
					State<Integer, Ev> incomingState = state.parent.labelToState.get(incomingStates.get(0));
					
					
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
