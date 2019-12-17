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

public class _cls_orig1 implements _callable{

public static LinkedHashMap<_cls_orig1,_cls_orig1> _cls_orig1_instances = new LinkedHashMap<_cls_orig1,_cls_orig1>();

_cls_orig0 parent;
public static Boolean success;
public static UserInfo u1;
public static int cid;
public UserInfo u;
int no_automata = 1;
 public HashMap <DayOfWeek ,Double >dayDeposits =new HashMap <DayOfWeek ,Double >();

public static void initialize(){}
//inheritance could not be used because of the automatic call to super()
//when the constructor is called...we need to keep the SAME parent if this exists!

public _cls_orig1( UserInfo u) {
parent = _cls_orig0._get_cls_orig0_inst();
this.u = u;
}

public void initialisation() {
}

public static _cls_orig1 _get_cls_orig1_inst( UserInfo u) { synchronized(_cls_orig1_instances){
_cls_orig1 _inst = new _cls_orig1( u);
if (_cls_orig1_instances.containsKey(_inst))
{
_cls_orig1 tmp = _cls_orig1_instances.get(_inst);
 return _cls_orig1_instances.get(_inst);
}
else
{
 _inst.initialisation();
 _cls_orig1_instances.put(_inst,_inst);
 return _inst;
}
}
}

public boolean equals(Object o) {
 if ((o instanceof _cls_orig1)
 && (u == null || u.equals(((_cls_orig1)o).u))
 && (parent == null || parent.equals(((_cls_orig1)o).parent)))
{return true;}
else
{return false;}
}

public int hashCode() {
return 0;
}

public void _call(String _info, int... _event){
synchronized(_cls_orig1_instances){
_performLogic_riskProperty(_info, _event);
}
}

public void _call_all_filtered(String _info, int... _event){
}

public static void _call_all(String _info, int... _event){

_cls_orig1[] a = new _cls_orig1[1];
synchronized(_cls_orig1_instances){
a = _cls_orig1_instances.keySet().toArray(a);}
for (_cls_orig1 _inst : a)

if (_inst != null) _inst._call(_info, _event);
}

public void _killThis(){
try{
if (--no_automata == 0){
synchronized(_cls_orig1_instances){
_cls_orig1_instances.remove(this);}
}
else if (no_automata < 0)
{throw new Exception("no_automata < 0!!");}
}catch(Exception ex){ex.printStackTrace();}
}

int _state_id_riskProperty = 135;

public void _performLogic_riskProperty(String _info, int... _event) {

_cls_orig0.pw.println("[riskProperty]AUTOMATON::> riskProperty("+u + " " + ") STATE::>"+ _string_riskProperty(_state_id_riskProperty, 0));
_cls_orig0.pw.flush();

if (0==1){}
else if (_state_id_riskProperty==133){
		if (1==0){}
		else if ((_occurredEvent(_event,234/*payFromCard_exit*/))){
		
		_state_id_riskProperty = 131;//moving to state bad
		_goto_riskProperty(_info);
		}
		else if ((_occurredEvent(_event,236/*transferToOtherAccount_exit*/))){
		
		_state_id_riskProperty = 131;//moving to state bad
		_goto_riskProperty(_info);
		}
		else if ((_occurredEvent(_event,228/*blacklist*/)) && (risk (u )<=0.5 )){
		
		_state_id_riskProperty = 132;//moving to state blacklistedLowRisk
		_goto_riskProperty(_info);
		}
		else if ((_occurredEvent(_event,232/*whitelist*/))){
		
		_state_id_riskProperty = 134;//moving to state activated
		_goto_riskProperty(_info);
		}
		else if ((_occurredEvent(_event,230/*greylist*/))){
		
		_state_id_riskProperty = 134;//moving to state activated
		_goto_riskProperty(_info);
		}
}
else if (_state_id_riskProperty==135){
		if (1==0){}
		else if ((_occurredEvent(_event,226/*activate*/))){
		
		_state_id_riskProperty = 134;//moving to state activated
		_goto_riskProperty(_info);
		}
		else if ((_occurredEvent(_event,234/*payFromCard_exit*/))){
		
		_state_id_riskProperty = 131;//moving to state bad
		_goto_riskProperty(_info);
		}
		else if ((_occurredEvent(_event,236/*transferToOtherAccount_exit*/))){
		
		_state_id_riskProperty = 131;//moving to state bad
		_goto_riskProperty(_info);
		}
}
else if (_state_id_riskProperty==134){
		if (1==0){}
		else if ((_occurredEvent(_event,228/*blacklist*/)) && (risk (u )<=0.5 )){
		
		_state_id_riskProperty = 132;//moving to state blacklistedLowRisk
		_goto_riskProperty(_info);
		}
		else if ((_occurredEvent(_event,228/*blacklist*/)) && (risk (u )>0.5 )){
		
		_state_id_riskProperty = 133;//moving to state blacklistedHighRisk
		_goto_riskProperty(_info);
		}
}
else if (_state_id_riskProperty==132){
		if (1==0){}
		else if ((_occurredEvent(_event,234/*payFromCard_exit*/)) && ((!TransactionServer .ts .getCompanyInfo (cid ).isWhitelisted ()))){
		
		_state_id_riskProperty = 131;//moving to state bad
		_goto_riskProperty(_info);
		}
		else if ((_occurredEvent(_event,236/*transferToOtherAccount_exit*/))){
		
		_state_id_riskProperty = 131;//moving to state bad
		_goto_riskProperty(_info);
		}
		else if ((_occurredEvent(_event,228/*blacklist*/)) && (risk (u )>0.5 )){
		
		_state_id_riskProperty = 133;//moving to state blacklistedHighRisk
		_goto_riskProperty(_info);
		}
		else if ((_occurredEvent(_event,232/*whitelist*/))){
		
		_state_id_riskProperty = 134;//moving to state activated
		_goto_riskProperty(_info);
		}
		else if ((_occurredEvent(_event,230/*greylist*/))){
		
		_state_id_riskProperty = 134;//moving to state activated
		_goto_riskProperty(_info);
		}
}
}

public void _goto_riskProperty(String _info){
_cls_orig0.pw.println("[riskProperty]MOVED ON METHODCALL: "+ _info +" TO STATE::> " + _string_riskProperty(_state_id_riskProperty, 1));
_cls_orig0.pw.flush();
}

public String _string_riskProperty(int _state_id, int _mode){
switch(_state_id){
case 131: if (_mode == 0) return "bad"; else return "!!!SYSTEM REACHED BAD STATE!!! bad "+new _BadStateExceptionorig().toString()+" ";
case 133: if (_mode == 0) return "blacklistedHighRisk"; else return "blacklistedHighRisk";
case 135: if (_mode == 0) return "start"; else return "start";
case 132: if (_mode == 0) return "blacklistedLowRisk"; else return "blacklistedLowRisk";
case 134: if (_mode == 0) return "activated"; else return "activated";
default: return "!!!SYSTEM REACHED AN UNKNOWN STATE!!!";
}
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