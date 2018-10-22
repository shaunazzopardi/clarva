package fsm.date;

import fsm.date.events.DateEvent;

public class DateLabel {

	public DateEvent event;
		
	public String condition;
	
	public String action;
	
	public DateLabel(DateEvent event, String condition, String action){
		this.event = event;
		this.condition = condition;
		this.action = action;
	}

//	public DateLabel(String otherKindOfEvent, String condition, String action){
//		this.otherKindOfEvent = otherKindOfEvent;
//		this.condition = condition;
//		this.action = action;
//		this.dateEvent = null;
//	}
	
	public boolean equals(Object obj){
		if(obj instanceof fsm.date.DateLabel){
			fsm.date.DateLabel other = (fsm.date.DateLabel) obj;

			if(other.event.equals(event)){
				if(other.condition.equals(condition)){
					if(other.action.equals(action)){
						return true;
					}
				}
			}
		}
		
		return false;

	}

	public String toString(){
		return event.toString() + "\\" + condition.toString() + "\\" + action.toString();
	}
}
