package larva;


import main.java.server.proxies.entityinfo.UserInfo;
import main.java.server.entities.users.User.PrivacyLevel;

import java.util.LinkedHashMap;
import java.io.PrintWriter;

public class _cls_residual1 implements _callable{

public static LinkedHashMap<_cls_residual1,_cls_residual1> _cls_residual1_instances = new LinkedHashMap<_cls_residual1,_cls_residual1>();

_cls_residual0 parent;
public static UserInfo uu;
public static boolean success;
public static double value;
public UserInfo u;
int no_automata = 1;
 public double cumulativeValue =0.0 ;
 public double limit =100 ;

public static void initialize(){}
//inheritance could not be used because of the automatic call to super()
//when the constructor is called...we need to keep the SAME parent if this exists!

public _cls_residual1( UserInfo u) {
parent = _cls_residual0._get_cls_residual0_inst();
this.u = u;
}

public void initialisation() {
}

public static _cls_residual1 _get_cls_residual1_inst( UserInfo u) { synchronized(_cls_residual1_instances){
_cls_residual1 _inst = new _cls_residual1( u);
if (_cls_residual1_instances.containsKey(_inst))
{
_cls_residual1 tmp = _cls_residual1_instances.get(_inst);
 return _cls_residual1_instances.get(_inst);
}
else
{
 _inst.initialisation();
 _cls_residual1_instances.put(_inst,_inst);
 return _inst;
}
}
}

public boolean equals(Object o) {
 if ((o instanceof _cls_residual1)
 && (u == null || u.equals(((_cls_residual1)o).u))
 && (parent == null || parent.equals(((_cls_residual1)o).parent)))
{return true;}
else
{return false;}
}

public int hashCode() {
return 0;
}

public void _call(String _info, int... _event){
synchronized(_cls_residual1_instances){
_performLogic_dataProtectionResidual(_info, _event);
}
}

public void _call_all_filtered(String _info, int... _event){
}

public static void _call_all(String _info, int... _event){

_cls_residual1[] a = new _cls_residual1[1];
synchronized(_cls_residual1_instances){
a = _cls_residual1_instances.keySet().toArray(a);}
for (_cls_residual1 _inst : a)

if (_inst != null) _inst._call(_info, _event);
}

public void _killThis(){
try{
if (--no_automata == 0){
synchronized(_cls_residual1_instances){
_cls_residual1_instances.remove(this);}
}
else if (no_automata < 0)
{throw new Exception("no_automata < 0!!");}
}catch(Exception ex){ex.printStackTrace();}
}

int _state_id_dataProtectionResidual = 102;

public void _performLogic_dataProtectionResidual(String _info, int... _event) {

_cls_residual0.pw.println("[dataProtectionResidual]AUTOMATON::> dataProtectionResidual("+u + " " + ") STATE::>"+ _string_dataProtectionResidual(_state_id_dataProtectionResidual, 0));
_cls_residual0.pw.flush();

if (0==1){}
else if (_state_id_dataProtectionResidual==102){
		if (1==0){}
		else if ((_occurredEvent(_event,172/*register*/))){
		
		_state_id_dataProtectionResidual = 99;//moving to state two
		_goto_dataProtectionResidual(_info);
		}
}
else if (_state_id_dataProtectionResidual==99){
		if (1==0){}
		else if ((_occurredEvent(_event,168/*transact*/)) && (cumulativeValue <=limit )){
		cumulativeValue -=value ;

		_state_id_dataProtectionResidual = 99;//moving to state two
		_goto_dataProtectionResidual(_info);
		}
		else if ((_occurredEvent(_event,168/*transact*/)) && (cumulativeValue >limit )){
		
		_state_id_dataProtectionResidual = 98;//moving to state bad
		_goto_dataProtectionResidual(_info);
		}
		else if ((_occurredEvent(_event,170/*authenticate*/)) && (success )){
		
		_state_id_dataProtectionResidual = 100;//moving to state three
		_goto_dataProtectionResidual(_info);
		}
}
else if (_state_id_dataProtectionResidual==100){
		if (1==0){}
		else if ((_occurredEvent(_event,174/*enterDeregister*/))){
		
		_state_id_dataProtectionResidual = 101;//moving to state four
		_goto_dataProtectionResidual(_info);
		}
}
}

public void _goto_dataProtectionResidual(String _info){
_cls_residual0.pw.println("[dataProtectionResidual]MOVED ON METHODCALL: "+ _info +" TO STATE::> " + _string_dataProtectionResidual(_state_id_dataProtectionResidual, 1));
_cls_residual0.pw.flush();
}

public String _string_dataProtectionResidual(int _state_id, int _mode){
switch(_state_id){
case 98: if (_mode == 0) return "bad"; else return "!!!SYSTEM REACHED BAD STATE!!! bad "+new _BadStateExceptionresidual().toString()+" ";
case 101: if (_mode == 0) return "four"; else return "four";
case 102: if (_mode == 0) return "one"; else return "one";
case 97: if (_mode == 0) return "five"; else return "(((SYSTEM REACHED AN ACCEPTED STATE)))  five";
case 99: if (_mode == 0) return "two"; else return "two";
case 100: if (_mode == 0) return "three"; else return "three";
default: return "!!!SYSTEM REACHED AN UNKNOWN STATE!!!";
}
}

public boolean _occurredEvent(int[] _events, int event){
for (int i:_events) if (i == event) return true;
return false;
}
}