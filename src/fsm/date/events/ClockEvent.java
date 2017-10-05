package fsm.date.events;

public class ClockEvent extends DateEvent{

	public String clockName;
	public Boolean repeats;
	public Long secondsAfterWhichTriggered;
	
	public ClockEvent(String name, boolean repeats, Long secondsAfterWhichTriggered) {
		super(name);
		this.repeats = repeats;
		this.secondsAfterWhichTriggered = secondsAfterWhichTriggered;
	}

	public boolean equals(Object obj){
		if(obj instanceof fsm.date.events.ClockEvent){
			fsm.date.events.ClockEvent other = (fsm.date.events.ClockEvent) obj;
			
			if(this.clockName.equals(other.clockName)
					&& this.repeats.equals(other.repeats)
					&& this.secondsAfterWhichTriggered.equals(other.secondsAfterWhichTriggered)){
				return true;
			}
		}
		
		return false;
	}
	
	public String toDefinition(){
		String definition = "";
		definition += name + "() = {";
		definition += clockName + "@";
		if(this.repeats) definition += "%";
		definition += this.secondsAfterWhichTriggered;
		
		definition += "}";
		
		return definition;
	}
	
}
