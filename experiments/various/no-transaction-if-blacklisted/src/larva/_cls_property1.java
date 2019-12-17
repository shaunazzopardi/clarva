package larva;


import main.java.server.*;
import main.java.server.proxies.entityinfo.UserInfo;
import main.java.paymentapp.*;

import java.util.LinkedHashMap;
import java.io.PrintWriter;

public class _cls_property1 implements _callable{

public static LinkedHashMap<_cls_property1,_cls_property1> _cls_property1_instances = new LinkedHashMap<_cls_property1,_cls_property1>();

_cls_property0 parent;
public static Boolean success;
public static UserInfo u1;
public static int cid;
public UserInfo u;
int no_automata = 1;

public static void initialize(){}
//inheritance could not be used because of the automatic call to super()
//when the constructor is called...we need to keep the SAME parent if this exists!

public _cls_property1( UserInfo u) {
parent = _cls_property0._get_cls_property0_inst();
this.u = u;
}

public void initialisation() {
}

public static _cls_property1 _get_cls_property1_inst( UserInfo u) { synchronized(_cls_property1_instances){
_cls_property1 _inst = new _cls_property1( u);
if (_cls_property1_instances.containsKey(_inst))
{
_cls_property1 tmp = _cls_property1_instances.get(_inst);
 return _cls_property1_instances.get(_inst);
}
else
{
 _inst.initialisation();
 _cls_property1_instances.put(_inst,_inst);
 return _inst;
}
}
}

public boolean equals(Object o) {
 if ((o instanceof _cls_property1)
 && (u == null || u.equals(((_cls_property1)o).u))
 && (parent == null || parent.equals(((_cls_property1)o).parent)))
{return true;}
else
{return false;}
}

public int hashCode() {
return 0;
}

public void _call(String _info, int... _event){
synchronized(_cls_property1_instances){
_performLogic_noTransactionIfBlackListedProperty(_info, _event);
}
}

public void _call_all_filtered(String _info, int... _event){
}

public static void _call_all(String _info, int... _event){

_cls_property1[] a = new _cls_property1[1];
synchronized(_cls_property1_instances){
a = _cls_property1_instances.keySet().toArray(a);}
for (_cls_property1 _inst : a)

if (_inst != null) _inst._call(_info, _event);
}

public void _killThis(){
try{
if (--no_automata == 0){
synchronized(_cls_property1_instances){
_cls_property1_instances.remove(this);}
}
else if (no_automata < 0)
{throw new Exception("no_automata < 0!!");}
}catch(Exception ex){ex.printStackTrace();}
}

int _state_id_noTransactionIfBlackListedProperty = 130;

public void _performLogic_noTransactionIfBlackListedProperty(String _info, int... _event) {

_cls_property0.pw.println("[noTransactionIfBlackListedProperty]AUTOMATON::> noTransactionIfBlackListedProperty("+u + " " + ") STATE::>"+ _string_noTransactionIfBlackListedProperty(_state_id_noTransactionIfBlackListedProperty, 0));
_cls_property0.pw.flush();

if (0==1){}
else if (_state_id_noTransactionIfBlackListedProperty==130){
		if (1==0){}
		else if ((_occurredEvent(_event,224/*payFromCard_exit*/)) && (u .blacklisted )){
		
		_state_id_noTransactionIfBlackListedProperty = 129;//moving to state bad
		_goto_noTransactionIfBlackListedProperty(_info);
		}
}
}

public void _goto_noTransactionIfBlackListedProperty(String _info){
_cls_property0.pw.println("[noTransactionIfBlackListedProperty]MOVED ON METHODCALL: "+ _info +" TO STATE::> " + _string_noTransactionIfBlackListedProperty(_state_id_noTransactionIfBlackListedProperty, 1));
_cls_property0.pw.flush();
}

public String _string_noTransactionIfBlackListedProperty(int _state_id, int _mode){
switch(_state_id){
case 129: if (_mode == 0) return "bad"; else return "!!!SYSTEM REACHED BAD STATE!!! bad "+new _BadStateExceptionproperty().toString()+" ";
case 130: if (_mode == 0) return "start"; else return "start";
default: return "!!!SYSTEM REACHED AN UNKNOWN STATE!!!";
}
}

public boolean _occurredEvent(int[] _events, int event){
for (int i:_events) if (i == event) return true;
return false;
}
}