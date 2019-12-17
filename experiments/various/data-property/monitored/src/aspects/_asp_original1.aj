package aspects;

import main.java.server.proxies.entityinfo.UserInfo;
import main.java.server.entities.users.User.PrivacyLevel;

import larva.*;
public aspect _asp_original1 {

boolean initialized = false;

after():(staticinitialization(*)){
if (!initialized){
	initialized = true;
	_cls_original1.initialize();
}
}
after ( UserInfo uu) returning (boolean success) : (call(* UserInfo.authenticate(..)) && target(uu) && !cflow(adviceexecution()) && !cflow(within(larva.*))  && !(within(larva.*))) {

synchronized(_asp_original0.lock){
UserInfo u;
u =uu ;

_cls_original1 _cls_inst = _cls_original1._get_cls_original1_inst( u);
_cls_inst.uu = uu;
_cls_inst.success = success;
_cls_inst._call(thisJoinPoint.getSignature().toString(), 192/*authenticate*/);
_cls_inst._call_all_filtered(thisJoinPoint.getSignature().toString(), 192/*authenticate*/);
}
}
after () returning (UserInfo uu) : (call(* *.createUser(..)) && !cflow(adviceexecution()) && !cflow(within(larva.*))  && !(within(larva.*))) {

synchronized(_asp_original0.lock){
UserInfo u;
u =uu ;

_cls_original1 _cls_inst = _cls_original1._get_cls_original1_inst( u);
_cls_inst.uu = uu;
_cls_inst._call(thisJoinPoint.getSignature().toString(), 194/*register*/);
_cls_inst._call_all_filtered(thisJoinPoint.getSignature().toString(), 194/*register*/);
}
}
after ( UserInfo uu) returning () : (call(* UserInfo.deregister(..)) && target(uu) && !cflow(adviceexecution()) && !cflow(within(larva.*))  && !(within(larva.*))) {

synchronized(_asp_original0.lock){
UserInfo u;
u =uu ;

_cls_original1 _cls_inst = _cls_original1._get_cls_original1_inst( u);
_cls_inst.uu = uu;
_cls_inst._call(thisJoinPoint.getSignature().toString(), 198/*exitingDeregister*/);
_cls_inst._call_all_filtered(thisJoinPoint.getSignature().toString(), 198/*exitingDeregister*/);
}
}
before ( UserInfo uu) : (call(* UserInfo.sanitizeInfo(..)) && target(uu) && !cflow(adviceexecution()) && !cflow(within(larva.*))  && !(within(larva.*))) {

synchronized(_asp_original0.lock){
UserInfo u;
u =uu ;

_cls_original1 _cls_inst = _cls_original1._get_cls_original1_inst( u);
_cls_inst.uu = uu;
_cls_inst._call(thisJoinPoint.getSignature().toString(), 200/*sanitize*/);
_cls_inst._call_all_filtered(thisJoinPoint.getSignature().toString(), 200/*sanitize*/);
}
}
before ( UserInfo uu) : (call(* *.askAdmin(..)) && args(uu) && !cflow(adviceexecution()) && !cflow(within(larva.*))  && !(within(larva.*))) {

synchronized(_asp_original0.lock){
UserInfo u;
u =uu ;

_cls_original1 _cls_inst = _cls_original1._get_cls_original1_inst( u);
_cls_inst.uu = uu;
_cls_inst._call(thisJoinPoint.getSignature().toString(), 202/*infoLossEvent*/);
_cls_inst._call_all_filtered(thisJoinPoint.getSignature().toString(), 202/*infoLossEvent*/);
}
}
before ( UserInfo uu,double value) : (call(* *.payToCompanyFromCard(..)) && args(uu,*,*,*,value) && !cflow(adviceexecution()) && !cflow(within(larva.*))  && !(within(larva.*))) {

synchronized(_asp_original0.lock){
UserInfo u;
u =uu ;

_cls_original1 _cls_inst = _cls_original1._get_cls_original1_inst( u);
_cls_inst.uu = uu;
_cls_inst.value = value;
_cls_inst._call(thisJoinPoint.getSignature().toString(), 190/*transact*/);
_cls_inst._call_all_filtered(thisJoinPoint.getSignature().toString(), 190/*transact*/);
}
}
before ( UserInfo uu) : (call(* UserInfo.deregister(..)) && target(uu) && !cflow(adviceexecution()) && !cflow(within(larva.*))  && !(within(larva.*))) {

synchronized(_asp_original0.lock){
UserInfo u;
u =uu ;

_cls_original1 _cls_inst = _cls_original1._get_cls_original1_inst( u);
_cls_inst.uu = uu;
_cls_inst._call(thisJoinPoint.getSignature().toString(), 196/*enterDeregister*/);
_cls_inst._call_all_filtered(thisJoinPoint.getSignature().toString(), 196/*enterDeregister*/);
}
}
}