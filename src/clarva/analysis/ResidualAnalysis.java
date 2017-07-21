package clarva.analysis;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import clarva.analysis.cfg.CFG;
import clarva.analysis.cfg.Shadow;
import clarva.matching.Matching;
import fsm.Event;
import fsm.FSM;
import fsm.date.DateFSM;
import fsm.date.DateLabel;
import fsm.date.SubsetDate;
import fsm.date.events.MethodCall;
import fsm.helper.Pair;
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.Unit;

public class ResidualAnalysis {

	public static SubsetDate QuickCheckAnalysis(DateFSM property, MethodsAnalysis ma){
		List<MethodCall> usedActions = new ArrayList<MethodCall>();
		
		for(MethodCall event : ma.dateEventToSootMethods.keySet()){
			if(ma.dateEventToSootMethods.get(event) != null
					&& ma.dateEventToSootMethods.get(event).size() != 0) 
				usedActions.add(event);
		}
		
		return new SubsetDate(property, usedActions);
	}
	
	public static Map<Shadow, SubsetDate> OrphansAnalysis(DateFSM f, MethodsAnalysis ma, Matching an){
		Map<Shadow, SubsetDate> residuals = new HashMap<Shadow, SubsetDate>();
		//Map<Shadow, Set<Shadow>> compatibleShadows = new HashMap<Shadow, Set<Shadow>>();
		
		if(ma.allShadows.size() == 0) residuals.put(new Shadow(), new SubsetDate(f));
		for(Shadow s : ma.allShadows){
			Set<MethodCall> eventsToKeep = new HashSet<MethodCall>();
			
			for(Shadow ss : ma.allShadows){
				if(an.flowInsensitiveCompatible(s, ss)){
					eventsToKeep.add((MethodCall) ss.event);
				}
			}
			
			SubsetDate shadowResidual = new SubsetDate(f, new ArrayList<MethodCall>(eventsToKeep));
			residuals.put(s, shadowResidual);
		}
		
		return removeEmptyDates(residuals);
	}

	public static Pair<Map<Shadow, SubsetDate>, List<Pair<String,String>>> ControlFlowAnalysis(Map<Shadow, SubsetDate> residuals, MethodsAnalysis ma, CFGAnalysis cfga){
		Map<MethodOrMethodContext, CFG> wholeProgramCFGApproximations = new HashMap<MethodOrMethodContext, CFG>();
		
		//methodFSM not keeping only reachable methods
		for(MethodOrMethodContext method : cfga.methodCFG.keySet()){
			CFG wholeProgramCFG = cfga.methodCFGToWholeProgramCFG(method);
			wholeProgramCFGApproximations.put(method, wholeProgramCFG);
		}
		
		List<Shadow> allShadowsUpToMustAlias = new ArrayList<Shadow>();
		Map<Shadow, Set<Shadow>> mustAlias = new HashMap<Shadow, Set<Shadow>>();
		
 		for(Shadow s : ma.allShadows){
 			Set<Shadow> must = new HashSet<Shadow>();
 			mustAlias.put(s, must);
			for(Shadow ss : ma.allShadows){
				if(s == ss || s.mustAlias(ss)){
					must.add(ss);
				}
			}
		}
 		
 		for(Shadow s : ma.allShadows){
 			
 			boolean includedAlready = false;
 			for(Shadow ss : mustAlias.get(s)){
 				if(allShadowsUpToMustAlias.contains(ss)){
 					includedAlready = true;
 				}
 			}
 			
 			if(!includedAlready) allShadowsUpToMustAlias.add(s);
 		}
		
		residuals = removeEmptyDates(residuals);		

		//first iteration should be over compatible shadow sets probably
		for(Shadow s : allShadowsUpToMustAlias){
			for(CFG approx : wholeProgramCFGApproximations.values()){		
				SubsetDate oldResidual = residuals.get(s);
				SubsetDate newResidual = cfga.sufficientResidual(s, approx, oldResidual);
				if(newResidual == null || newResidual.neverFails){
//					System.out.println("here");
					newResidual = cfga.sufficientResidual(s, approx, oldResidual);
				}
				residuals.put(s, newResidual);
				
//				if(!residuals.toString().toLowerCase().contains("property")){
//					System.out.println("here");
//				}
			}
		}		
		residuals = removeEmptyDates(residuals);		
			
		//we should also check for transitions/eventsinthecomposition that only ever loop in the same state
		Set<Event<Shadow>> toDisable = cfga.canBeDisabled;
		Pair<Map<Shadow, SubsetDate>, List<Pair<String,String>>> residualsAndPPF = new Pair<Map<Shadow, SubsetDate>, List<Pair<String,String>>>(residuals, cfga.ppfs);
		
		return residualsAndPPF;
	}

	public static SubsetDate residualsUnion(Map<Shadow, SubsetDate> residuals){
		if(residuals.size() != 0){
			Iterator<SubsetDate> dateIterator = residuals.values().iterator();
			
			SubsetDate unionOfResiduals = null;
			
			while(dateIterator.hasNext()){
				SubsetDate date = dateIterator.next();
				if(unionOfResiduals == null){
					unionOfResiduals = date;
				}
				else{
					unionOfResiduals.add(date);
				}
			}
			
			unionOfResiduals.removeStatesNotReachableFromInitialState();
			
			return unionOfResiduals;
		}
		else{
			return null;
		}
	}
	

//	public static SubsetDate residualsIntersection(Map<Shadow, SubsetDate> residuals){
//		Iterator<SubsetDate> dateIterator = residuals.values().iterator();
//		
//		SubsetDate intersectionOfResiduals = null;
//		
//		while(dateIterator.hasNext()){
//			if(intersectionOfResiduals == null){
//				intersectionOfResiduals = dateIterator.next();
//			}
//			else{
//				intersectionOfResiduals.remove(dateIterator.next());
//			}
//		}
//		
//		intersectionOfResiduals.removeStatesNotReachableFromInitialState();
//		
//		return intersectionOfResiduals;
//	}
	
	private static Map<Shadow, SubsetDate> removeEmptyDates(Map<Shadow, SubsetDate> residuals){
		List<Shadow> toRemove = new ArrayList<Shadow>();
		
		for(Shadow s : residuals.keySet()){
			SubsetDate respectiveDate = residuals.get(s);
			if(respectiveDate == null
					|| respectiveDate.neverFails){
				toRemove.add(s);
			}
		}
		
		for(Shadow s : toRemove){
			residuals.remove(s);
		}
		
		return residuals;
	}
}