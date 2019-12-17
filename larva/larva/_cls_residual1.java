package larva;


import main.Menu;
import main.User;

import java.util.LinkedHashMap;
import java.io.PrintWriter;

public class _cls_residual1 implements _callable{

public static LinkedHashMap<_cls_residual1,_cls_residual1> _cls_residual1_instances = new LinkedHashMap<_cls_residual1,_cls_residual1>();

_cls_residual0 parent;
public static double val;
public static long from;
public static long id;
public static long to;
public static User u1;
public User u;
int no_automata = 3;
 public double balance =0 ;
 public int lim =500 ;

public static void initialize(){}
//inheritance could not be used because of the automatic call to super()
//when the constructor is called...we need to keep the SAME parent if this exists!

public _cls_residual1( User u) {
parent = _cls_residual0._get_cls_residual0_inst();
this.u = u;
}

public void initialisation() {
}

public static _cls_residual1 _get_cls_residual1_inst( User u) { synchronized(_cls_residual1_instances){
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
_performLogic_firstProperty(_info, _event);
_performLogic_thirdProperty(_info, _event);
_performLogic_fourthProperty(_info, _event);
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

int _state_id_firstProperty = 3;

public void _performLogic_firstProperty(String _info, int... _event) {

_cls_residual0.pw.println("[firstProperty]AUTOMATON::> firstProperty("+u + " " + ") STATE::>"+ _string_firstProperty(_state_id_firstProperty, 0));
_cls_residual0.pw.flush();

if (0==1){}
else if (_state_id_firstProperty==2){
		if (1==0){}
		else if ((_occurredEvent(_event,2/*logout*/))){
		
		_state_id_firstProperty = 3;//moving to state loggedOut
		_goto_firstProperty(_info);
		}
		else if ((_occurredEvent(_event,4/*delete_enter*/))){
		
		_state_id_firstProperty = 0;//moving to state inDelete
		_goto_firstProperty(_info);
		}
}
else if (_state_id_firstProperty==3){
		if (1==0){}
		else if ((_occurredEvent(_event,4/*delete_enter*/))){
		
		_state_id_firstProperty = 1;//moving to state bad
		_goto_firstProperty(_info);
		}
		else if ((_occurredEvent(_event,0/*login*/)) && (u .activated )){
		
		_state_id_firstProperty = 2;//moving to state loggedIn
		_goto_firstProperty(_info);
		}
}
}

public void _goto_firstProperty(String _info){
_cls_residual0.pw.println("[firstProperty]MOVED ON METHODCALL: "+ _info +" TO STATE::> " + _string_firstProperty(_state_id_firstProperty, 1));
_cls_residual0.pw.flush();
}

public String _string_firstProperty(int _state_id, int _mode){
switch(_state_id){
case 0: if (_mode == 0) return "inDelete"; else return "(((SYSTEM REACHED AN ACCEPTED STATE)))  inDelete";
case 1: if (_mode == 0) return "bad"; else return "!!!SYSTEM REACHED BAD STATE!!! bad "+new _BadStateExceptionresidual().toString()+" ";
case 2: if (_mode == 0) return "loggedIn"; else return "loggedIn";
case 3: if (_mode == 0) return "loggedOut"; else return "loggedOut";
default: return "!!!SYSTEM REACHED AN UNKNOWN STATE!!!";
}
}
int _state_id_thirdProperty = 6;

public void _performLogic_thirdProperty(String _info, int... _event) {

_cls_residual0.pw.println("[thirdProperty]AUTOMATON::> thirdProperty("+u + " " + ") STATE::>"+ _string_thirdProperty(_state_id_thirdProperty, 0));
_cls_residual0.pw.flush();

if (0==1){}
else if (_state_id_thirdProperty==6){
		if (1==0){}
		else if ((_occurredEvent(_event,4/*delete_enter*/))){
		
		_state_id_thirdProperty = 5;//moving to state afterDel
		_goto_thirdProperty(_info);
		}
}
else if (_state_id_thirdProperty==5){
		if (1==0){}
		else if ((_occurredEvent(_event,10/*transact_enter*/))){
		
		_state_id_thirdProperty = 4;//moving to state bad
		_goto_thirdProperty(_info);
		}
}
}

public void _goto_thirdProperty(String _info){
_cls_residual0.pw.println("[thirdProperty]MOVED ON METHODCALL: "+ _info +" TO STATE::> " + _string_thirdProperty(_state_id_thirdProperty, 1));
_cls_residual0.pw.flush();
}

public String _string_thirdProperty(int _state_id, int _mode){
switch(_state_id){
case 4: if (_mode == 0) return "bad"; else return "!!!SYSTEM REACHED BAD STATE!!! bad "+new _BadStateExceptionresidual().toString()+" ";
case 6: if (_mode == 0) return "beforeDel"; else return "beforeDel";
case 5: if (_mode == 0) return "afterDel"; else return "afterDel";
default: return "!!!SYSTEM REACHED AN UNKNOWN STATE!!!";
}
}
int _state_id_fourthProperty = 8;

public void _performLogic_fourthProperty(String _info, int... _event) {

_cls_residual0.pw.println("[fourthProperty]AUTOMATON::> fourthProperty("+u + " " + ") STATE::>"+ _string_fourthProperty(_state_id_fourthProperty, 0));
_cls_residual0.pw.flush();

if (0==1){}
else if (_state_id_fourthProperty==8){
		if (1==0){}
		else if ((_occurredEvent(_event,10/*transact_enter*/)) && (val +balance <=lim )){
		balance +=val ;

		_state_id_fourthProperty = 8;//moving to state init
		_goto_fourthProperty(_info);
		}
		else if ((_occurredEvent(_event,10/*transact_enter*/)) && (val +balance >lim )){
		
		_state_id_fourthProperty = 7;//moving to state bad
		_goto_fourthProperty(_info);
		}
}
}

public void _goto_fourthProperty(String _info){
_cls_residual0.pw.println("[fourthProperty]MOVED ON METHODCALL: "+ _info +" TO STATE::> " + _string_fourthProperty(_state_id_fourthProperty, 1));
_cls_residual0.pw.flush();
}

public String _string_fourthProperty(int _state_id, int _mode){
switch(_state_id){
case 8: if (_mode == 0) return "init"; else return "init";
case 7: if (_mode == 0) return "bad"; else return "!!!SYSTEM REACHED BAD STATE!!! bad "+new _BadStateExceptionresidual().toString()+" ";
default: return "!!!SYSTEM REACHED AN UNKNOWN STATE!!!";
}
}

public boolean _occurredEvent(int[] _events, int event){
for (int i:_events) if (i == event) return true;
return false;
}
}