package clarva.analysis;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import clarva.analysis.cfg.CFG;
import clarva.analysis.cfg.CFGEvent;
import clarva.java.JavaMethodIdentifier;
import clarva.java.JavaEvent;
import clarva.matching.Aliasing;
import com.google.common.collect.Sets;
import fsm.Event;
import fsm.date.DateFSM;
import fsm.date.SubsetDate;
import fsm.date.events.DateEvent;
import fsm.helper.Pair;

public class ControlFlowResidualAnalysis {

	public static SubsetDate QuickCheckAnalysis(DateFSM property,
                                                List<DateEvent> allEvents){
		return new SubsetDate(property, allEvents);
	}

//	public static Map<Shadow, SubsetDate> OrphansAnalysis(DateFSM f, MethodsAnalysis ma, Matching an){
	public static <T extends CFGEvent> Map<T, SubsetDate> OrphansAnalysis(DateFSM f,
                                                          Set<T> allCFGEvents,
                                                          Aliasing aliasing){
		Map<T, SubsetDate> residuals = new HashMap<>();
		//Map<Shadow, Set<Shadow>> compatibleShadows = new HashMap<Shadow, Set<Shadow>>();

//		if(allCFGEvents.size() == 0) residuals.put(new Shadow(), new SubsetDate(f));
		for(T s : allCFGEvents){
			Set<DateEvent> eventsToKeep = new HashSet<>();

			for(T ss : allCFGEvents){
				if(aliasing.mayAlias(s, ss)){
					eventsToKeep.add(ss.dateEvent);
				}
			}

			SubsetDate shadowResidual = new SubsetDate(f, new ArrayList<>(eventsToKeep));
			residuals.put(s, shadowResidual);
		}

		return removeEmptyDates(residuals);
	}

//	public static Pair<Map<Shadow, SubsetDate>, List<Pair<String,String>>> ControlFlowAnalysis(Map<Shadow, SubsetDate> residuals, MethodsAnalysis ma, CFGAnalysis cfga){
	public static <St, T extends CFGEvent, S extends JavaMethodIdentifier> Map<T, SubsetDate> ControlFlowAnalysis(
	        Map<T, SubsetDate> residuals,
            Set<T> allShadows,
            CFGAnalysis<St, T, S> cfga,
            Aliasing<T> aliasing){

		Map<S, CFG<St, T>> wholeProgramCFGApproximations = new HashMap<>();

		//methodFSM not keeping only reachable methods
		for(S method : cfga.methodCFG.keySet()){
			CFG<St, T> wholeProgramCFG = cfga.methodCFGToWholeProgramCFG(method);
			wholeProgramCFGApproximations.put(method, wholeProgramCFG);
		}

		List<T> allShadowsUpToMustAlias = new ArrayList<>();
		Map<T, Set<T>> mustAlias = new HashMap<>();

 		for(T s : allShadows){
 			Set<T> must = new HashSet<>();
 			mustAlias.put(s, must);
			for(T ss : allShadows){
				if(s == ss || aliasing.mustAlias(s, ss)){
					must.add(ss);
				}
			}
		}

 		for(T s : allShadows){

 			boolean includedAlready = false;
 			for(T ss : mustAlias.get(s)){
 				if(allShadowsUpToMustAlias.contains(ss)){
 					includedAlready = true;
 				}
 			}

 			if(!includedAlready) allShadowsUpToMustAlias.add(s);
 		}

		residuals = removeEmptyDates(residuals);

		//first iteration should be over compatible shadow sets probably
		for(T s : allShadowsUpToMustAlias){
			for(CFG<St, T> approx : wholeProgramCFGApproximations.values()){
				SubsetDate oldResidual = residuals.get(s);
				SubsetDate newResidual = cfga.sufficientResidual(s, approx, oldResidual, aliasing);
				if(newResidual == null || newResidual.neverFails){
//					System.out.println("here");
					newResidual = cfga.sufficientResidual(s, approx, oldResidual, aliasing);
				}
				residuals.put(s, newResidual);

//				if(!residuals.toString().toLowerCase().contains("property")){
//					System.out.println("here");
//				}
			}
		}
		residuals = removeEmptyDates(residuals);

		//we should also check for transitions/eventsinthecomposition that only ever loop in the same state
//		Set<Event<T>> toDisable = cfga.canBeDisabled;
//		Pair<Map<T, SubsetDate>, List<Pair<String,String>>> residualsAndPPF = new Pair<>(residuals, cfga.ppfs);

		return residuals;
	}

	public static <St, T extends CFGEvent, S extends JavaMethodIdentifier> Map<T, Pair<SubsetDate, Set<Event<T>>>> IntraProceduralControlFlowAnalysis(
	        Map<T, SubsetDate> residuals,
            Set<T> allShadows,
            CFGAnalysis<St, T, S> cfga,
            Aliasing<T> aliasing){

		Map<S, CFG<St, T>> wholeProgramCFGApproximations = new HashMap<>();

		//methodFSM not keeping only reachable methods
		for(S method : cfga.methodCFG.keySet()){
			if(method.toString().contains("main")){
				System.out.print("");
			}
			CFG<St, T> wholeProgramCFG = cfga.methodCFGToWholeProgramCFG(method);
			wholeProgramCFGApproximations.put(method, wholeProgramCFG);
		}

		List<T> allShadowsUpToMustAlias = new ArrayList<>();
		Map<T, Set<T>> mustAlias = new HashMap<>();

 		for(T s : allShadows){
 			Set<T> must = new HashSet<>();
 			mustAlias.put(s, must);
			for(T ss : allShadows){
				if(s == ss || aliasing.mustAlias(s, ss)){
					must.add(ss);
				}
			}
		}

 		for(T s : allShadows){

 			boolean includedAlready = false;
 			for(T ss : mustAlias.get(s)){
 				if(allShadowsUpToMustAlias.contains(ss)){
 					includedAlready = true;
 				}
 			}

 			if(!includedAlready) allShadowsUpToMustAlias.add(s);
 		}

		residuals = removeEmptyDates(residuals);

 		Map<T, Pair<SubsetDate, Set<Event<T>>>> shadowsToUsefulEventsAndResiduals = new HashMap<>();

		//first iteration should be over compatible shadow sets probably
		for(T s : allShadowsUpToMustAlias){
			//TODO check that before and after are being computed appropriately
			for(CFG<St, T> approx : wholeProgramCFGApproximations.values()){
				SubsetDate oldResidual = residuals.get(s);
				Pair<SubsetDate, Set<Event<T>>> newResidualAndUsefulEvents = cfga.sufficientDATETransitions(s, approx, oldResidual, aliasing);
//				if(newResidual == null || newResidual.neverFails){
////					System.out.println("here");
//					newResidual = cfga.sufficientResidual(s, approx, oldResidual, aliasing);
//				}
				if(shadowsToUsefulEventsAndResiduals.containsKey(s)){
					Pair<SubsetDate, Set<Event<T>>> oldSubsetDateSetPair = shadowsToUsefulEventsAndResiduals.get(s);

					SubsetDate newDate = newResidualAndUsefulEvents.first;
					newDate.add(oldSubsetDateSetPair.first, false);

					Set<Event<T>> newUsefulEvents = newResidualAndUsefulEvents.second;
					newUsefulEvents.addAll(oldSubsetDateSetPair.second);

					shadowsToUsefulEventsAndResiduals.put(s, new Pair<>(newDate, newUsefulEvents));
				}
				else{
					shadowsToUsefulEventsAndResiduals.put(s, newResidualAndUsefulEvents);
				}

//				if(!residuals.toString().toLowerCase().contains("property")){
//					System.out.println("here");
//				}
			}
		}
//		residuals = removeEmptyDates(residuals);
//
//		//we should also check for transitions/eventsinthecomposition that only ever loop in the same state
//		Set<Event<T>> toDisable = cfga.canBeDisabled;
//

//		Pair<Map<T, SubsetDate>, List<Pair<String,String>>> residualsAndPPF = new Pair<>(residuals, cfga.ppfs);

		return shadowsToUsefulEventsAndResiduals;
	}

	public static <T> SubsetDate residualsUnion(Map<T, SubsetDate> residuals){
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

			unionOfResiduals.reachabilityReduction();

			return unionOfResiduals;
		}
		else{
			return null;
		}
	}


	public static <T> SubsetDate residualsUnionWithoutReductions(Map<T, SubsetDate> residuals){
		if(residuals.size() != 0){
			Iterator<SubsetDate> dateIterator = residuals.values().iterator();

			SubsetDate unionOfResiduals = null;

			while(dateIterator.hasNext()){
				SubsetDate date = dateIterator.next();
				if(unionOfResiduals == null){
					unionOfResiduals = date;
				}
				else{
					unionOfResiduals.add(date, false);
				}
			}

			unionOfResiduals.reachabilityReduction();

			return unionOfResiduals;
		}
		else{
			return null;
		}
	}

	public static <T> Pair<SubsetDate, Set<Event<T>>> residualsAndEventsUnion(Map<T, Pair<SubsetDate, Set<Event<T>>>> residuals){
		if(residuals.size() != 0){
			Iterator< Pair<SubsetDate, Set<Event<T>>>> dateIterator = residuals.values().iterator();

			SubsetDate unionOfResiduals = null;
			Set<Event<T>> unionOfEvents = null;

			while(dateIterator.hasNext()){
				Pair<SubsetDate, Set<Event<T>>> current = dateIterator.next();

				SubsetDate date = current.first;
				if(unionOfResiduals == null){
					unionOfResiduals = date;
				}
				else{
					unionOfResiduals.add(date);
				}

				Set<Event<T>> cfgEvents = current.second;
				if(unionOfEvents == null){
					unionOfEvents = cfgEvents;
				}
				else{
					unionOfEvents.addAll(cfgEvents);
				}
			}

			unionOfResiduals.reachabilityReduction();

			return new Pair(unionOfResiduals, unionOfEvents);
		}
		else{
			return null;
		}
	}

	public static <T> Pair<SubsetDate, Set<Event<T>>> residualsAndEventsUnionWithoutReductions(Map<T, Pair<SubsetDate, Set<Event<T>>>> residuals){
		if(residuals.size() != 0){
			Iterator< Pair<SubsetDate, Set<Event<T>>>> dateIterator = residuals.values().iterator();

			SubsetDate unionOfResiduals = null;
			Set<Event<T>> unionOfEvents = null;

			while(dateIterator.hasNext()){
				Pair<SubsetDate, Set<Event<T>>> current = dateIterator.next();

				SubsetDate date = current.first;
				if(unionOfResiduals == null){
					unionOfResiduals = date;
				}
				else{
					unionOfResiduals.add(date, false);
				}

				Set<Event<T>> cfgEvents = current.second;
				if(unionOfEvents == null){
					unionOfEvents = cfgEvents;
				}
				else{
					unionOfEvents.addAll(cfgEvents);
				}
			}

			unionOfResiduals.reachabilityReduction();

			return new Pair(unionOfResiduals, unionOfEvents);
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
	
	private static <T extends CFGEvent> Map<T, SubsetDate> removeEmptyDates(Map<T, SubsetDate> residuals){
		List<T> toRemove = new ArrayList<>();
		
		for(T s : residuals.keySet()){
			SubsetDate respectiveDate = residuals.get(s);
			if(respectiveDate == null
					|| respectiveDate.neverFails){
				toRemove.add(s);
			}
		}
		
		for(T s : toRemove){
			residuals.remove(s);
		}
		
		return residuals;
	}

}