package fsm.date;

import compiler.Event;
import compiler.Token;
import compiler.Trigger;
import fsm.date.events.DateEvent;

import java.util.List;
import java.util.Set;

public class ForEach extends Global{

	public String variableType;
	public String variableIdentifier;

	public ForEach(String variableType,
			String variableIdentifier,
			List<String> variableDeclarations,
			Set<fsm.date.ForEach> forEaches,
			compiler.Events events){
		super(variableDeclarations, forEaches, "", events);
		this.variableType = variableType;
		this.variableIdentifier = variableIdentifier;
		this.events = events;

		for(fsm.date.ForEach forEach : forEaches){
			this.addForEach(forEach.variableIdentifier, forEach);
		}
	}

	public ForEach(String variableType,
			String variableIdentifier,
			List<String> variableDeclarations,
			compiler.Events events){
		super(variableDeclarations, "", events);
		this.variableType = variableType;
		this.variableIdentifier = variableIdentifier;
		this.events = events;
	}

	public ForEach(String variableType,
			String variableIdentifier,
			List<String> variableDeclarations,
			List<DateFSM> properties,
			compiler.Events events){
		super(variableDeclarations, properties, "", events);
		this.variableType = variableType;
		this.variableIdentifier = variableIdentifier;
		this.events = events;
	}

	public String toString(){
		String representation = "";

		representation += "FOREACH(" + variableType + " " + variableIdentifier + ")";

		representation += "{\n\n";

		if(!this.variableDeclarations.isEmpty()){
			representation += "VARIABLES{\n";
			for(String varDec : this.variableDeclarations){
				representation += "\t" + varDec.replaceAll(" \\( \\) ", "()") + ";\n";
			}

			representation += "}\n\n";

		}

		if(!events.events.isEmpty()){
			representation += "EVENTS{\n";
			for(compiler.Trigger trigger : events.events.values()){
				if(trigger instanceof compiler.Event){
					DateEvent de = DateFSM.triggerToMethodCall.get(trigger);
					if(de != null) representation += "\t" + de.toDefinition() + "\n";
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
		
		for(fsm.date.ForEach foreach : this.forEaches){
			representation += foreach.toString() + "\n";
		}
		
//		representation += "METHODS{\n";
//		for(String method : this.methods){
//			representation += method + "\n";
//		}
		representation += "\n}\n\n";
		
		return representation;
	}

}
