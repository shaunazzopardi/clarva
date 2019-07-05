package clarva.java;

import fsm.Event;
import fsm.date.DateFSM;
import fsm.date.DateLabel;
import fsm.date.events.MethodCall;
import fsm.date.events.MethodCall.ActionType;
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.internal.AbstractInstanceInvokeExpr;
import soot.jimple.internal.AbstractVirtualInvokeExpr;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.util.queue.QueueReader;

import java.util.*;
import java.util.Map.Entry;

////////////////////
// Class that pre-computes and stores some information necessary for analysis
//
//TODO create generic version?
public class JavaMethodsAnalysis {

    //Associate soot methods with MethodCall date events and vice-versa
    // e.g. a method foo may be associated with the date events entry(foo) and exit(foo)
    //
    //@ invariant \forall MethodCall x ; dateEventToSootMethods.containsKey(x) ; (\forall MethodOrMethodContext y ; dateEventToSootMethods.get(x).contains(y) ;  sootMethodToDateEvents.get(y).contains(x))
    public Map<MethodCall, List<MethodOrMethodContext>> dateEventToSootMethods = new HashMap<>();
    public Map<MethodOrMethodContext, List<MethodCall>> sootMethodToDateEvents = new HashMap<>();

    //Associates method with the invocations it makes and vice-versa
    //
    //@ invariant \forall MethodOrMethodContext x ; invocationsMadeByMethod.containsKey(x) ; (\forall InvokeExpr y ; invocationsMadeByMethod.get(x).contains(y) ;  methodMakingInvocation.get(y).contains(x))
    public Map<MethodOrMethodContext, List<InvokeExpr>> invocationsMadeByMethod = new HashMap<>();
    public Map<InvokeExpr, MethodOrMethodContext> methodMakingInvocation = new HashMap<>();

    //Associates each method with each invocation expression that possibly calls it at runtime
    public Map<InvokeExpr, MethodOrMethodContext> invokedMethod = new HashMap<>();

    //Methods reachable from specified entry-points (e.g. the main method)
    public List<MethodOrMethodContext> reachableMethods;

    //Invocation associated with stateemnt
    public Map<InvokeExpr, Stmt> invokeExprInStmt = new HashMap<>();

    //All invocations
    public Set<InvokeExpr> allMethodCalls;

    //Associates units with methods in which they appear
    public Map<Unit, MethodOrMethodContext> unitsInMethod = new HashMap<>();

    //@ invariant \forall Unit u, InvokeExpr ie; unitsToInvokeExpr.containsKey(u) && invokeExprToUnit.containsKey(ie) ; invokeExprToUnit.get(ie).equals(u) <==> unitsToInvokeExpr.get(u).equals(ie)
    //TODO can a unit be attached with multiple invoke exprs?
    public Map<Unit, InvokeExpr> unitsToInvokeExpr = new HashMap<>();
    public Map<InvokeExpr, Unit> invokeExprToUnit = new HashMap<>();

    //Java events associated with each unit
    //@ invariant \forall Unit u; unitJavaEvents.containsKey(u); allEventsInProgram.containsAll(unitJavaEvents.get(u))
    public Map<Unit, List<JavaEvent>> unitJavaEvents = new HashMap<>();

    //All java events
    public Set<JavaEvent> allEventsInProgram = new HashSet<>();


    public JavaMethodsAnalysis(Scene scene, Set<Event<DateLabel>> events) {
        this.reachableMethods = reachableMethods(scene);

        //sets actionEvents and eventAction fields
        matchMethodsWithEvents(events, reachableMethods);

        allMethodCalls = allMethodCalls(scene);

        for (Unit unit : unitsToInvokeExpr.keySet()) {
            invokeExprToUnit.put(unitsToInvokeExpr.get(unit), unit);
            if (unitsToInvokeExpr.containsKey(unit)) {
                InvokeExpr expr = unitsToInvokeExpr.get(unit);

                if (invokedMethod.containsKey(expr)) {
                    MethodOrMethodContext method = invokedMethod.get(expr);

                    unitsInMethod.put(unit, method);
                }
            }
        }
    }

    public static List<MethodOrMethodContext> reachableMethods(Scene scene) {

        ReachableMethods reachableMethodsObject = scene.getReachableMethods();

        QueueReader<MethodOrMethodContext> reachableMethodsQueue = reachableMethodsObject.listener();

        List<MethodOrMethodContext> reachableMethods = new ArrayList<>();

        MethodOrMethodContext method;
        while (reachableMethodsQueue.hasNext()) {

            method = reachableMethodsQueue.next();

            reachableMethods.add(method);

        }

        return reachableMethods;
    }

    public Set<Event<DateLabel>> methodsNotCalled(DateFSM f) {
        Set<Event<DateLabel>> toRemove = new HashSet<Event<DateLabel>>();

        for (Event<DateLabel> action : f.alphabet) {
            if (dateEventToSootMethods.get(action).size() == 0) {
                toRemove.add(action);
            }
        }

        return toRemove;
    }

    public void matchMethodsWithEvents(Set<Event<DateLabel>> allEvents, List<MethodOrMethodContext> reachableMethods) {

        dateEventToSootMethods = new HashMap<>();
        sootMethodToDateEvents = new HashMap<>();

        for (Event<DateLabel> actionMethodCall : allEvents) {
            if (actionMethodCall.label.event instanceof MethodCall) {
                MethodCall event = (MethodCall) actionMethodCall.label.event;

                List<MethodOrMethodContext> methods = new ArrayList<>();

                for (int i = 0; i < reachableMethods.size(); i++) {
                    MethodOrMethodContext method = reachableMethods.get(i);

                    if (Matching.matchesMethod(method.method(), event)) {
                        methods.add(method);

                        if (sootMethodToDateEvents.containsKey(method)
                                && !sootMethodToDateEvents.get(method).contains(event)) {
                            sootMethodToDateEvents.get(method).add(event);
                        } else if (!sootMethodToDateEvents.containsKey(method)) {
                            List<MethodCall> corr = new ArrayList<MethodCall>();
                            corr.add(event);
                            sootMethodToDateEvents.put(method, corr);
                        }
                    }
                }

                dateEventToSootMethods.put(event, methods);
            }
        }

        Map<MethodOrMethodContext, List<MethodCall>> sootMethodToDateEventsNew = new HashMap<MethodOrMethodContext, List<MethodCall>>();

        for (Entry<MethodOrMethodContext, List<MethodCall>> entry : sootMethodToDateEvents.entrySet()) {
            sootMethodToDateEventsNew.put(entry.getKey(), this.sortAccordingToType(entry.getValue()));
        }

        sootMethodToDateEvents = sootMethodToDateEventsNew;
    }

    public Set<InvokeExpr> allMethodCalls(Scene scene) {

        //Get the method call graph of the scene
        CallGraph cg = scene.getCallGraph();

        //initialize fields
        Set<InvokeExpr> allMethodCalls = new HashSet<InvokeExpr>();

        invokeExprInStmt = new HashMap<>();
        invocationsMadeByMethod = new HashMap<>();
        invokedMethod = new HashMap<>();

        unitsToInvokeExpr = new HashMap<>();

        //Get an iterator over all edges
        Iterator<Edge> edges = cg.iterator();
//		System.out.println(cg);
        Edge edge;
        while (edges.hasNext()) {
            edge = edges.next();

            //get edge source statement (the
            Stmt sourceStatement = edge.srcStmt();
            //	int sourceLineNumber = sourceStatement.getJavaSourceStartLineNumber();

            if (sourceStatement != null && sourceStatement.containsInvokeExpr()) {

                InvokeExpr expr = sourceStatement.getInvokeExpr();
                if (expr instanceof InvokeExpr) {
                    allMethodCalls.addAll(populateFieldsForInvokeExpr(sourceStatement, edge.srcUnit(), edge.getSrc(), expr));
                }
            }
        }

        for (MethodOrMethodContext method : reachableMethods) {
            if (method.method().hasActiveBody()) {

                for (Unit currentUnit : method.method().getActiveBody().getUnits()) {
                    if (Stmt.class.isAssignableFrom(currentUnit.getClass())) {
                        Stmt stmt = (Stmt) currentUnit;

                        if (stmt.containsInvokeExpr()) {
                            this.populateFieldsForInvokeExpr(stmt, currentUnit, method.method(), stmt.getInvokeExpr());
                        }
                    }

                }
            }
        }

        return allMethodCalls;
    }

    public Set<InvokeExpr> populateFieldsForInvokeExpr(Stmt invocationStatement, Unit invocationUnit, MethodOrMethodContext invokingMethod, InvokeExpr expr) {
        Set<InvokeExpr> allMethodCalls = new HashSet<InvokeExpr>();

        allMethodCalls.add(expr);

        unitsToInvokeExpr.put(invocationUnit, expr);

        methodMakingInvocation.put(expr, invokingMethod);

        invokeExprInStmt.put(expr, invocationStatement);

        invokedMethod.put(expr, expr.getMethodRef().resolve());

        if (invocationsMadeByMethod.containsKey(expr.getMethodRef().resolve())) {
            invocationsMadeByMethod.get(expr.getMethodRef().resolve()).add(expr);
        } else {

            List<InvokeExpr> invoked = new ArrayList<InvokeExpr>();
            invoked.add(expr);

            invocationsMadeByMethod.put(expr.getMethodRef().resolve(), invoked);
        }

        //for each matching DateLabel
        //better to turn all these to match method calls rather than dateEvent<datelabel>
        List<MethodCall> matchingMethodCalls = sootMethodToDateEvents.get(invokedMethod.get(expr));

        if (matchingMethodCalls != null) {

            //TODO make sure all actual invoke exprs are being associated with a for each var

            List<Value> args = new ArrayList<>();
            Value classObject = null;

            if (AbstractVirtualInvokeExpr.class.isAssignableFrom(expr.getClass())) {

                AbstractVirtualInvokeExpr invokeExpr = (AbstractVirtualInvokeExpr) expr;
                classObject = ((InstanceInvokeExpr) expr).getBase();

                args.addAll(invokeExpr.getArgs());

            } else if (AbstractInstanceInvokeExpr.class.isAssignableFrom(expr.getClass())) {

                AbstractInstanceInvokeExpr invokeExpr = (AbstractInstanceInvokeExpr) expr;
                classObject = ((InstanceInvokeExpr) expr).getBase();

                args.addAll(invokeExpr.getArgs());
            } else {//(AbstractStaticInvokeExpr.class.isAssignableFrom(invoke.getClass())){
                args.addAll(expr.getArgs());
            }

            if (args == null) {
                args = new ArrayList<Value>();
            }

            List<JavaEvent> shadows = new ArrayList<JavaEvent>();

            this.unitJavaEvents.put(invocationUnit, shadows);

            for (int i = 0; i < matchingMethodCalls.size(); i++) {
                //		List<Map<String, Value>> listOfForEachVarsValue = new ArrayList<Map<String, Value>>();

                Map<String, Value> forEachVarValue = new HashMap<String, Value>();

                MethodCall current = matchingMethodCalls.get(i);

                for (String forEachVar : current.forEachVariables) {
                    if (classObject != null
                            && current.whereMap.get(forEachVar).equals(current.objectIdentifier)) {

                        forEachVarValue.put(forEachVar, classObject);
                    } else if (current.whereMap.get(forEachVar).equals(current.returnIdentifier)
                            && AssignStmt.class.isAssignableFrom(invocationStatement.getClass())) {
                        //&& current.isConstructor){

                        forEachVarValue.put(forEachVar, ((AssignStmt) invocationStatement).getLeftOp());
                    } else {

                        for (int j = 0; j < current.argIdentifiers.size(); j++) {
                            if (current.whereMap.get(forEachVar).equals(current.argIdentifiers.get(j))) {
                                forEachVarValue.put(forEachVar, args.get(j));
                            }
                        }
                    }
                }

                JavaEvent shadow = new JavaEvent(expr, invocationStatement, matchingMethodCalls.get(i), forEachVarValue);

                shadows.add(shadow);
                this.allEventsInProgram.add(shadow);
            }
        }

        return allMethodCalls;
    }


    public boolean before(MethodCall event, MethodCall otherEvent) {
        if (event.name == otherEvent.name
                && event.argTypes.equals(otherEvent.argTypes)) {
            List<ActionType> actionTypes = Arrays.asList(MethodCall.ActionType.values());
            if (actionTypes.indexOf(event.type) < actionTypes.indexOf(otherEvent.type)) {
                return true;
            }
        }

        return false;
    }

    public boolean after(MethodCall event, MethodCall otherEvent) {
        if (event.name == otherEvent.name
                && event.argTypes.equals(otherEvent.argTypes)) {
            List<ActionType> actionTypes = Arrays.asList(MethodCall.ActionType.values());
            if (actionTypes.indexOf(event.type) > actionTypes.indexOf(otherEvent.type)) {
                return true;
            }
        }

        return false;
    }

    public List<MethodCall> sortAccordingToType(List<MethodCall> unsortedList) {
        Set<MethodCall> set = new HashSet<MethodCall>(unsortedList);
        List<MethodCall> noDuplicateList = new ArrayList<MethodCall>(set);
        List<MethodCall> sortedList = new ArrayList<MethodCall>();


        if (noDuplicateList.size() <= 1) sortedList = noDuplicateList;
        else {
            sortedList.add(noDuplicateList.get(0));
            for (MethodCall call : noDuplicateList) {
                if (!sortedList.contains(call)) {
                    for (MethodCall sortedCall : sortedList) {
                        int sortedCallIndex = sortedList.indexOf(sortedCall);
                        if (before(call, sortedCall)) {
                            sortedList.add(sortedCallIndex, call);
                        } else if (sortedCallIndex + 1 <= sortedList.size()) {
                            sortedList.add(sortedCallIndex, call);
                        } else sortedList.add(call);
                    }
                }
            }
        }

        return sortedList;
    }
}
