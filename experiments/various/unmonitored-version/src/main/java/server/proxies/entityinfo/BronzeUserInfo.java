package main.java.server.proxies.entityinfo;

import java.util.ArrayList;
import java.util.List;

import main.java.server.entities.users.User;
import main.java.server.proxies.entityinfo.UserInfo;

public class BronzeUserInfo extends UserInfo {

	public BronzeUserInfo(User user){
	super(user);
	}
	
	public BronzeUserInfo(int id){
		super(id);
	}	
	
}
