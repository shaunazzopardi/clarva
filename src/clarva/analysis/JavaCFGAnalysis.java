package clarva.analysis;

import clarva.analysis.cfg.CFG;
import clarva.analysis.cfg.CFGEvent;
import clarva.java.JavaMethodIdentifier;
import clarva.java.Shadow;
import clarva.matching.MethodIdentifier;
import fsm.Event;
import fsm.FSM;
import fsm.State;
import fsm.Transition;
import fsm.date.DateFSM;
import fsm.date.DateLabel;
import fsm.date.events.ChannelEvent;
import fsm.date.events.ClockEvent;
import fsm.date.events.DateEvent;
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
import java.util.function.Predicate;

public class JavaCFGAnalysis extends CFGAnalysis<Shadow, JavaMethodIdentifier>{

    public MethodsAnalysis ma;

    public Map<JavaMethodIdentifier, TrapUnitGraph> methodGraph;

    public Map<Unit, JavaMethodIdentifier> unitCalledBy;

    public Map<JavaMethodIdentifier, LocalMustAliasAnalysis> methodMustAlias;
    public Map<JavaMethodIdentifier, LocalMustNotAliasAnalysis> methodMustNotAlias;


    Map<InvokeExpr, List<Event<Shadow>>> shadowsBeforeCall = new HashMap<>();
    Map<InvokeExpr, List<Event<Shadow>>> shadowsAfterCall = new HashMap<>();
    List<String> ppfs = new ArrayList<>();

    public JavaCFGAnalysis(MethodsAnalysis ma) {

        this.ma = ma;

        epsilonAction = new Event<>(new Shadow());

        methodGraph = new HashMap<>();
        methodCFG = new HashMap<>();
        FSMMethod = new HashMap<>();
        methodNoDirectMethodCall = new HashMap<>();
        allStatesNull = new HashMap<>();
        methodMustAlias = new HashMap<>();
        methodMustNotAlias = new HashMap<>();

        unitCalledBy = new HashMap<>();

        eventsPossiblyOccurringBeforeMethod = new HashMap<>();
        eventsPossiblyOccurringAfterMethod = new HashMap<>();

        methodNoMatchingMethodCall = new HashMap<>();
        allMethodsSucceeding = new HashMap<>();

        Set<MethodOrMethodContext> methodsToKeep = this.methodFSMsToGenerateFor(new HashSet<>(ma.sootMethodToDateEvents.keySet()));

        for (MethodOrMethodContext method : methodsToKeep) {
            CFG<Shadow> fsm = new CFG<>();
            methodCFG.put(JavaMethodIdentifier.get(method), fsm);
            FSMMethod.put(fsm, JavaMethodIdentifier.get(method));
        }

        for (MethodOrMethodContext method : methodsToKeep) {
            if (method.method().hasActiveBody()) {
                TrapUnitGraph cfg = new TrapUnitGraph(method.method().getActiveBody());

                methodGraph.put(JavaMethodIdentifier.get(method), cfg);
            }
        }


        for (MethodOrMethodContext method : methodsToKeep) {
            if (method.method().hasActiveBody()) {
                //				System.out.println(method.method().getSignature());
                createCFG(JavaMethodIdentifier.get(method));

                if (this.methodCFG.get(method).transitions.size() == 0) {
                    this.FSMMethod.remove(this.methodCFG.get(method));
                    this.methodCFG.remove(method);
                    this.methodGraph.remove(method);
                }
            }
        }


        for (JavaMethodIdentifier method : methodGraph.keySet()) {
            methodMustAlias.put(method, new StrongLocalMustAliasAnalysis(methodGraph.get(method)));
            methodMustNotAlias.put(method, new LocalMustNotAliasAnalysis(methodGraph.get(method)));
        }

        this.tagShadowsWithBindingRepresentatives();
        this.shadowsBeforeCall();

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

    public void createCFG(JavaMethodIdentifier method) {
        Set<JavaMethodIdentifier> methodsCalledByMethod;
        if (this.allMethodsSucceeding.get(method) == null) {
            methodsCalledByMethod = new HashSet<>();
            this.allMethodsSucceeding.put(method, methodsCalledByMethod);
        } else {
            methodsCalledByMethod = this.allMethodsSucceeding.get(method);
        }

        //If method cfg was created
        if (methodGraph.containsKey(method)) {
            //Get cfg of the method
            UnitGraph sootCFG = methodGraph.get(method);
            //Get (empty) fsm of the method
            CFG<Shadow> cfg = methodCFG.get(method);

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
                        methodsCalledByMethod.add(JavaMethodIdentifier.get(currentUnitMethod));
                        //						allStatesNull = false;
                        //						methodsCalledByMethod.add(currentUnitMethod);
                    }
                } else {

                    List<Shadow> shadows = ma.unitShadows.get(currentUnit);

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
        for (MethodIdentifier method : methodCFG.keySet()) {
            CFG<Shadow> fsm = methodCFG.get(method);
            //check that fsm is actually the method's fsm, not replaced by another method's fsm
            if (this.FSMMethod.get(fsm).equals(method)) {
                for (Transition<Integer, Shadow> transition : fsm.transitions) {
                    ((Shadow)transition.event.label).inferBinding(((JavaMethodIdentifier) method).methodOrMethodContext.method(),
                            methodMustAlias.get(method),
                            methodMustNotAlias.get(method));
                }
            }
        }
    }

    public Pair<Set<Shadow>, Set<Shadow>> mustAndMayAliasShadows(Shadow shadow) {
        Set<Shadow> must = new HashSet<>();
        Set<Shadow> may = new HashSet<>();

        for (Shadow other : ma.allShadows) {
            if (shadow.mustAlias(other)) {
                must.add(other);
            } else if (shadow.mayAlias(other)) {
                may.add(other);
            }
        }

        return new Pair<>(must, may);
    }

    public Set<Event<Shadow>> restOfProgramShadows(JavaMethodIdentifier method) {
        Set<Event<Shadow>> restOfProgramShadows = new HashSet<>();

        for (JavaMethodIdentifier otherMethod : this.methodCFG.keySet()) {
            if (!method.equals(otherMethod)) {
                restOfProgramShadows.addAll(this.methodCFG.get(otherMethod).alphabet);
            }
        }

        return restOfProgramShadows;
    }

    @Override
    public void createChannelAndClockEvents(DateFSM date) {
        for(Transition<String, DateLabel> transition : date.transitions){
            DateEvent dateEvent = transition.event.label.event;
            if(DateEvent.class.equals(ClockEvent.class)){
                clockCFGEvents.put((ClockEvent) dateEvent, new Shadow((ClockEvent) dateEvent));
            } else if(DateEvent.class.equals(ChannelEvent.class)){
                channelCFGEvents.put((ChannelEvent) dateEvent, new Shadow((ChannelEvent) dateEvent));
            }
        }
    }

    public Set<Event<Shadow>> relevantShadows(JavaMethodIdentifier method, JavaMethodIdentifier methodInkovedInThis) {
        Set<Event<Shadow>> relevantShadows = new HashSet<>();

        if (this.methodCFG.get(method) == null) return relevantShadows;

        relevantShadows.addAll(this.methodCFG.get(method).alphabet);

        if (this.allMethodsSucceeding.get(method) != null) {
            for (JavaMethodIdentifier otherMethod : this.allMethodsSucceeding.get(method)) {
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

    public List<Event<Shadow>> shadowsBefore(JavaMethodIdentifier method,
                                             List<JavaMethodIdentifier> methodsAlreadyTraversed) {
        methodsAlreadyTraversed.add(method);

        List<Event<Shadow>> before = new ArrayList<>();

        if(method.getClass().equals(JavaMethodIdentifier.class)) {
            JavaMethodIdentifier javaMethod = method;
            if (!javaMethod.methodOrMethodContext.method().isEntryMethod()
                    && ma.reachableMethods.contains(method)) {

                Set<JavaMethodIdentifier> methodsCallingMethod = new HashSet<>();
                for (InvokeExpr call : ma.methodInvokedWhere.get(method)) {
                    before.addAll(shadowsBeforeCall.get(call));
                    methodsCallingMethod.add(JavaMethodIdentifier.get(ma.invokeExprInMethod.get(call)));
                }

                methodsCallingMethod.remove(method);

                methodsCallingMethod.forEach(m -> {
                    if (!methodsAlreadyTraversed.contains(m))
                        before.addAll(shadowsBefore(m, methodsAlreadyTraversed));
                });
            }
        }
        return before;
    }

    public List<Event<Shadow>> shadowsAfter(JavaMethodIdentifier method) {

        List<Event<Shadow>> after = new ArrayList<>();

        List<JavaMethodIdentifier> methodsToTraverse = new ArrayList<>();
        methodsToTraverse.add(method);

        List<MethodIdentifier> methodsDone = new ArrayList<>();

        while (methodsToTraverse.size() > 0) {
            methodsDone.addAll(methodsToTraverse);

            List<JavaMethodIdentifier> newMethodsToTraverse = new ArrayList<>();

            for (JavaMethodIdentifier toDo : methodsToTraverse) {
                if (ma.methodInvokedWhere.get(toDo) != null) {
                    for (InvokeExpr call : ma.methodInvokedWhere.get(toDo)) {
                        after.addAll(shadowsAfterCall.get(call));
                        newMethodsToTraverse.add(JavaMethodIdentifier.get(ma.invokeExprInMethod.get(call)));
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
        for (MethodIdentifier method : this.methodCFG.keySet()) {
            if (ma.methodInvokedWhere.get(method) != null) {
                relevantCalls.addAll(ma.methodInvokedWhere.get(method));
            }
        }

        for (InvokeExpr call : relevantCalls) {
            List<Event<Shadow>> before = new ArrayList<>();
            List<Event<Shadow>> after = new ArrayList<>();

            Unit invokeExprUnit = ma.invokeExprToUnit.get(call);
            MethodOrMethodContext method = call.getMethod();

            JavaMethodIdentifier callingMethod = this.unitCalledBy.get(invokeExprUnit);

            if (callingMethod != null && this.methodCFG.keySet().contains(callingMethod)) {

                CFG<Shadow> callingMethodCFG = this.methodCFG.get(callingMethod);
                State<Integer, Shadow> stateInCallingMethodCFG = callingMethodCFG.labelToState.get(invokeExprUnit);

                //the below is needed just in case of loops, so there is no infinite looping
                List<State<Integer, Shadow>> alreadyTraversed = new ArrayList<>();

                List<State<Integer, Shadow>> currentStates = new ArrayList<>();
                currentStates.add(stateInCallingMethodCFG);

                //while we haven't traversed all possible previous states/units
                while (currentStates.size() > 0) {
                    List<State<Integer, Shadow>> newCurrentStates = new ArrayList<>();

                    //for each state
                    for (int i = 0; i < currentStates.size(); i++) {
                        State<Integer, Shadow> state = currentStates.get(i);

                        //for each incoming transition dateEvent
                        for (Event<Shadow> event : state.incomingTransitions.keySet()) {
                            //if the dateEvent is not the empty dateEvent
                            //then add it to the before set
                            if (!event.label.epsilon) {
                                before.add(event);
                            }
                            //add all incoming states with that dateEvent to the states to propagate backwards next
                            newCurrentStates.addAll((Collection<? extends State<Integer, Shadow>>) state.parent.labelToState.get(state.incomingTransitions.get(event)));
                        }

                        //if the current state represents an invocation to another method
                        //i.e. if it has an internal fsm
                        //then add all relevant shadows of the method to the before set
                        if (state.getInternalFSM() != null) {
                            JavaMethodIdentifier methodCalledHere = this.FSMMethod.get(state.getInternalFSM());
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
                            JavaMethodIdentifier methodCalledHere = this.FSMMethod.get(state.getInternalFSM());
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

    public Pair<List<Event<Shadow>>, List<Event<Shadow>>> shadowsBeforeAndAfter(JavaMethodIdentifier method) {
        List<Event<Shadow>> before = new ArrayList<>();
        List<Event<Shadow>> after = new ArrayList<>();

        Pair<List<Event<Shadow>>, List<Event<Shadow>>> beforeAfter = new Pair<>(before, after);

        if (ma.methodInvokedWhere.get(method) != null) {
            for (InvokeExpr invokeExpr : ma.methodInvokedWhere.get(method)) {
                before.addAll(this.shadowsBeforeCall.get(invokeExpr));
                after.addAll(this.shadowsAfterCall.get(invokeExpr));
            }

            before.addAll(this.shadowsBefore(method, new ArrayList<>()));
            after.addAll(this.shadowsAfter(method));
        }

        return beforeAfter;
    }

    public CFG methodCFGToWholeProgramCFG(JavaMethodIdentifier method) {
        CFG<Shadow> methodCFG = this.methodCFG.get(method);

        //if we create new fsm we run out of memory
        CFG<Shadow> wholeProgramCFG = methodCFG;// new CFG(methodCFG);//new FSM<Unit, Shadow>(methodCFG);
        boolean changed = false;
        if (!Scene.v().getMainMethod().equals(method.methodOrMethodContext)) {
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
                JavaMethodIdentifier methodInvokedHere = this.FSMMethod.get(state.getInternalFSM());

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

    public void identifyPotentialPointsOfFailure(FSM<Pair<Integer, Set<String>>, CFGEvent> composition, DateFSM date){
        for (State<Pair<Integer, Set<String>>, CFGEvent> state : composition.states) {

            for (String label : state.label.second) {

                State<String, DateLabel> propertyState = date.labelToState.get(label);
                if (date.badStates.contains(propertyState)) {
                    for (Event<CFGEvent> event : state.incomingTransitions.keySet()) {
                        if (event.label != null) {
                            Stmt stmt = ((Stmt)((Shadow) event.label).unit);
                            int lineNumber = stmt.getJavaSourceStartLineNumber();
                            String className = this.unitCalledBy.get(stmt).methodOrMethodContext.method().getDeclaringClass().getName();
                            String methodName = this.unitCalledBy.get(stmt).methodOrMethodContext.method().getName();
//                            this.ppfs.add(new Pair<String, String>(lineNumber + ": " + className + "." + methodName, event.label.toString()));
                            this.ppfs.add(lineNumber + ": " + className + "." + methodName + ": " + event.label.toString());
                        }
                    }
                }
            }
        }
    }




    public boolean recursive(JavaMethodIdentifier method) {
        return this.allMethodsSucceeding.get(method).contains(method);
    }

    public boolean selfContainedMethod(JavaMethodIdentifier method) {
        //consider all the shadows in a method's CFG
        //
        if (this.methodCFG.keySet().contains(method)
                && this.methodCFG.get(method) != null
                && this.methodCFG.get(method).states.size() == 1) {
            List<Event<Shadow>> shadowsInMethod = new ArrayList<>(this.methodCFG.get(method).alphabet);
            List<Local> parameters = method.methodOrMethodContext.method().getActiveBody().getParameterLocals();
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

    public boolean inLoop(JavaMethodIdentifier method, Stmt s) {
        LoopFinder loopFinder = new LoopFinder();
        loopFinder.transform(method.methodOrMethodContext.method().getActiveBody());
        for (Loop loop : loopFinder.loops()) {
            if (loop.getLoopStatements().contains(s)) {
                return true;
            }
        }

        return false;
    }
}