package clarva.analysis;


import clarva.analysis.cfg.CFG;
import clarva.analysis.cfg.CFGEvent;
import clarva.java.JavaEvent;
import clarva.java.JavaMethodIdentifier;
import clarva.matching.Aliasing;
import fsm.Event;
import fsm.Transition;
import fsm.date.DateFSM;
import fsm.date.DateLabel;
import fsm.date.SubsetDate;
import fsm.date.events.DateEvent;
import fsm.helper.Pair;

import java.util.*;

public class ControlFlowResidualAnalysis {

    public static SubsetDate QuickCheckAnalysis(DateFSM property,
                                                List<DateEvent> allEvents) {
        return new SubsetDate(property, allEvents);
    }

    //	public static Map<Shadow, SubsetDate> OrphansAnalysis(DateFSM f, MethodsAnalysis ma, Matching an){
    public static <T extends CFGEvent> Map<T, SubsetDate> OrphansAnalysis(DateFSM f,
                                                                          Set<T> allCFGEvents,
                                                                          Aliasing aliasing) {
        Map<T, SubsetDate> residuals = new HashMap<>();
        //Map<Shadow, Set<Shadow>> compatibleShadows = new HashMap<Shadow, Set<Shadow>>();

//		if(allCFGEvents.size() == 0) residuals.put(new Shadow(), new SubsetDate(f));
        for (T s : allCFGEvents) {
            Set<DateEvent> eventsToKeep = new HashSet<>();

            for (T ss : allCFGEvents) {
                if (aliasing.mayAlias(s, ss)) {
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
            Aliasing<T> aliasing) {

        Map<S, CFG<St, T>> wholeProgramCFGApproximations = new HashMap<>();

        //methodFSM not keeping only reachable methods
        for (S method : cfga.methodCFG.keySet()) {
            CFG<St, T> wholeProgramCFG = cfga.methodCFGToWholeProgramCFG(method);
            wholeProgramCFGApproximations.put(method, wholeProgramCFG);
        }

        List<T> allShadowsUpToMustAlias = new ArrayList<>();
        Map<T, Set<T>> mustAlias = new HashMap<>();

        for (T s : allShadows) {
            Set<T> must = new HashSet<>();
            mustAlias.put(s, must);
            for (T ss : allShadows) {
                if (s == ss || aliasing.mustAlias(s, ss)) {
                    must.add(ss);
                }
            }
        }

        for (T s : allShadows) {

            boolean includedAlready = false;
            for (T ss : mustAlias.get(s)) {
                if (allShadowsUpToMustAlias.contains(ss)) {
                    includedAlready = true;
                }
            }

            if (!includedAlready) allShadowsUpToMustAlias.add(s);
        }

        residuals = removeEmptyDates(residuals);

        //first iteration should be over compatible shadow sets probably
        for (T s : allShadowsUpToMustAlias) {
            for (CFG<St, T> approx : wholeProgramCFGApproximations.values()) {
                SubsetDate oldResidual = residuals.get(s);
                SubsetDate newResidual = cfga.sufficientResidual(s, approx, oldResidual, aliasing);
                if (newResidual == null || newResidual.neverFails) {
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


    public static <St, T extends CFGEvent, S extends JavaMethodIdentifier> Map<T, ResidualArtifact> IntraProceduralControlFlowAnalysis(
            Map<S, CFG<St, T>> wholeProgramCFGApproximations,
            Set<Event<T>> instrumentedEvents,
            Map<T, ResidualArtifact> residuals,
            Set<T> allShadows,
            CFGAnalysis<St, T, S> cfga,
            Aliasing<T> aliasing,
            Map<S, Set<T>> methodShadows) {


        Set<ResidualArtifact> localShadowToResidual = new HashSet<>();

        for (CFG<St, T> approx : wholeProgramCFGApproximations.values()) {
            if (approx.methodID == null) {
                continue;
            }
            String methodName = approx.methodID.toString();
            Set<T> shadowsUpToMustAlias;

            if (methodShadows.get(approx.methodID) == null) {
                shadowsUpToMustAlias = new HashSet<>();
                shadowsUpToMustAlias.add((T) new JavaEvent());
            } else {
                shadowsUpToMustAlias = paritionUpToMustAlias(methodShadows.get(approx.methodID), aliasing);
                shadowsUpToMustAlias.retainAll(residuals.keySet());
            }

            if (methodName.contains("transactionGreylistedMenu")) {
                System.out.print("");
            }

            for (T shadow : shadowsUpToMustAlias) {

                SubsetDate oldResidual = residuals.get(shadow).fullDate;
                ResidualArtifact newResidualAndUsefulEvents
                        = cfga.sufficientDATETransitionsWithSynch(shadow, approx, oldResidual, aliasing, instrumentedEvents);//residuals.get(shadow).second);
//				Pair<SubsetDate, Set<Event<T>>> newResidualAndUsefulEvents
//						= cfga.sufficientDATETransitionsWithSynch(shadow, approx, oldResidual, aliasing, instrumentedEvents);//residuals.get(shadow).second);

                localShadowToResidual.add(newResidualAndUsefulEvents);
            }
        }


        Map<T, ResidualArtifact> reducedLocalShadowToResidual = new HashMap<>();

        for (ResidualArtifact<T> residualArtifact : localShadowToResidual) {
            T shadow = (T) residualArtifact.shadow;
//			SubsetDate date = residualArtifact.fullDate;

            Map<Event<T>, Set<Transition<String, DateLabel>>> eventsAssociatedWithTransitions = residualArtifact.eventsAssociatedWithTransitions;
            Map<Event<T>, Set<Transition<String, DateLabel>>> newEventsAssociatedWithTransitions = new HashMap<Event<T>, Set<Transition<String, DateLabel>>>();

            for (Event<T> event : eventsAssociatedWithTransitions.keySet()) {
                Set<Transition<String, DateLabel>> newAssociatedTransitions = new HashSet<Transition<String, DateLabel>>(eventsAssociatedWithTransitions.get(event));

                if (JavaEvent.class.isAssignableFrom(event.label.getClass())) {
                    JavaEvent javaEvent = (JavaEvent) event.label;

                    //if the event occurs outside the method being abstracted
                    if (!((JavaEvent) shadow).callingMethod.equals(javaEvent.callingMethod)) {

                        //check each other abstraction (we should always find one such entry, otherwise it does not occur)
                        for (ResidualArtifact residualArtifact1 : localShadowToResidual) {
                            //and if we find another entry where the event occurs in the abstracted method
                            if ((((JavaEvent) residualArtifact1.shadow).callingMethod.equals(javaEvent.callingMethod))) {

                                //then keep only the DATE transitions associated with that event in the abstraction in which it occurs
                                if (residualArtifact1.eventsAssociatedWithTransitions.containsKey(event)) {
                                    newAssociatedTransitions.retainAll((Collection<?>) residualArtifact1.eventsAssociatedWithTransitions.get(event));
                                } else {
                                    newAssociatedTransitions.clear();
                                }
                            }
                        }
                    } else { //else if the event occurs inside the method being abstracted, then simply replicate it
                        newAssociatedTransitions = eventsAssociatedWithTransitions.get(event);
                    }
                }

                if (newAssociatedTransitions.size() > 0) {
                    newEventsAssociatedWithTransitions.put(event, newAssociatedTransitions);
                }
            }

            if (newEventsAssociatedWithTransitions.size() > 0) {
                Set<Transition<String, DateLabel>> usedTransitions = new HashSet<>();
                for (Set<Transition<String, DateLabel>> set : newEventsAssociatedWithTransitions.values()) {
                    usedTransitions.addAll(set);
                }

                SubsetDate subsetDate = new SubsetDate(residualArtifact.fullDate, usedTransitions);
                reducedLocalShadowToResidual.put(shadow, new ResidualArtifact(residualArtifact.shadow, subsetDate, newEventsAssociatedWithTransitions));
            }
        }


        return reducedLocalShadowToResidual;
    }
//			}

//		}
//		residuals = removeEmptyDates(residuals);
//
//		//we should also check for transitions/eventsinthecomposition that only ever loop in the same state
//		Set<Event<T>> toDisable = cfga.canBeDisabled;
//

//		Pair<Map<T, SubsetDate>, List<Pair<String,String>>> residualsAndPPF = new Pair<>(residuals, cfga.ppfs);


    public static <T extends CFGEvent> Set<T> paritionUpToMustAlias(Set<T> shadows, Aliasing aliasing) {
        Set<T> allShadowsUpToMustAlias = new HashSet<>();
        Map<T, Set<T>> mustAlias = new HashMap<>();

        if (shadows == null) {
            System.out.print("");
        }

        for (T s : shadows) {
            Set<T> must = new HashSet<>();
            mustAlias.put(s, must);
            for (T ss : shadows) {
                if (s == ss || aliasing.mustAlias(s, ss)) {
                    must.add(ss);
                }
            }
        }

        for (T s : shadows) {

            boolean includedAlready = false;
            for (T ss : mustAlias.get(s)) {
                if (allShadowsUpToMustAlias.contains(ss)) {
                    includedAlready = true;
                }
            }

            if (!includedAlready) allShadowsUpToMustAlias.add(s);
        }

        return allShadowsUpToMustAlias;
    }

    public static <T> SubsetDate residualsUnion(Map<T, SubsetDate> residuals) {
        if (residuals.size() != 0) {
            Iterator<SubsetDate> dateIterator = residuals.values().iterator();

            SubsetDate unionOfResiduals = null;

            while (dateIterator.hasNext()) {
                SubsetDate date = dateIterator.next();
                if (unionOfResiduals == null) {
                    unionOfResiduals = date;
                } else {
                    unionOfResiduals.add(date);
                }
            }

            unionOfResiduals.reachabilityReduction();

            return unionOfResiduals;
        } else {
            return null;
        }
    }


    public static <T> SubsetDate residualsUnionWithoutReductions(Map<T, SubsetDate> residuals) {
        if (residuals.size() != 0) {
            Iterator<SubsetDate> dateIterator = residuals.values().iterator();

            SubsetDate unionOfResiduals = null;

            while (dateIterator.hasNext()) {
                SubsetDate date = dateIterator.next();
                if (unionOfResiduals == null) {
                    unionOfResiduals = date;
                } else {
                    unionOfResiduals.add(date, false);
                }
            }

            unionOfResiduals.reachabilityReduction();

            return unionOfResiduals;
        } else {
            return null;
        }
    }

    public static <T> Pair<SubsetDate, Set<Event<T>>> residualsAndEventsUnion(Map<T, Pair<SubsetDate, Set<Event<T>>>> residuals) {
        if (residuals.size() != 0) {
            Iterator<Pair<SubsetDate, Set<Event<T>>>> dateIterator = residuals.values().iterator();

            SubsetDate unionOfResiduals = null;
            Set<Event<T>> unionOfEvents = null;

            while (dateIterator.hasNext()) {
                Pair<SubsetDate, Set<Event<T>>> current = dateIterator.next();

                SubsetDate date = current.first;
                if (unionOfResiduals == null) {
                    unionOfResiduals = date;
                } else {
                    unionOfResiduals.add(date);
                }

                Set<Event<T>> cfgEvents = current.second;
                if (unionOfEvents == null) {
                    unionOfEvents = cfgEvents;
                } else {
                    unionOfEvents.addAll(cfgEvents);
                }
            }

            unionOfResiduals.reachabilityReduction();

            return new Pair(unionOfResiduals, unionOfEvents);
        } else {
            return null;
        }
    }

    public static <T extends CFGEvent> Pair<SubsetDate, Set<Event<T>>> residualsAndEventsUnionWithoutReductions(DateFSM date, Aliasing aliasing, Map<T, ResidualArtifact> residuals) {

        Map<T, ResidualArtifact> cleanResidualSet = new HashMap<>();
        Set<Set<T>> mayAliasingSets = new HashSet<>();

        for (T shadow : residuals.keySet()) {
            Set<T> aliasingSet = new HashSet<>();

            if (residuals.get(shadow).fullDate.transitions.size() != 0 && !residuals.get(shadow).eventsAssociatedWithTransitions.isEmpty()) {
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

        for (Set<T> aliasingSet : mayAliasingSets) {
            List<Transition<String, DateLabel>> usedTransitions = new ArrayList<>();
            Set<Event<T>> usedEvents = new HashSet<>();

            Set<T> cleanedAliasingSet = new HashSet<>(aliasingSet);
            cleanedAliasingSet.retainAll(cleanResidualSet.keySet());

            for (T shadow : cleanedAliasingSet) {
                for (Event<T> event : (Collection<? extends Event<T>>) cleanResidualSet.get(shadow).eventsAssociatedWithTransitions.keySet()) {
                    if (((JavaEvent) event.label).callingMethod.equals(((JavaEvent) shadow).callingMethod)) {
                        usedTransitions.addAll((Collection<? extends Transition<String, DateLabel>>) cleanResidualSet.get(shadow).eventsAssociatedWithTransitions.get(event));
                        usedEvents.add(event);
                    }
                }
            }

            //this automatically performs reachability reduction
            SubsetDate subsetDate = new SubsetDate(date, usedTransitions);
            if (!subsetDate.neverFails) {
                setsOfDates.add(subsetDate);
                for (Event<T> event : usedEvents) {
                    if (!event.label.epsilon) {
                        if (subsetDate.eventUsedInGuardedCommand.values().contains(event.label.dateEvent)) {
                            allUsefulEvents.add(event);
                        }
                    }
                }
            }
        }


        if (setsOfDates.size() != 0) {
            Iterator<SubsetDate> dateIterator = setsOfDates.iterator();

            SubsetDate unionOfResiduals = null;

            while (dateIterator.hasNext()) {

                SubsetDate currentDate = dateIterator.next();
                if (unionOfResiduals == null) {
                    unionOfResiduals = currentDate;
                } else {
                    unionOfResiduals.add(currentDate, false);
                }
            }

            unionOfResiduals.reachabilityReduction();

            return new Pair(unionOfResiduals, allUsefulEvents);
        } else {
            return new Pair(new SubsetDate(date, new ArrayList<Transition<String, DateLabel>>()), new HashSet<>());
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

    private static <T extends CFGEvent> Map<T, SubsetDate> removeEmptyDates(Map<T, SubsetDate> residuals) {
        List<T> toRemove = new ArrayList<>();

        for (T s : residuals.keySet()) {
            SubsetDate respectiveDate = residuals.get(s);
            if (respectiveDate == null
                    || respectiveDate.neverFails) {
                toRemove.add(s);
            }
        }

        for (T s : toRemove) {
            residuals.remove(s);
        }

        return residuals;
    }

}