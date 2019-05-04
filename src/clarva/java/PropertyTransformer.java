package clarva.java;

import java.io.*;
import java.util.*;

import clarva.analysis.ControlFlowResidualAnalysis;

import clarva.analysis.cfg.CFGEvent;
import clarva.matching.Aliasing;
import com.google.common.io.Files;
import compiler.Compiler;
import compiler.Global;
import compiler.ParseException;
import compiler.ParsingString;
import fsm.Event;
import fsm.date.events.DateEvent;
import fsm.helper.Pair;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.AbstractInstanceInvokeExpr;
import soot.jimple.internal.AbstractInvokeExpr;
import soot.jimple.internal.AbstractStaticInvokeExpr;
import soot.jimple.internal.AbstractVirtualInvokeExpr;
import soot.jimple.toolkits.callgraph.CallGraph;
import fsm.date.DateFSM;
import fsm.date.ForEach;
import fsm.date.SubsetDate;
import fsm.main.DateToFSM;
import soot.options.Options;
import soot.util.Chain;
import soot.util.JasminOutputStream;

public class PropertyTransformer extends SceneTransformer{

	//public static String larvaProperty = "../DateToFSM/lrv/bank.lrv";
	
	public static List<DateFSM> properties = new ArrayList<DateFSM>();
	
	public static List<fsm.date.Global> dateFSMHierarchy = new ArrayList<fsm.date.Global>();

	public static Map<fsm.date.Global, String> globalToFileName = new HashMap<fsm.date.Global, String>();
	
	public static CallGraph cg;
	
	public static List<SootMethod> reachableMethods;

	public static boolean allSatisfied = true;

//	public static InfoflowCFG icfg;
//	public static AliasFinder af;

    public static Map<DateFSM, Set<Event<JavaEvent>>> eventsToMonitorFor = new HashMap<>();

	@Override
	protected void internalTransform(String phaseName, Map<String, String> options) {

		List<fsm.date.Global> residualGlobals = new ArrayList<fsm.date.Global>();
		
		//System.out.println(dateFSMHierarchy);

		for(fsm.date.Global global : dateFSMHierarchy){
			Pair<fsm.date.Global, Boolean> globalBooleanPair = computeGlobalResidual(global);
			fsm.date.Global globalResidual = globalBooleanPair.first;
			residualGlobals.add(globalResidual);

			if(!globalBooleanPair.second) {
				allSatisfied = false;
				try {
					PrintWriter writer = new PrintWriter(clarva.Main.rootOutputDir + "/" + dateFSMHierarchy.indexOf(global) + "residual.lrv", "UTF-8");
					writer.println(residualGlobals.get(residualGlobals.size() - 1).toStringWithEventInterface());
					writer.close();
				} catch (IOException e) {
					// do something
				}
			}
		//	System.out.println(global);
		}

		for(Set<Event<JavaEvent>> events : eventsToMonitorFor.values()) {
			for (Event<JavaEvent> event : events) {
				event.label.callingMethod.retrieveActiveBody();

			}
		}

		Set<Pair<SootClass, SootMethod>> toRemove = new HashSet<>();

		Chain<SootClass> classes = Scene.v().getApplicationClasses();
		for(SootClass sootClass : classes) {
			for (SootMethod method : sootClass.getMethods()) {
				if (!method.hasActiveBody()) {
				    try {
                        method.retrieveActiveBody();
                    }catch(Exception e){
				        toRemove.add(new Pair<>(sootClass, method));
				    }
				}
			}
		}

		for(Pair<SootClass, SootMethod> pair : toRemove){
			pair.first.removeMethod(pair.second);
			if(pair.first.getMethods().size() == 0){
				Scene.v().getApplicationClasses().remove(pair.first);
			}
		}
	}
	
	public Pair<fsm.date.Global,Boolean> computeGlobalResidual(fsm.date.Global global){
		boolean satisfied = true;

		for(DateFSM property : new ArrayList<DateFSM>(global.properties)){
			global.properties.remove(property);
			DateFSM residual = computeResidual(property);

			if(!residual.neverFails) {
				satisfied = false;
				global.properties.add(residual);

				try {
					ProgramModifier.instrument();
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else{
				System.out.println(residual.propertyName + " is always satisfied!");
			}
		}
		
		for(ForEach foreach : new HashSet<ForEach>(global.forEaches)){
			global.forEaches.remove(foreach);
			Pair<fsm.date.Global, Boolean> globalBooleanPair = computeGlobalResidual(foreach);
			ForEach forEach = (ForEach) globalBooleanPair.first;
			satisfied &= globalBooleanPair.second;
			if(!globalBooleanPair.second) {
				global.forEaches.add(forEach);
			}
		}
		
		return new Pair<>(global, satisfied);
	}

	class JavaFlowInsensitiveAliasing implements Aliasing<JavaEvent> {

		Matching an;

		public JavaFlowInsensitiveAliasing(Matching an){
			this.an = an;
		}

		@Override
		public boolean mayAlias(JavaEvent cfgEvent1, JavaEvent cfgEvent2) {
			return an.flowInsensitiveCompatible(cfgEvent1, cfgEvent2);
		}

		//this method should not be used
		@Override
		public boolean mustAlias(JavaEvent cfgEvent1, JavaEvent cfgEvent2) {
			return cfgEvent1.mustAlias(cfgEvent2);
		}
	}

	class JavaFlowSensitiveAliasing implements Aliasing<JavaEvent> {
		public JavaFlowSensitiveAliasing(){
		}

		@Override
		public boolean mayAlias(JavaEvent cfgEvent1, JavaEvent cfgEvent2) {
			return cfgEvent1.mayAlias(cfgEvent2);
		}

		@Override
		public boolean mustAlias(JavaEvent cfgEvent1, JavaEvent cfgEvent2) {
			return cfgEvent1.mustAlias(cfgEvent2);
		}
	}

	public DateFSM computeResidual(DateFSM property){
		MethodsAnalysis ma = new MethodsAnalysis(Scene.v(), property.alphabet);

		List<DateEvent> allUsedEvents = AnalysisAdaptor.allEvents(ma);
		SubsetDate residual = ControlFlowResidualAnalysis.QuickCheckAnalysis(property, allUsedEvents);
		

		if(residual.neverFails){
			return residual;
//			actionOnEnd(residual); 
//			return;
		}		
		else{
//			System.out.println("After 1st:");
//			System.out.println(residual);

			Matching am = new Matching(ma);

			//need to reduce shadows up to must-alias here.. using less precise flow-insensitive points-to analysis
			Map<JavaEvent,SubsetDate> residuals = ControlFlowResidualAnalysis.OrphansAnalysis(residual,
					ma.allShadows,
					new JavaFlowInsensitiveAliasing(am));
//			Map<Shadow,SubsetDate> residuals = ResidualAnalysis.OrphansAnalysis(residual, ma, am);


			if(residuals.values().size() == 0){

				return new DateFSM(property.propertyName);
//				actionOnEnd(new DateFSM()); 
//				return;
			}
			else{
				SubsetDate unionOfResiduals2;

				unionOfResiduals2 = ControlFlowResidualAnalysis.residualsUnion(residuals);

//				System.out.println("After 2nd:");
//				System.out.println(unionOfResiduals2);

				JavaCFGAnalysis cfga = new JavaCFGAnalysis(ma);
				
//				residuals = ControlFlowResidualAnalysis.ControlFlowAnalysis(residuals,
				Map<JavaEvent, Pair<SubsetDate, Set<Event<JavaEvent>>>> residualsAndUsefulEvents = ControlFlowResidualAnalysis.IntraProceduralControlFlowAnalysis(residuals,
                        ma.allShadows,
                        cfga,
                        new JavaFlowSensitiveAliasing());

				Pair<SubsetDate, Set<Event<JavaEvent>>> union = ControlFlowResidualAnalysis.residualsAndEventsUnionWithoutReductions(residualsAndUsefulEvents);

                eventsToMonitorFor.put(property, union.second);

                if(!union.first.neverFails) {
//					System.out.println("After 3rd:");
//					System.out.println(union.first);
				}

				return union.first;
//				actionOnEnd(unionOfResiduals);
//				
//				System.out.println();
//				System.out.println("Potential points of failure:");
//				System.out.println();
//				for(Pair<String,String> ppf : residualsAndPPF.second){
//					System.out.println(ppf);
//				}

			}
		}

	}
//	
//	public void actionOnEnd(DateFSM fsm){
//		System.out.println("\n\n\nResidual Property:\n");
//		System.out.println(fsm);
//	}
		
	public static void generateFiniteStateMachines(List<String> larvaProperty) {
		
		BufferedReader br;
		try {
			List<Global> globals = new ArrayList<Global>();

			Map<Global, fsm.date.Global> globalToGlobal = new HashMap<Global, fsm.date.Global>();
			for(String prop : larvaProperty){
				br = new BufferedReader(new InputStreamReader(new FileInputStream(prop)));
				StringBuilder text = new StringBuilder();
				String temp;
				while ((temp = br.readLine()) != null)   {
					if (temp.indexOf("%%") != -1)//remove comments
						temp=temp.substring(0,temp.indexOf("%%"));
					text.append(temp.trim() + "\r\n");
				}
				Compiler p = new Compiler(new ParsingString(text));
				try {
					p.parse();
									
					Global global = Compiler.global;
					globals.add(global);
					
				//	globalToFileName.put(global, value)
					//properties = DateToFSM.parseForDates(globals, new ArrayList<compiler.Variable>());
				
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
			
			dateFSMHierarchy = DateToFSM.parse(globals);
			
			properties = new ArrayList<DateFSM>();
			
			for(fsm.date.Global glob : dateFSMHierarchy){
				properties.addAll(glob.properties());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
//	public static HashMap<MethodCall, Set<SootMethod>> adviceEvents(Set<MethodCall> allActions){
//		
//		HashMap<MethodCall, Set<SootMethod>> adviceEvents = new HashMap<MethodCall, Set<SootMethod>>();
//		
//		for(MethodCall action : allActions){
//			
//			HashSet<SootMethod> methods = new HashSet<SootMethod>();
//			
//			for(SootMethod method : reachableMethods){
//				
//				if(matches(method, action)){
//					methods.add(method);
//				}
//			}
//			
//			adviceEvents.put(action, methods);
//		}
//		
//		return adviceEvents;
//	}

//	public static ArrayList<DateFSM> propertyToFSM(Global global){
//		
//		compiler.Events events = global.events;
//				
//		ArrayList<DateFSM> properties = new ArrayList<DateFSM>();
//		
//		LinkedHashMap<String, Property> propertiesMap = global.logics;
//		
//		Set<Entry<String, Property>> entrySet = propertiesMap.entrySet();
//		
//		Iterator<Entry<String, Property>> iterator = entrySet.iterator();
//		
//		while(iterator.hasNext())
//		{
//			Entry<String, Property> entry = iterator.next();
//			String key = entry.getKey();
//			Property property = entry.getValue();
//			
//			fsm.DateFSM propertyFSM = new fsm.DateFSM(property, new ArrayList<Variable>(events.variables.values()));
//			
//			Iterator<fsm.Transition<String,MethodCall>> iter = propertyFSM.transitions.iterator();
//			
//			while(iter.hasNext())
//			{
//				fsm.Transition<String,MethodCall> next = iter.next();
//				
//				System.out.println(next.toString());
//			}
//			
//			propertyFSM.lsm.generateDependencies(propertyFSM.startingState, new ArrayList<fsm.State<String,MethodCall>>(), new ArrayList<Action<MethodCall>>());
//			
//			properties.add(propertyFSM);
//			//System.out.println("Hello");
//			//Convert each Property to FSM
//			
//			//Can monitors activate events? or events only arise from vanilla system?			
//		}
//		
//		ArrayList<compiler.Foreach> forEaches = global.foreaches;
//		
//		for(compiler.Foreach forEach : forEaches){
//			properties.addAll(propertyToFSM(forEach));
//		}
//		
//		return properties;
//	}
	
//	public FSM callGraphToFSM(CallGraph cg){
//		FSM<MethodOrMethodContext> fsm = new FSM<MethodOrMethodContext>();
//		
//		Iterator<Edge> edgesIterator = cg.iterator();
//		
//		Edge edge;
//		
//		while(edgesIterator.hasNext()){
//			edge = edgesIterator.next();
//			
//			MethodOrMethodContext src = edge.getSrc();
//			MethodOrMethodContext tgt = edge.getTgt();
//
//			if(){
//				State srcState = new State(src);
//				State tgtState = new State(tgt);
//				fsm.addState(srcState);
//				fsm.addState(tgtState);
//			}
//			
//		}
//		
//		return fsm;
//	}
	
	//public Action matchesWhichAction(MethodOrMethodContext method, Collection<Action> actions){
		
	//}
//
//	public static Set<MethodCall> QuickCheck(DateFSM f){
//		
//		HashMap<MethodCall, Set<SootMethod>> actionEvents = adviceEvents(f.actions);
//
//		Set<MethodCall> toRemove = new HashSet<MethodCall>();
//		
//		for(MethodCall action : f.actions){
//			if(actionEvents.get(action).size() == 0){
//				toRemove.add(action);
//			}
//		}
//		
//		return toRemove;
//	}
//	
//	public static Set<Dependency> OrphansAnalysis(DateFSM f){
//		
//		//for each dependency, check that the strong methods are compatible with each other, if not then the dependency can be removed.
//		//for each dependency, check that the weak methods are compatible with all the other, remove the uncompatible weak methods from dependency.
//		//get the actions in all the remaining dependencies, toRemove.add(f.actions - dependencyActionsLeft)
//		
//		//generate trace from dependency.
//		//make new automaton only from the traces left after the previous analysis 
//		
//		HashMap<MethodCall, Set<SootMethod>> actionEvents = adviceEvents(f.actions);
//		
//		Set<Transition> actionsToRemove = new HashSet<Transition>();
//
//		Set<Dependency> toRemove = new HashSet<Dependency>();
//		
//		for(MethodCall action : f.actions){
//			
//			for(Dependency dependency : f.lsm.dependencies){
//				
//					if(dependency.contains(action)
//							&& !toRemove.contains(dependency))
//					{
//						outerloop:
//						for(Action strongAction : dependency.strong){
//							
//							for(SootMethod method : actionEvents.get(action)){
//
//								for(SootMethod strongMethod : actionEvents.get(strongAction)){
//									if(!compatible(method, strongMethod)){
//										toRemove.add(dependency);
//										break outerloop;
//									}
//								}
//							}
//						}
//					}
//				
//			}
//		}
//		
//		return toRemove;
//	}
//	
}
