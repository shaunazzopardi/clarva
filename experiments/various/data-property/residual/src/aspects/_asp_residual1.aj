package aspects;

import main.java.server.proxies.entityinfo.UserInfo;
import main.java.server.entities.users.User.PrivacyLevel;

import larva.*;
public aspect _asp_residual1 {

boolean initialized = false;

after():(staticinitialization(*)){
if (!initialized){
	initialized = true;
	_cls_residual1.initialize();
}
}
after ( UserInfo uu) returning (boolean success) : (call(* UserInfo.authenticate(..)) && target(uu) && !cflow(adviceexecution()) && !cflow(within(larva.*))  && !(within(larva.*))) {

synchronized(_asp_residual0.lock){
UserInfo u;
u =uu ;

_cls_residual1 _cls_inst = _cls_residual1._get_cls_residual1_inst( u);
_cls_inst.uu = uu;
_cls_inst.success = success;
_cls_inst._call(thisJoinPoint.getSignature().toString(), 170/*authenticate*/);
_cls_inst._call_all_filtered(thisJoinPoint.getSignature().toString(), 170/*authenticate*/);
}
}
after () returning (UserInfo uu) : (call(* *.createUser(..)) && !cflow(adviceexecution()) && !cflow(within(larva.*))  && !(within(larva.*))) {

synchronized(_asp_residual0.lock){
UserInfo u;
u =uu ;

_cls_residual1 _cls_inst = _cls_residual1._get_cls_residual1_inst( u);
_cls_inst.uu = uu;
_cls_inst._call(thisJoinPoint.getSignature().toString(), 172/*register*/);
_cls_inst._call_all_filtered(thisJoinPoint.getSignature().toString(), 172/*register*/);
}
}
before ( UserInfo uu,double value) : (call(* *.payToCompanyFromCard(..)) && args(uu,*,*,*,value) && !cflow(adviceexecution()) && !cflow(within(larva.*))  && !(within(larva.*))) {

synchronized(_asp_residual0.lock){
UserInfo u;
u =uu ;

_cls_residual1 _cls_inst = _cls_residual1._get_cls_residual1_inst( u);
_cls_inst.uu = uu;
_cls_inst.value = value;
_cls_inst._call(thisJoinPoint.getSignature().toString(), 168/*transact*/);
_cls_inst._call_all_filtered(thisJoinPoint.getSignature().toString(), 168/*transact*/);
}
}
before ( UserInfo uu) : (call(* UserInfo.deregister(..)) && target(uu) && !cflow(adviceexecution()) && !cflow(within(larva.*))  && !(within(larva.*))) {

synchronized(_asp_residual0.lock){
UserInfo u;
u =uu ;

_cls_residual1 _cls_inst = _cls_residual1._get_cls_residual1_inst( u);
_cls_inst.uu = uu;
_cls_inst._call(thisJoinPoint.getSignature().toString(), 174/*enterDeregister*/);
_cls_inst._call_all_filtered(thisJoinPoint.getSignature().toString(), 174/*enterDeregister*/);
}
}
}