package fsm.date.events;

import compiler.Token;

import java.util.*;
import java.util.Map.Entry;

public class ChannelEvent extends DateEvent{

	public String channelName;
	public String parameterType;
	public String parameterName;
	public Map<String,String> whereMap;

//	public ChannelEvent(String name, String channelName, String parameterType, String parameterName) {
//		super(name);
//		this.channelName = channelName;
//		this.parameterType = parameterType;
//		this.parameterName = parameterName;
//	}
	

	public ChannelEvent(String name, String channelName, String parameterType, String parameterName, List<Token> whereClause) {
		super(name);
		this.channelName = channelName;
		this.parameterType = parameterType;
		this.parameterName = parameterName;
		this.whereMap = new HashMap<String,String>();
		
		List<Token> whereClauses = new ArrayList<Token>();
		whereClauses.addAll(whereClause);
		Iterator<Token> iterator = whereClauses.iterator();
		
		whereMap = new HashMap<String,String>();
		
		Token next;
		String current = "";
		while(iterator.hasNext()){
			next = iterator.next();
			
			if(!next.text.contains(";")){
				
				current += next;
			}
			else{				
				if(current.contains("=")){
					String[] equalVars = current.split("=");
					
					whereMap.put(equalVars[0], equalVars[1]);
				}
				current = "";
			}
		}
	}
	
	public boolean equals(Object obj){
		if(obj instanceof fsm.date.events.ChannelEvent){
			fsm.date.events.ChannelEvent other = (fsm.date.events.ChannelEvent) obj;
			
			if(this.name.equals(other.name)
					&& this.parameterName.equals(other.parameterName)
					&& this.parameterType.equals(other.parameterType)){
				return true;
			}
		}
		
		return false;
	}
	
	public String toDefinition(){
		String definition = "";
		
		definition += name;
		definition += "(" + parameterType + " " + parameterName + ")";
		definition += " = ";
		definition += "{";
		definition += channelName + ".receive(";
		definition += parameterName + ")}";
		
		if(!whereMap.isEmpty()){
			definition += " where{";
			for(Entry<String,String> entry : this.whereMap.entrySet()){
				definition += entry.getKey() + "=" + entry.getValue() + ";";
			}
			
			definition += "}";
		}
		
		return definition;
	}
}
