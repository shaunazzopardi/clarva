package larva;


import main.java.server.*;
import main.java.server.proxies.entityinfo.UserInfo;
import main.java.paymentapp.*;
import main.java.server.entities.companies.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.time.DayOfWeek;

import java.util.LinkedHashMap;
import java.io.PrintWriter;

public class _cls_orig0 implements _callable{

public static PrintWriter pw; 
public static _cls_orig0 root;

public static LinkedHashMap<_cls_orig0,_cls_orig0> _cls_orig0_instances = new LinkedHashMap<_cls_orig0,_cls_orig0>();
static{
try{
RunningClock.start();
pw = new PrintWriter("./output_orig.txt");

root = new _cls_orig0();
_cls_orig0_instances.put(root, root);
  root.initialisation();
}catch(Exception ex)
{ex.printStackTrace();}
}

_cls_orig0 parent; //to remain null - this class does not have a parent!
int no_automata = 0;
 public Map <Integer ,Integer >transCheckedButNotTaken =new HashMap <Integer ,Integer >();
 public Map <Integer ,Integer >transCheckedAndTaken =new HashMap <Integer ,Integer >();

public static void initialize(){}
//inheritance could not be used because of the automatic call to super()
//when the constructor is called...we need to keep the SAME parent if this exists!

public _cls_orig0() {
}

public void initialisation() {
}

public static _cls_orig0 _get_cls_orig0_inst() { synchronized(_cls_orig0_instances){
 return root;
}
}

public boolean equals(Object o) {
 if ((o instanceof _cls_orig0))
{return true;}
else
{return false;}
}

public int hashCode() {
return 0;
}

public void _call(String _info, int... _event){
synchronized(_cls_orig0_instances){
}
}

public void _call_all_filtered(String _info, int... _event){

_cls_orig1[] a1 = new _cls_orig1[1];
synchronized(_cls_orig1._cls_orig1_instances){
a1 = _cls_orig1._cls_orig1_instances.keySet().toArray(a1);}
for (_cls_orig1 _inst : a1)
if (_inst != null){
_inst._call(_info, _event); 
_inst._call_all_filtered(_info, _event);
}
}

public static void _call_all(String _info, int... _event){

_cls_orig0[] a = new _cls_orig0[1];
synchronized(_cls_orig0_instances){
a = _cls_orig0_instances.keySet().toArray(a);}
for (_cls_orig0 _inst : a)

if (_inst != null) _inst._call(_info, _event);
}

public void _killThis(){
try{
if (--no_automata == 0){
synchronized(_cls_orig0_instances){
_cls_orig0_instances.remove(this);}
}
else if (no_automata < 0)
{throw new Exception("no_automata < 0!!");}
}catch(Exception ex){ex.printStackTrace();}
}


public boolean _occurredEvent(int[] _events, int event){
for (int i:_events) if (i == event) return true;
return false;
}


double risk(UserInfo u){
int noOfSuspiciousTransactions = 0;
List<Transaction> userHistory = UserInfo.userHistory.get(u.getId());
for(Transaction t : userHistory){
if(t.destination.isBlacklisted()){
noOfSuspiciousTransactions++;

if(t.destination.getClass().equals(Company.class)){
for(UserInfo ui : ((Company) t.destination).usersTransactingWithCompany){
if(ui.blacklisted) noOfSuspiciousTransactions += 0.1;
}
}
}
else if(t.destination.isGreylisted()) noOfSuspiciousTransactions += 0.5;
}

return userHistory.size() > 0 ? noOfSuspiciousTransactions > userHistory.size() ? noOfSuspiciousTransactions/userHistory.size() : 1 : 0;
}
}