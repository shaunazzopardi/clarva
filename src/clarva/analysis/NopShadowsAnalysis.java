package clarva.analysis;

import java.util.List;

import fsm.FSM;
import fsm.date.DateFSM;
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.Unit;
import soot.jimple.toolkits.pointer.InstanceKey;
import soot.jimple.toolkits.pointer.LocalMustAliasAnalysis;
import soot.jimple.toolkits.pointer.LocalMustNotAliasAnalysis;

public class NopShadowsAnalysis {

	//CFGAnalysis cfga;
	
	//This will return nothing but will simply edit the program to disable any
	//shadows that are activated uselessly
	public static void NopShadowsAnalysis(CFGAnalysis cfga, DateFSM property){		
		FSM mainMethodExecutionFSM = cfga.methodCFG.get(Scene.v().getMainMethod());
		
		//from the main method we need to extract all consistent traces and go through
		
		
	}
	
//	public List<Unit> unitsToDisable(){
//		
//	}
}
