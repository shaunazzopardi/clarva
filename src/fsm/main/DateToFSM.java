package fsm.main;

import compiler.Compiler;
import compiler.*;
import fsm.date.DateFSM;
import fsm.date.ForEach;
import fsm.date.Global;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class DateToFSM {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
				
		String inputDir = "./lrv/bank.lrv";

		BufferedReader br;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(inputDir)));
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
				
			//	Methods methods = p.methods;
				
				Set<compiler.Global> larvaGlobals = new HashSet<compiler.Global>();
				larvaGlobals.add(p.global);
				
				List<DateFSM> allProperties = parseForDates(larvaGlobals, new ArrayList<Variable>());
				
				List<Global> globals = parse(larvaGlobals);
				
				if(globals.size() > 0){
					if(compiler.Global.methods.toJava() != null
							&& compiler.Global.methods.toJava() != ""){
						globals.get(0).addMethod(compiler.Global.methods.toJava());
					}
				}
//				for(DateFSM propertyFSM : properties){
//					propertyFSM.lsm.generateDependencies(propertyFSM.startingState, 
//														new ArrayList<fsm.State<String,DateLabel>>(), 
//														new ArrayList<Event<DateLabel>>());
//				}
				
		/*		LinkedHashMap<String, Property> propertiesMap = global.logics;
												
				Set<Entry<String, Property>> entrySet = propertiesMap.entrySet();
				
				Iterator<Entry<String, Property>> iterator = entrySet.iterator();
				
				while(iterator.hasNext())
				{
					Entry<String, Property> entry = iterator.next();
					String key = entry.getKey();
					Property property = entry.getValue();
					
					fsm.DateFSM propertyFSM = new fsm.DateFSM(property);
					
					Iterator<fsm.Transition> iter = propertyFSM.transitions.iterator();
					
					while(iter.hasNext())
					{
						fsm.Transition next = iter.next();
						
						System.out.println(next.toString());
					}
					
					propertyFSM.lsm.generateDependencies(propertyFSM.startingState, new ArrayList<fsm.State<String>>(), new ArrayList<Action>());
					
//					//System.out.println("Hello");
//					//Convert each Property to FSM
//					
//					//Can monitors activate events? or events only arise from vanilla system?
				}*/
				
				
				
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
		
	public static List<DateFSM> parseForDates(Collection<compiler.Global> globals, List<Variable> variables){
		List<DateFSM> dates = new ArrayList<DateFSM>();
		for(compiler.Global g : globals){
			variables.addAll(g.contextVariables.values());
			for(compiler.Property p : g.logics.values()){
				dates.add(new DateFSM(p, variables));
			}
			
			dates.addAll(parseForDates((Collection)g.foreaches, variables));
		}
		
		return dates;
	}
	
	public static String tokenListToString(ArrayList<Token> tokenList){
		String varDec = "";
		
		for(int i = 0; i < tokenList.size(); i++){
			varDec += tokenList.get(i).text + " ";
		}
		
		return varDec;
	}
	
	public static ForEach parse(compiler.Foreach foreach, List<Variable> forEachVariables){
		ForEach forEach;
		
		List<String> vardecs = new ArrayList<String>();
		for(ArrayList<Token> varDec : foreach.local.values()){
			vardecs.add(tokenListToString(varDec));
		}
		
		String variableName = foreach.context.get(0).toString();
		String variableType = foreach.contextVariables.get(variableName).getVariableType();
		
		forEachVariables.add(foreach.contextVariables.get(variableName));
		
		Set<ForEach> subForEaches = new HashSet<ForEach>();
		
		for(compiler.Foreach subforeach : foreach.foreaches){
			subForEaches.add(parse(subforeach, forEachVariables));
		}
		
		forEach = new ForEach(variableType, variableName, vardecs, subForEaches, foreach.events);
		
		for(compiler.Property p : foreach.logics.values()){
			forEach.addDate(new DateFSM(p, forEachVariables));
		}
		
		return forEach;
	}
	
	public static List<Global> parse(Collection<compiler.Global> larvaGlobals){
		List<Global> globals = new ArrayList<Global>();
		
		for(compiler.Global g : larvaGlobals){
			List<String> vardecs = new ArrayList<String>();

			for(ArrayList<Token> varDec : g.local.values()){
				vardecs.add(tokenListToString(varDec));
			}
						
			Global global = new Global(vardecs, g.imports.toString(), g.events);
			
			for(compiler.Property p : g.logics.values()){
				global.addDate(new DateFSM(p, new ArrayList<Variable>(g.contextVariables.values())));
			}

//			for(Foreach fr : ((Collection<Foreach>)g.foreaches)){
//				globa
//			}
							
			for(compiler.Foreach foreach : g.foreaches){
				ForEach forEach = parse(foreach, new ArrayList<Variable>());
				global.addForEach(forEach.variableIdentifier, forEach);
			}
			
			globals.add(global);
		}
		
		return globals;
	}

//	public ArrayList<DateFSM> propertyToFSM(Global global){
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
//			fsm.DateFSM propertyFSM = new fsm.DateFSM(property);
//			
//			Iterator<fsm.Transition> iter = propertyFSM.transitions.iterator();
//			
//			while(iter.hasNext())
//			{
//				fsm.Transition next = iter.next();
//				
//				System.out.println(next.toString());
//			}
//			
//			propertyFSM.lsm.generateDependencies(propertyFSM.startingState, new ArrayList<fsm.State<String>>(), new ArrayList<Action>());
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
	
//	public static void getAllEvents(){
//		
//		String inputDir = "D:/Google Drive/Shaun PhD/Clarva/Larva/LARVA complete package/Larva example scripts/bank.lrv";
//
//		BufferedReader br;
//		try {
//			br = new BufferedReader(new InputStreamReader(new FileInputStream(inputDir)));
//			StringBuilder text = new StringBuilder();
//			String temp;
//			while ((temp = br.readLine()) != null)   {
//				if (temp.indexOf("%%") != -1)//remove comments
//					temp=temp.substring(0,temp.indexOf("%%"));
//				text.append(temp.trim() + "\r\n");
//			}
//			Compiler p = new Compiler(new ParsingString(text));
//			try {
//				p.parse();
//				
//				Methods methods = p.methods;
//				
//				Global global = p.global;
//				
//				Events events = global.events;
//				
//				HashMap<String, Trigger> eventTriggers = events.events;
//				
//				Collection<Trigger> triggers = events.events.values();
//				
//				Set<Entry<String, Trigger>> entrySet = eventTriggers.entrySet();
//				
//				Iterator<Entry<String, Trigger>> iterator = entrySet.iterator();
//				
//				while(iterator.hasNext())
//				{
//					Entry<String, Trigger> entry = iterator.next();
//					String key = entry.getKey();
//					Trigger trigger = entry.getValue();
//					
//					//Class of method is Event.target
//					//uponEntry/UponException/etc is Event.type
//					
//					if(trigger.getClass().equals(EventCollection.class))
//					{
//						ArrayList<Trigger> eventCollection = ((EventCollection) trigger).events;
//					}
//					
//					String toJava = methods.toJava();
//					
//					System.out.println(toJava);
//					
//				}
//				
//			} catch (ParseException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
}
