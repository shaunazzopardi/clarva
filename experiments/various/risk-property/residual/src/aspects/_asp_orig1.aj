package aspects;

import main.java.server.*;
import main.java.server.proxies.entityinfo.UserInfo;
import main.java.paymentapp.*;
import main.java.server.entities.companies.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.time.DayOfWeek;

import larva.*;
public aspect _asp_orig1 {

boolean initialized = false;

after():(staticinitialization(*)){
if (!initialized){
	initialized = true;
	_cls_orig1.initialize();
}
}
after ( UserInfo u1) returning (Boolean success) : (call(* *.checkIfActivated(..)) && args(u1) && !cflow(adviceexecution()) && !cflow(within(larva.*))  && !(within(larva.*))) {

synchronized(_asp_orig0.lock){
UserInfo u;
u =u1 ;

_cls_orig1 _cls_inst = _cls_orig1._get_cls_orig1_inst( u);
_cls_inst.success = success;
_cls_inst.u1 = u1;
_cls_inst._call(thisJoinPoint.getSignature().toString(), 238/*activate*/);
_cls_inst._call_all_filtered(thisJoinPoint.getSignature().toString(), 238/*activate*/);
}
}
after ( UserInfo u1,int cid) returning (Boolean success) : (call(* *.payToCompanyFromCard(..)) && args(u1,cid,*,*,*) && !cflow(adviceexecution()) && !cflow(within(larva.*))  && !(within(larva.*))) {

synchronized(_asp_orig0.lock){
UserInfo u;
u =u1 ;

_cls_orig1 _cls_inst = _cls_orig1._get_cls_orig1_inst( u);
_cls_inst.success = success;
_cls_inst.u1 = u1;
_cls_inst.cid = cid;
_cls_inst._call(thisJoinPoint.getSignature().toString(), 240/*payFromCard_exit*/);
_cls_inst._call_all_filtered(thisJoinPoint.getSignature().toString(), 240/*payFromCard_exit*/);
}
}
}