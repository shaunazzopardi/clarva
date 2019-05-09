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

public abstract class CFGAnalysis<St, T extends CFGEvent, MethodID extends MethodIdentifier> {

    public Map<MethodID, CFG<St, T>> methodCFG;
    public Map<CFG<St, T>, MethodID> CFGMethod;
    public Map<MethodID, Boolean> methodNoDirectMethodCall;
    public Map<MethodID, Boolean> methodNoMatchingMethodCall;
    public Map<MethodID, Boolean> allStatesNull;
    public Map<MethodID, Aliasing> methodAliasing;

    public Map<MethodID, List<Event<T>>> eventsPossiblyOccurringBeforeMethod;
    public Map<MethodID, List<Event<T>>> eventsPossiblyOccurringAfterMethod;

//    public Map<MethodID, Set<MethodID>> allMethodsSucceeding;

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
//        allMethodsSucceeding = new HashMap<>();

        channelCFGEvents = new HashMap<>();
        clockCFGEvents = new HashMap<>();
        //epsilon action needs to be instantiated
    }

    public abstract void createChannelAndClockEvents(DateFSM date);

//    public Map<MethodID, Set<MethodID>> calls = new HashMap<>();
//
//    public void generateMethodCallGraph(){
//    public void generateMethodCallGraph(){
//
//        for (MethodID methodID : this.methodCFG.keySet()) {
//            Set<MethodID> succeeding = new HashSet<>();
//
//            for(FSM cfg : methodCFG.get(methodID).internalFSMs){
//                if(CFGMethod.get(cfg) != null) {
//                    succeeding.add(CFGMethod.get(cfg));
//                }
//            }
//
//            Set<MethodID> toAdd = new HashSet<>();
//
//            do{
//                for(MethodID succ : succeeding){
//                    if(this.calls.containsKey(succ)) {
//                        toAdd.addAll(this.calls.get(succ));
//                    }
//                    toAdd.removeAll(succeeding);
//                }
//
//                succeeding.addAll(toAdd);
//            } while(toAdd.size() != 0);
//
//            this.calls.put(methodID, succeeding);
//        }
//    }

    public abstract void createCFG(MethodIdentifier method);

    public Pair<Set<Event<T>>, Boolean> relevantShadows(MethodID method, MethodID methodInvokedInThis) {
        Set<Event<T>> relevantShadows = new HashSet<Event<T>>();

        boolean reentersInvokingMethod = false;

        if (method == null || methodInvokedInThis == null || method.equals(methodInvokedInThis))
            return new Pair<>(relevantShadows, reentersInvokingMethod);
        else {
            CFG<St, T> methodsCFG = this.methodCFG.get(method);
            if (methodsCFG == null) return new Pair<>(relevantShadows, reentersInvokingMethod);

            Set<MethodID> calledMethods = new HashSet<>();

            Set<MethodID> currentMethods = new HashSet<>();
            currentMethods.add(method);

            do {
                calledMethods.addAll(currentMethods);

                Set<MethodID> nextMethods = new HashSet<>();

                for (MethodID methodID : currentMethods) {
                    CFG<St, T> methodIDCFG = this.methodCFG.get(methodID);

                    if (methodIDCFG == null) {
                        createCFG(methodID);
                        methodIDCFG = this.methodCFG.get(methodID);
                    }
                    for (FSM cfg : methodIDCFG.internalFSMs) {
                        if (cfg != null && ((CFG) cfg).methodID != null) {
                            nextMethods.add((MethodID) ((CFG) cfg).methodID);
                        }
                    }
                }

                if (nextMethods.contains(methodInvokedInThis)) {
                    reentersInvokingMethod = true;
                }

                nextMethods.removeAll(calledMethods);

                currentMethods = nextMethods;
            } while (currentMethods.size() > 0);
            //collect transitively called method's

            for (MethodID methodID : calledMethods) {
                CFG<St, T> methodIDCFG = this.methodCFG.get(methodID);
                relevantShadows.addAll(getLocalEvents(methodIDCFG));
//                relevantShadows.addAll(methodIDCFG.alphabet);
            }
        }

//        relevantShadows.addAll((Collection<? extends Event<T>>) this.methodCFG.get(method).alphabet);
//
//        if (this.calls.get(method) != null) {
//            for (MethodID otherMethod : this.calls.get(method)) {
//                if (!otherMethod.equals(method)
//                        && !otherMethod.equals(methodInvokedInThis)) {
//                    if (this.methodCFG.get(otherMethod) != null)
//                        relevantShadows.addAll((Collection<? extends Event<T>>) this.methodCFG.get(otherMethod).alphabet);
//                }
//            }
//        }

        relevantShadows.removeIf(new Predicate<Event<T>>() {
            @Override
            public boolean test(Event<T> e) {
                return e.label.epsilon;
            }
        });
        return new Pair<>(relevantShadows, reentersInvokingMethod);
    }

    public Set<Event<T>> getLocalEvents(CFG<St, T> methodIDCFG){
        Set<Event<T>> localEvents = new HashSet<>();

        for(Transition<Integer, T> t : methodIDCFG.transitions){
            if(!t.source.equals(t.destination)) {
                localEvents.add(t.event);
            }
        }

        return localEvents;
    }

//    Map<State, Set<MethodID>> statesPossiblyCalling
//
//    public abstract List<State> callingStates(MethodID method);

    public abstract List<Event<T>> shadowsBefore(MethodID method,
                                                 List<MethodID> methodsAlreadyTraversed);

    public abstract List<Event<T>> shadowsAfter(MethodID method);

    public abstract Pair<Set<Event<T>>, Set<Event<T>>> shadowsBeforeAndAfter(MethodID method); //{
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
//

//    public CFG<St, T> nDepthWholeProgramCFG(int n, MethodID method){
//        if(n == 0){
//            return approximateCallSites(method, methodCFG.get(method));
//        }
//        else{
//            CFG<St, T> wholeProgramCFG = methodCFG.get(method);
//
//            String prefix = "state";
//            int i = 0;
//            for(State<Integer, T> state : wholeProgramCFG.states){
//                i++;
//
//                if(state.getInternalFSM() != null & state.getInternalFSM().transitions.size() > 0){
//                    FSM<Integer, T> newFSM = state.getInternalFSM();
//
//                }
//            }
//        }
//    }

    public CFG<St, T> approximateCallSites(MethodID method, CFG<St, T> methodCFG) {
        CFG<St, T> wholeProgramCFG = methodCFG;// new CFG(methodCFG);//new FSM<Unit, Shadow>(methodCFG);

        ArrayList<State<Integer, T>> stateList = new ArrayList<>(wholeProgramCFG.states);
        for (int i = 0; i < stateList.size(); i++) {
            State<Integer, T> state = stateList.get(i);

            if (method.toString().contains("mutateSome")
                    && state.label == 18) {
                System.out.print("");
            }

            if (state.getInternalFSM() != null) {
                //add loops for all shadows relevant to internalFSM
                MethodID methodInvokedHere = this.CFGMethod.get(state.getInternalFSM());


                Pair<Set<Event<T>>, Boolean> relevantShadowsAndReentrantCall = this.relevantShadows(methodInvokedHere, method);

                relevantShadowsAndReentrantCall.first.removeAll(methodCFG.alphabet);
                for (Event<T> shadow : relevantShadowsAndReentrantCall.first) {
                    wholeProgramCFG.addTransition(state, shadow, state);
                }

                //are the two conditions disjuncted below equal? they should be i think
                if (methodInvokedHere == null) {
                    state.setInternalFSM(null);
                } else if (relevantShadowsAndReentrantCall.second) {
                    for (State<Integer, T> initial : wholeProgramCFG.initial) {
                        wholeProgramCFG.addTransition(state, epsilonAction, initial);
                    }

                    for (State<Integer, T> finalState : wholeProgramCFG.finalStates) {
                        wholeProgramCFG.addTransition(finalState, epsilonAction, state);
                    }
                }

//                state.setInternalFSM(null);
            }
        }

        return wholeProgramCFG;
    }

    public CFG<St, T> approximateBeforeAndAfter(MethodID method, CFG<St, T> methodCFG) {

        if (method.toString().contains("performInvalidUse")) {
            System.out.print("");
        }

        CFG<St, T> wholeProgramCFG = methodCFG;// new CFG(methodCFG);//new FSM<Unit, Shadow>(methodCFG);
        if (!mainMethods.contains(method)) {

            Pair<Set<Event<T>>, Set<Event<T>>> beforeAfter = this.shadowsBeforeAndAfter(method);

            for (State<Integer, T> initial : wholeProgramCFG.initial) {
                Set<Event<T>> before = new HashSet<>(beforeAfter.first);
                before.removeAll(methodCFG.alphabet);

                for (Event<T> shadow : before) {
                    wholeProgramCFG.addTransition(initial, shadow, initial);
                }
            }

            for (State<Integer, T> finalState : wholeProgramCFG.finalStates) {
                //add restofprogram alphabet loops
                Set<Event<T>> after = new HashSet<>(beforeAfter.second);
                after.removeAll(methodCFG.alphabet);

                for (Event<T> shadow : after) {
                    wholeProgramCFG.addTransition(finalState, shadow, finalState);
                }

                if (after.size() != beforeAfter.second.size()) {
                    //we should only do this if methods can invoke this method can again invoke it after
                    for (State<Integer, T> initial : wholeProgramCFG.initial) {
                        wholeProgramCFG.addTransition(finalState, this.epsilonAction, initial);
                    }
                }
            }
        }

        return wholeProgramCFG;
    }

    public CFG<St, T> methodCFGToWholeProgramCFG(MethodID method) {
        CFG<St, T> methodCFG = this.methodCFG.get(method);
        if (method.toString().contains("loggedInMenu")) {
            System.out.print("");
        }
        //if we create new fsm we run out of memory
        CFG<St, T> wholeProgramCFG = methodCFG;// new CFG(methodCFG);//new FSM<Unit, Shadow>(methodCFG);

        return approximateCallSites(method, approximateBeforeAndAfter(method, wholeProgramCFG));
    }

    public SubsetDate sufficientResidual(T s, CFG wholeProgramCFG, DateFSM property, Aliasing aliasing) {
        SubsetDate residual;

        Set<Transition<String, DateLabel>> transitionsToKeep = new HashSet<Transition<String, DateLabel>>();

        if (property == null) return new SubsetDate(new DateFSM());
        FSM<Pair<Integer, Set<String>>, T> composition = this.semiSynchronousComposition(wholeProgramCFG, property, s, aliasing);
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

    public SubsetDate residualFromCompositionWithSemiSynch(DateFSM property, FSM<Pair<Integer, Set<String>>, T> composition) {
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


    public Pair<SubsetDate, Set<Event<T>>> residualFromCompositionWithSynch(DateFSM property, FSM<Pair<Integer, String>, T> composition) {
        SubsetDate residual;

        Set<Transition<String, DateLabel>> transitionsToKeep = new HashSet<Transition<String, DateLabel>>();

        if (property == null) return new Pair(new SubsetDate(new DateFSM()), new HashSet<>());

        Map<String, Set<String>> stateToNewHoareTripleMethods = new HashMap<String, Set<String>>();
        for (String label : property.stateHoareTripleMethod.keySet()) {
            stateToNewHoareTripleMethods.put(label, new HashSet<String>(property.stateHoareTripleMethod.get(label)));
        }

        for (State<Pair<Integer, String>, T> state : composition.states) {
            if (stateToNewHoareTripleMethods.get(state.label.second) != null) {
                for (Event<T> event : state.outgoingTransitions.keySet()) {
                    if (!event.label.epsilon) {
                        String methodName = event.label.dateEvent.name;
                        String methodClass = ((MethodCall) event.label.dateEvent).objectType;

                        String method = methodClass + "." + methodName;
                        stateToNewHoareTripleMethods.get(state.label.second).add(method);
                    }
                }
            }
        }


        Set<Transition<Pair<Integer, String>, T>> compositionTransitionsToKeep = new HashSet<>();

        for (Transition<Pair<Integer, String>, T> transition : composition.transitions) {
            if (!transition.event.label.epsilon) {
                if (reachesBadState(transition.source, property)) {
                    compositionTransitionsToKeep.add(transition);
                    //transition.event = epsilonAction;
                }
            }
        }


        //TODO depth-first search CFG from initial state

//        Stack<State<Pair<Integer, String>, T>> stateStack = new Stack<>();
//        Stack<List<Transition<String, DateLabel>>> transitionStack = new Stack<>();
//
//
//        do{
//
//        }while(true);

//
        Set<Event<T>> usefulEvents = new HashSet<>();

        for (Transition<Pair<Integer, String>, T> transition : compositionTransitionsToKeep) {
            String label = transition.source.label.second;
            //Set<String> destinationPropertyStates = transition.source.label.second;
            DateEvent event = transition.event.label.dateEvent;

            State<String, DateLabel> source = property.labelToState.get(label);
            for (Event<DateLabel> eventConditionAction : source.outgoingTransitions.keySet()) {
                if (eventConditionAction.label.event.equals(event)) {
                    usefulEvents.add(transition.event);
                    for (String destinationLabel : source.outgoingTransitions.get(eventConditionAction)) {
                        State<String, DateLabel> destination = property.labelToState.get(destinationLabel);
                        transitionsToKeep.add(new Transition<String, DateLabel>(source, destination, eventConditionAction));
                    }
                }
            }
        }

        residual = new SubsetDate(property, transitionsToKeep, stateToNewHoareTripleMethods);

        return new Pair(residual, usefulEvents);
    }

    public boolean reachesBadState(State<Pair<Integer, String>, T> state, DateFSM property) {

        Set<String> reachedDateStates = reachedStates(state);

        for (State<String, DateLabel> bad : property.badStates) {
            if (reachedDateStates.contains(bad.label)) {
                return true;
            }
        }

        return false;
    }
//    public boolean reachesBadState(State<Pair<Integer, String>, T> state, DateFSM property){
//        if(property.badStates.contains(property.labelToState.get(state.label.second))){
//            return true;
//        } else{
//            for(Set<Pair<Integer, String>> stateSets : state.outgoingTransitions.values()){
//                for(Pair<Integer, String> stateLabel : stateSets){
//                    State<Pair<Integer, String>, T> nextState = state.parent.labelToState.get(stateLabel);
//                    if(reachesBadState(nextState, property)){
//                        return true;
//                    }
//                }
//            }
//        }
//
//        return false;
//    }

    public Set<String> reachedStates(State<Pair<Integer, String>, T> state) {
        Set<State<Pair<Integer, String>, T>> statesReached = new HashSet<>();

        Set<State<Pair<Integer, String>, T>> currentStates = new HashSet<>();
        currentStates.add(state);

        do {
            Set<State<Pair<Integer, String>, T>> nextStates = new HashSet<>();

            for (State<Pair<Integer, String>, T> st : currentStates) {
                for (Set<Pair<Integer, String>> labels : st.outgoingTransitions.values()) {
                    for (Pair<Integer, String> label : labels) {
                        nextStates.add(state.parent.labelToState.get(label));
                    }
                }
            }

            nextStates.removeAll(statesReached);
            statesReached.addAll(nextStates);
            currentStates = nextStates;

        } while (currentStates.size() > 0);

        Set<String> dateStates = new HashSet<>();

        for (State<Pair<Integer, String>, T> st : statesReached) {
            dateStates.add(st.label.second);
        }

        return dateStates;
    }

    public Pair<SubsetDate, Set<Event<T>>> sufficientDATETransitionsWithSemiSynch(T s, CFG wholeProgramCFG, DateFSM property, Aliasing aliasing) {
        SubsetDate residual;

        Set<Transition<String, DateLabel>> transitionsToKeep = new HashSet<Transition<String, DateLabel>>();

        if (property == null) return new Pair(new SubsetDate(new DateFSM()), new HashSet());

        FSM<Pair<Integer, Set<String>>, T> composition = this.semiSynchronousComposition(wholeProgramCFG, property, s, aliasing);
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

            if (event != null) {
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
                                    if (!destination.equals(source)
                                            || eventConditionAction.label.action.replaceAll(";", "").trim().equals("")
                                            || !eventConditionAction.label.event.getClass().equals(MethodCall.class)) {
                                        transitionsToKeep.add(new Transition<String, DateLabel>(source, destination, eventConditionAction));
                                    } else {
                                        //For debugging
                                        System.out.print("");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        //using subsetDate instead of property since it is reduced for reachability
        SubsetDate subsetDate = residualFromCompositionWithSemiSynch(property, composition);

        residual = new SubsetDate(subsetDate, transitionsToKeep, stateToNewHoareTripleMethods, false);

        //For debugging
        if (residual.startingState.label.equals("")) {
            System.out.print("");
        }
        return new Pair(residual, composition.alphabet);
    }


    public ResidualArtifact sufficientDATETransitionsWithSynch(T s, CFG<St, T> wholeProgramCFG, DateFSM property, Aliasing aliasing, Set<Event<T>> instrumentedEvents) {
        SubsetDate residual;

        if (wholeProgramCFG.name.contains("loggedIn")) {
            System.out.print("");
        }

        Set<Transition<String, DateLabel>> transitionsToKeep = new HashSet<Transition<String, DateLabel>>();

        if (property == null) return new ResidualArtifact(s, new SubsetDate(new DateFSM()), new HashMap());

        FSM<Pair<Integer, String>, T> composition = this.synchronousComposition(wholeProgramCFG, property, s, aliasing, instrumentedEvents);
        //		System.out.println(s);
        //		System.out.println(wholeProgramCFG);
        //		System.out.println(composition);

        Map<String, Set<String>> stateToNewHoareTripleMethods = new HashMap<String, Set<String>>();
        for (String label : property.stateHoareTripleMethod.keySet()) {
            stateToNewHoareTripleMethods.put(label, new HashSet<String>(property.stateHoareTripleMethod.get(label)));
        }

        Map<Transition<String, DateLabel>, Set<Event<T>>> dateTransitionsToKeepToProgTransitions = new HashMap<>();
        Map<Transition<String, DateLabel>, Set<Event<T>>> dateTransitionsToKeepToCallTransitions = new HashMap<>();

        Map<Event<T>, Set<Transition<String, DateLabel>>> eventsUsingTransitions = new HashMap<>();

        Map<St, Set<Event<T>>> usedEventsAtCallSites = new HashMap<>();

        for (Transition<Pair<Integer, String>, T> transition : composition.transitions) {

            String label = transition.source.label.second;
            //Set<String> destinationPropertyStates = transition.source.label.second;
            DateEvent event = transition.event.label.dateEvent;

            Set<Transition<String, DateLabel>> eventsAssociatedWithTransitions;
            if (eventsUsingTransitions.containsKey(transition.event)) {
                eventsAssociatedWithTransitions = eventsUsingTransitions.get(transition.event);
            } else {
                eventsAssociatedWithTransitions = new HashSet<>();
                eventsUsingTransitions.put(transition.event, eventsAssociatedWithTransitions);
            }

            if (transition.source.label.first == 6 && label.equals("two")) {
                System.out.print("");
            }
            if (event != null) {
                //this is the difference from the other method
                //check that transition does not match transitions outside the method (i.e. loops on initial, final and call states

                State<String, DateLabel> source = property.labelToState.get(label);
                for (Event<DateLabel> eventConditionAction : source.outgoingTransitions.keySet()) {
                    if (eventConditionAction.label.event.equals(event)) {
                        for (String destinationLabel : source.outgoingTransitions.get(eventConditionAction)) {
                            State<String, DateLabel> destination = property.labelToState.get(destinationLabel);

                            if (event.getClass().equals(MethodCall.class)
                                    && (!destination.equals(source)
                                    || !eventConditionAction.label.action.replaceAll(";", "").trim().equals(""))) {

                                Transition<String, DateLabel> dateTransition = new Transition<>(source, destination, eventConditionAction);

                                eventsAssociatedWithTransitions.add(dateTransition);

//                                if (!(transition.source.label.first.equals(transition.destination.label.first))) {
//
//                                    transitionsToKeep.add(dateTransition);
//
//                                    if (dateTransitionsToKeepToProgTransitions.containsKey(dateTransition)) {
//                                        dateTransitionsToKeepToProgTransitions.get(dateTransition).add(transition.event);
//                                    } else {
//                                        Set<Event<T>> events = new HashSet<>();
//                                        events.add(transition.event);
//                                        dateTransitionsToKeepToProgTransitions.put(dateTransition, events);
//                                    }
//                                } else {
//                                    CFG<St, T> originalCFG = methodCFG.get(wholeProgramCFG.methodID);
//                                    if (originalCFG.integerToState.get(transition.source.label.first).getInternalFSM() != null) {
//                                        St invocationStatement = originalCFG.stateToLabel.get(originalCFG.integerToState.get(transition.source.label.first)).statement;
//
//                                        if (usedEventsAtCallSites.containsKey(invocationStatement)) {
//                                            usedEventsAtCallSites.get(invocationStatement).add(transition.event);
//                                        } else {
//                                            Set<Event<T>> set = new HashSet<>();
//                                            set.add(transition.event);
//                                            usedEventsAtCallSites.put(invocationStatement, set);
//                                        }
//
//                                        if (dateTransitionsToKeepToCallTransitions.containsKey(dateTransition)) {
//                                            dateTransitionsToKeepToCallTransitions.get(dateTransition).add(transition.event);
//                                        } else {
//                                            Set<Event<T>> set = new HashSet<>();
//                                            set.add(transition.event);
//                                            dateTransitionsToKeepToCallTransitions.put(dateTransition, set);
//                                        }
//                                    }
//                                }
                            }
                        }
                    }
                }
//                    } else{
//                        if(transition.source.getInternalFSM() != null){
//                            if(!transition.event.label.epsilon){
//                                usedEventsAtCallSites.put(transition.source.label.first, transition.event);
//                            }
//                        }
//                    }
            }
        }

        //using subsetDate instead of property since it is reduced for reachability
        Pair<SubsetDate, Set<Event<T>>> subsetDateAndUsefulEvents = residualFromCompositionWithSynch(property, composition);
//        residual = new SubsetDate(subsetDateAndUsefulEvents.first, transitionsToKeep, stateToNewHoareTripleMethods, false);


//        Set<Event<T>> eventsToKeep = new HashSet<>();

//        for(Transition t : residual.transitions){
//            eventsToKeep.addAll(dateTransitionsToKeepToProgTransitions.get(t));
//        }

//        Set<Event<T>> eventsToKeep = subsetDateAndUsefulEvents.second;

        //Use the below to turn off instrumentation on a context-sensitive basis
//        Set<Event<T>> eventsToKeepForCallStates = new HashSet<>();
//        for(Transition t : subsetDateAndUsefulEvents.first.transitions) {
//            if(dateTransitionsToKeepToCallTransitions.containsKey(t)) {
//                eventsToKeepForCallStates.addAll(dateTransitionsToKeepToCallTransitions.get(t));
//            }
//        }
//
//        for(St st : usedEventsAtCallSites.keySet()){
//            int size = usedEventsAtCallSites.get(st).size();
//            usedEventsAtCallSites.get(st).retainAll(eventsToKeepForCallStates);
//            if(usedEventsAtCallSites.get(st).size() != size){
//                System.out.print("");
//            }
//        }


        return new ResidualArtifact(s, subsetDateAndUsefulEvents.first, eventsUsingTransitions);
    }

//    Map<CFG, Map<State<String, DateLabel>, Pair<Set<State<String, DateLabel>>, Set<Event<T>>>>> methodSummaries;
//
//    public Pair<SubsetDate, Set<Event<T>>> summarise(T s, CFG methodCFG, DateFSM property, Aliasing aliasing, State<String, DateLabel> initPropertyState){
//
//    }


    public FSM<Pair<Integer, String>, T> synchronousComposition(CFG cfg,
                                                                DateFSM date,
                                                                T shadow,
                                                                Aliasing aliasing,
                                                                Set<Event<T>> instrumentedEvents) {
        FSM<Pair<Integer, String>, T> composition = new FSM<>();

        if (cfg.name.contains("sendAndProcessSome")) {
            System.out.print("");
        }

        //to create program events/shadows corresponding to DATE-specific events
        createChannelAndClockEvents(date);

        //the current states from which to consider transitions
        Set<State<Pair<Integer, String>, T>> statesToTransitionOn = new HashSet<>();

        Set<State<Integer, T>> initial = cfg.initial;

        for (State<Integer, T> initState : initial) {
            Pair<Integer, String> initialTag = new Pair<Integer, String>(initState.label, date.startingState.label);
            statesToTransitionOn.add(composition.getOrAddState(initialTag));
        }

        while (statesToTransitionOn.size() > 0) {

            Set<State<Pair<Integer, String>, T>> statesTransitionedTo = new HashSet<>();

            for (State<Pair<Integer, String>, T> current : statesToTransitionOn) {
                State<String, DateLabel> currentDateState = date.getOrAddState(current.label.second);
                State<Integer, T> currentProgState = cfg.getOrAddState(current.label.first);

                if (current.label.first == 18) {
                    System.out.print("");
                }

                for (Event<T> progEvent : currentProgState.outgoingTransitions.keySet()) {
                    if (progEvent.toString().contains("black")) {
                        System.out.print("");
                    }

                    for (Integer nextProgStateLabel : currentProgState.outgoingTransitions.get(progEvent)) {

                        //if transition is marked by the empty action
                        //or if its event may not alias with the transition event
                        //or if its event is not to be instrumented
                        //then simply transition with the empty action
                        //else check if there is a match with a property event
                        if (progEvent.label.epsilon || !aliasing.mayAlias(shadow, progEvent.label) || (instrumentedEvents != null && !instrumentedEvents.contains(progEvent))) {
                            State<Pair<Integer, String>, T> next = composition.getOrAddState(new Pair<>(nextProgStateLabel, currentDateState.label));

                            composition.addTransition(current, epsilonAction, next);

                            statesTransitionedTo.add(next);
                        } else {

                            //if the events do not must-alias, then consider that they do not, and then consider that they do
                            if (!aliasing.mustAlias(shadow, progEvent.label)) {
                                State<Pair<Integer, String>, T> next = composition.getOrAddState(new Pair<>(nextProgStateLabel, currentDateState.label));

                                composition.addTransition(current, epsilonAction, next);

                                statesTransitionedTo.add(next);
                            }

                            for (Event<DateLabel> dateEventTriple : currentDateState.outgoingTransitions.keySet()) {
                                for (String nextDateStateLabel : currentDateState.outgoingTransitions.get(dateEventTriple)) {
                                    //if event is a clock or channel event then just transition and remain in the same prog state
                                    if (dateEventTriple.label.event.getClass().equals(ClockEvent.class)) {
                                        State<Pair<Integer, String>, T> next = composition.getOrAddState(new Pair<>(current.label.first, nextDateStateLabel));

                                        composition.addTransition(current, new Event<>(clockCFGEvents.get(dateEventTriple)), next);

                                        statesTransitionedTo.add(next);
                                    } else if (dateEventTriple.label.event.getClass().equals(ChannelEvent.class)) {
                                        State<Pair<Integer, String>, T> next = composition.getOrAddState(new Pair<>(current.label.first, nextDateStateLabel));

                                        composition.addTransition(current, new Event<>(channelCFGEvents.get(dateEventTriple)), next);

                                        statesTransitionedTo.add(next);
                                    }

                                    //then find matching date transitions
                                    DateEvent progDateEvent = progEvent.label.dateEvent;
                                    if (dateEventTriple.label.event.equals(progDateEvent)) {
                                        if (!dateEventTriple.label.condition.equals("false")) {
                                            State<Pair<Integer, String>, T> next = composition.getOrAddState(new Pair<>(nextProgStateLabel, nextDateStateLabel));

                                            composition.addTransition(current, progEvent, next);

                                            statesTransitionedTo.add(next);
                                        }

                                        if (!dateEventTriple.label.condition.equals("true")) {
                                            State<Pair<Integer, String>, T> next = composition.getOrAddState(new Pair<>(nextProgStateLabel, nextDateStateLabel));

                                            composition.addTransition(current, epsilonAction, current);

                                            statesTransitionedTo.add(next);
                                        }
                                    }

                                }
                            }
                        }
                    }
                }
            }


            statesToTransitionOn.clear();

            for (State<Pair<Integer, String>, T> state : statesTransitionedTo) {
                if (!(state.outgoingTransitions.entrySet().size() > 0)//this condition makes sure we iterate over states only one time
                ) {
                    statesToTransitionOn.add(state);
                }
            }
        }

        return composition;
    }

    //assuming flat fsm
    public FSM<Pair<Integer, Set<String>>, T> semiSynchronousComposition(CFG cfg,
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

            //for debugging
            if (state.label.first.equals("58")) {
                System.out.print("");
            }

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

                        //for debugging
                        if (destinationStateLabel == 58) {
                            System.out.print("");
                        }

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