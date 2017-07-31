package clarva.analysis.cfg;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import fsm.date.events.ChannelEvent;
import fsm.date.events.ClockEvent;
import fsm.date.events.DateEvent;
import fsm.date.events.MethodCall;
import soot.Local;
import soot.SootMethod;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.pointer.InstanceKey;
import soot.jimple.toolkits.pointer.LocalMustAliasAnalysis;
import soot.jimple.toolkits.pointer.LocalMustNotAliasAnalysis;

public class Shadow extends CFGEvent{

	public InvokeExpr invocation;
	public DateEvent event;
	public Map<String, InstanceKey> objectBinding = new HashMap<String, InstanceKey>();
	public Stmt unit;
	public SootMethod callingMethod;
	public Map<String, Value> valueBinding;
	
	public boolean epsilon = false;
	
	public Shadow(){
		this.epsilon = true;
	}
	
	public Shadow(Stmt unit){
		epsilon = true;
		this.unit = unit;
	}
	
	public Shadow(ChannelEvent event){
		this.event = event;
		this.epsilon = true;
	}
	
	public Shadow(ClockEvent event){
		this.event = event;
		this.epsilon = true;
	}
	
	public Shadow(InvokeExpr invocation, Stmt unit, MethodCall event, Map<String, Value> valueBinding){
		this.invocation = invocation;
		this.event = event;
		this.unit = unit;
		this.valueBinding = valueBinding;
	}
//	static int i = 0;
	public void inferBinding(SootMethod method, LocalMustAliasAnalysis methodMustAlias,  LocalMustNotAliasAnalysis methodMustNotAlias){
		this.callingMethod = method;
		if(epsilon) return;
		
		MethodCall methodCallEvent = (MethodCall) event;
		for(String var : methodCallEvent.forEachVariables){
			Local local = (Local) valueBinding.get(var);
			Stmt stmt = (Stmt) unit;
			//we have a problem with the following kind of vars: foreachvars(Account a, User u = a.u)
			//We can't calculate a.u currenty. but do we need to really?
			//In this case no, since shadow(a) != shadow(a') => shadow(u) != shadow(u')
			//But in general we can have an aribitrary a.equals() function that does not depend on a.u
			//e.g. a.equals(a') <= a.value = a'.value
			//If ppDate's don't use Larva's method of instanstiating fields we don't have a problem
			//Otherwise heq, maybe Bodden's Boomerang can help
//			i++;
//			System.out.println(i);
			
//			////what about when local = this?
//			if(local.getName().equals("this")) {
//				objectBinding.put(var, null);
//				continue;
//			}
			
			InstanceKey key = new InstanceKey(local, stmt, method, methodMustAlias, methodMustNotAlias);
			objectBinding.put(var, key);
		}
	}
	
	public boolean mayAlias(Shadow s){
		//if shadows are in same method
		if(s.callingMethod.equals(this.callingMethod)){
			for(String var : this.objectBinding.keySet()){
				if(s.objectBinding.keySet().contains(var)){
					if(s.objectBinding.get(var) == null
							|| this.objectBinding.get(var) == null) return true;
					
					if(s.objectBinding.get(var).mayNotAlias(this.objectBinding.get(var))){
						return false;
					}
				}
			}
		}
		else{
			
		}
		
		return true;
	}
	
	public boolean mustAlias(Shadow s){
		for(String var : this.objectBinding.keySet()){
			if(s.objectBinding.keySet().contains(var)){
				if(s.objectBinding.get(var) == null
						|| this.objectBinding.get(var) == null) return false;

				if(!s.objectBinding.get(var).mustAlias(this.objectBinding.get(var))){
					return false;
				}
			}
		}
		
		return true;
	}
	
	public boolean equals(Object obj){
		if(obj.getClass().equals(this.getClass())){
			Shadow other = (Shadow) obj;
			if(other.unit != null
					&& this.unit != null){
				if(other.unit.equals(this.unit)){
					if(other.event != null
							&& this.event != null){
						if(other.event.equals(this.event)
								&& this.invocation.equals(other.invocation)){
							return true;
						}
					}
				}
			}
			
			if(this.epsilon && other.epsilon) 
				return true;

		}
		
		
		return false;
	}
	
	public String toString(){
		if(this.epsilon)
			return "epsilon";
		else return invocation.toString() + " in " + unit.toString();
	}
	
	public int hashCode(){
		HashCodeBuilder hcb = new HashCodeBuilder(17, 37);
		
		if(epsilon) return hcb.toHashCode();
		else{
			
			if(this.unit != null){
				hcb.append(this.unit);
			}
	
			if(this.event != null){
				hcb.append(this.unit);
			}
			
			if(this.invocation != null){
				hcb.append(this.invocation);
			}
		}
		return hcb.toHashCode();
	}
}