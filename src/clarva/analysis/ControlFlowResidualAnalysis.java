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
import clarva.java.JavaEvent;
import clarva.java.JavaMethodIdentifier;
import clarva.matching.Aliasing;
import clarva.matching.MethodIdentifier;
import fsm.Event;
import fsm.Transition;
import fsm.date.DateFSM;
import fsm.date.DateLabel;
import fsm.date.SubsetDate;
import fsm.date.events.DateEvent;
import fsm.helper.Pair;
import jas.Method;
import soot.util.ArraySet;

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
			Map<T, Pair<SubsetDate, Set<Event<T>>>> residuals,
			Set<T> allShadows,
            CFGAnalysis<St, T, S> cfga,
			Aliasing<T> aliasing,
			Map<S, Set<T>> methodShadows) {

		Map<S, CFG<St, T>> wholeProgramCFGApproximations = new HashMap<>();

		//methodFSM not keeping only reachable methods
		for (S method : cfga.methodCFG.keySet()) {
			if (method.toString().contains("transactionGreylistedMenu")) {
				System.out.print("");
			}

			CFG<St, T> wholeProgramCFG = cfga.methodCFGToWholeProgramCFG(method);
			wholeProgramCFGApproximations.put(method, wholeProgramCFG);
		}

		Map<T, Pair<SubsetDate, Set<Event<T>>>> localShadowToResidual = new HashMap<>();

		for (CFG<St, T> approx : wholeProgramCFGApproximations.values()) {

			Set<T> shadowsUpToMustAlias = paritionUpToMustAlias(methodShadows.get(approx.methodID), aliasing);
			shadowsUpToMustAlias.retainAll(residuals.keySet());

			String methodName = approx.methodID.toString();
			if(methodName.contains("transactionGreylistedMenu")){
				System.out.print("");
			}

			for (T shadow : shadowsUpToMustAlias) {

				SubsetDate oldResidual = residuals.get(shadow).first;
				Pair<SubsetDate, Set<Event<T>>> newResidualAndUsefulEvents = cfga.sufficientDATETransitionsWithSynch(shadow, approx, oldResidual, aliasing, residuals.get(shadow).second);

				localShadowToResidual.put(shadow, newResidualAndUsefulEvents);
			}
		}

		return localShadowToResidual;
	}
//			}

//		}
//		residuals = removeEmptyDates(residuals);
//
//		//we should also check for transitions/eventsinthecomposition that only ever loop in the same state
//		Set<Event<T>> toDisable = cfga.canBeDisabled;
//

//		Pair<Map<T, SubsetDate>, List<Pair<String,String>>> residualsAndPPF = new Pair<>(residuals, cfga.ppfs);


	public static <T extends CFGEvent> Set<T> paritionUpToMustAlias(Set<T> shadows, Aliasing aliasing){
		Set<T> allShadowsUpToMustAlias = new HashSet<>();
		Map<T, Set<T>> mustAlias = new HashMap<>();

 		for(T s : shadows){
 			Set<T> must = new HashSet<>();
 			mustAlias.put(s, must);
			for(T ss : shadows){
				if(s == ss || aliasing.mustAlias(s, ss)){
					must.add(ss);
				}
			}
		}

 		for(T s : shadows){

 			boolean includedAlready = false;
 			for(T ss : mustAlias.get(s)){
 				if(allShadowsUpToMustAlias.contains(ss)){
 					includedAlready = true;
 				}
 			}

 			if(!includedAlready) allShadowsUpToMustAlias.add(s);
 		}

 		return allShadowsUpToMustAlias;
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

	public static <T extends CFGEvent> Pair<SubsetDate, Set<Event<T>>> residualsAndEventsUnionWithoutReductions(DateFSM date, Aliasing aliasing, Map<T, Pair<SubsetDate, Set<Event<T>>>> residuals){

		Map<T, Pair<SubsetDate, Set<Event<T>>>> cleanResidualSet = new HashMap<>();
		Set<Set<T>> mayAliasingSets = new HashSet<>();

		for(T shadow : residuals.keySet()) {
			Set<T> aliasingSet = new HashSet<>();

			if (residuals.get(shadow).first.transitions.size() != 0 && residuals.get(shadow).second.size() != 0) {
				cleanResidualSet.put(shadow, residuals.get(shadow));

				for (T shadow2 : residuals.keySet()) {
					residuals.keySet().forEach(t -> {
						if (aliasing.mayAlias(shadow, t)) {
							aliasingSet.add(shadow2);
						}
					});
				}
			}

			mayAliasingSets.add(aliasingSet);
		}

		Set<SubsetDate> setsOfDates = new HashSet<>();
		Set<Event<T>> allUsefulEvents = new HashSet<>();

		for(Set<T> aliasingSet : mayAliasingSets){
			List<Transition<String, DateLabel>> usedTransitions = new ArrayList<>();
			Set<Event<T>> usedEvents = new HashSet<>();

			Set<T> cleanedAliasingSet = new HashSet<>(aliasingSet);
			cleanedAliasingSet.retainAll(cleanResidualSet.keySet());

			for(T shadow : cleanedAliasingSet){
				SubsetDate subsetDate = cleanResidualSet.get(shadow).first;

				usedTransitions.addAll(subsetDate.transitions);

				Set<Event<T>> instrumentedEvents = cleanResidualSet.get(shadow).second;

				usedEvents.addAll(instrumentedEvents);
			}

			//this automatically performs reachability reduction
			SubsetDate subsetDate = new SubsetDate(date, usedTransitions);
			if(!subsetDate.neverFails) {
				setsOfDates.add(subsetDate);
				for(Event<T> event : usedEvents){
					if(!event.label.epsilon) {
						if (subsetDate.eventUsedInGuardedCommand.values().contains(event.label.dateEvent)) {
							allUsefulEvents.add(event);
						}
					}
				}
			}
		}


		if(residuals.size() != 0){
			Iterator<SubsetDate> dateIterator = setsOfDates.iterator();

			SubsetDate unionOfResiduals = null;

			while(dateIterator.hasNext()){

				SubsetDate currentDate = dateIterator.next();
				if(unionOfResiduals == null){
					unionOfResiduals = currentDate;
				}
				else{
					unionOfResiduals.add(currentDate, false);
				}
			}

			unionOfResiduals.reachabilityReduction();

			return new Pair(unionOfResiduals, allUsefulEvents);
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