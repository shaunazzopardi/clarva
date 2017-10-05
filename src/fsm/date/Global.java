package fsm.date;

import compiler.Event;
import compiler.Token;
import compiler.Trigger;
import fsm.date.events.DateEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Global {

	List<String> variableDeclarations;
	compiler.Events events;
	public List<DateFSM> properties;
	public Set<ForEach> forEaches;
	List<String> methods;
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

		if(!events.events.isEmpty()){
			representation += "EVENTS{\n\n";
			for(compiler.Trigger trigger : events.events.values()){
				if(trigger instanceof compiler.Event){
					DateEvent de = DateFSM.triggerToMethodCall.get(trigger);
					if(de != null){
						representation += "\t" + de.toDefinition() + "\n";
					}
				}
				else if(trigger instanceof compiler.EventCollection){
					compiler.EventCollection eventCollection = (compiler.EventCollection) trigger;
					
					String events = "";
					for(int i = 0; i < eventCollection.events.size(); i++){
						Trigger trig = eventCollection.events.get(i);
						
						if(trig instanceof Event){
							if(DateFSM.triggerToMethodCall.keySet().contains(trig)){
								if(!events.equals("")){
									events += "|";
								}
								events += trig.getName().toString();
							}
						}
					}
					
					if(!events.equals("")){
						representation += "\t" + eventCollection.getName().toString() + " = {" + events + "}";
						if(!eventCollection.filter.isEmpty()){
							representation += "filter {";
							for(Token token : eventCollection.filter){
								representation += token.toString() + ";";
							}
							representation += "}";
						}
						
						representation += "\n";
					}
					
				//	representation += "}\n";
				}
			}
			
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

	
}
