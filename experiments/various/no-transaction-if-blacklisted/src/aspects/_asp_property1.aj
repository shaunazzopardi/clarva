package aspects;

import main.java.server.*;
import main.java.server.proxies.entityinfo.UserInfo;
import main.java.paymentapp.*;

import larva.*;
public aspect _asp_property1 {

boolean initialized = false;

after():(staticinitialization(*)){
if (!initialized){
	initialized = true;
	_cls_property1.initialize();
}
}
after ( UserInfo u1,int cid) returning (Boolean success) : (call(* *.payToCompanyFromCard(..)) && args(u1,cid,*,*,*) && !cflow(adviceexecution()) && !cflow(within(larva.*))  && !(within(larva.*))) {

synchronized(_asp_property0.lock){
UserInfo u;
u =u1 ;

_cls_property1 _cls_inst = _cls_property1._get_cls_property1_inst( u);
_cls_inst.success = success;
_cls_inst.u1 = u1;
_cls_inst.cid = cid;
_cls_inst._call(thisJoinPoint.getSignature().toString(), 224/*payFromCard_exit*/);
_cls_inst._call_all_filtered(thisJoinPoint.getSignature().toString(), 224/*payFromCard_exit*/);
}
}
}