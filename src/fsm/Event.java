package fsm;

import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Event<T> {

	public T label;
	
	public Event(){
		label = null;
	}
	
	public Event(T event){
		this.label = event;
	}
	
	@Override
	public int hashCode() {
		HashCodeBuilder hcb = new HashCodeBuilder(17,37);
		
		if(label != null) hcb.append(label);
		
		return hcb.toHashCode();
	}
	
	@Override
	public boolean equals(Object obj){
	    if(obj instanceof fsm.Event){
	    	fsm.Event objAct = (fsm.Event) obj;
	    	
	    	if(this.label == null
	    			&& objAct.label == null) return true;
	    	
	    	else return objAct.label.equals(label);
	    }
	    
	    return false;
	}
	
	public String toString(){
		if(label == null) return "";
		return this.label.toString();
	}
}
