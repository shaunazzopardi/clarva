package larva;


import main.java.server.proxies.entityinfo.UserInfo;
import main.java.server.entities.users.User.PrivacyLevel;

import java.util.LinkedHashMap;
import java.io.PrintWriter;

public class _cls_original0 implements _callable{

public static PrintWriter pw; 
public static _cls_original0 root;

public static LinkedHashMap<_cls_original0,_cls_original0> _cls_original0_instances = new LinkedHashMap<_cls_original0,_cls_original0>();
static{
try{
RunningClock.start();
pw = new PrintWriter("./output_original.txt");

root = new _cls_original0();
_cls_original0_instances.put(root, root);
  root.initialisation();
}catch(Exception ex)
{ex.printStackTrace();}
}

_cls_original0 parent; //to remain null - this class does not have a parent!
int no_automata = 0;

public static void initialize(){}
//inheritance could not be used because of the automatic call to super()
//when the constructor is called...we need to keep the SAME parent if this exists!

public _cls_original0() {
}

public void initialisation() {
}

public static _cls_original0 _get_cls_original0_inst() { synchronized(_cls_original0_instances){
 return root;
}
}

public boolean equals(Object o) {
 if ((o instanceof _cls_original0))
{return true;}
else
{return false;}
}

public int hashCode() {
return 0;
}

public void _call(String _info, int... _event){
synchronized(_cls_original0_instances){
}
}

public void _call_all_filtered(String _info, int... _event){

_cls_original1[] a1 = new _cls_original1[1];
synchronized(_cls_original1._cls_original1_instances){
a1 = _cls_original1._cls_original1_instances.keySet().toArray(a1);}
for (_cls_original1 _inst : a1)
if (_inst != null){
_inst._call(_info, _event); 
_inst._call_all_filtered(_info, _event);
}
}

public static void _call_all(String _info, int... _event){

_cls_original0[] a = new _cls_original0[1];
synchronized(_cls_original0_instances){
a = _cls_original0_instances.keySet().toArray(a);}
for (_cls_original0 _inst : a)

if (_inst != null) _inst._call(_info, _event);
}

public void _killThis(){
try{
if (--no_automata == 0){
synchronized(_cls_original0_instances){
_cls_original0_instances.remove(this);}
}
else if (no_automata < 0)
{throw new Exception("no_automata < 0!!");}
}catch(Exception ex){ex.printStackTrace();}
}


public boolean _occurredEvent(int[] _events, int event){
for (int i:_events) if (i == event) return true;
return false;
}
}