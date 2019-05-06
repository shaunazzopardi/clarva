package fsm.date;

import compiler.Event;
import compiler.Token;
import compiler.Trigger;
import fsm.date.events.DateEvent;
import fsm.date.events.MethodCall;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class Global {

	public List<String> variableDeclarations;
	public compiler.Events events;
	public List<DateFSM> properties;
	public Set<ForEach> forEaches;
	public List<String> methods;
	public String imports;
	
	public Global(List<String> variableDeclarations, 
			List<DateFSM> properties, 
			Set<ForEach> forEaches, 
			List<String> methods,
			String imports,
			compiler.Events events){
		this.variableDeclarations = variableDeclarations;
		this.properties = properties;
		this.forEaches = forEaches;
		this.methods = methods;
		this.imports = imports;
		this.events = events;
	}
	
	public Global(List<String> variableDeclarations, 
			Set<ForEach> forEaches,
			String imports,
			compiler.Events events){
		this.variableDeclarations = variableDeclarations;
		this.properties = new ArrayList<DateFSM>();
		this.forEaches = forEaches;
		this.imports = imports;
		this.methods = new ArrayList<String>();
	}
	
	public Global(List<String> variableDeclarations,
			String imports,
			compiler.Events events){
		this.variableDeclarations = variableDeclarations;
		this.imports = imports;
		this.properties = new ArrayList<DateFSM>();
		this.forEaches = new HashSet<ForEach>();
		this.methods = new ArrayList<String>();
		this.events = events;
	}
	
	public Global(List<String> variableDeclarations, 
			List<DateFSM> properties,
			String imports,
			compiler.Events events){
		this.variableDeclarations = variableDeclarations;
		this.properties = properties;
		this.imports = imports;
		this.forEaches = new HashSet<ForEach>();
		this.methods = new ArrayList<String>();
		this.events = events;
	}
	
	public void addDate(DateFSM prop){
		this.properties.add(prop);
	}
	
	public void addForEach(String var, ForEach forEach){
		this.forEaches.add(forEach);
	}
	
	public void addMethod(String methodDec){
		this.methods.add(methodDec);
	}
	
	public List<DateFSM> properties(){
		ArrayList<DateFSM> all = new ArrayList<DateFSM>();
		all.addAll(properties);
		for(ForEach forEach : this.forEaches){
			all.addAll(forEach.properties());
		}
		return all;
	}
	
	public String toString(){
		String representation = "";
		
		if(!this.imports.isEmpty()){
			representation+= "IMPORTS{\n";
			
			representation += imports;
			
			representation += "\n}\n\n";
		}
		
		representation += "GLOBAL";

		representation += "{\n\n";
		
		if(!this.variableDeclarations.isEmpty()){
			representation += "\tVARIABLES{\n";
			for(String varDec : this.variableDeclarations){
				representation += "\t" + varDec.replaceAll(" \\( \\) ", "()") + ";\n";
			}
			
			representation += "}\n\n";
		
		}

		List<compiler.EventCollection> eventCollections = new ArrayList<>();

		if(!events.events.isEmpty()){
			representation += "EVENTS{\n\n";
			for(compiler.Trigger trigger : events.events.values()){
				if(trigger instanceof compiler.Event){
					DateEvent de = DateFSM.triggerToDateEvent.get(trigger);
					if(de != null){
						representation += "\t" + de.toDefinition() + "\n";
					}
				}
				else if(trigger instanceof compiler.EventCollection){
					eventCollections.add((compiler.EventCollection) trigger);
				}
			}

			for(compiler.EventCollection eventCollection : eventCollections) {

				if(eventCollection.events.size() == 1){
					Trigger trig = eventCollection.events.get(0);
					representation = representation.replaceAll(Pattern.quote(trig.name.toString()), trig.name.toString().substring(1, trig.name.toString().length() - 1));
				}
				else {
					String events = "";
					for (int i = 0; i < eventCollection.events.size(); i++) {
						Trigger trig = eventCollection.events.get(i);

						if (trig instanceof Event) {
							if (DateFSM.triggerToDateEvent.keySet().contains(trig)) {
								if (!events.equals("")) {
									events += "|";
								}
								events += trig.getName().toString();
							}
						}
					}

					if (!events.equals("")) {
						representation += "\t" + eventCollection.getName().toString() + "() = {" + events + "}";
						if (!eventCollection.filter.isEmpty()) {
							representation += "filter {";
							for (Token token : eventCollection.filter) {
								representation += token.toString() + ";";
							}
							representation += "}";
						}

						representation += "\n";
					}
				}
			}
			//	representation += "}\n";
			
			representation += "\n}\n\n";
		}
		
		for(DateFSM prop : this.properties){
			if(prop != null)
				representation += prop.toString() + "\n";
		}
		
		for(ForEach foreach : this.forEaches){
			representation += foreach.toString() + "\n";
		}
		
		representation += "\n}\n\n";
		
		if(!methods.isEmpty()){
			representation += "METHODS{\n\n";
			for(String method : this.methods){
				representation += method + "\n";
			}
			representation += "\n}";
		}
		
		
		return representation;
	}

	public String toStringWithEventInterface(){
		String representation = "";

		if(!this.imports.isEmpty()){
			representation+= "IMPORTS{\n";

			representation += imports;

			representation += "\n}\n\n";
		}

		representation += "GLOBAL";

		representation += "{\n\n";

		if(!this.variableDeclarations.isEmpty()){
			representation += "\tVARIABLES{\n";
			for(String varDec : this.variableDeclarations){
				representation += "\t" + varDec.replaceAll(" \\( \\) ", "()") + ";\n";
			}

			representation += "}\n\n";

		}

		List<compiler.EventCollection> eventCollections = new ArrayList<>();

		if(!events.events.isEmpty()){
			representation += "EVENTS{\n\n";
			for(compiler.Trigger trigger : events.events.values()){
				if(trigger instanceof compiler.Event){
					DateEvent de = DateFSM.triggerToDateEvent.get(trigger);
					if(de != null){
						if(de.getClass().equals(MethodCall.class)){
							representation += "\t" + Global.toDefinitionWithEventInterface((MethodCall) de) + "\n";
						} else {
							representation += "\t" + de.toDefinition() + "\n";
						}
					}
				}
				else if(trigger instanceof compiler.EventCollection){
					eventCollections.add((compiler.EventCollection) trigger);
				}
			}

			for(compiler.EventCollection eventCollection : eventCollections) {

				if(eventCollection.events.size() == 1){
					Trigger trig = eventCollection.events.get(0);
					representation = representation.replaceAll(Pattern.quote(trig.name.toString()), trig.name.toString().substring(1, trig.name.toString().length() - 1));
				}
				else {
					String events = "";
					for (int i = 0; i < eventCollection.events.size(); i++) {
						Trigger trig = eventCollection.events.get(i);

						if (trig instanceof Event) {
							if (DateFSM.triggerToDateEvent.keySet().contains(trig)) {
								if (!events.equals("")) {
									events += "|";
								}
								events += trig.getName().toString();
							}
						}
					}

					if (!events.equals("")) {
						representation += "\t" + eventCollection.getName().toString() + "() = {" + events + "}";
						if (!eventCollection.filter.isEmpty()) {
							representation += "filter {";
							for (Token token : eventCollection.filter) {
								representation += token.toString() + ";";
							}
							representation += "}";
						}

						representation += "\n";
					}
				}
			}
			//	representation += "}\n";

			representation += "\n}\n\n";
		}

		for(DateFSM prop : this.properties){
			if(prop != null)
				representation += prop.toString() + "\n";
		}

		for(ForEach foreach : this.forEaches){
			representation += foreach.toStringWithEventInterface() + "\n";
		}

		representation += "\n}\n\n";

		if(!methods.isEmpty()){
			representation += "METHODS{\n\n";
			for(String method : this.methods){
				representation += method + "\n";
			}
			representation += "\n}";
		}


		return representation;
	}

	public static String toDefinitionWithEventInterface(MethodCall call){
		String definition = "";

		definition += call.scriptFileActionName + "(";
		for(int i = 0; i < call.argTypes.size(); i++){
			if(i > 0) definition += ", ";
			definition += call.argTypes.get(i) + " " + call.argIdentifiers.get(i);
		}

		definition += ") = {";



//		if(!call.objectType.equals("")){
//			if(!call.objectIdentifier.equals("")
//					&& call.argIdentifiers.contains(call.objectIdentifier)){
//				definition += call.objectIdentifier;
//			}
//			else if (call.objectIdentifier.equals("")) definition += call.objectType;
//			else if(call.objectType.equals("*") && call.objectIdentifier.equals("*")) definition += "*";
//			else definition += call.objectType + " " + call.objectIdentifier;
//		}
//		else{
//			if(!call.argIdentifiers.contains(call.objectIdentifier)) definition += "*";
//			else definition += call.objectIdentifier;
//		}

		definition += call.name + "Monitored(";

		if(!call.objectType.equals("")){
			if(!call.objectIdentifier.equals("")
					&& call.argIdentifiers.contains(call.objectIdentifier)){
				definition += call.objectIdentifier;
			}
			else if (call.objectIdentifier.equals("")) definition += call.objectType;
			else if(call.objectType.equals("*") && call.objectIdentifier.equals("*")) definition += "*";
			else definition += call.objectType + " " + call.objectIdentifier;
		}
		else{
			if(!call.argIdentifiers.contains(call.objectIdentifier)) definition += "*";
			else definition += call.objectIdentifier;
		}

		for(int i = 0; i < call.argTypes.size(); i++){
			definition += ", ";
			if(call.argIdentifiers.contains(call.argIdentifiers.get(i))){
				definition += call.argIdentifiers.get(i);
			}
			else if(call.argIdentifiers.get(i).equals("")){
				definition += call.argTypes.get(i);
			}
			else definition += call.argTypes.get(i) + " " + call.argIdentifiers.get(i);
		}

		definition += ")";

		if(call.type.equals(MethodCall.ActionType.uponReturning)
				|| call.type.equals(MethodCall.ActionType.uponHandling)
				|| call.type.equals(MethodCall.ActionType.uponThrowing)){
			definition += call.type + "(";

			if(call.returnIdentifier != ""){
				if(call.argIdentifiers.contains(call.returnIdentifier)){
					definition += call.returnIdentifier;
				}
				else definition += call.returnType + " " + call.returnIdentifier;
			}
			definition += ")";
		}
		else definition += call.type;

		definition += "}";

		if(!call.whereMap.isEmpty()){
			definition += "where {";
			for(String var : call.whereMap.keySet()){
				definition += var + "=" + call.whereMap.get(var) + ";";
			}
			definition += "}";
		}

		if(!call.within.isEmpty()){
			definition += "within {" + call.within + "}";
		}

		return definition;
	}
}
