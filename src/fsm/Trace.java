package fsm;

public class Trace<T>{

//	public FSM parent;
//	
//	public boolean traceEnds;
//	
//	public List<Action> actions;
//	
//	public State<T> currentState;
//	public int nextAction;
//	
//	public Trace(FSM<T,S> parent, State<T> initialState, List<Action> actions){
//		this.parent = parent;
//		this.actions = actions;
//		nextAction = 0;
//		currentState = initialState;
//		
//		//sanity check: check that doing the actions leads to a final state
//		
//		if(parent.traceAlwaysEnds(actions)){
//			traceEnds = true;
//		}
//		else{
//			traceEnds = false;
//		}
//		
//		//if FSM is non-deterministic then no guarantee that 
//		//this object will represent always the same sequence of states
//	}
//	
//	public Action succAction(State state){
//		if(state.outgoingTransitions.size() == 0){
//			return null;
//		}
//		else{
//			return (Action)(state.outgoingTransitions.entrySet().toArray()[0]);
//		}
//	}
	
}
