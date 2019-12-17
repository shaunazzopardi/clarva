package aspects;

import main.java.server.*;
import main.java.server.proxies.entityinfo.UserInfo;
import main.java.paymentapp.*;
import java.util.*;

import larva.*;
public aspect _asp_property0 {

public static Object lock = new Object();

boolean initialized = false;

after():(staticinitialization(*)){
if (!initialized){
	initialized = true;
	_cls_property0.initialize();
}
}
after () returning (int id) : (call(* *.createAccount(..)) && !cflow(adviceexecution()) && !cflow(within(larva.*))  && !(within(larva.*))) {

synchronized(_asp_property0.lock){

_cls_property0 _cls_inst = _cls_property0._get_cls_property0_inst();
_cls_inst.id = id;
_cls_inst._call(thisJoinPoint.getSignature().toString(), 204/*createAccount*/);
_cls_inst._call_all_filtered(thisJoinPoint.getSignature().toString(), 204/*createAccount*/);
}
}
}