package clarva.analysis;

import clarva.analysis.cfg.CFG;
import clarva.analysis.cfg.CFGEvent;
import clarva.analysis.cfg.CFGState;
import clarva.analysis.factchecking.FactChecker;
import clarva.matching.MethodIdentifier;
import fsm.Event;
import fsm.State;
import fsm.date.DateFSM;
import fsm.date.DateLabel;
import fsm.date.events.DateEvent;

import java.util.*;

public class BackwardsAnalysis {
    Set<CFG> program;
    FactChecker factChecker;
    CFGAnalysis analysis;

    public BackwardsAnalysis(Set<CFG> program, FactChecker factChecker){
        this.program = program;
        this.factChecker = factChecker;
    }

    //get all states in a CFG that happen after event e
    public Set<State> allProgramStatesAfterEvent(DateEvent e){
        Set<State> states = new HashSet<>();

        for(CFG cfg : program){
            for(Object statementObject : cfg.statements){
                CFGState cfgState = (CFGState) statementObject;
                State fsmState = (State) cfg.labelToState.get(cfgState);
                for(Object entryObject : fsmState.incomingTransitions.entrySet()){
                    Map.Entry<Event, Set> entry = (Map.Entry<Event, Set>) entryObject;
                    if(((DateEvent) entry.getKey().label).equals(e)){
                        states.add(fsmState);
//                        for(Object state : entry.getValue()){
//                            states.add((CFGState) cfg.stateToLabel.get((State) state));
//                        }
                    }
                }
            }
        }

        return states;
    }

    //for each bad state in the DEA, and for each event into it
    // get all states in CFG that have an incoming transition tagged by the same event
    public Set<State> identifyPotentialPointsOfFailure(DateFSM date, Set<CFG> program){
        Set<State<String, DateLabel>> badStates = date.badStates;
        Set<State> potentialPointsOfFailure = new HashSet<>();
        for(State st : badStates){
            for(Object e : st.incomingTransitions.keySet()){
                potentialPointsOfFailure.addAll(allProgramStatesAfterEvent((DateEvent) e));
            }
        }

        return potentialPointsOfFailure;
    }

    //a configuration of the backwards analysis is made up of a mapping between
    //datestates and sets of cfg states
    class Configuration{
        //Property: <dst, cst> \in todo => <dst, cst> \not\in already done

        //<dst, cst> \in todo  => continue backwards analysing
        Map<State, Set<State>> todo;

        //<dst, cst> \in alreadyDone => backwards analysing has already been done
        Map<State, Set<State>> alreadyDone;

        public Configuration(Map<State, Set<State>> todo, Map<State, Set<State>> alreadyDone){
            this.todo = todo;
            this.alreadyDone = alreadyDone;
        }
    }

    public Configuration addStateToDoIfNotInAlreadyDone(Configuration configuration, State dateState, State cfgState){
        if(configuration.alreadyDone.get(dateState) != null){
            if(configuration.alreadyDone.get(dateState).contains(cfgState)){
                return configuration;
            }
        }

        if(configuration.todo.keySet().contains(dateState)){
            configuration.todo.get(dateState).add(cfgState);
        } else{
            Set<State> cfgStates = new HashSet<>();
            cfgStates.add(cfgState);

            configuration.todo.put(dateState, cfgStates);
        }

        return configuration;
    }

    //one step backwards controlled by
    public Configuration oneStepBackwards(Configuration configuration){
        Map<State, Set<State>> todo = new HashMap<>();
        Map<State, Set<State>> alreadyDone = new HashMap<>(configuration.alreadyDone);
        alreadyDone.putAll(configuration.todo);

        Configuration newConfig = new Configuration(todo, alreadyDone);

        for(Map.Entry<State, Set<State>> entries : configuration.todo.entrySet()){
            State dateState = entries.getKey();
            Set<State> cfgStates = entries.getValue();

            for(State cfgState : cfgStates){
                if(cfgState.parent.initial.contains(cfgState)){
                    //TODO deal with method call event??

                    //get the statement associated with each cfgstate
                    //get each statement possibly calling this method
                    //for each such statement get it s state in its CFG
                    //and tag it to the current DEA state

                    MethodIdentifier method = (MethodIdentifier) analysis.FSMMethod.get(cfgState.parent);
                    Set callingStatements = (Set) analysis.methodCalledByStates.get(method);

                    for(Object stmt : callingStatements){
                        MethodIdentifier containingMethod = (MethodIdentifier) analysis.statementCalledBy.get(stmt);
                        CFG containingCFG = (CFG) analysis.methodCFG.get(containingMethod);
                        State callingState = (State) containingCFG.labelToState.get(new CFGState<>(stmt));
                        addStateToDoIfNotInAlreadyDone(newConfig, dateState, callingState);
                    }

                } else if(((Set) analysis.statesCallMethods.get(cfgState)).size() > 0){
                    //TODO MethodCall Event

                    for(MethodIdentifier calledMethod : ((Set<MethodIdentifier>) analysis.statesCallMethods.get(cfgState))){
                        CFG calledCFG = (CFG) analysis.methodCFG.get(calledMethod);
                        for(Object init : calledCFG.initial){
                            addStateToDoIfNotInAlreadyDone(newConfig, dateState, (State) init);
                        }
                    }
                }
                else{
                    for(Object entryObject : cfgState.incomingTransitions.keySet()){
                        Map.Entry<CFGEvent, Set<State>> entry = (Map.Entry<CFGEvent, Set<State>>) entryObject ;
                        DateEvent dateEvent = entry.getKey().dateEvent;

                        //TODO handle epsilon events?

                        for(Object prevDateEventEntryObject : dateState.incomingTransitions.keySet()){
                            Map.Entry<Event, Set<State>> prevDateEventEntry = (Map.Entry<Event, Set<State>>) prevDateEventEntryObject;

                            Event<DateLabel> prevDateEvent = (Event<DateLabel>) prevDateEventEntry.getKey();
                            if(dateEvent.equals(((DateFSM) dateState.parent).eventUsedInGuardedCommand.get(prevDateEvent))){
                                for(State dateSt : prevDateEventEntry.getValue()) {
                                    for(State cfgSt : entry.getValue()) {
                                        if(!configuration.alreadyDone.get(dateSt).contains(cfgSt)) {
                                            //TODO theorem proving to be added here
                                            addStateToDoIfNotInAlreadyDone(newConfig, dateSt, cfgSt);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return newConfig;
    }


    public void backwardsAnalysis(DateFSM date, Set<CFG> program, CFGAnalysis cfgAnalysis) {

        this.analysis = cfgAnalysis;
        Set<State> potentialPointsOfFailure = identifyPotentialPointsOfFailure(date, program);
        Set badStates = date.badStates;

        Map<State, Set<State>> todo = new HashMap<>();
        Map<State, Set<State>> alreadyDone = new HashMap<>();

        for(Object bad : badStates){
            todo.put((State) bad, potentialPointsOfFailure);
        }

        Configuration configuration = new Configuration(todo, alreadyDone);

        while(configuration.todo.size() > 0){
            configuration = oneStepBackwards(configuration);
        }

        //Algorithm: Backwards analyse DATEFsm date against CFG cfg
        //for each bad state b in the date
        //  for each DATEEvent e into b
        //      for each CFGTransition t in cfg associated with e
        //          start algorithm on the outgoing state of t

        //Algorithm: Backwards analyse from (cfgstate, datestate, facts)
        //for each incoming transition ct into cfgstate
        //  if the event of ct is an epsilon event
        //      then re-rerun algorithm for (outgoing(ct), datestate, facts)
        //  else
        //      for each incoming transition dt into datestate
        //          if the event of dt matches that of ct
        //
        //
        //
    }
}
