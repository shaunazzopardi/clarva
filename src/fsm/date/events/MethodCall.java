package fsm.date.events;

import compiler.Event;
import compiler.EventCollection;
import compiler.Token;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.*;

public class MethodCall extends DateEvent{

	//public String action;
	//public String name;
	public boolean isConstructor;
	public boolean staticMethod;
	public String objectType;
	public String objectIdentifier;

	public ArrayList<String> parameterTypes;
	public ArrayList<String> parameterIdentifiers;

	public ArrayList<String> argTypes;
	public ArrayList<String> argIdentifiers;

	public String returnType;
	public String returnIdentifier;
	public ActionType type;

	public List<String> forEachVariables;
	public List<String> forEachVariableTypes;

	//for each variables constructed as fields
	public List<String> forEachVariablesAsFields;
	public List<String> forEachVariableTypesAsFields;

	public String scriptFileActionName;

	public String within;
//	public ArrayList<String> variableTypes;
//	public ArrayList<String> variableNames;

	//Larva implementation stuff
	public ArrayList<String> whereClause;
	//foreachVariables -> {MethodArgs} \cup {MethodArgsFields} \cup {MethodArgsMethods}
	public Map<String,String> whereMap;

	public enum ActionType{before, uponEntry, uponThrowing, uponHandling, uponReturning}

	public MethodCall(Event event,
                      EventCollection collectiveEvent,
                      List<String> forEachVariables,
                      List<String> forEachVariableTypes,
                      List<Token> whereClause)
	{
	//	super("");
		super("");
		if(event.within != null){
			within = event.within.toString();
		}
		else within = "";
//		if(event.type != Event.EventType.channel
//				&& event.type != Event.EventType.clock
//				&& event.type != Event.EventType.clockCycle)
//		{
			scriptFileActionName = event.getName().toString();
			this.name = event.methodName.text;

			if(Character.isUpperCase(event.target.text.charAt(0))){
				this.objectType = event.target.text;
				this.objectIdentifier = "";
				this.staticMethod = true;
				if(name.equals("new")) isConstructor = true;
			}
			else{
				this.objectIdentifier = event.target.text;
				this.objectType = event.variables.get(event.target.text).getVariableType();
				this.staticMethod = false;
				this.isConstructor = false;
			}

			this.argIdentifiers = new ArrayList<String>();
			this.argTypes = new ArrayList<String>();

			for(int i = 0; i < event.args.size(); i++){
				String var = event.args.get(i).text;
				this.argIdentifiers.add(var);
				this.argTypes.add(event.variables.get(var).getVariableType());
			}

			if(event.returned != null)
			{
				returnIdentifier = event.returned.text;
				returnType = event.variables.get(returnIdentifier).getVariableType();
			}
			else
			{
				returnIdentifier = "";
				returnType = "*";
			}

			ArrayList<Token> parameterList = collectiveEvent.parameterList;

			parameterTypes = new ArrayList<String>();
			parameterIdentifiers = new ArrayList<String>();

			for(int i = 0; i < parameterList.size() - 1; i+=3)
			{
				String type = "";
				type += parameterList.get(i).toString();
				i++;
				while(parameterList.get(i).toString().equals(".")){
					type += parameterList.get(i).toString();
					i++;
					type += parameterList.get(i).toString();
					i++;
				}

				String identifier = parameterList.get(i).toString();
				parameterTypes.add(type);
				parameterIdentifiers.add(identifier);
			}

			if(event.type.toString().equals(fsm.date.events.MethodCall.ActionType.uponThrowing.toString()))
			{
				type = fsm.date.events.MethodCall.ActionType.uponThrowing;
			}
			else if(event.type.toString().equals(fsm.date.events.MethodCall.ActionType.uponReturning.toString()))
			{
				type = fsm.date.events.MethodCall.ActionType.uponReturning;
			}
			else if(event.type.toString().equals(fsm.date.events.MethodCall.ActionType.uponHandling.toString()))
			{
				type = fsm.date.events.MethodCall.ActionType.uponHandling;
			}
			else if(event.type.toString().equals(fsm.date.events.MethodCall.ActionType.uponThrowing.toString()))
			{
				type = fsm.date.events.MethodCall.ActionType.uponThrowing;
			}
			else if(event.type.toString().equals(fsm.date.events.MethodCall.ActionType.before.toString()))
			{
				type = fsm.date.events.MethodCall.ActionType.before;
			}else if(event.type.toString().equals(fsm.date.events.MethodCall.ActionType.uponEntry.toString()))
			{
				type = fsm.date.events.MethodCall.ActionType.uponEntry;
			}

			List<Token> whereClauses = new ArrayList<Token>();
			whereClauses.addAll(whereClause);
			whereClauses.addAll(collectiveEvent.whereClause);
			Iterator<Token> iterator = whereClauses.iterator();

			this.whereClause = new ArrayList<String>();
			whereMap = new HashMap<String,String>();

			Token next;
			String current = "";
			while(iterator.hasNext()){
				next = iterator.next();

				if(!next.text.contains(";")){

					current += next;
				}
				else{
					this.whereClause.add(new String(current));

					if(current.contains("=")){
						String[] equalVars = current.split("=");

						whereMap.put(equalVars[0], equalVars[1]);
					}
					current = "";
				}
			}

			this.forEachVariables = new ArrayList<String>();
			this.forEachVariableTypes = new ArrayList<String>();
			this.forEachVariablesAsFields = new ArrayList<String>();
			this.forEachVariableTypesAsFields = new ArrayList<String>();

			for(int i = 0; i < forEachVariables.size(); i++){
				String var = forEachVariables.get(i);
				String varType = forEachVariableTypes.get(i);

				if(this.whereMap.get(var).contains(".")){
					this.forEachVariablesAsFields.add(var);
					this.forEachVariableTypesAsFields.add(varType);
				}
				else{
					this.forEachVariables.add(var);
					this.forEachVariableTypes.add(varType);
				}
			}
//		}
//		else
//		{
//			notAnAction = true;
//		}

//		processUnobtainableForEachVars();
	}

	public MethodCall(Event event,
                      List<String> forEachVariables,
                      List<String> forEachVariableTypes,
                      List<Token> whereClause)
	{
	//	super("");
		super("");
		if(event.within != null){
			within = event.within.toString();
		}
		else within = "";

//		if(event.type != Event.EventType.channel
//				&& event.type != Event.EventType.clock
//				&& event.type != Event.EventType.clockCycle)
//		{
			scriptFileActionName = event.getName().toString();
			this.name = event.methodName.text;

			if(Character.isUpperCase(event.target.text.charAt(0))){
				this.objectType = event.target.text;
				this.objectIdentifier = "";
				this.staticMethod = true;
				if(name.equals("new")) isConstructor = true;
			}
			else{
				this.objectIdentifier = event.target.text;
				this.objectType = event.variables.get(event.target.text).getVariableType();
				this.staticMethod = false;
				this.isConstructor = false;
			}

			this.argIdentifiers = new ArrayList<String>();
			this.argTypes = new ArrayList<String>();

			for(int i = 0; i < event.args.size(); i++){
				String var = event.args.get(i).text;
				this.argIdentifiers.add(var);
				this.argTypes.add(event.variables.get(var).getVariableType());
			}

			if(event.returned != null)
			{
				returnIdentifier = event.returned.text;
				returnType = event.variables.get(returnIdentifier).getVariableType();
			}
			else
			{
				returnIdentifier = "";
				returnType = "*";
			}

			ArrayList<Token> parameterList = event.parameterList;

			parameterTypes = new ArrayList<String>();
			parameterIdentifiers = new ArrayList<String>();

			for(int i = 0; i < parameterList.size() - 1; i+=3)
			{
				parameterTypes.add(parameterList.get(i).toString().replace(" ", ""));
				parameterIdentifiers.add(parameterList.get(i + 1).toString().replace(" ", ""));
			}

			if(event.type.toString().equals(fsm.date.events.MethodCall.ActionType.uponThrowing.toString()))
			{
				type = fsm.date.events.MethodCall.ActionType.uponThrowing;
			}
			else if(event.type.toString().equals(fsm.date.events.MethodCall.ActionType.uponReturning.toString()))
			{
				type = fsm.date.events.MethodCall.ActionType.uponReturning;
			}
			else if(event.type.toString().equals(fsm.date.events.MethodCall.ActionType.uponHandling.toString()))
			{
				type = fsm.date.events.MethodCall.ActionType.uponHandling;
			}
			else if(event.type.toString().equals(fsm.date.events.MethodCall.ActionType.uponThrowing.toString()))
			{
				type = fsm.date.events.MethodCall.ActionType.uponThrowing;
			}
			else if(event.type.toString().equals(fsm.date.events.MethodCall.ActionType.before.toString()))
			{
				type = fsm.date.events.MethodCall.ActionType.before;
			}else if(event.type.toString().equals(fsm.date.events.MethodCall.ActionType.uponEntry.toString()))
			{
				type = fsm.date.events.MethodCall.ActionType.uponEntry;
			}
			
			Iterator<Token> iterator =  whereClause.iterator();
			
			this.whereClause = new ArrayList<String>();
			whereMap = new HashMap<String,String>();
			
			Token next;
			String current = "";
			while(iterator.hasNext()){
				next = iterator.next();
				
				if(!next.text.contains(";")){
					
					current += next;
				}
				else{
					this.whereClause.add(new String(current));
					
					if(current.contains("=")){
						String[] equalVars = current.split("=");
						
						whereMap.put(equalVars[0], equalVars[1]);
					}
					current = "";
				}
			}
			
			this.forEachVariables = new ArrayList<String>();
			this.forEachVariableTypes = new ArrayList<String>();
			this.forEachVariablesAsFields = new ArrayList<String>();
			this.forEachVariableTypesAsFields = new ArrayList<String>();

			for(int i = 0; i < forEachVariables.size(); i++){
				String var = forEachVariables.get(i);
				String varType = forEachVariableTypes.get(i);
				
				if(this.whereMap.get(var).contains(".")){
					this.forEachVariablesAsFields.add(var);
					this.forEachVariableTypesAsFields.add(varType);
				}
				else{
					this.forEachVariables.add(var);
					this.forEachVariableTypes.add(varType);
				}
			}
			

//		}
//		else
//		{
//			notAnAction = true;
//		}
		
//		processUnobtainableForEachVars();
	}
	
//	public MethodCall(String methodName, 
//						String className, 
//						ArrayList<String> parameterTypeList, 
//						ArrayList<String> parameterIdentifierList,
//						ActionType type, 
//						String returnIdentifier,
//						String returnType,
//						List<String> forEachVariables,
//						List<String> forEachVariableTypes,
//						ArrayList<String> whereClause,
//						String scriptFileActionName,
//						Map<String,String> whereMap){
//	//	super("");
//		this.scriptFileActionName = scriptFileActionName;
//
//		this.methodName = methodName;
//		this.objectType = className;
//		this.parameterTypes = parameterTypeList;
//		this.parameterIdentifiers = parameterIdentifierList;
//		this.type = type;
//		this.returnIdentifier = returnIdentifier;
//		this.returnType = returnType;
//		
//		this.forEachVariables = new ArrayList<String>(forEachVariables);
//		this.forEachVariableTypes = new ArrayList<String>(forEachVariableTypes);
//		
//		this.whereClause = whereClause;
//		this.whereMap = whereMap;
//		
//		processUnobtainableForEachVars();
//	}
//	
//	public MethodCall(String methodName, 
//			String className, 
//			ArrayList<String> parameterTypeList, 
//			ArrayList<String> parameterIdentifierList,
//			ActionType type, 
//			String returnIdentifier,
//			String returnType,
//			List<String> forEachVariables,
//			List<String> forEachVariableTypes,
//			String scriptFileActionName,
//			Map<String,String> whereMap){
//		//	super("");
//		this.scriptFileActionName = scriptFileActionName;
//		this.methodName = methodName;
//		this.objectType = className;
//		this.parameterTypes = parameterTypeList;
//		this.parameterIdentifiers = parameterIdentifierList;
//		this.type = type;
//		this.returnIdentifier = returnIdentifier;
//		this.returnType = returnType;
//		
//		this.forEachVariables = forEachVariables;
//		this.forEachVariableTypes = forEachVariableTypes;
//		
//		this.whereClause = new ArrayList<String>();
//		this.whereMap = whereMap;
//		
//		processUnobtainableForEachVars();
//}
//	
//	//this is because we cannot get the foreachvars constructed from the field of another object
	//so if a foreachvar is not assigned to a method parameter, or it s target, then we ignore it
	//hopefully this is just temporary
	public void processUnobtainableForEachVars(){
		Set<String> varsToRemove = new HashSet<String>();
		outer:
		for(String var : this.forEachVariables){
			if(this.whereMap.get(var).equals(this.objectIdentifier)) continue outer;
			for(String par : this.parameterIdentifiers){
				if(this.whereMap.get(var).equals(par)) continue outer;
			}
			varsToRemove.add(var);
		}
		
		this.forEachVariables.removeAll(varsToRemove);
	}
	
	public String actionTypeToString(){		
		if(this.type == ActionType.uponReturning){
			return this.type.toString() + "(" + this.returnIdentifier + ")";
		}
		else{
			return this.type.toString();
		}
		
	}
	
	public String toString(){
		return this.scriptFileActionName.substring(1, this.scriptFileActionName.length()-1);
//		String action = objectType + "." + methodName;
//		
//		action += "(";
//		
//		for(int i = 0; i < parameterTypes.size() - 1; i++){
//			action += parameterTypes.get(i) + ",";
//		}
//		
//		if(parameterTypes.size() == 0)
//		{
//			action += ")";
//		}
//		else 
//		{
//			action += parameterTypes.get(parameterTypes.size() - 1) + ")";
//		}
//		
//		return action;
	}
	
	public int hashCode(){
		if(this.isConstructor)
			System.out.println("");
		
		HashCodeBuilder hcb = new HashCodeBuilder(17, 37);
		
		hcb.append(name);

		for(String argType : argTypes){
			hcb.append(argType);
		}

		hcb.append(objectType);
		hcb.append(returnType);
		hcb.append(type);

		for(String forEachVarType : forEachVariableTypes){
			hcb.append(forEachVarType);
		}
		
		return hcb.toHashCode();
	}
	
	public boolean equals(Object obj){
		if(obj == null) return false;
		if(obj.getClass().equals(fsm.date.events.MethodCall.class)){
			fsm.date.events.MethodCall action = (fsm.date.events.MethodCall) obj;
			
			if(this.isConstructor)
				System.out.println("");

			if(action.objectType.equals(this.objectType)){
				if(action.name.equals(this.name)){
					if(action.returnType.equals(this.returnType)
							&& action.returnIdentifier.equals(this.returnIdentifier)){
						
						if(action.argTypes.size() == this.argTypes.size()){
							
							for(int i = 0; i < action.argTypes.size(); i++){
								if(!action.argTypes.get(i).equals(this.argTypes.get(i))){
									return false;
								}
							}
						}
						else return false;
						
						if(action.forEachVariables.size() == this.forEachVariables.size()){
							
							for(int i = 0; i < action.forEachVariables.size(); i++){
								if(!action.forEachVariables.get(i).equals(this.forEachVariables.get(i))){
									return false;
								}
							}
							
						}
						
						if(action.forEachVariableTypes.size() == this.forEachVariableTypes.size()){
							
							for(int i = 0; i < action.forEachVariableTypes.size(); i++){
								if(!action.forEachVariableTypes.get(i).equals(this.forEachVariableTypes.get(i))){
									return false;
								}
							}
							
						}
						
						if(action.whereMap.size() == this.whereMap.size()){
							
							for(String key : this.whereMap.keySet()){
								if(!action.whereMap.get(key).equals(this.whereMap.get(key))){
									return false;
								}
							}
						}

						return true;

					}
				}
			}
		}
		
		return false;
	}
	
	public String toDefinition(){
		String definition = "";
		
		definition += this.scriptFileActionName + "(";
		for(int i = 0; i < this.argTypes.size(); i++){
			if(i > 0) definition += ", ";
			definition += argTypes.get(i) + " " + argIdentifiers.get(i);
		}
		
		definition += ") = {";
		
		if(!objectType.equals("")){
			if(!objectIdentifier.equals("")
					&& argIdentifiers.contains(objectIdentifier)){
				definition += objectIdentifier;
			}
			else if (objectIdentifier.equals("")) definition += objectType;
			else if(objectType.equals("*") && objectIdentifier.equals("*")) definition += "*";
			else definition += objectType + " " + objectIdentifier;
		}
		else{
			if(!argIdentifiers.contains(objectIdentifier)) definition += "*";
			else definition += objectIdentifier;
		}
		
		definition += "." + name + "(";
		
		for(int i = 0; i < this.argTypes.size(); i++){
			if(i > 0) definition += ", ";
			if(argIdentifiers.contains(argIdentifiers.get(i))){
				definition += argIdentifiers.get(i);
			}
			else if(argIdentifiers.get(i).equals("")){
				definition += argTypes.get(i);
			}
			else definition += argTypes.get(i) + " " + argIdentifiers.get(i);
		}
		
		definition += ")";
		
		if(this.type.equals(ActionType.uponReturning)
				|| this.type.equals(ActionType.uponHandling)
				|| this.type.equals(ActionType.uponThrowing)){
			definition += this.type + "(";
			
			if(this.returnIdentifier != ""){
				if(this.argIdentifiers.contains(this.returnIdentifier)){
					definition += this.returnIdentifier;
				}
				else definition += this.returnType + " " + this.returnIdentifier;
			}
			definition += ")";
		}
		else definition += this.type;
		
		definition += "}";
		
		if(!whereMap.isEmpty()){
			definition += "where {";
			for(String var : whereMap.keySet()){
				definition += var + "=" + whereMap.get(var) + ";";
			}
			definition += "}";
		}
		
		if(!within.isEmpty()){
			definition += "within {" + within + "}";
		}
		
		return definition;
	}
	
}
