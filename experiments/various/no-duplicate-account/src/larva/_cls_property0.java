package larva;


import main.java.server.*;
import main.java.server.proxies.entityinfo.UserInfo;
import main.java.paymentapp.*;
import java.util.*;

import java.util.LinkedHashMap;
import java.io.PrintWriter;

public class _cls_property0 implements _callable{

public static PrintWriter pw; 
public static _cls_property0 root;

public static LinkedHashMap<_cls_property0,_cls_property0> _cls_property0_instances = new LinkedHashMap<_cls_property0,_cls_property0>();
static{
try{
RunningClock.start();
pw = new PrintWriter("./output_property.txt");

root = new _cls_property0();
_cls_property0_instances.put(root, root);
  root.initialisation();
}catch(Exception ex)
{ex.printStackTrace();}
}

_cls_property0 parent; //to remain null - this class does not have a parent!
public static int id;
int no_automata = 1;
 public List <Integer >accIds =new ArrayList <Integer >();

public static void initialize(){}
//inheritance could not be used because of the automatic call to super()
//when the constructor is called...we need to keep the SAME parent if this exists!

public _cls_property0() {
}

public void initialisation() {
}

public static _cls_property0 _get_cls_property0_inst() { synchronized(_cls_property0_instances){
 return root;
}
}

public boolean equals(Object o) {
 if ((o instanceof _cls_property0))
{return true;}
else
{return false;}
}

public int hashCode() {
return 0;
}

public void _call(String _info, int... _event){
synchronized(_cls_property0_instances){
_performLogic_noDuplicateAccountID(_info, _event);
}
}

public void _call_all_filtered(String _info, int... _event){
}

public static void _call_all(String _info, int... _event){

_cls_property0[] a = new _cls_property0[1];
synchronized(_cls_property0_instances){
a = _cls_property0_instances.keySet().toArray(a);}
for (_cls_property0 _inst : a)

if (_inst != null) _inst._call(_info, _event);
}

public void _killThis(){
try{
if (--no_automata == 0){
synchronized(_cls_property0_instances){
_cls_property0_instances.remove(this);}
}
else if (no_automata < 0)
{throw new Exception("no_automata < 0!!");}
}catch(Exception ex){ex.printStackTrace();}
}

int _state_id_noDuplicateAccountID = 116;

public void _performLogic_noDuplicateAccountID(String _info, int... _event) {

_cls_property0.pw.println("[noDuplicateAccountID]AUTOMATON::> noDuplicateAccountID("+") STATE::>"+ _string_noDuplicateAccountID(_state_id_noDuplicateAccountID, 0));
_cls_property0.pw.flush();

if (0==1){}
else if (_state_id_noDuplicateAccountID==116){
		if (1==0){}
		else if ((_occurredEvent(_event,204/*createAccount*/)) && (accIds .contains (id ))){
		
		_state_id_noDuplicateAccountID = 115;//moving to state bad
		_goto_noDuplicateAccountID(_info);
		}
		else if ((_occurredEvent(_event,204/*createAccount*/)) && (!accIds .contains (id ))){
		accIds .add (id );

		_state_id_noDuplicateAccountID = 116;//moving to state start
		_goto_noDuplicateAccountID(_info);
		}
}
}

public void _goto_noDuplicateAccountID(String _info){
_cls_property0.pw.println("[noDuplicateAccountID]MOVED ON METHODCALL: "+ _info +" TO STATE::> " + _string_noDuplicateAccountID(_state_id_noDuplicateAccountID, 1));
_cls_property0.pw.flush();
}

public String _string_noDuplicateAccountID(int _state_id, int _mode){
switch(_state_id){
case 115: if (_mode == 0) return "bad"; else return "!!!SYSTEM REACHED BAD STATE!!! bad "+new _BadStateExceptionproperty().toString()+" ";
case 116: if (_mode == 0) return "start"; else return "start";
default: return "!!!SYSTEM REACHED AN UNKNOWN STATE!!!";
}
}

public boolean _occurredEvent(int[] _events, int event){
for (int i:_events) if (i == event) return true;
return false;
}
}