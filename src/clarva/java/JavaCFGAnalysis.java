package clarva.java;

import clarva.analysis.CFGAnalysis;
import clarva.analysis.cfg.CFG;
import clarva.analysis.cfg.CFGEvent;
import clarva.analysis.cfg.CFGState;
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
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.Unit;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.internal.JNopStmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.pointer.LocalMustAliasAnalysis;
import soot.jimple.toolkits.pointer.LocalMustNotAliasAnalysis;
import soot.jimple.toolkits.pointer.StrongLocalMustAliasAnalysis;
import soot.toolkits.graph.TrapUnitGraph;
import soot.toolkits.graph.UnitGraph;

import java.util.*;

public class JavaCFGAnalysis extends CFGAnalysis<Unit, JavaEvent, JavaMethodIdentifier> {

    public static JavaCFGAnalysis cfgAnalysis;

    public MethodsAnalysis ma;

    public Map<JavaMethodIdentifier, TrapUnitGraph> methodGraph;

    public Map<JavaMethodIdentifier, LocalMustAliasAnalysis> methodMustAlias;
    public Map<JavaMethodIdentifier, LocalMustNotAliasAnalysis> methodMustNotAlias;


    Map<InvokeExpr, Set<Event<JavaEvent>>> shadowsBeforeCall = new HashMap<>();
    Map<InvokeExpr, Set<Event<JavaEvent>>> shadowsAfterCall = new HashMap<>();

    List<String> ppfs = new ArrayList<>();

    public JavaCFGAnalysis(MethodsAnalysis ma) {
        super();
        this.ma = ma;
        cfgAnalysis = this;

        mainMethods.add(JavaMethodIdentifier.get(Scene.v().getMainMethod()));

        for (Map.Entry<InvokeExpr, MethodOrMethodContext> entry : ma.invokedMethod.entrySet()) {
            Set<JavaMethodIdentifier> methods = new HashSet<>();
            methods.add(JavaMethodIdentifier.get(entry.getValue()));
            this.statesCallMethods.put(ma.invokeExprToUnit.get(entry.getKey()), methods);

            Set<Unit> stmts = new HashSet<>();
            stmts.add(ma.invokeExprToUnit.get(entry.getKey()));

            if (this.methodCalledByStates.get(JavaMethodIdentifier.get(entry.getValue())) != null) {
                this.methodCalledByStates.get(JavaMethodIdentifier.get(entry.getValue())).addAll(stmts);
            } else {
                this.methodCalledByStates.put(JavaMethodIdentifier.get(entry.getValue()), stmts);
            }

        }

        this.epsilonAction = new Event<>(new JavaEvent());

        methodGraph = new HashMap<>();
        methodCFG = new HashMap<>();
        CFGMethod = new HashMap<>();
        methodNoDirectMethodCall = new HashMap<>();
        allStatesNull = new HashMap<>();
        methodMustAlias = new HashMap<>();
        methodMustNotAlias = new HashMap<>();

        statementCalledBy = new HashMap<>();

        eventsPossiblyOccurringBeforeMethod = new HashMap<>();
        eventsPossiblyOccurringAfterMethod = new HashMap<>();

        methodNoMatchingMethodCall = new HashMap<>();
//        allMethodsSucceeding = new HashMap<>();

        Set<MethodOrMethodContext> methodsToKeep = this.methodFSMsToGenerateFor(new HashSet<>(ma.sootMethodToDateEvents.keySet()));

        for (MethodOrMethodContext method : methodsToKeep) {
            CFG<Unit, JavaEvent> fsm = new CFG<>();
            fsm.methodID = JavaMethodIdentifier.get(method);
            fsm.name = method.method().getName();
            methodCFG.put(JavaMethodIdentifier.get(method), fsm);

            CFGMethod.put(fsm, JavaMethodIdentifier.get(method));
        }

        for (MethodOrMethodContext method : methodsToKeep) {
            if (method.method().hasActiveBody()) {
                TrapUnitGraph cfg = new TrapUnitGraph(method.method().getActiveBody());

                methodGraph.put(JavaMethodIdentifier.get(method), cfg);
            }
        }


        for (MethodOrMethodContext method : methodsToKeep) {
            if (method.method().hasActiveBody()) {
                createCFG(JavaMethodIdentifier.get(method));

                if (this.methodCFG.get(JavaMethodIdentifier.get(method)).transitions.size() == 0) {
                    this.CFGMethod.remove(this.methodCFG.get(JavaMethodIdentifier.get(method)));
                    this.methodCFG.remove(JavaMethodIdentifier.get(method));
                    //TODO this doesn t match methodGraph type
                    this.methodGraph.remove(method);
                }
            }
        }


        for (JavaMethodIdentifier method : methodGraph.keySet()) {
            methodMustAlias.put(method, new StrongLocalMustAliasAnalysis(methodGraph.get(method)));
            methodMustNotAlias.put(method, new LocalMustNotAliasAnalysis(methodGraph.get(method)));
        }

        this.tagShadowsWithBindingRepresentatives();
//        generateMethodCallGraph();
        shadowsOutsideCall();
    }

    //methods contains all methods that match events, not methods that call events
    public Set<MethodOrMethodContext> methodFSMsToGenerateFor(Set<MethodOrMethodContext> methods) {
        Set<MethodOrMethodContext> methodsToKeep = new HashSet<MethodOrMethodContext>();

        Set<MethodOrMethodContext> methodsAlreadyDealtWith = new HashSet<MethodOrMethodContext>();
        Set<MethodOrMethodContext> methodsToResolve = new HashSet<MethodOrMethodContext>();

        methodsToResolve.addAll(methods);

        CallGraph cg = Scene.v().getCallGraph();

        boolean notFinished = true;
        while (notFinished) {
            notFinished = false;
            Set<MethodOrMethodContext> methodsToResolve2 = new HashSet<MethodOrMethodContext>();

            methodsToResolve.removeAll(methodsAlreadyDealtWith);

            //if all methods to resolve already resolved
            if (methodsToResolve.size() == 0) {
//                return methodsToKeep;
                notFinished = false;
                break;
            }

            methodsAlreadyDealtWith.addAll(methodsToResolve);

            for (MethodOrMethodContext method : methodsToResolve) {
//                if (this.reachableFromMainMethod(method)) {
                    Iterator<Edge> edgesIntoMethod = cg.edgesInto(method);
                    while (edgesIntoMethod.hasNext()) {
                        methodsToResolve2.add(edgesIntoMethod.next().getSrc());
                        notFinished = true;
                    }
//                }

                if(method.method().getDeclaringClass().isApplicationClass() && !method.method().isJavaLibraryMethod()){
                    methodsToKeep.add(method);
                }
            }

//            methodsToKeep.addAll(methodsToResolve);
            methodsToResolve.clear();
            methodsToResolve = methodsToResolve2;
        }

        methodsToKeep.removeAll(methods);

//        for(MethodOrMethodContext method : methodsToKeep){
//            try {
//                ProgramModifier.createUnmonitoredCopy(method.method());
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }

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

    @Override
    public void createCFG(MethodIdentifier methodID) {
        if (JavaMethodIdentifier.class.isAssignableFrom(methodID.getClass())) {

            JavaMethodIdentifier method = (JavaMethodIdentifier) methodID;

            //If method cfg was created
            if (methodGraph.containsKey(method)) {
                //Get cfg of the method
                UnitGraph sootCFG = methodGraph.get(method);
                //Get (empty) fsm of the method
                CFG<Unit, JavaEvent> cfg = methodCFG.get(method);

                if (cfg == null) {
                    methodCFG.put(method, new CFG());
                    cfg = methodCFG.get(method);
                }

                Iterator<Unit> unitIterator = sootCFG.iterator();

                Unit currentUnit;
                while (unitIterator.hasNext()) {
                    currentUnit = unitIterator.next();
                    statementCalledBy.put(currentUnit, method);

                    //This takes care of not having duplicates states
                    //with the same unit.
                    //Note: The same statements at different points of a program
                    //correspond to different statements.
                    State<Integer, JavaEvent> currentUnitState = cfg.getOrAddState(new CFGState<Unit>(currentUnit));

                    if (ma.unitsContainingMethods.get(currentUnit) != null
                            && this.methodCFG.keySet().contains(JavaMethodIdentifier.get(ma.unitsContainingMethods.get(currentUnit)))) {
                        MethodOrMethodContext methodCall = ma.unitsContainingMethods.get(currentUnit);
                        currentUnitState.setInternalFSM(methodCFG.get(JavaMethodIdentifier.get(methodCall)));
                    }

//                Moved below to end of MethodsAnalysis.allMethodCalls()
//
//                //sometimes invocation statements are not dealt with properly by soot
//                //here we try do see if an invocation is a shadow, just in case
//                if (!ma.unitShadows.containsKey(currentUnit)) {
//                    if(Stmt.class.isAssignableFrom(currentUnit.getClass())){
//                        Stmt stmt = (Stmt) currentUnit;
//
//                        if(stmt.containsInvokeExpr()){
//                            ma.populateFieldsForInvokeExpr(stmt, currentUnit, method.methodOrMethodContext, stmt.getInvokeExpr());
//                        }
//                    }
//                }

                    //If unit is not caught by some action in the property
                    //then create a transition from each predecessor state
                    //with the empty action to the current unit state
                    //and create a transition from the current unit state
                    //to each successor state with the empty action
                    if (!ma.unitShadows.containsKey(currentUnit)) {

                        for (Unit succ : sootCFG.getSuccsOf(currentUnit)) {
                            State<Integer, JavaEvent> succState = cfg.getOrAddState(new CFGState<Unit>(succ));

                            //fsm.addTransition(currentUnitState, this.tauAction, succState);
                            cfg.addTransition(currentUnitState, this.epsilonAction, succState);
                        }

                        MethodOrMethodContext currentUnitMethod = ma.unitsContainingMethods.get(currentUnit);
                        //If the unit is not atomic (i.e. it has a cfg and fsm)
                        //then set the unit's fsm reference.
                        if (methodCFG.containsKey(JavaMethodIdentifier.get(currentUnitMethod))) {
                            currentUnitState.setInternalFSM(methodCFG.get(JavaMethodIdentifier.get(currentUnitMethod)));
//                        methodsCalledByMethod.add(JavaMethodIdentifier.get(currentUnitMethod));
                            //						allStatesNull = false;
                            //						methodsCalledByMethod.add(currentUnitMethod);
                        }
                    } else {

                        List<JavaEvent> shadows = ma.unitShadows.get(currentUnit);

                        State<Integer, JavaEvent> source = currentUnitState;

                        for (int i = 0; i < shadows.size() - 1; i++) {
                            State<Integer, JavaEvent> destination = cfg.getOrAddState(new CFGState<>(new JNopStmt()));
                            Event<JavaEvent> event = new Event<JavaEvent>(shadows.get(i));
                            allEventShadows.add(event);
                            cfg.addTransition(source, event, destination);
                            source = destination;
                        }

                        for (Unit succ : sootCFG.getSuccsOf(currentUnit)) {
                            State<Integer, JavaEvent> destination = cfg.getOrAddState(new CFGState<>(succ));
                            Event<JavaEvent> event = new Event<JavaEvent>(shadows.get(shadows.size() - 1));
                            allEventShadows.add(event);

                            cfg.addTransition(source, event, destination);
                        }

                    }
                    //How to handle uponreturning and uponthrowing??
                    //	}

                }
                for (Unit init : sootCFG.getHeads()) {
                    State<Integer, JavaEvent> initState = cfg.labelToState.get(new CFGState(init));
                    cfg.addInitialState(initState);
                }

                for (Unit tail : sootCFG.getTails()) {
                    State<Integer, JavaEvent> finalState = cfg.labelToState.get(new CFGState(tail));
                    cfg.addFinalState(finalState);
                }

                //			//Remove states with only empty outgoing transitions (i.e. with statements that do not match actions)
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

                methodCFG.put(method, cfg);
            }


            //	methodNoDirectMethodCall.put(method, noDirectMethodCalls);
            //	this.allStatesNull.put(method, allStatesNull);
            //return fsm;
        }
    }

    public void tagShadowsWithBindingRepresentatives() {
        for (MethodIdentifier method : methodCFG.keySet()) {
            CFG<Unit, JavaEvent> fsm = methodCFG.get(method);
            //check that fsm is actually the method's fsm, not replaced by another method's fsm
            if (this.CFGMethod.get(fsm).equals(method)) {
                for (Transition<Integer, JavaEvent> transition : fsm.transitions) {
                    ((JavaEvent) transition.event.label).inferBinding(((JavaMethodIdentifier) method).methodOrMethodContext.method(),
                            methodMustAlias.get(method),
                            methodMustNotAlias.get(method));
                }
            }
        }
    }

    public Pair<Set<JavaEvent>, Set<JavaEvent>> mustAndMayAliasShadows(JavaEvent shadow) {
        Set<JavaEvent> must = new HashSet<>();
        Set<JavaEvent> may = new HashSet<>();

        for (JavaEvent other : ma.allShadows) {
            if (shadow.mustAlias(other)) {
                must.add(other);
            } else if (shadow.mayAlias(other)) {
                may.add(other);
            }
        }

        return new Pair<>(must, may);
    }

    public Set<Event<JavaEvent>> restOfProgramShadows(JavaMethodIdentifier method) {
        Set<Event<JavaEvent>> restOfProgramShadows = new HashSet<>();

        for (JavaMethodIdentifier otherMethod : this.methodCFG.keySet()) {
            if (!method.equals(otherMethod)) {
                restOfProgramShadows.addAll(this.methodCFG.get(otherMethod).alphabet);
            }
        }

        return restOfProgramShadows;
    }

    @Override
    public void createChannelAndClockEvents(DateFSM date) {
        for (Transition<String, DateLabel> transition : date.transitions) {
            DateEvent dateEvent = transition.event.label.event;
            if (DateEvent.class.equals(ClockEvent.class)) {
                clockCFGEvents.put((ClockEvent) dateEvent, new JavaEvent((ClockEvent) dateEvent));
            } else if (DateEvent.class.equals(ChannelEvent.class)) {
                channelCFGEvents.put((ChannelEvent) dateEvent, new JavaEvent((ChannelEvent) dateEvent));
            }
        }
    }

    //
//    public Set<Event<JavaEvent>> relevantShadows(JavaMethodIdentifier method, JavaMethodIdentifier methodInvokedInThis) {
//        Set<Event<JavaEvent>> relevantShadows = new HashSet<>();
//
//        if (this.methodCFG.get(method) == null) return relevantShadows;
//
//        relevantShadows.addAll(this.methodCFG.get(method).alphabet);
//
//        if (this.allMethodsSucceeding.get(method) != null) {
//            for (JavaMethodIdentifier otherMethod : this.allMethodsSucceeding.get(method)) {
//                if (!otherMethod.equals(method)
//                        && !otherMethod.equals(methodInvokedInThis)) {
//                    if (this.methodCFG.get(otherMethod) != null)
//                        relevantShadows.addAll(this.methodCFG.get(otherMethod).alphabet);
//                }
//            }
//        }
//
//        relevantShadows.removeIf(new Predicate<Event<JavaEvent>>() {
//            @Override
//            public boolean test(Event<JavaEvent> e) {
//                return e.label.epsilon;
//            }
//        });
//        return relevantShadows;
//    }
//
    public List<Event<JavaEvent>> shadowsBefore(JavaMethodIdentifier method,
                                                List<JavaMethodIdentifier> methodsAlreadyTraversed) {
        methodsAlreadyTraversed.add(method);

        List<Event<JavaEvent>> before = new ArrayList<>();

        if (!method.methodOrMethodContext.method().isEntryMethod()
                && ma.reachableMethods.contains(method.methodOrMethodContext)) {

            Set<JavaMethodIdentifier> methodsCallingMethod = new HashSet<>();

            if (ma.methodInvokedWhere.containsKey(method.methodOrMethodContext)) {
                for (InvokeExpr call : ma.methodInvokedWhere.get(method.methodOrMethodContext)) {
                    if (!shadowsBeforeCall.containsKey(call)) {
                    } else {
                        before.addAll(shadowsBeforeCall.get(call));
                    }
                        methodsCallingMethod.add(JavaMethodIdentifier.get(ma.invokeExprInMethod.get(call)));
                }
            } else {
                //this can happen for main methods of a thread for example.
                //Assumption: properties cannot span different threads
            }

            methodsCallingMethod.remove(method);

            methodsCallingMethod.forEach(m -> {
                if (!methodsAlreadyTraversed.contains(m))
                    before.addAll(shadowsBefore(m, methodsAlreadyTraversed));
            });
        }

        return before;
    }

    public List<Event<JavaEvent>> shadowsAfter(JavaMethodIdentifier method) {

        List<Event<JavaEvent>> after = new ArrayList<>();

        List<JavaMethodIdentifier> methodsToTraverse = new ArrayList<>();
        methodsToTraverse.add(method);

        List<MethodIdentifier> methodsDone = new ArrayList<>();

        while (methodsToTraverse.size() > 0) {
            methodsDone.addAll(methodsToTraverse);

            List<JavaMethodIdentifier> newMethodsToTraverse = new ArrayList<>();

            for (JavaMethodIdentifier toDo : methodsToTraverse) {
                if (ma.methodInvokedWhere.get(toDo.methodOrMethodContext) != null) {
                    for (InvokeExpr call : ma.methodInvokedWhere.get(toDo.methodOrMethodContext)) {
                        if(shadowsAfterCall.containsKey(call)) {
                            after.addAll(shadowsAfterCall.get(call));
                        }
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
//

    public void shadowsOutsideCallInsideCallingMethod(InvokeExpr call) {
        Set<Event<JavaEvent>> before = new HashSet<>();
        Set<Event<JavaEvent>> after = new HashSet<>();

        Unit invokeExprUnit = ma.invokeExprToUnit.get(call);
        MethodOrMethodContext method = call.getMethod();

        JavaMethodIdentifier callingMethod = (JavaMethodIdentifier) this.statementCalledBy.get(invokeExprUnit);

        if (callingMethod != null && this.methodCFG.keySet().contains(callingMethod)) {

            CFG<Unit, JavaEvent> callingMethodCFG = this.methodCFG.get(callingMethod);
            State<Integer, JavaEvent> stateInCallingMethodCFG = callingMethodCFG.labelToState.get(new CFGState(invokeExprUnit));

            //the below is needed just in case of loops, so there is no infinite looping
            List<State<Integer, JavaEvent>> alreadyTraversed = new ArrayList<>();

            List<State<Integer, JavaEvent>> currentStates = new ArrayList<>();
            currentStates.add(stateInCallingMethodCFG);

            //while we haven't traversed all possible previous states/statements
            while (currentStates.size() > 0) {
                List<State<Integer, JavaEvent>> newCurrentStates = new ArrayList<>();

                //for each state
                for (int i = 0; i < currentStates.size(); i++) {
                    State<Integer, JavaEvent> state = currentStates.get(i);

                    //for each incoming transition dateEvent
                    for (Event<JavaEvent> event : state.incomingTransitions.keySet()) {
                        //if the dateEvent is not the empty dateEvent
                        //then add it to the before set
                        if (!event.label.epsilon) {
                            before.add(event);
                        }
                        //add all incoming states with that dateEvent to the states to propagate backwards next
                        Set<Integer> incomingStateLabels = state.incomingTransitions.get(event);
                        for (Integer label : incomingStateLabels) {
                            newCurrentStates.add(state.parent.labelToState.get(label));
                        }
                    }

                    //if the current state represents an invocation to another method
                    //i.e. if it has an internal fsm
                    //then add all relevant shadows of the method to the before set
                    for (State<Integer, JavaEvent> nextState : newCurrentStates) {
                        if (nextState.getInternalFSM() != null) {
                            JavaMethodIdentifier methodCalledHere = this.CFGMethod.get(nextState.getInternalFSM());
                            if (methodCalledHere == null) nextState.setInternalFSM(null);
                            else if (!methodCalledHere.methodOrMethodContext.equals(method)) {
                                before.addAll(this.relevantShadows(methodCalledHere, callingMethod).first);// method));
                            }
                        }
                    }
                }

                alreadyTraversed.addAll(currentStates);
                newCurrentStates.removeAll(alreadyTraversed);
                currentStates = newCurrentStates;
            }


            //calculate after

            alreadyTraversed = new ArrayList<State<Integer, JavaEvent>>();
            currentStates = new ArrayList<State<Integer, JavaEvent>>();
            currentStates.add(stateInCallingMethodCFG);
            while (currentStates.size() > 0) {
                List<State<Integer, JavaEvent>> newCurrentStates = new ArrayList<State<Integer, JavaEvent>>();

                for (int i = 0; i < currentStates.size(); i++) {
                    State<Integer, JavaEvent> state = currentStates.get(i);

                    for (Event<JavaEvent> event : state.outgoingTransitions.keySet()) {
                        if (!event.label.epsilon) {
                            after.add(event);
                        }
                        Set<Integer> outgoingStateLabels = state.outgoingTransitions.get(event);
                        for (Integer label : outgoingStateLabels) {
                            newCurrentStates.add(state.parent.labelToState.get(label));
                        }
                    }

                    if (state.getInternalFSM() != null) {
                        JavaMethodIdentifier methodCalledHere = this.CFGMethod.get(state.getInternalFSM());
                        if (methodCalledHere == null) state.setInternalFSM(null);

                        else if (!methodCalledHere.methodOrMethodContext.equals(method)) {
                            after.addAll(this.relevantShadows(methodCalledHere, callingMethod).first);//method));
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

    public void shadowsOutsideCallArbitraryDepth() {
        for (InvokeExpr invokeExpr : shadowsBeforeCall.keySet()) {
            Unit invokeExprUnit = ma.invokeExprToUnit.get(invokeExpr);
            MethodOrMethodContext method = invokeExpr.getMethod();

            JavaMethodIdentifier callingMethod = (JavaMethodIdentifier) this.statementCalledBy.get(invokeExprUnit);

            if (callingMethod != null) {
                shadowsBeforeCall.get(invokeExpr).addAll(shadowsBefore(callingMethod, new ArrayList<>()));
            }
        }

        for (InvokeExpr invokeExpr : shadowsAfterCall.keySet()) {
            Unit invokeExprUnit = ma.invokeExprToUnit.get(invokeExpr);
            MethodOrMethodContext method = invokeExpr.getMethod();

            JavaMethodIdentifier callingMethod = (JavaMethodIdentifier) this.statementCalledBy.get(invokeExprUnit);

            if (callingMethod != null) {
                shadowsAfterCall.get(invokeExpr).addAll(shadowsAfter(callingMethod));
            }
        }


    }

    public void shadowsOutsideCall() {
        //get the set of invocations of the methods we are analysing
        List<InvokeExpr> relevantCalls = new ArrayList<InvokeExpr>();
        for (JavaMethodIdentifier method : this.methodCFG.keySet()) {
            if (ma.methodInvokedWhere.get(method.methodOrMethodContext) != null) {
                relevantCalls.addAll(ma.methodInvokedWhere.get(method.methodOrMethodContext));
            }
        }
        //   ma.methodInvokedWhere.get(method.methodOrMethodContext)
        //for each invocation, traverse the method it appears in both backwards and forwards till the start and end of
        //the method are reached, and collect the events encountered
        for (InvokeExpr call : relevantCalls) {
            shadowsOutsideCallInsideCallingMethod(call);
        }

        shadowsOutsideCallArbitraryDepth();
    }


    public Pair<Set<Event<JavaEvent>>, Set<Event<JavaEvent>>> shadowsBeforeAndAfter(JavaMethodIdentifier method) {
        Set<Event<JavaEvent>> before = new HashSet<>();
        Set<Event<JavaEvent>> after = new HashSet<>();

        Pair<Set<Event<JavaEvent>>, Set<Event<JavaEvent>>> beforeAfter = new Pair<>(before, after);

        List<InvokeExpr> invocations = ma.methodInvokedWhere.get(method.methodOrMethodContext);

        if (invocations != null) {
            for (InvokeExpr invokeExpr : invocations) {
                if (!this.shadowsBeforeCall.containsKey(invokeExpr)) {
                } else{
                    before.addAll(this.shadowsBeforeCall.get(invokeExpr));
                    after.addAll(this.shadowsAfterCall.get(invokeExpr));
                }
            }

            before.addAll(this.shadowsBefore(method, new ArrayList<>()));
            after.addAll(this.shadowsAfter(method));
        }

        return beforeAfter;
    }

    public void identifyPotentialPointsOfFailure(FSM<Pair<Integer, Set<String>>, CFGEvent> composition, DateFSM date) {
        for (State<Pair<Integer, Set<String>>, CFGEvent> state : composition.states) {

            for (String label : state.label.second) {

                State<String, DateLabel> propertyState = date.labelToState.get(label);
                if (date.badStates.contains(propertyState)) {
                    for (Event<CFGEvent> event : state.incomingTransitions.keySet()) {
                        if (event.label != null) {
                            Stmt stmt = ((Stmt) ((JavaEvent) event.label).unit);
                            int lineNumber = stmt.getJavaSourceStartLineNumber();
                            String className = ((JavaMethodIdentifier) this.statementCalledBy.get(stmt)).methodOrMethodContext.method().getDeclaringClass().getName();
                            String methodName = ((JavaMethodIdentifier) this.statementCalledBy.get(stmt)).methodOrMethodContext.method().getName();
//                            this.ppfs.add(new Pair<String, String>(lineNumber + ": " + className + "." + methodName, event.label.toString()));
                            this.ppfs.add(lineNumber + ": " + className + "." + methodName + ": " + event.label.toString());
                        }
                    }
                }
            }
        }
    }

//////////////////////////////////////////////////////////////////////////////////////////////////////
//////The methods below were intended ot be used in conjunction with Starvoors
//////////////////////////////////////////////////////////////////////////////////////////////////////
//    public boolean recursive(JavaMethodIdentifier method) {
//        return this.allMethodsSucceeding.get(method).contains(method);
//    }
//
//    public boolean selfContainedMethod(JavaMethodIdentifier method) {
//        //consider all the shadows in a method's CFG
//        //
//        if (this.methodCFG.keySet().contains(method)
//                && this.methodCFG.get(method) != null
//                && this.methodCFG.get(method).states.size() == 1) {
//            List<Event<JavaEvent>> shadowsInMethod = new ArrayList<>(this.methodCFG.get(method).alphabet);
//            List<Local> parameters = method.methodOrMethodContext.method().getActiveBody().getParameterLocals();
//            if (parameters.size() == 0) {
//                //still need to check if any of the pointers in the shadow bindings
//                //are assigned to some other pointer (not local to the method)
//                return true;
//            }
//            return false;
//        } else {
//            return true;
//        }
//    }

//    public boolean inLoop(JavaMethodIdentifier method, Stmt s) {
//        LoopFinder loopFinder = new LoopFinder();
//        loopFinder.transform(method.methodOrMethodContext.method().getActiveBody());
//        for (Loop loop : loopFinder.loops()) {
//            if (loop.getLoopStatements().contains(s)) {
//                return true;
//            }
//        }
//
//        return false;
//    }
//////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////
}