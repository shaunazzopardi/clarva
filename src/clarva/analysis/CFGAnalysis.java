package clarva.analysis;

import clarva.analysis.cfg.CFG;
import clarva.analysis.cfg.Shadow;
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
import soot.Local;
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.Unit;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.internal.JNopStmt;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.jimple.toolkits.annotation.logic.LoopFinder;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.pointer.LocalMustAliasAnalysis;
import soot.jimple.toolkits.pointer.LocalMustNotAliasAnalysis;
import soot.jimple.toolkits.pointer.StrongLocalMustAliasAnalysis;
import soot.toolkits.graph.TrapUnitGraph;
import soot.toolkits.graph.UnitGraph;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;

public class CFGAnalysis {

    public MethodsAnalysis ma;

    public Map<MethodOrMethodContext, TrapUnitGraph> methodGraph;
    public Map<MethodOrMethodContext, CFG> methodCFG;
    public Map<CFG, MethodOrMethodContext> FSMMethod;
    public Map<MethodOrMethodContext, Boolean> methodNoDirectMethodCall;
    public Map<MethodOrMethodContext, Boolean> methodNoMatchingMethodCall;
    public Map<MethodOrMethodContext, Boolean> allStatesNull;
    public Map<MethodOrMethodContext, LocalMustAliasAnalysis> methodMustAlias;
    public Map<MethodOrMethodContext, LocalMustNotAliasAnalysis> methodMustNotAlias;

    public Map<Unit, MethodOrMethodContext> unitCalledBy;

    public Map<MethodOrMethodContext, List<Event<Shadow>>> eventsPossiblyOccurringBeforeMethod;
    public Map<MethodOrMethodContext, List<Event<Shadow>>> eventsPossiblyOccurringAfterMethod;

    public Map<MethodOrMethodContext, Set<MethodOrMethodContext>> allMethodsSucceeding;
    //
    Set<Event<Shadow>> allEventShadows = new HashSet<Event<Shadow>>();
    //We use this action to denote tau-actions
    //i.e. actions (points of execution) that
    //are irrelevant to the properties considered
    Event<Shadow> epsilonAction = new Event<Shadow>(new Shadow());
    Map<InvokeExpr, List<Event<Shadow>>> shadowsBeforeCall = new HashMap<InvokeExpr, List<Event<Shadow>>>();
    Map<InvokeExpr, List<Event<Shadow>>> shadowsAfterCall = new HashMap<InvokeExpr, List<Event<Shadow>>>();
    List<Pair<String, String>> ppfs = new ArrayList<Pair<String, String>>();
    Set<Event<Shadow>> canBeDisabled = new HashSet<Event<Shadow>>(this.allEventShadows);

    public CFGAnalysis(MethodsAnalysis ma) {

        this.ma = ma;

        methodGraph = new HashMap<MethodOrMethodContext, TrapUnitGraph>();
        methodCFG = new HashMap<MethodOrMethodContext, CFG>();
        FSMMethod = new HashMap<CFG, MethodOrMethodContext>();
        methodNoDirectMethodCall = new HashMap<MethodOrMethodContext, Boolean>();
        allStatesNull = new HashMap<MethodOrMethodContext, Boolean>();
        methodMustAlias = new HashMap<MethodOrMethodContext, LocalMustAliasAnalysis>();
        methodMustNotAlias = new HashMap<MethodOrMethodContext, LocalMustNotAliasAnalysis>();

        unitCalledBy = new HashMap<Unit, MethodOrMethodContext>();

        eventsPossiblyOccurringBeforeMethod = new HashMap<MethodOrMethodContext, List<Event<Shadow>>>();
        eventsPossiblyOccurringAfterMethod = new HashMap<MethodOrMethodContext, List<Event<Shadow>>>();

        methodNoMatchingMethodCall = new HashMap<MethodOrMethodContext, Boolean>();
        allMethodsSucceeding = new HashMap<MethodOrMethodContext, Set<MethodOrMethodContext>>();

        //	unitBindingRepresentatives = new HashMap<State<Unit,DateLabel>, Map<String, InstanceKey>>();

        Set<MethodOrMethodContext> methodsToKeep = this.methodFSMsToGenerateFor(new HashSet<MethodOrMethodContext>(ma.sootMethodToDateEvents.keySet()));
        //		for(List<MethodOrMethodContext> methods : ma.dateEventToSootMethods.values()){
        //			methodsToKeep.removeAll(methods);
        //		}

        for (MethodOrMethodContext method : methodsToKeep) {
            CFG fsm = new CFG();
            methodCFG.put(method, fsm);
            FSMMethod.put(fsm, method);
        }

        for (MethodOrMethodContext method : methodsToKeep) {
            if (method.method().hasActiveBody()) {
                TrapUnitGraph cfg = new TrapUnitGraph(method.method().getActiveBody());

                methodGraph.put(method, cfg);
            }
        }


        for (MethodOrMethodContext method : methodsToKeep) {
            if (method.method().hasActiveBody()) {
                //				System.out.println(method.method().getSignature());
                createCFG(method);

                if (this.methodCFG.get(method).transitions.size() == 0) {
                    this.FSMMethod.remove(this.methodCFG.get(method));
                    this.methodCFG.remove(method);
                    this.methodGraph.remove(method);
                }
            }
        }


        for (MethodOrMethodContext method : methodGraph.keySet()) {
            methodMustAlias.put(method, new StrongLocalMustAliasAnalysis(methodGraph.get(method)));
            methodMustNotAlias.put(method, new LocalMustNotAliasAnalysis(methodGraph.get(method)));
        }

        this.tagShadowsWithBindingRepresentatives();
        this.shadowsBeforeCall();

    }

    public boolean inLoop(MethodOrMethodContext method, Stmt s) {
        LoopFinder loopFinder = new LoopFinder();
        loopFinder.transform(method.method().getActiveBody());
        for (Loop loop : loopFinder.loops()) {
            if (loop.getLoopStatements().contains(s)) {
                return true;
            }
        }

        return false;
    }

    //methods contains all method that match events, not methods that call events
    public Set<MethodOrMethodContext> methodFSMsToGenerateFor(Set<MethodOrMethodContext> methods) {
        Set<MethodOrMethodContext> methodsToKeep = new HashSet<MethodOrMethodContext>();

        Set<MethodOrMethodContext> methodsToResolve = new HashSet<MethodOrMethodContext>();

        methodsToResolve.addAll(methods);

        CallGraph cg = Scene.v().getCallGraph();

        boolean notFinished = true;
        while (notFinished) {
            notFinished = false;
            Set<MethodOrMethodContext> methodsToResolve2 = new HashSet<MethodOrMethodContext>();

            //if all methods to resolve already resolved
            if (methodsToKeep.containsAll(methodsToResolve)) {
                return methodsToKeep;
            }

            for (MethodOrMethodContext method : methodsToResolve) {
                if (this.reachableFromMainMethod(method)) {
                    Iterator<Edge> edgesIntoMethod = cg.edgesInto(method);
                    while (edgesIntoMethod.hasNext()) {
                        methodsToResolve2.add(edgesIntoMethod.next().getSrc());
                        notFinished = true;
                    }
                }
            }

            methodsToKeep.addAll(methodsToResolve);
            methodsToResolve.clear();
            methodsToResolve = methodsToResolve2;
        }

        methodsToKeep.removeAll(methods);

        return methodsToKeep;
    }

    public boolean reachableFromMainMethod(MethodOrMethodContext method) {
        CallGraph cg = Scene.v().getCallGraph();

        boolean notFinished = true;

        List<MethodOrMethodContext> alreadyProcessed = new ArrayList<MethodOrMethodContext>();

        List<MethodOrMethodContext> currentMethods = new ArrayList<MethodOrMethodContext>();
        currentMethods.add(method);

        while (notFinished) {
            List<MethodOrMethodContext> nextMethods = new ArrayList<MethodOrMethodContext>();

            for (MethodOrMethodContext currentMethod : currentMethods) {
                Iterator<Edge> edgesIntoMethod = cg.edgesInto(currentMethod);
                while (edgesIntoMethod.hasNext()) {
                    MethodOrMethodContext prevMethod = edgesIntoMethod.next().getSrc();
                    if (prevMethod.equals(Scene.v().getMainMethod())) {
                        return true;
                    } else if (!alreadyProcessed.contains(prevMethod)) {
                        nextMethods.add(prevMethod);
                    }
                }
            }

            if (nextMethods.size() == 0) notFinished = false;

            currentMethods = new ArrayList<MethodOrMethodContext>(nextMethods);
            nextMethods.clear();
        }

        return false;
    }

    public void createCFG(MethodOrMethodContext method) {
        //		boolean noDirectMethodCalls = true;
        //		boolean allStatesNull = true;
        //
        Set<MethodOrMethodContext> methodsCalledByMethod;
        if (this.allMethodsSucceeding.get(method) == null) {
            methodsCalledByMethod = new HashSet<MethodOrMethodContext>();
            this.allMethodsSucceeding.put(method, methodsCalledByMethod);
        } else {
            methodsCalledByMethod = this.allMethodsSucceeding.get(method);
        }

        //If method cfg was created
        if (methodGraph.containsKey(method)) {
            //Get cfg of the method
            UnitGraph sootCFG = methodGraph.get(method);
            //Get (empty) fsm of the method
            CFG cfg = methodCFG.get(method);

            Iterator<Unit> unitIterator = sootCFG.iterator();

            Unit currentUnit;
            while (unitIterator.hasNext()) {
                currentUnit = unitIterator.next();
                unitCalledBy.put(currentUnit, method);
                //This takes care of not having duplicates states
                //with the same unit.
                //Note: The same statements at different points of a program
                //correspond to different units.
                State<Integer, Shadow> currentUnitState = cfg.getOrAddState(currentUnit);

                if (ma.unitsContainingMethods.get(currentUnit) != null
                        && this.methodCFG.keySet().contains(ma.unitsContainingMethods.get(currentUnit))) {
                    MethodOrMethodContext methodCall = ma.unitsContainingMethods.get(currentUnit);
                    currentUnitState.setInternalFSM(methodCFG.get(methodCall));
                }

                //If unit is not caught by some action in the property
                //then create a transition from each predecessor state
                //with the empty action to the current unit state
                //and create a transition from the current unit state
                //to each successor state with the empty action
                if (!ma.unitShadows.containsKey(currentUnit)) {
                    //for(Unit pred : cfg.getPredsOf(currentUnit)){
                    //	State predState = fsm.labelToState.get(pred);

                    //	fsm.addTransition(predState, emptyAction, currentUnitState);
                    //}

                    for (Unit succ : sootCFG.getSuccsOf(currentUnit)) {
                        State<Integer, Shadow> succState = cfg.getOrAddState(succ);

                        //fsm.addTransition(currentUnitState, this.tauAction, succState);
                        cfg.addTransition(currentUnitState, this.epsilonAction, succState);
                    }

                    MethodOrMethodContext currentUnitMethod = ma.unitsContainingMethods.get(currentUnit);
                    //If the unit is not atomic (i.e. it has a cfg and fsm)
                    //then set the unit's fsm reference.
                    if (methodCFG.containsKey(currentUnitMethod)) {
                        currentUnitState.setInternalFSM(methodCFG.get(currentUnitMethod));
                        methodsCalledByMethod.add(currentUnitMethod);
                        //						allStatesNull = false;
                        //						methodsCalledByMethod.add(currentUnitMethod);
                    }
                } else {

                    //	BindingRepresentative br = new BindingRepresentative();
                    //		this.methodMustAlias.get(method).

                    //					noDirectMethodCalls = false;

                    List<Shadow> shadows = ma.unitShadows.get(currentUnit);

                    //				if(actions.size() == 1){
                    //					fsm.addTransition(currentUnitState, actions.iterator().next(), currentUnitState);
                    //				}
                    //				else{
                    //						Event<Shadow> before = epsilonAction;
                    //						Event<Shadow> uponEntry = epsilonAction;
                    //						Event<Shadow> uponThrowing = epsilonAction;
                    //						Event<Shadow> uponHandling = epsilonAction;
                    //						Event<Shadow> uponReturning = epsilonAction;
                    //						//we don't take into account the possibility of having
                    //						//multiple actions matching at the same point
                    //
                    //						//before, uponEntry, uponThrowing, uponHandling, uponReturning
                    //						for(Shadow shadow : shadows){
                    //							//Assuming that actions do not contain duplicates
                    //							switch(shadow.event.type){
                    //								case before : before = new Event<Shadow>(shadow); break;
                    //								case uponEntry : uponEntry = new Event<Shadow>(shadow); break;
                    //								case uponThrowing : uponThrowing = new Event<Shadow>(shadow); break;
                    //								case uponHandling : uponHandling = new Event<Shadow>(shadow); break;
                    //								case uponReturning : uponReturning = new Event<Shadow>(shadow); break;
                    //							}
                    //						}

                    //						for(Unit pred : cfg.getPredsOf(currentUnit)){
                    //							State predUnitState = fsm.lab.elToState.get(pred);
                    //
                    //							if(uponEntry != null){
                    //								if(before != null){
                    //									State nopState = fsm.labelToState.get(new JNopStmt());
                    //									fsm.addTransition(predUnitState, before, nopState);
                    //									fsm.addTransition(nopState, uponEntry, currentUnitState);
                    //								}
                    //								else{
                    //									fsm.addTransition(predUnitState, uponEntry, currentUnitState);
                    //								}
                    //							}
                    //							else if(before != null){
                    //								fsm.addTransition(predUnitState, before, currentUnitState);
                    //								}
                    //								else{
                    //									fsm.addTransition(predUnitState, emptyAction, currentUnitState);
                    //								}
                    //
                    //
                    //						}
                    //
                    //						for(Unit succ : cfg.getSuccsOf(currentUnit)){
                    //							State succUnitState = fsm.labelToState.get(succ);
                    //
                    //							if(uponReturning != null){
                    //								fsm.addTransition(currentUnitState, before, succUnitState);
                    //							}
                    //							else{
                    //								fsm.addTransition(currentUnitState, emptyAction, succUnitState);
                    //							}
                    //						}

                    State<Integer, Shadow> source = currentUnitState;
                    for (int i = 0; i < shadows.size() - 1; i++) {
                        State<Integer, Shadow> destination = cfg.getOrAddState(new JNopStmt());
                        Event<Shadow> event = new Event<Shadow>(shadows.get(i));
                        allEventShadows.add(event);
                        cfg.addTransition(source, event, destination);
                        source = destination;
                    }

                    for (Unit succ : sootCFG.getSuccsOf(currentUnit)) {
                        State<Integer, Shadow> destination = cfg.getOrAddState(succ);
                        Event<Shadow> event = new Event<Shadow>(shadows.get(shadows.size() - 1));
                        allEventShadows.add(event);

                        cfg.addTransition(source, event, destination);
                    }

                    //						Set<Transition<Integer, Shadow>> transitions = new HashSet<Transition<Integer, Shadow>>(cfg.transitions);
                    //
                    //						here:
                    //						for(Transition<Integer, Shadow> t : transitions){
                    //						//for(Unit succ : sootCFG.getSuccsOf(currentUnit)){
                    //							if(t.event.equals(epsilonAction)) continue here;
                    //
                    //							State<Integer,Shadow> succUnitState = cfg.getOrAddState(t.destination);
                    //							t.remove();
                    //							cfg.transitions.remove(t);
                    ////							if(uponEntry != null){
                    ////								if(before != null){
                    //									State<Integer,Shadow> nopState = cfg.getOrAddState(new JNopStmt());
                    //									State<Integer,Shadow> nopState2 = cfg.getOrAddState(new JNopStmt());
                    //									cfg.addTransition(currentUnitState, before, nopState);
                    //									cfg.addTransition(nopState, uponEntry, nopState2);
                    //
                    //									MethodOrMethodContext currentUnitMethod = ma.unitsContainingMethods.get(currentUnit);
                    //									//If the unit is not atomic (i.e. it has a cfg and fsm)
                    //									//then set the unit's fsm reference.
                    //									if(methodCFG.containsKey(currentUnitMethod)){
                    //										nopState2.setInternalFSM(methodCFG.get(currentUnitMethod));
                    ////										allStatesNull = false;
                    ////										methodsCalledByMethod.add(currentUnitMethod);
                    //									}
                    //									else{
                    //										//currentUnitState.fsm = null;
                    //									}
                    //
                    //									cfg.addTransition(nopState2, uponReturning, succUnitState);
                    //
                    //									nopState2.setInternalFSM(currentUnitState.getInternalFSM());
                    //									currentUnitState.setInternalFSM(null);

                    //								}
                    //								else{
                    //									fsm.addTransition(currentUnitState, uponEntry, succUnitState);
                    //								}
                    //							}
                    //							else if(before != null){
                    //									fsm.addTransition(currentUnitState, before, succUnitState);
                    //								}
                    //								else{
                    //									fsm.addTransition(currentUnitState, emptyAction, succUnitState);
                    //								}
                    //

                    //}

                }
                //How to handle uponreturning and uponthrowing??
                //	}

            }
            for (Unit init : sootCFG.getHeads()) {
                State<Integer, Shadow> initState = cfg.labelToState.get(init);
                cfg.addInitialState(initState);
            }

            for (Unit tail : sootCFG.getTails()) {
                State<Integer, Shadow> finalState = cfg.labelToState.get(tail);
                cfg.addFinalState(finalState);
            }

            //			//Remove states with only empty outgoing transitions (i.e. with units that do not match actions)
            //			Set<State<Unit, DateLabel>> emptyStates = new HashSet<State<Unit, DateLabel>>();
            //
            //			for(State<Unit, DateLabel> state : fsm.states){
            //				if(state.fsm != null){
            //					boolean emptyState = true;
            //					for(Action<DateLabel> action : state.outgoingTransitions.keySet()){
            //						if(!action.equals(emptyAction)){
            //							emptyState = false;
            //						}
            //					}
            //
            //					if(emptyState){
            //						emptyStates.add(state);
            //					}
            //				}
            //			}
            //
            //	fsm.reduced(emptyStates, false);
            cfg.reduce();
            this.selfContainedMethod(method);
            //			System.out.println(cfg);
        }


        //	methodNoDirectMethodCall.put(method, noDirectMethodCalls);
        //	this.allStatesNull.put(method, allStatesNull);
        //return fsm;
    }

    public void tagShadowsWithBindingRepresentatives() {
        for (MethodOrMethodContext method : methodCFG.keySet()) {
            CFG fsm = methodCFG.get(method);
            //check that fsm is actually the method's fsm, not replaced by another method's fsm
            if (this.FSMMethod.get(fsm).equals(method)) {
                for (Transition<Integer, Shadow> transition : fsm.transitions) {
                    transition.event.label.inferBinding(method.method(), methodMustAlias.get(method), methodMustNotAlias.get(method));
                }
            }
        }
    }

    public Pair<Set<Shadow>, Set<Shadow>> mustAndMayAliasShadows(Shadow shadow) {
        Set<Shadow> must = new HashSet<Shadow>();
        Set<Shadow> may = new HashSet<Shadow>();

        for (Shadow other : ma.allShadows) {
            if (shadow.mustAlias(other)) {
                must.add(other);
            } else if (shadow.mayAlias(other)) {
                may.add(other);
            }
        }

        return new Pair<Set<Shadow>, Set<Shadow>>(must, may);
    }

    public Set<Event<Shadow>> restOfProgramShadows(MethodOrMethodContext method) {
        Set<Event<Shadow>> restOfProgramShadows = new HashSet<Event<Shadow>>();

        for (MethodOrMethodContext otherMethod : this.methodCFG.keySet()) {
            if (!method.equals(otherMethod)) {
                restOfProgramShadows.addAll(this.methodCFG.get(otherMethod).alphabet);
            }
        }

        return restOfProgramShadows;
    }

    public Set<Event<Shadow>> relevantShadows(MethodOrMethodContext method, MethodOrMethodContext methodInkovedInThis) {
        Set<Event<Shadow>> relevantShadows = new HashSet<Event<Shadow>>();

        if (this.methodCFG.get(method) == null) return relevantShadows;

        relevantShadows.addAll(this.methodCFG.get(method).alphabet);

        if (this.allMethodsSucceeding.get(method) != null) {
            for (MethodOrMethodContext otherMethod : this.allMethodsSucceeding.get(method)) {
                if (!otherMethod.equals(method)
                        && !otherMethod.equals(methodInkovedInThis)) {
                    if (this.methodCFG.get(otherMethod) != null)
                        relevantShadows.addAll(this.methodCFG.get(otherMethod).alphabet);
                }
            }
        }

        relevantShadows.removeIf(new Predicate<Event<Shadow>>() {
            @Override
            public boolean test(Event<Shadow> e) {
                return e.label.epsilon;
            }
        });
        return relevantShadows;
    }

    public List<Event<Shadow>> shadowsBefore(MethodOrMethodContext method, List<MethodOrMethodContext> methodsAlreadyTraversed) {
        methodsAlreadyTraversed.add(method);

        List<Event<Shadow>> before = new ArrayList<Event<Shadow>>();

        if (!method.method().isEntryMethod()
                && ma.reachableMethods.contains(method)) {

            Set<MethodOrMethodContext> methodsCallingMethod = new HashSet<MethodOrMethodContext>();
            for (InvokeExpr call : ma.methodInvokedWhere.get(method)) {
                before.addAll(shadowsBeforeCall.get(call));
                methodsCallingMethod.add(ma.invokeExprInMethod.get(call));
            }

            methodsCallingMethod.remove(method);

            methodsCallingMethod.forEach(m -> {
                if (!methodsAlreadyTraversed.contains(m))
                    before.addAll(shadowsBefore(m, methodsAlreadyTraversed));
            });
        }

        return before;
    }

    public List<Event<Shadow>> shadowsAfter(MethodOrMethodContext method) {

        List<Event<Shadow>> after = new ArrayList<Event<Shadow>>();

        List<MethodOrMethodContext> methodsToTraverse = new ArrayList<MethodOrMethodContext>();
        methodsToTraverse.add(method);

        List<MethodOrMethodContext> methodsDone = new ArrayList<MethodOrMethodContext>();

        while (methodsToTraverse.size() > 0) {
            methodsDone.addAll(methodsToTraverse);

            List<MethodOrMethodContext> newMethodsToTraverse = new ArrayList<MethodOrMethodContext>();

            for (MethodOrMethodContext toDo : methodsToTraverse) {
                if (ma.methodInvokedWhere.get(toDo) != null) {
                    for (InvokeExpr call : ma.methodInvokedWhere.get(toDo)) {
                        after.addAll(shadowsAfterCall.get(call));
                        newMethodsToTraverse.add(ma.invokeExprInMethod.get(call));
                    }
                }
            }

            methodsToTraverse = newMethodsToTraverse;
            methodsToTraverse.removeAll(methodsDone);
        }

        return after;


//		for(MethodOrMethodContext methodSucceeding : this.allMethodsSucceeding.get(method)){
//			if(!method.equals(methodSucceeding)
//					&& this.methodCFG.containsKey(methodSucceeding)
//					&& this.methodCFG.get(methodSucceeding) != null){
//				after.addAll(this.methodCFG.get(methodSucceeding).alphabet);
//			}
//		}
//
//		return after;
    }

    public void shadowsBeforeCall() {
        List<InvokeExpr> relevantCalls = new ArrayList<InvokeExpr>();
        for (MethodOrMethodContext method : this.methodCFG.keySet()) {
            if (ma.methodInvokedWhere.get(method) != null) {
                relevantCalls.addAll(ma.methodInvokedWhere.get(method));
            }
        }

        for (InvokeExpr call : relevantCalls) {
            List<Event<Shadow>> before = new ArrayList<Event<Shadow>>();
            List<Event<Shadow>> after = new ArrayList<Event<Shadow>>();

            Unit invokeExprUnit = ma.invokeExprToUnit.get(call);
            MethodOrMethodContext method = call.getMethod();

            MethodOrMethodContext callingMethod = this.unitCalledBy.get(invokeExprUnit);

            if (callingMethod != null && this.methodCFG.keySet().contains(callingMethod)) {

                CFG callingMethodCFG = this.methodCFG.get(callingMethod);
                State<Integer, Shadow> stateInCallingMethodCFG = callingMethodCFG.labelToState.get(invokeExprUnit);

                //the below is needed just in case of loops, so there is no infinite looping
                List<State<Integer, Shadow>> alreadyTraversed = new ArrayList<State<Integer, Shadow>>();

                List<State<Integer, Shadow>> currentStates = new ArrayList<State<Integer, Shadow>>();
                currentStates.add(stateInCallingMethodCFG);

                //while we haven't traversed all possible previous states/units
                while (currentStates.size() > 0) {
                    List<State<Integer, Shadow>> newCurrentStates = new ArrayList<State<Integer, Shadow>>();

                    //for each state
                    for (int i = 0; i < currentStates.size(); i++) {
                        State<Integer, Shadow> state = currentStates.get(i);

                        //for each incoming transition event
                        for (Event<Shadow> event : state.incomingTransitions.keySet()) {
                            //if the event is not the empty event
                            //then add it to the before set
                            if (!event.label.epsilon) {
                                before.add(event);
                            }
                            //add all incoming states with that event to the states to propagate backwards next
                            newCurrentStates.addAll((Collection<? extends State<Integer, Shadow>>) state.parent.labelToState.get(state.incomingTransitions.get(event)));
                        }

                        //if the current state represents an invocation to another method
                        //i.e. if it has an internal fsm
                        //then add all relevant shadows of the method to the before set
                        if (state.getInternalFSM() != null) {
                            MethodOrMethodContext methodCalledHere = this.FSMMethod.get(state.getInternalFSM());
                            if (methodCalledHere == null) state.setInternalFSM(null);
                            else if (!methodCalledHere.equals(method)) {
                                before.addAll(this.relevantShadows(methodCalledHere, callingMethod));// method));
                            }
                        }
                    }

                    alreadyTraversed.addAll(currentStates);
                    newCurrentStates.removeAll(alreadyTraversed);
                    currentStates = newCurrentStates;
                }

                //calculate after

                alreadyTraversed = new ArrayList<State<Integer, Shadow>>();
                currentStates = new ArrayList<State<Integer, Shadow>>();
                currentStates.add(stateInCallingMethodCFG);
                while (currentStates.size() > 0) {
                    List<State<Integer, Shadow>> newCurrentStates = new ArrayList<State<Integer, Shadow>>();

                    for (int i = 0; i < currentStates.size(); i++) {
                        State<Integer, Shadow> state = currentStates.get(i);

                        for (Event<Shadow> event : state.outgoingTransitions.keySet()) {
                            if (!event.label.epsilon) {
                                after.add(event);
                            }
                            newCurrentStates.addAll((Collection<? extends State<Integer, Shadow>>) state.parent.labelToState.get(state.outgoingTransitions.get(event)));
                        }

                        if (state.getInternalFSM() != null) {
                            MethodOrMethodContext methodCalledHere = this.FSMMethod.get(state.getInternalFSM());
                            if (methodCalledHere == null) state.setInternalFSM(null);

                            else if (!methodCalledHere.equals(method)) {
                                after.addAll(this.relevantShadows(methodCalledHere, callingMethod));//method));
                                //								after.addAll(state.getInternalFSM().alphabet);
                                //
                                //								for(MethodOrMethodContext methodPossiblyInvoked : this.allMethodsSucceeding.get(methodCalledHere)){
                                //									if(!methodPossiblyInvoked.equals(method)
                                //											&& this.methodCFG.keySet().contains(methodPossiblyInvoked)){
                                //										after.addAll(this.methodCFG.get(methodPossiblyInvoked).alphabet);
                                //									}
                                //								}
                            }
                        }
                    }


                    alreadyTraversed.addAll(currentStates);
                    newCurrentStates.removeAll(alreadyTraversed);
                    currentStates = newCurrentStates;
                }

            }

            before.remove(epsilonAction);
            after.remove(epsilonAction);

            this.shadowsBeforeCall.put(call, before);
            this.shadowsAfterCall.put(call, after);
        }
    }

    public Pair<List<Event<Shadow>>, List<Event<Shadow>>> shadowsBeforeAndAfter(MethodOrMethodContext method) {
        List<Event<Shadow>> before = new ArrayList<Event<Shadow>>();
        List<Event<Shadow>> after = new ArrayList<Event<Shadow>>();

        Pair<List<Event<Shadow>>, List<Event<Shadow>>> beforeAfter = new Pair<List<Event<Shadow>>, List<Event<Shadow>>>(before, after);

        if (ma.methodInvokedWhere.get(method) != null) {
            for (InvokeExpr invokeExpr : ma.methodInvokedWhere.get(method)) {
                before.addAll(this.shadowsBeforeCall.get(invokeExpr));
                after.addAll(this.shadowsAfterCall.get(invokeExpr));
            }

            before.addAll(this.shadowsBefore(method, new ArrayList<MethodOrMethodContext>()));
            after.addAll(this.shadowsAfter(method));
        }

        return beforeAfter;
    }

    public Pair<List<Event<Shadow>>, List<Event<Shadow>>> shadowsBeforeAndAfterOld(MethodOrMethodContext method) {

        List<Event<Shadow>> before = new ArrayList<Event<Shadow>>();
        List<Event<Shadow>> after = new ArrayList<Event<Shadow>>();

        Pair<List<Event<Shadow>>, List<Event<Shadow>>> beforeAfter = new Pair<List<Event<Shadow>>, List<Event<Shadow>>>(before, after);

        //calculate before
        //traverse methodCFG back to initial state from states corresponding to the invokeexpr unit

        if (ma.methodInvokedWhere.get(method) != null) {
            for (InvokeExpr invokeExpr : ma.methodInvokedWhere.get(method)) {
                Unit invokeExprUnit = ma.invokeExprToUnit.get(invokeExpr);

                MethodOrMethodContext callingMethod = this.unitCalledBy.get(invokeExprUnit);
                if (callingMethod != null
                        && !callingMethod.equals(method)
                        && this.methodCFG.keySet().contains(callingMethod)) {

                    CFG callingMethodCFG = this.methodCFG.get(callingMethod);
                    State<Integer, Shadow> stateInCallingMethodCFG = callingMethodCFG.labelToState.get(invokeExprUnit);

                    //the below is needed just in case of loops, so there is no infinite looping
                    List<State<Integer, Shadow>> alreadyTraversed = new ArrayList<State<Integer, Shadow>>();

                    List<State<Integer, Shadow>> currentStates = new ArrayList<State<Integer, Shadow>>();
                    currentStates.add(stateInCallingMethodCFG);

                    //while we haven't traversed all possible previous states/units
                    while (currentStates.size() > 0) {
                        List<State<Integer, Shadow>> newCurrentStates = new ArrayList<State<Integer, Shadow>>();

                        //for each state
                        for (int i = 0; i < currentStates.size(); i++) {
                            State<Integer, Shadow> state = currentStates.get(i);

                            //for each incoming transition event
                            for (Event<Shadow> event : state.incomingTransitions.keySet()) {
                                //if the event is not the empty event
                                //then add it to the before set
                                if (!event.label.epsilon) {
                                    before.add(event);
                                }
                                //add all incoming states with that event to the states to propagate backwards next
                                newCurrentStates.addAll((Collection<? extends State<Integer, Shadow>>) state.parent.labelToState.get(state.incomingTransitions.get(event)));
                            }

                            //if the current state represents an invocation to another method
                            //i.e. if it has an internal fsm
                            //then add all relevant shadows of the method to the before set
                            if (state.getInternalFSM() != null) {
                                MethodOrMethodContext methodCalledHere = this.FSMMethod.get(state.getInternalFSM());
                                if (methodCalledHere == null) state.setInternalFSM(null);
                                else if (!methodCalledHere.equals(method)) {
                                    before.addAll(this.relevantShadows(methodCalledHere, method));
                                }
                            }
                        }
                        alreadyTraversed.addAll(currentStates);
                        newCurrentStates.removeAll(alreadyTraversed);
                        currentStates = newCurrentStates;
                    }

                    before.addAll(this.shadowsBefore(callingMethod, new ArrayList<MethodOrMethodContext>()));
                    //calculate after

                    alreadyTraversed = new ArrayList<State<Integer, Shadow>>();
                    currentStates = new ArrayList<State<Integer, Shadow>>();
                    currentStates.add(stateInCallingMethodCFG);
                    while (currentStates.size() > 0) {
                        List<State<Integer, Shadow>> newCurrentStates = new ArrayList<State<Integer, Shadow>>();

                        for (int i = 0; i < currentStates.size(); i++) {
                            State<Integer, Shadow> state = currentStates.get(i);
                            for (Event<Shadow> event : state.outgoingTransitions.keySet()) {
                                if (!event.label.epsilon) {
                                    after.add(event);
                                }
                                newCurrentStates.addAll((Collection<? extends State<Integer, Shadow>>) state.parent.labelToState.get(state.outgoingTransitions.get(event)));
                            }

                            if (state.getInternalFSM() != null) {
                                MethodOrMethodContext methodCalledHere = this.FSMMethod.get(state.getInternalFSM());
                                if (methodCalledHere == null) state.setInternalFSM(null);

                                else if (!methodCalledHere.equals(method)) {
                                    after.addAll(this.relevantShadows(methodCalledHere, method));
                                    //								after.addAll(state.getInternalFSM().alphabet);
                                    //
                                    //								for(MethodOrMethodContext methodPossiblyInvoked : this.allMethodsSucceeding.get(methodCalledHere)){
                                    //									if(!methodPossiblyInvoked.equals(method)
                                    //											&& this.methodCFG.keySet().contains(methodPossiblyInvoked)){
                                    //										after.addAll(this.methodCFG.get(methodPossiblyInvoked).alphabet);
                                    //									}
                                    //								}
                                }
                            }
                        }

                        alreadyTraversed.addAll(currentStates);
                        newCurrentStates.removeAll(alreadyTraversed);
                        currentStates = newCurrentStates;
                    }

                    for (MethodOrMethodContext methodSucceeding : this.allMethodsSucceeding.get(method)) {
                        if (!method.equals(methodSucceeding)
                                && this.methodCFG.containsKey(methodSucceeding)
                                && this.methodCFG.get(methodSucceeding) != null) {
                            after.addAll(this.methodCFG.get(methodSucceeding).alphabet);
                        }
                    }
                }
            }
        }

        before.remove(epsilonAction);
        after.remove(epsilonAction);

        return beforeAfter;
    }

    public CFG methodCFGToWholeProgramCFG(MethodOrMethodContext method) {
        CFG methodCFG = this.methodCFG.get(method);

        //if we create new fsm we run out of memory
        CFG wholeProgramCFG = methodCFG;// new CFG(methodCFG);//new FSM<Unit, Shadow>(methodCFG);
        boolean changed = false;
        if (!Scene.v().getMainMethod().equals(method)) {
            Pair<List<Event<Shadow>>, List<Event<Shadow>>> beforeAfter = this.shadowsBeforeAndAfter(method);

            for (State<Integer, Shadow> initial : wholeProgramCFG.initial) {
                for (Event<Shadow> shadow : beforeAfter.first) {
                    wholeProgramCFG.addTransition(initial, shadow, initial);
                    changed = true;
                }
            }

            for (State<Integer, Shadow> finalState : wholeProgramCFG.finalStates) {
                //add restofprogram alphabet loops
                for (Event<Shadow> shadow : beforeAfter.second) {
                    wholeProgramCFG.addTransition(finalState, shadow, finalState);
                    changed = true;
                }

                //we should only do this if methods can invoke this method can again invoke it after
                for (State<Integer, Shadow> initial : wholeProgramCFG.initial) {
                    wholeProgramCFG.addTransition(finalState, this.epsilonAction, initial);
                    changed = true;
                }
            }
        }

        ArrayList<State<Integer, Shadow>> stateList = new ArrayList<State<Integer, Shadow>>(wholeProgramCFG.states);
        for (int i = 0; i < stateList.size(); i++) {
            State<Integer, Shadow> state = stateList.get(i);
            if (state.getInternalFSM() != null) {
                //add loops for all shadows relevant to internalFSM
                MethodOrMethodContext methodInvokedHere = this.FSMMethod.get(state.getInternalFSM());

                //are the two conditions disjuncted below equal? they should be i think
                if (methodInvokedHere == null) {
                    state.setInternalFSM(null);
                } else if (methodInvokedHere.equals(method)
                        || this.allMethodsSucceeding.get(method).contains(methodInvokedHere)) {
                    for (State<Integer, Shadow> initial : wholeProgramCFG.initial) {
                        wholeProgramCFG.addTransition(state, epsilonAction, initial);
                        changed = true;
                    }
                }

                Set<Event<Shadow>> relevantShadows = this.relevantShadows(methodInvokedHere, method);
                for (Event<Shadow> shadow : relevantShadows) {
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

    public SubsetDate sufficientResidual(Shadow s, CFG wholeProgramCFG, DateFSM property) {
        SubsetDate residual;

        Set<Transition<String, DateLabel>> transitionsToKeep = new HashSet<Transition<String, DateLabel>>();

        if (property == null) return new SubsetDate(new DateFSM());
        FSM<Pair<Integer, Set<String>>, Shadow> composition = this.synchronousComposition(wholeProgramCFG, property, s);
        //		System.out.println(s);
        //		System.out.println(wholeProgramCFG);
        //		System.out.println(composition);

        Map<String, Set<String>> stateToNewHoareTripleMethods = new HashMap<String, Set<String>>();
        for (String label : property.stateHoareTripleMethod.keySet()) {
            stateToNewHoareTripleMethods.put(label, new HashSet<String>(property.stateHoareTripleMethod.get(label)));
        }

        for (State<Pair<Integer, Set<String>>, Shadow> state : composition.states) {
            if (state.label.second.size() > 0) {
                for (String label : state.label.second) {
                    if (stateToNewHoareTripleMethods.get(label) != null) {
                        for (Event<Shadow> event : state.outgoingTransitions.keySet()) {
                            if (!event.label.epsilon) {
                                String methodName = event.label.event.name;
                                String methodClass = ((MethodCall) event.label.event).objectType;

                                String method = methodClass + "." + methodName;
                                stateToNewHoareTripleMethods.get(label).add(method);
                            }
                        }
                    }
                }

            }
        }

        for (Transition<Pair<Integer, Set<String>>, Shadow> transition : composition.transitions) {
            Set<String> sourcePropertyStates = transition.source.label.second;
            //Set<String> destinationPropertyStates = transition.source.label.second;
            DateEvent event = transition.event.label.event;

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

    //assuming flat fsm
    public FSM<Pair<Integer, Set<String>>, Shadow> synchronousComposition(CFG cfg, DateFSM date, Shadow shadow) {
        //Pair<Set<Shadow>,Set<Shadow>> mustAndMay = this.mustAndMayAliasShadows(shadow);
        FSM<Pair<Integer, Set<String>>, Shadow> composition = new FSM<Pair<Integer, Set<String>>, Shadow>();

        Set<State<Pair<Integer, Set<String>>, Shadow>> statesToTransitionOn = new HashSet<State<Pair<Integer, Set<String>>, Shadow>>();

        Iterator<State<Integer, Shadow>> initial = cfg.initial.iterator();
        if (!initial.hasNext()) return composition;

        if (date.neverFails) return composition;
        //get initial property state
        Set<String> initialPropertiesStates = new HashSet<String>();
        initialPropertiesStates.add(date.startingState.label);

        Pair<Integer, Set<String>> initialTag = new Pair<Integer, Set<String>>(initial.next().label, initialPropertiesStates);
        statesToTransitionOn.add(composition.getOrAddState(initialTag));

        while (statesToTransitionOn.size() > 0) {

            //			 for(State<Pair<Unit, Set<String>>,Shadow> state : statesToTransitionOn){
            //				 if(state.getInternalFSM() != null){
            //					 //create transition from state -> tau -> (initial state subfsm, state.propstates)
            //					 //
            //				 }
            //			 }

            Set<State<Pair<Integer, Set<String>>, Shadow>> statesTransitionedTo;

            //	 statesToTransitionOn = this.expandPropertyStates(statesToTransitionOn);
            //pushes composition states with clock and channel events
            //i.e. takes all possible clock and channel events
            statesTransitionedTo = pushStates(composition, date, cfg, statesToTransitionOn);
            //pushes composition states with methodcalls
            statesTransitionedTo = pushStatesOneStep(composition, shadow, date, cfg, statesTransitionedTo);
            statesToTransitionOn.clear();

            for (State<Pair<Integer, Set<String>>, Shadow> state : statesTransitionedTo) {
                if (!cfg.finalStates.contains(cfg.labelToState.get(state.label.first))
                        && !(state.outgoingTransitions.entrySet().size() > 0)//this condition makes sure we iterate over states only one time
                        && state.label.second.size() != 0) {
                    statesToTransitionOn.add(state);
                }
            }
        }

        for (State<Pair<Integer, Set<String>>, Shadow> state : composition.states) {

            for (String label : state.label.second) {

                State<String, DateLabel> propertyState = date.labelToState.get(label);
                if (date.badStates.contains(propertyState)) {
                    for (Event<Shadow> event : state.incomingTransitions.keySet()) {
                        if (event.label.unit != null) {
                            int lineNumber = ((Stmt) event.label.unit).getJavaSourceStartLineNumber();
                            String className = this.unitCalledBy.get(event.label.unit).method().getDeclaringClass().getName();
                            String methodName = this.unitCalledBy.get(event.label.unit).method().getName();
                            this.ppfs.add(new Pair<String, String>(lineNumber + ": " + className + "." + methodName, event.label.unit.toString()));
                        }
                    }
                }
            }
        }

        composition.removeUnusedEvents();
        //events here correspond to transitions in the wholeprogram CFG
        this.canBeDisabled.removeAll(composition.alphabet);
        return composition;
    }

    //	public Set<State<Pair<Integer,Set<String>>,Shadow>> expandPropertyState(FSM<Pair<Integer,Set<String>>,Shadow> composition, State<Pair<Integer,Set<String>>,Shadow> state){
    //		Set<State<Pair<Integer,Set<String>>,Shadow>> states = new HashSet<State<Pair<Integer,Set<String>>,Shadow>>();
    //
    //		for(String propertyStateLabel : state.label.second){
    //			State<String, DateLabel> propertyState =
    //		}
    //
    //		for(Entry<Event<DateLabel>, Set<State<String, DateLabel>>> entry : state.outgoingTransitions.entrySet()){
    //			DateEvent event = entry.getKey().label.event;
    //			if(!(event instanceof MethodCall)){
    //
    //				for(State<String, DateLabel> nextState : entry.getValue()){
    //					if(event instanceof ChannelEvent)
    //						composition.addTransition(state, new Event<Shadow>(new Shadow((ChannelEvent)event)), nextState);
    //					else if(event instanceof ClockEvent)
    //						composition.addTransition(state, new Event<Shadow>(new Shadow((ClockEvent)event)), nextState);
    //					states.addAll(expandPropertyState(composition, nextState));
    //				}
    //				states.addAll(entry.getValue());
    //			}
    //			else{
    //				states.add(state);
    //			}
    //		}
    //
    //		return states;
    //	}
    //
    //	public Set<State<String, DateLabel>> expandPropertyStates(FSM<Pair<Integer,Set<String>>,Shadow> composition, DateFSM date, Set<State<String, DateLabel>> states, ){
    //		Set<State<String, DateLabel>> nextStates = new HashSet<State<String, DateLabel>>();
    //
    //		for(State<String, DateLabel> state : states){
    //			nextStates.addAll(expandPropertyState(state));
    //		}
    //
    //		return nextStates;
    //	}


    public Set<State<Pair<Integer, Set<String>>, Shadow>> pushStatesOneStep(FSM<Pair<Integer, Set<String>>, Shadow> composition, Shadow shadow, DateFSM date, CFG cfg, Set<State<Pair<Integer, Set<String>>, Shadow>> statesToTransitionOn) {

        Set<State<Pair<Integer, Set<String>>, Shadow>> statesTransitionedTo = new HashSet<State<Pair<Integer, Set<String>>, Shadow>>();

        for (State<Pair<Integer, Set<String>>, Shadow> state : statesToTransitionOn) {
            CFG stateFSM = cfg;//methodCFG.get(unitCalledBy.get(cfg.units.get(state.label.first)));
            //get the cfgState of the state in the composition we are now considering
            State<Integer, Shadow> cfgState = stateFSM.getOrAddState(state.label.first);
            //get the property states of the state in the composition
            Set<String> propertyStates = state.label.second;


            //for each possible transition w.r.t to the cfg state
            for (Event<Shadow> event : cfgState.outgoingTransitions.keySet()) {
                Set<String> nextPropertyStates = new HashSet<String>();
                //if the event is an epsilon transition then do not transition w.r.t to the property states
                if (event.label == null || event.label.epsilon || !event.label.mayAlias(shadow)) {
                    nextPropertyStates.addAll(propertyStates);
                } else {
                    //if the events not must-alias but may-alias then keep all the previous states also
                    if (!event.label.mustAlias(shadow) && event.label.mayAlias(shadow)) {
                        nextPropertyStates.addAll(propertyStates);
                    }
                    //for each property state label
                    for (String propertyStateLabel : propertyStates) {
                        //get the property state from the label
                        State<String, DateLabel> propertyState = date.getOrAddState(propertyStateLabel);

                        // if(propertyState.outgoingTransitions.get(event.label.event) != null){
                        //for all property state transitions

                        for (Event<DateLabel> eventConditionAction : propertyState.outgoingTransitions.keySet()) {
                            //if one can transition from the property state using the shadow's matched event
                            if (eventConditionAction.label.event.equals(event.label.event)) {
                                //then transition for all transitions using this event
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
                        State<Integer, Shadow> destinationState = cfgState.parent.labelToState.get(destinationStateLabel);

                        //create a new label with the cfg state after the shadow and the calculated next property states
                        Pair<Integer, Set<String>> nextStateLabel = new Pair<Integer, Set<String>>(destinationState.label, nextPropertyStates);

                        //add the state created in the previous line to the composition
                        State<Pair<Integer, Set<String>>, Shadow> nextState = composition.getOrAddState(nextStateLabel);

                        composition.addTransition(state, event, nextState);

                        statesTransitionedTo.add(nextState);

                    }
                }
            }
        }


        //statesTransitionedTo.removeAll(statesToTransitionOn);
        return statesTransitionedTo;

    }

    public Set<State<Pair<Integer, Set<String>>, Shadow>> pushStates(FSM<Pair<Integer, Set<String>>, Shadow> composition, DateFSM date, CFG cfg, Set<State<Pair<Integer, Set<String>>, Shadow>> statesToTransitionOn) {

        Set<State<Pair<Integer, Set<String>>, Shadow>> statesTransitionedTo = new HashSet<State<Pair<Integer, Set<String>>, Shadow>>();

        for (State<Pair<Integer, Set<String>>, Shadow> state : statesToTransitionOn) {
            CFG stateFSM = cfg;//methodCFG.get(unitCalledBy.get(cfg.units.get(state.label.first)));
            //get the cfgState of the state in the composition we are now considering
            State<Integer, Shadow> cfgState = stateFSM.getOrAddState(state.label.first);
            //get the property states of the state in the composition
            Set<String> propertyStates = state.label.second;

            for (String propertyStateLabel : propertyStates) {
                State<String, DateLabel> propertyState = date.getOrAddState(propertyStateLabel);

                for (Entry<Event<DateLabel>, Set<String>> entry : propertyState.outgoingTransitions.entrySet()) {

//					Entry<Event<DateLabel>,Set<State<String,DateLabel>>> entry;

                    if (entry.getKey().label.event instanceof ChannelEvent) {
                        ChannelEvent channelEvent = (ChannelEvent) entry.getKey().label.event;
                        Event<Shadow> event = new Event<Shadow>(new Shadow(channelEvent));

                        Set<String> newPropertyStates = new HashSet<String>(propertyStates);
                        newPropertyStates.remove(propertyStateLabel);

                        for (String nextStateLabel : entry.getValue()) {
                            newPropertyStates.add(nextStateLabel);
                        }

                        Pair<Integer, Set<String>> newStateLabel = new Pair<Integer, Set<String>>(cfgState.label, newPropertyStates);

                        State<Pair<Integer, Set<String>>, Shadow> newState = composition.getOrAddState(newStateLabel);
                        composition.addTransition(state, event, newState);

                        statesTransitionedTo.add(newState);
                    } else if (entry.getKey().label.event instanceof ClockEvent) {
                        ClockEvent clockEvent = (ClockEvent) entry.getKey().label.event;
                        Event<Shadow> event = new Event<Shadow>(new Shadow(clockEvent));

                        Set<String> newPropertyStates = new HashSet<String>(propertyStates);
                        newPropertyStates.remove(propertyStateLabel);

                        for (String nextStateLabel : entry.getValue()) {
                            newPropertyStates.add(nextStateLabel);
                        }

                        Pair<Integer, Set<String>> newStateLabel = new Pair<Integer, Set<String>>(cfgState.label, newPropertyStates);

                        State<Pair<Integer, Set<String>>, Shadow> newState = composition.getOrAddState(newStateLabel);
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


    public boolean recursive(MethodOrMethodContext method) {
        return this.allMethodsSucceeding.get(method).contains(method);
    }

    public boolean selfContainedMethod(MethodOrMethodContext method) {
        //consider all the shadows in a method's CFG
        //
        if (this.methodCFG.keySet().contains(method)
                && this.methodCFG.get(method) != null
                && this.methodCFG.get(method).states.size() == 1) {
            List<Event<Shadow>> shadowsInMethod = new ArrayList<Event<Shadow>>(this.methodCFG.get(method).alphabet);
            List<Local> parameters = method.method().getActiveBody().getParameterLocals();
            if (parameters.size() == 0) {
                //still need to check if any of the pointers in the shadow bindings
                //are assigned to some other pointer (not local to the method)
                return true;
            }
            return false;
        } else {
            return true;
        }
    }
}