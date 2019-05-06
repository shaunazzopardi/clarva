package clarva.java;

import polyglot.ast.Assign;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.AbstractInstanceInvokeExpr;
import soot.jimple.internal.AbstractVirtualInvokeExpr;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.ContextSensitiveCallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.util.queue.QueueReader;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import fsm.Event;
import fsm.date.DateFSM;
import fsm.date.DateLabel;
import fsm.date.events.MethodCall;
import fsm.date.events.MethodCall.ActionType;

public class MethodsAnalysis {

//	public Map<Event<DateLabel>, List<MethodOrMethodContext>> propertyTransitionToMethods;
//	public Map<MethodOrMethodContext, List<Event<DateLabel>>> methodToPropertyTransitions; 
	//a method can match multiple method calls 
	//since we don't/can't match for point of triggering
	//e.g. do() : [do()before(), do()uponReturning()] 
	
//	public Map<InvokeExpr, List<Value>> methodArgs;
//	public Map<InvokeExpr, Value> methodObject;
	
	//order of second value corresponds to order in eventAction
	//public Map<Shadow, List<Map<String, Value>>> shadowBindings;

	public Map<MethodCall, List<MethodOrMethodContext>> dateEventToSootMethods;
	public Map<MethodOrMethodContext, List<MethodCall>> sootMethodToDateEvents; 

	public Map<MethodOrMethodContext, List<InvokeExpr>> methodInvokedWhere;
	public Map<InvokeExpr, MethodOrMethodContext> invokeExprInMethod;

	public Map<InvokeExpr, MethodOrMethodContext> invokedMethod;
		
	public List<MethodOrMethodContext> reachableMethods;
	
	public Map<InvokeExpr, Stmt> invokeExprInStmt;
	
	public Set<InvokeExpr> allMethodCalls; //put this in callgraphanalysis

	public Map<Unit, MethodOrMethodContext> unitsContainingMethods; //can a unit be attached with multiple invoke exprs?
	public Map<MethodOrMethodContext, List<Unit>> methodsInUnits; //can a unit be attached with multiple invoke exprs?
	public Map<Unit, InvokeExpr> unitsToInvokeExpr; //can a unit be attached with multiple invoke exprs?
	public Map<InvokeExpr, Unit> invokeExprToUnit; //can a unit be attached with multiple invoke exprs?
	//public Map<Unit, List<Event<DateLabel>>> unitsToMethodCalls; //can a unit be attached with multiple invoke exprs?
		
	public Map<Unit, List<JavaEvent>> unitShadows;
	public Set<JavaEvent> allShadows;
	//public Map<MethodOrMethodContext, Map<String, Value>> shadowBindings;
//	public Map<InvokeExpr, Local> invokeExprLocals;
//	public Map<Local, String> localCorrespondingToVariable;
	
	public MethodsAnalysis(Scene scene, Set<Event<DateLabel>> events){
//		while(!Scene.v().doneResolving()){}
		
		CallGraph cg = scene.getCallGraph();

		try{
		    PrintWriter writer = new PrintWriter("cg2-FiTS.txt", "UTF-8");
		    writer.println(cg);
		    writer.close();
		} catch (IOException e) {
		   // do something
		}
//		SootClass userInfo = Scene.v().getSootClass("transactionsystem.UserInfo");
//		SootClass scenarios = Scene.v().getSootClass("transactionsystem.ScenariosWithObjects");
//
//		SootMethod main = scene.getMainMethod();
		unitsContainingMethods = new HashMap<Unit, MethodOrMethodContext>();
		methodsInUnits = new HashMap<MethodOrMethodContext, List<Unit>>();
		//unitsToMethodCalls = new HashMap<Unit, List<Event<DateLabel>>>();
		
		//shadowBindings = new HashMap<MethodOrMethodContext, Map<String, Value>>();
		
		invokeExprToUnit = new HashMap<InvokeExpr, Unit>();
		
		invokeExprInMethod = new HashMap<InvokeExpr,MethodOrMethodContext>();
		
		this.unitShadows = new HashMap<Unit, List<JavaEvent>>();
		allShadows = new HashSet<JavaEvent>();
		
		this.reachableMethods = reachableMethods(scene);
//		List<MethodOrMethodContext> toExclude = new ArrayList<MethodOrMethodContext>();
//		
//		for(MethodOrMethodContext method : this.reachableMethods){
//			if(method.method().isJavaLibraryMethod()){
//				toExclude.add(method);
//			}
//		}
//		
//		this.reachableMethods.removeAll(toExclude);
		
//		SootClass userInfo = Scene.v().getSootClass("transactionsystem.UserInfo");
		
		//sets actionEvents and eventAction fields
		matchMethodsWithEvents(events, reachableMethods);
		
		allMethodCalls = allMethodCalls(scene);

//		invokeExprLocals = new HashMap<InvokeExpr, Local>();
//		localCorrespondingToVariable = new HashMap<Local, String>();

		for(Unit unit : unitsToInvokeExpr.keySet()){
			invokeExprToUnit.put(unitsToInvokeExpr.get(unit), unit);
			if(unitsToInvokeExpr.containsKey(unit)){
				InvokeExpr expr = unitsToInvokeExpr.get(unit);
				
				if(invokedMethod.containsKey(expr)){
					MethodOrMethodContext method = invokedMethod.get(expr);
					
					unitsContainingMethods.put(unit, method);
//					if(this.eventMethods.containsKey(method)){
//						List<Event<DateLabel>> DateLabel = eventAction.get(method);
//						
//						unitsToMethodCalls.put(unit, DateLabel);
//					}
				}
			}
		}
	}

	
	public Set<Event<DateLabel>> methodsNotCalled(DateFSM f){
		Set<Event<DateLabel>> toRemove = new HashSet<Event<DateLabel>>();
		
		for(Event<DateLabel> action : f.alphabet){
			if(dateEventToSootMethods.get(action).size() == 0){
				toRemove.add(action);
			}
		}
		
		return toRemove;
	}
//	
//	public List<Shadow> orderShadowList(List<Shadow> shadow){
//		
//	}
	
	public void matchMethodsWithEvents(Set<Event<DateLabel>> allEvents, List<MethodOrMethodContext> reachableMethods){
		
		dateEventToSootMethods = new HashMap<MethodCall, List<MethodOrMethodContext>>();	
		sootMethodToDateEvents = new HashMap<MethodOrMethodContext, List<MethodCall>>();

		for(Event<DateLabel> actionMethodCall : allEvents){
			if(actionMethodCall.label.event instanceof MethodCall){
				MethodCall event = (MethodCall) actionMethodCall.label.event;

				List<MethodOrMethodContext> methods = new ArrayList<MethodOrMethodContext>();
				
				for(int i = 0; i < reachableMethods.size(); i++){
					if(reachableMethods.get(i).method().getName().contains("addInnerNode")){
						System.out.print("");
					}
					MethodOrMethodContext method = reachableMethods.get(i);

					String name = method.method().getName();
//					if(name.contains("addLeaf")){
//						System.out.println("Here");
//					}
//					if(method.toString().contains("PurseList")
//							&& method.toString().contains("init")){
//						System.out.println("");
//					}
					
					if(Matching.matchesMethod(method.method(), event)){
						methods.add(method);
						
						if(sootMethodToDateEvents.containsKey(method)
							&& !sootMethodToDateEvents.get(method).contains(event)){
							sootMethodToDateEvents.get(method).add(event);
						}
						else if(!sootMethodToDateEvents.containsKey(method)){
							List<MethodCall> corr = new ArrayList<MethodCall>();
							corr.add(event);
							sootMethodToDateEvents.put(method, corr);
						}
					}

					if(method.method().hasActiveBody()) {

						for (Unit unit : method.method().retrieveActiveBody().getUnits()) {
							SootMethod invokedMethod = null;

							if(InvokeStmt.class.isAssignableFrom(unit.getClass())){
								invokedMethod = ((InvokeStmt) unit).getInvokeExpr().getMethod();
							} else if(AssignStmt.class.isAssignableFrom(unit.getClass())
										&& InvokeExpr.class.isAssignableFrom(((AssignStmt) unit).getRightOp().getClass())){
								invokedMethod = ((InvokeExpr) ((AssignStmt) unit).getRightOp()).getMethod();
							}

							if(invokedMethod != null) {

								if (Matching.matchesMethod(invokedMethod, event)) {
									methods.add(invokedMethod);

									if (sootMethodToDateEvents.containsKey(invokedMethod)
											&& !sootMethodToDateEvents.get(invokedMethod).contains(event)) {
										sootMethodToDateEvents.get(invokedMethod).add(event);
									} else if (!sootMethodToDateEvents.containsKey(invokedMethod)) {
										List<MethodCall> corr = new ArrayList<MethodCall>();
										corr.add(event);
										sootMethodToDateEvents.put(invokedMethod, corr);
									}
								}
							}
						}
					}
				}
				
				dateEventToSootMethods.put(event, methods);
			}
		}
		
		Map<MethodOrMethodContext, List<MethodCall>> sootMethodToDateEventsNew = new HashMap<MethodOrMethodContext, List<MethodCall>>();
		
		for(Entry<MethodOrMethodContext,List<MethodCall>> entry : sootMethodToDateEvents.entrySet()){
			sootMethodToDateEventsNew.put(entry.getKey(), this.sortAccordingToType(entry.getValue()));
		}
		
		sootMethodToDateEvents = sootMethodToDateEventsNew;
	}

	public static List<MethodOrMethodContext> reachableMethods(Scene scene){

		ReachableMethods reachableMethodsObject = Scene.v().getReachableMethods();
		
		QueueReader<MethodOrMethodContext> reachableMethodsQueue = reachableMethodsObject.listener();

		List<MethodOrMethodContext> reachableMethods = new ArrayList<MethodOrMethodContext>();
		
		MethodOrMethodContext method;
		while(reachableMethodsQueue.hasNext()){
			
			method = reachableMethodsQueue.next();

			reachableMethods.add(method);
			
		}
		
		return reachableMethods;
	}

	public Set<InvokeExpr> allMethodCalls(Scene scene){		
		
		//Get the method call graph of the scene
		CallGraph cg = scene.getCallGraph();
		
		//initialize fields
		Set<InvokeExpr> allMethodCalls = new HashSet<InvokeExpr>();
		
	//	methodObject = new HashMap<InvokeExpr, Value>();
	//	methodArgs = new HashMap<InvokeExpr, List<Value>>();
		
//		shadowBindings = new HashMap<Shadow, List<Map<String, Value>>>();
		
		invokeExprInStmt = new HashMap<InvokeExpr, Stmt>();
		methodInvokedWhere = new HashMap<MethodOrMethodContext, List<InvokeExpr>>();		
		invokedMethod = new HashMap<InvokeExpr, MethodOrMethodContext>();		
		
		unitsToInvokeExpr = new HashMap<Unit, InvokeExpr>();
		
		//Get an iterator over all edges
		Iterator<Edge> edges = cg.iterator();
//		System.out.println(cg);
		Edge edge;
		while(edges.hasNext()){
				edge = edges.next();
				
				//get edge source statement (the 
				Stmt sourceStatement = edge.srcStmt(); 
			//	int sourceLineNumber = sourceStatement.getJavaSourceStartLineNumber();

			//FOR DEBUGGING
			try {
				if (sourceStatement.toString().contains("black")) {
					System.out.print("");
				}
			} catch(Exception e){
					System.out.print("");
				}
				if(sourceStatement != null && sourceStatement.containsInvokeExpr()){

					InvokeExpr expr = sourceStatement.getInvokeExpr();
					//if(expr instanceof InstanceInvokeExpr){
					if(expr instanceof InvokeExpr){
						allMethodCalls.add(expr);

						unitsToInvokeExpr.put(edge.srcUnit(), expr);
						
						invokeExprInMethod.put(expr, edge.getSrc());

						invokeExprInStmt.put(expr, sourceStatement);
						
						invokedMethod.put(expr, expr.getMethodRef().resolve());
						
						if(methodInvokedWhere.containsKey(expr.getMethodRef().resolve())){
							methodInvokedWhere.get(expr.getMethodRef().resolve()).add(expr);
						}
						else{
							
							List<InvokeExpr> invoked = new ArrayList<InvokeExpr>();
							invoked.add(expr);
							
							methodInvokedWhere.put(expr.getMethodRef().resolve(), invoked);
						}

						//for each matching DateLabel
						//better to turn all these to match method calls rather than dateEvent<datelabel>
						List<MethodCall> matchingMethodCalls = sootMethodToDateEvents.get(invokedMethod.get(expr));

						if(matchingMethodCalls != null){

							//TODO make sure all actual invoke exprs are being associated with a for each var

							List<Value> args = new ArrayList<>();
							Value classObject = null;

							if (AbstractVirtualInvokeExpr.class.isAssignableFrom(expr.getClass())) {

								AbstractVirtualInvokeExpr invokeExpr = (AbstractVirtualInvokeExpr) expr;
								classObject = ((InstanceInvokeExpr)expr).getBase();

								args.addAll(invokeExpr.getArgs());

							} else if (AbstractInstanceInvokeExpr.class.isAssignableFrom(expr.getClass())) {

								AbstractInstanceInvokeExpr invokeExpr = (AbstractInstanceInvokeExpr) expr;
								classObject = ((InstanceInvokeExpr)expr).getBase();

								args.addAll(invokeExpr.getArgs());
							} else {//(AbstractStaticInvokeExpr.class.isAssignableFrom(invoke.getClass())){
								args.addAll(expr.getArgs());
							}



							if(args == null){
								args = new ArrayList<Value>();
							}
							
							List<JavaEvent> shadows = new ArrayList<JavaEvent>();
							
							this.unitShadows.put(edge.srcUnit(), shadows);
							
							for(int i = 0; i < matchingMethodCalls.size() ; i++){
						//		List<Map<String, Value>> listOfForEachVarsValue = new ArrayList<Map<String, Value>>();
								
								Map<String, Value> forEachVarValue = new HashMap<String, Value>();

								JavaEvent shadow = new JavaEvent(expr, sourceStatement, matchingMethodCalls.get(i), forEachVarValue);
							//	shadowBindings.put(shadow, listOfForEachVarsValue);
								shadows.add(shadow);
								this.allShadows.add(shadow);
								MethodCall current = matchingMethodCalls.get(i);								
								
							//	listOfForEachVarsValue.add(forEachVarValue);

								if(current.toString().contains("addLeaf")){
									System.out.print("");
									//TODO check why for MufinB1 benchmark addleaf method not being matched with return variable
								}



								for(String forEachVar : current.forEachVariables){
									if(classObject != null
										&& current.whereMap.get(forEachVar).equals(current.objectIdentifier)){

										forEachVarValue.put(forEachVar, classObject);
									}
									else if(current.whereMap.get(forEachVar).equals(current.returnIdentifier)
											&& AssignStmt.class.isAssignableFrom(sourceStatement.getClass())){
											//&& current.isConstructor){

										forEachVarValue.put(forEachVar, ((AssignStmt) sourceStatement).getLeftOp());
									}
									else{

										for(int j = 0; j < current.argIdentifiers.size(); j++){
											if(current.whereMap.get(forEachVar).equals(current.argIdentifiers.get(j))){
												forEachVarValue.put(forEachVar, args.get(j));
											}
										}
									}
								}
								
	//							//sanity check
	//							if(forEachVarValue.keySet().size() != current.forEachVariables.size()){
	//								
	//							}
						}
					}
//						methodObject.put(expr, classObject);
//						methodArgs.put(expr, args);
						
						
					}
				}
			//}
		}
		
		
		return allMethodCalls;
	}
	
	public boolean before(MethodCall event, MethodCall otherEvent){
		if(event.name == otherEvent.name
				&& event.argTypes.equals(otherEvent.argTypes)){
			List<ActionType> actionTypes = Arrays.asList(MethodCall.ActionType.values());
			if(actionTypes.indexOf(event.type) < actionTypes.indexOf(otherEvent.type)){
				return true;
			}
		}
		
		return false;
	}
	
	public boolean after(MethodCall event, MethodCall otherEvent){
		if(event.name == otherEvent.name
				&& event.argTypes.equals(otherEvent.argTypes)){
			List<ActionType> actionTypes = Arrays.asList(MethodCall.ActionType.values());
			if(actionTypes.indexOf(event.type) > actionTypes.indexOf(otherEvent.type)){
				return true;
			}
		}
		
		return false;
	}
	
	public List<MethodCall> sortAccordingToType(List<MethodCall> unsortedList){
		Set<MethodCall> set = new HashSet<MethodCall>(unsortedList);
		List<MethodCall> noDuplicateList = new ArrayList<MethodCall>(set);
		List<MethodCall> sortedList = new ArrayList<MethodCall>();
		
		
		if(noDuplicateList.size() <= 1) sortedList = noDuplicateList;
		else{
			sortedList.add(noDuplicateList.get(0));
			for(MethodCall call : noDuplicateList){
				if(!sortedList.contains(call)){
					for(MethodCall sortedCall : sortedList){
						int sortedCallIndex = sortedList.indexOf(sortedCall);
						if(before(call, sortedCall)){
							sortedList.add(sortedCallIndex, call);
						}
						else if(sortedCallIndex + 1 <= sortedList.size()){
							sortedList.add(sortedCallIndex, call);
						}
						else sortedList.add(call);
					}
				}
			}
		}
		
		return sortedList;
	}
}
