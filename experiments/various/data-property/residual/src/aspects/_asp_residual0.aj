package aspects;

import main.java.server.proxies.entityinfo.UserInfo;
import main.java.server.entities.users.User.PrivacyLevel;

import larva.*;
public aspect _asp_residual0 {

public static Object lock = new Object();

boolean initialized = false;

after():(staticinitialization(*)){
if (!initialized){
	initialized = true;
	_cls_residual0.initialize();
}
}
}