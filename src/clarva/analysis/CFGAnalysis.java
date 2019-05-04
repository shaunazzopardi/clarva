package clarva.analysis;

import clarva.analysis.cfg.CFG;
import clarva.analysis.cfg.CFGEvent;
import clarva.matching.Aliasing;
import clarva.matching.MethodIdentifier;
import fsm.Event;
import fsm.FSM;
import fsm.State;
import fsm.Transition;
import fsm.date.DateFSM;
import fsm.date.DateLabel;
import fsm.date.SubsetDate;
import fsm.date.events.ChannelEvent;
import fsm.date.events.ClockEvent;
import fsm.date.events.DateEvent;
import fsm.date.events.MethodCall;
import fsm.helper.Pair;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;

public abstract class CFGAnalysis <St, T extends CFGEvent, MethodID extends MethodIdentifier>{

    public Map<MethodID, CFG<St, T>> methodCFG;
    public Map<CFG<St, T>, MethodID> CFGMethod;
    public Map<MethodID, Boolean> methodNoDirectMethodCall;
    public Map<MethodID, Boolean> methodNoMatchingMethodCall;
    public Map<MethodID, Boolean> allStatesNull;
    public Map<MethodID, Aliasing> methodAliasing;

    public Map<MethodID, List<Event<T>>> eventsPossiblyOccurringBeforeMethod;
    public Map<MethodID, List<Event<T>>> eventsPossiblyOccurringAfterMethod;

    public Map<MethodID, Set<MethodID>> allMethodsSucceeding;

    public Set<MethodID> reachableMethods;
    public Set<MethodID> mainMethods;

    public Map<St, Set<MethodID>> statesCallMethods;
    public Map<MethodID, Set<St>> methodCalledByStates;

    public Map<St, MethodIdentifier> statementCalledBy;


    public Map<ChannelEvent, T> channelCFGEvents;
    public Map<ClockEvent, T> clockCFGEvents;
    //
    public Set<Event<T>> allEventShadows = new HashSet<>();
    //We use this action to denote tau-actions
    //i.e. actions (points of execution) that
    //are irrelevant to the properties considered
    public Event<T> epsilonAction;

    //    List<Pair<String, String>> ppfs = new ArrayList<Pair<String, String>>();
//    public Set<Event<T>> canBeDisabled = new HashSet<Event<T>>();

    public CFGAnalysis() {
        mainMethods = new HashSet<>();
        methodCFG = new HashMap<>();
        CFGMethod = new HashMap<>();
        methodNoDirectMethodCall = new HashMap<>();
        allStatesNull = new HashMap<>();
        methodAliasing = new HashMap<>();

        statesCallMethods = new HashMap<>();
        methodCalledByStates = new HashMap<>();

        eventsPossiblyOccurringBeforeMethod = new HashMap<>();
        eventsPossiblyOccurringAfterMethod = new HashMap<>();

        methodNoMatchingMethodCall = new HashMap<>();
        allMethodsSucceeding = new HashMap<>();

        channelCFGEvents = new HashMap<>();
        clockCFGEvents = new HashMap<>();
        //epsilon action needs to be instantiated
    }

    public abstract void createChannelAndClockEvents(DateFSM date);

    public Map<MethodID, Set<MethodID>> calls = new HashMap<>();

    public void transitivelyCalling(){

        for (MethodID methodID : this.methodCFG.keySet()) {
            Set<MethodID> succeeding = new HashSet<>();


            for(FSM cfg : methodCFG.get(methodID).internalFSMs){
                if(CFGMethod.get(cfg) != null) {
                    succeeding.add(CFGMethod.get(cfg));
                }
            }

            Set<MethodID> toAdd = new HashSet<>();

            do{
                for(MethodID succ : succeeding){
                    if(this.calls.containsKey(succ)) {
                        toAdd.addAll(this.calls.get(succ));
                    }
                    toAdd.removeAll(succeeding);
                }

                succeeding.addAll(toAdd);
            } while(toAdd.size() != 0);

            this.calls.put(methodID, succeeding);
        }
    }

    public Set<Event<T>> relevantShadows(MethodID method, MethodID methodInvokedInThis) {
        Set<Event<T>> relevantShadows = new HashSet<Event<T>>();

        if (this.methodCFG.get(method) == null) return relevantShadows;

        relevantShadows.addAll((Collection<? extends Event<T>>) this.methodCFG.get(method).alphabet);

        if (this.calls.get(method) != null) {
            for (MethodID otherMethod : this.calls.get(method)) {
                if (!otherMethod.equals(method)
                        && !otherMethod.equals(methodInvokedInThis)) {
                    if (this.methodCFG.get(otherMethod) != null)
                        relevantShadows.addAll((Collection<? extends Event<T>>) this.methodCFG.get(otherMethod).alphabet);
                }
            }
        }

        relevantShadows.removeIf(new Predicate<Event<T>>() {
            @Override
            public boolean test(Event<T> e) {
                return e.label.epsilon;
            }
        });
        return relevantShadows;
    }

//    Map<State, Set<MethodID>> statesPossiblyCalling
//
//    public abstract List<State> callingStates(MethodID method);

    public abstract List<Event<T>> shadowsBefore(MethodID method,
                                             List<MethodID> methodsAlreadyTraversed);

    public abstract List<Event<T>> shadowsAfter(MethodID method);

    public abstract Pair<List<Event<T>>, List<Event<T>>> shadowsBeforeAndAfter(MethodID method); //{
//        List<Event<T>> before = new ArrayList<>();
//        List<Event<T>> after = new ArrayList<>();
//
//        Pair<List<Event<T>>, List<Event<T>>> beforeAfter = new Pair<>(before, after);
//
//        if (reachableMethods.contains(method)) {
//            before.addAll(this.shadowsBefore(method, new ArrayList<>()));
//            after.addAll(this.shadowsAfter(method));
//        }
//
//        return beforeAfter;
//    }

    public CFG<St, T> methodCFGToWholeProgramCFG(MethodID method) {
        CFG<St, T> methodCFG = this.methodCFG.get(method);

        //if we create new fsm we run out of memory
        CFG<St, T> wholeProgramCFG = methodCFG;// new CFG(methodCFG);//new FSM<Unit, Shadow>(methodCFG);
        boolean changed = false;
        if (!mainMethods.contains(method)) {

            Pair<List<Event<T>>, List<Event<T>>> beforeAfter = this.shadowsBeforeAndAfter(method);

            for (State<Integer, T> initial : wholeProgramCFG.initial) {
                for (Event<T> shadow : beforeAfter.first) {
                    wholeProgramCFG.addTransition(initial, shadow, initial);
                    changed = true;
                }
            }

            for (State<Integer, T> finalState : wholeProgramCFG.finalStates) {
                //add restofprogram alphabet loops
                for (Event<T> shadow : beforeAfter.second) {
                    wholeProgramCFG.addTransition(finalState, shadow, finalState);
                    changed = true;
                }

                //we should only do this if methods can invoke this method can again invoke it after
                for (State<Integer, T> initial : wholeProgramCFG.initial) {
                    wholeProgramCFG.addTransition(finalState, this.epsilonAction, initial);
                    changed = true;
                }
            }
        }

        ArrayList<State<Integer, T>> stateList = new ArrayList<>(wholeProgramCFG.states);
        for (int i = 0; i < stateList.size(); i++) {
            State<Integer, T> state = stateList.get(i);
            if (state.getInternalFSM() != null) {
                //add loops for all shadows relevant to internalFSM
                MethodID methodInvokedHere = this.CFGMethod.get(state.getInternalFSM());

                //are the two conditions disjuncted below equal? they should be i think
                if (methodInvokedHere == null) {
                    state.setInternalFSM(null);
                } else if (methodInvokedHere.equals(method)
                        || this.calls.get(methodInvokedHere).contains(method)) {
                    for (State<Integer, T> initial : wholeProgramCFG.initial) {
                        wholeProgramCFG.addTransition(state, epsilonAction, initial);
                        changed = true;
                    }
                }

                Set<Event<T>> relevantShadows = this.relevantShadows(methodInvokedHere, method);
                for (Event<T> shadow : relevantShadows) {
                    wholeProgramCFG.addTransition(state, shadow, state);
                    changed = true;
                }

                state.setInternalFSM(null);
            }
        }

        if (!changed) {
            //System.out.println("here");
        }

        return wholeProgramCFG;
    }

    public SubsetDate sufficientResidual(T s, CFG wholeProgramCFG, DateFSM property, Aliasing aliasing) {
        SubsetDate residual;

        Set<Transition<String, DateLabel>> transitionsToKeep = new HashSet<Transition<String, DateLabel>>();

        if (property == null) return new SubsetDate(new DateFSM());
        FSM<Pair<Integer, Set<String>>, T> composition = this.synchronousComposition(wholeProgramCFG, property, s, aliasing);
        //		System.out.println(s);
        //		System.out.println(wholeProgramCFG);
        //		System.out.println(composition);

        Map<String, Set<String>> stateToNewHoareTripleMethods = new HashMap<String, Set<String>>();
        for (String label : property.stateHoareTripleMethod.keySet()) {
            stateToNewHoareTripleMethods.put(label, new HashSet<String>(property.stateHoareTripleMethod.get(label)));
        }

        for (State<Pair<Integer, Set<String>>, T> state : composition.states) {
            if (state.label.second.size() > 0) {
                for (String label : state.label.second) {
                    if (stateToNewHoareTripleMethods.get(label) != null) {
                        for (Event<T> event : state.outgoingTransitions.keySet()) {
                            if (!event.label.epsilon) {
                                String methodName = event.label.dateEvent.name;
                                String methodClass = ((MethodCall) event.label.dateEvent).objectType;

                                String method = methodClass + "." + methodName;
                                stateToNewHoareTripleMethods.get(label).add(method);
                            }
                        }
                    }
                }

            }
        }

        for (Transition<Pair<Integer, Set<String>>, T> transition : composition.transitions) {
            Set<String> sourcePropertyStates = transition.source.label.second;
            //Set<String> destinationPropertyStates = transition.source.label.second;
            DateEvent event = transition.event.label.dateEvent;

            for (String label : sourcePropertyStates) {
                State<String, DateLabel> source = property.labelToState.get(label);
                for (Event<DateLabel> eventConditionAction : source.outgoingTransitions.keySet()) {
                    if (eventConditionAction.label.event.equals(event)) {
                        for (String destinationLabel : source.outgoingTransitions.get(eventConditionAction)) {
                            State<String, DateLabel> destination = property.labelToState.get(destinationLabel);
                            transitionsToKeep.add(new Transition<String, DateLabel>(source, destination, eventConditionAction));
                        }
                    }
                }

            }
        }

        residual = new SubsetDate(property, transitionsToKeep, stateToNewHoareTripleMethods);

        return residual;
    }

    public SubsetDate residualFromComposition(DateFSM property, FSM<Pair<Integer, Set<String>>, T> composition){
        SubsetDate residual;

        Set<Transition<String, DateLabel>> transitionsToKeep = new HashSet<Transition<String, DateLabel>>();

        if (property == null) return new SubsetDate(new DateFSM());

        Map<String, Set<String>> stateToNewHoareTripleMethods = new HashMap<String, Set<String>>();
        for (String label : property.stateHoareTripleMethod.keySet()) {
            stateToNewHoareTripleMethods.put(label, new HashSet<String>(property.stateHoareTripleMethod.get(label)));
        }

        for (State<Pair<Integer, Set<String>>, T> state : composition.states) {
            if (state.label.second.size() > 0) {
                for (String label : state.label.second) {
                    if (stateToNewHoareTripleMethods.get(label) != null) {
                        for (Event<T> event : state.outgoingTransitions.keySet()) {
                            if (!event.label.epsilon) {
                                String methodName = event.label.dateEvent.name;
                                String methodClass = ((MethodCall) event.label.dateEvent).objectType;

                                String method = methodClass + "." + methodName;
                                stateToNewHoareTripleMethods.get(label).add(method);
                            }
                        }
                    }
                }

            }
        }

        for (Transition<Pair<Integer, Set<String>>, T> transition : composition.transitions) {
            Set<String> sourcePropertyStates = transition.source.label.second;
            //Set<String> destinationPropertyStates = transition.source.label.second;
            DateEvent event = transition.event.label.dateEvent;

            for (String label : sourcePropertyStates) {
                State<String, DateLabel> source = property.labelToState.get(label);
                for (Event<DateLabel> eventConditionAction : source.outgoingTransitions.keySet()) {
                    if (eventConditionAction.label.event.equals(event)) {
                        for (String destinationLabel : source.outgoingTransitions.get(eventConditionAction)) {
                            State<String, DateLabel> destination = property.labelToState.get(destinationLabel);
                            transitionsToKeep.add(new Transition<String, DateLabel>(source, destination, eventConditionAction));
                        }
                    }
                }

            }
        }

        residual = new SubsetDate(property, transitionsToKeep, stateToNewHoareTripleMethods);

        return residual;
    }

    public Pair<SubsetDate, Set<Event<T>>> sufficientDATETransitions(T s, CFG wholeProgramCFG, DateFSM property, Aliasing aliasing) {
        SubsetDate residual;

        Set<Transition<String, DateLabel>> transitionsToKeep = new HashSet<Transition<String, DateLabel>>();

        if (property == null) return new Pair(new SubsetDate(new DateFSM()), new HashSet());

        FSM<Pair<Integer, Set<String>>, T> composition = this.synchronousComposition(wholeProgramCFG, property, s, aliasing);
        //		System.out.println(s);
        //		System.out.println(wholeProgramCFG);
        //		System.out.println(composition);

        Map<String, Set<String>> stateToNewHoareTripleMethods = new HashMap<String, Set<String>>();
        for (String label : property.stateHoareTripleMethod.keySet()) {
            stateToNewHoareTripleMethods.put(label, new HashSet<String>(property.stateHoareTripleMethod.get(label)));
        }


        for (Transition<Pair<Integer, Set<String>>, T> transition : composition.transitions) {
            Set<String> sourcePropertyStates = transition.source.label.second;
            //Set<String> destinationPropertyStates = transition.source.label.second;
            DateEvent event = transition.event.label.dateEvent;

            if(event != null) {
                for (String label : sourcePropertyStates) {
                    //this is the difference from the other method
                    //check that transition does not match transitions outside the method (i.e. loops on initial, final and call states
                    if (!(transition.source.label.first.equals(transition.destination.label.first)
                            && event.getClass().equals(MethodCall.class))) {
                        State<String, DateLabel> source = property.labelToState.get(label);
                        for (Event<DateLabel> eventConditionAction : source.outgoingTransitions.keySet()) {
                            if (eventConditionAction.label.event.equals(event)) {
                                for (String destinationLabel : source.outgoingTransitions.get(eventConditionAction)) {
                                    State<String, DateLabel> destination = property.labelToState.get(destinationLabel);
                                    transitionsToKeep.add(new Transition<String, DateLabel>(source, destination, eventConditionAction));
                                }
                            }
                        }
                    }
                }
            }
        }

        //using subsetDate instead of property since it is reduced for reachability
        SubsetDate subsetDate = residualFromComposition(property, composition);

        residual = new SubsetDate(subsetDate, transitionsToKeep, stateToNewHoareTripleMethods, false);

        return new Pair(residual, composition.alphabet);
    }

    //assuming flat fsm
    public FSM<Pair<Integer, Set<String>>, T> synchronousComposition(CFG cfg,
                                                                            DateFSM date,
                                                                            T shadow,
                                                                            Aliasing aliasing) {
        //Pair<Set<Shadow>,Set<Shadow>> mustAndMay = this.mustAndMayAliasShadows(shadow);
        FSM<Pair<Integer, Set<String>>, T> composition = new FSM<>();

        createChannelAndClockEvents(date);

        Set<State<Pair<Integer, Set<String>>, T>> statesToTransitionOn = new HashSet<>();

        Iterator<State<Integer, T>> initial = cfg.initial.iterator();
        if (!initial.hasNext()) return composition;

        if (date.neverFails) return composition;
        //get initial property state
        Set<String> initialPropertiesStates = new HashSet<String>();
        initialPropertiesStates.add(date.startingState.label);

        Pair<Integer, Set<String>> initialTag = new Pair<Integer, Set<String>>(initial.next().label, initialPropertiesStates);
        statesToTransitionOn.add(composition.getOrAddState(initialTag));

        while (statesToTransitionOn.size() > 0) {

            Set<State<Pair<Integer, Set<String>>, T>> statesTransitionedTo;

            //	 statesToTransitionOn = this.expandPropertyStates(statesToTransitionOn);
            //pushes composition states with clock and channel events
            //i.e. takes all possible clock and channel events
            statesTransitionedTo = pushStates(composition, date, cfg, statesToTransitionOn);
            //pushes composition states with methodcalls
            statesTransitionedTo = pushStatesOneStep(composition, shadow, date, cfg, statesTransitionedTo, aliasing);
            statesToTransitionOn.clear();

            for (State<Pair<Integer, Set<String>>, T> state : statesTransitionedTo) {
                if (!cfg.finalStates.contains(cfg.integerToState.get(state.label.first))
                        && !(state.outgoingTransitions.entrySet().size() > 0)//this condition makes sure we iterate over states only one time
                        && state.label.second.size() != 0) {
                    statesToTransitionOn.add(state);
                }
            }
        }

        composition.removeUnusedEvents();

        return composition;
    }



    public Set<State<Pair<Integer, Set<String>>, T>> pushStatesOneStep(FSM<Pair<Integer, Set<String>>, T> composition,
                                                                            T shadow,
                                                                            DateFSM date,
                                                                            CFG cfg,
                                                                            Set<State<Pair<Integer, Set<String>>, T>> statesToTransitionOn,
                                                                            Aliasing aliasing) {

        Set<State<Pair<Integer, Set<String>>, T>> statesTransitionedTo = new HashSet<>();

        for (State<Pair<Integer, Set<String>>, T> state : statesToTransitionOn) {
            CFG stateFSM = cfg;//methodCFG.get(statementCalledBy.get(cfg.statements.get(state.label.first)));
            //get the cfgState of the state in the composition we are now considering
            State<Integer, T> cfgState = stateFSM.getOrAddState(state.label.first);
            //get the property states of the state in the composition
            Set<String> propertyStates = state.label.second;


            //for each possible transition w.r.t to the cfg state
            for (Event<T> event : cfgState.outgoingTransitions.keySet()) {
                Set<String> nextPropertyStates = new HashSet<String>();
                //if the dateEvent is an epsilon transition then do not transition w.r.t to the property states
                if (event.label == null || event.label.epsilon || !aliasing.mayAlias(event.label, shadow)) {
                    nextPropertyStates.addAll(propertyStates);
                } else {
                    //if the events not must-alias but may-alias then keep all the previous states also
                    if (!aliasing.mustAlias(event.label, shadow) && aliasing.mayAlias(event.label, shadow)) {
                        nextPropertyStates.addAll(propertyStates);
                    }
                    //for each property state label
                    for (String propertyStateLabel : propertyStates) {
                        //get the property state from the label
                        State<String, DateLabel> propertyState = date.getOrAddState(propertyStateLabel);

                        // if(propertyState.outgoingTransitions.get(dateEvent.label.dateEvent) != null){
                        //for all property state transitions

                        for (Event<DateLabel> eventConditionAction : propertyState.outgoingTransitions.keySet()) {
                            //if one can transition from the property state using the shadow's matched dateEvent
                            if (eventConditionAction.label.event.equals(event.label.dateEvent)) {
                                //then transition for all transitions using this dateEvent
                                for (String nextStateLabel : propertyState.outgoingTransitions.get(eventConditionAction)) {
                                    nextPropertyStates.add(nextStateLabel);
                                }
                            }
                            //	 }
                        }

                        if (nextPropertyStates.size() == 0) nextPropertyStates.addAll(propertyStates);
                    }
                }

                //if the cfg state can transition with the shadow
                if (cfgState.outgoingTransitions.containsKey(event)) {
                    //get an iterator over all the possible outgoing states after the shadow
                    Iterator<Integer> iterator = cfgState.outgoingTransitions.get(event).iterator();
                    while (iterator.hasNext()) {

                        //get the first state on the iterator
                        Integer destinationStateLabel = iterator.next();
                        State<Integer, T> destinationState = cfgState.parent.labelToState.get(destinationStateLabel);

                        //create a new label with the cfg state after the shadow and the calculated next property states
                        Pair<Integer, Set<String>> nextStateLabel = new Pair<Integer, Set<String>>(destinationState.label, nextPropertyStates);

                        //add the state created in the previous line to the composition
                        State<Pair<Integer, Set<String>>, T> nextState = composition.getOrAddState(nextStateLabel);

                        composition.addTransition(state, event, nextState);

                        statesTransitionedTo.add(nextState);

                    }
                }
            }
        }


        //statesTransitionedTo.removeAll(statesToTransitionOn);
        return statesTransitionedTo;

    }

    public Set<State<Pair<Integer, Set<String>>, T>> pushStates(FSM<Pair<Integer, Set<String>>, T> composition,
                                                                       DateFSM date,
                                                                       CFG cfg,
                                                                       Set<State<Pair<Integer, Set<String>>, T>> statesToTransitionOn) {

        Set<State<Pair<Integer, Set<String>>, T>> statesTransitionedTo = new HashSet<>();

        for (State<Pair<Integer, Set<String>>, T> state : statesToTransitionOn) {
            CFG stateFSM = cfg;//methodCFG.get(statementCalledBy.get(cfg.statements.get(state.label.first)));
            //get the cfgState of the state in the composition we are now considering
            State<Integer, T> cfgState = stateFSM.getOrAddState(state.label.first);
            //get the property states of the state in the composition
            Set<String> propertyStates = state.label.second;

            for (String propertyStateLabel : propertyStates) {
                State<String, DateLabel> propertyState = date.getOrAddState(propertyStateLabel);

                for (Entry<Event<DateLabel>, Set<String>> entry : propertyState.outgoingTransitions.entrySet()) {

//					Entry<Event<DateLabel>,Set<State<String,DateLabel>>> entry;

                    if (entry.getKey().label.event instanceof ChannelEvent) {
                        ChannelEvent channelEvent = (ChannelEvent) entry.getKey().label.event;
//                        Event<T> event = new Event<>(new T(channelEvent));
                        Event<T> event = new Event(channelCFGEvents.get(channelEvent));

                        Set<String> newPropertyStates = new HashSet<String>(propertyStates);
                        newPropertyStates.remove(propertyStateLabel);

                        for (String nextStateLabel : entry.getValue()) {
                            newPropertyStates.add(nextStateLabel);
                        }

                        Pair<Integer, Set<String>> newStateLabel = new Pair<Integer, Set<String>>(cfgState.label, newPropertyStates);

                        State<Pair<Integer, Set<String>>, T> newState = composition.getOrAddState(newStateLabel);
                        composition.addTransition(state, event, newState);

                        statesTransitionedTo.add(newState);
                    } else if (entry.getKey().label.event instanceof ClockEvent) {
                        ClockEvent clockEvent = (ClockEvent) entry.getKey().label.event;
//                        Event<T> event = new Event<>(new T(clockEvent));
                        Event<T> event = new Event(clockCFGEvents.get(clockEvent));

                        Set<String> newPropertyStates = new HashSet<String>(propertyStates);
                        newPropertyStates.remove(propertyStateLabel);

                        for (String nextStateLabel : entry.getValue()) {
                            newPropertyStates.add(nextStateLabel);
                        }

                        Pair<Integer, Set<String>> newStateLabel = new Pair<Integer, Set<String>>(cfgState.label, newPropertyStates);

                        State<Pair<Integer, Set<String>>, T> newState = composition.getOrAddState(newStateLabel);
                        composition.addTransition(state, event, newState);

                        statesTransitionedTo.add(newState);
                    } else {
                        statesTransitionedTo.add(state);
                    }
                }
            }
        }

        return statesTransitionedTo;
    }
}