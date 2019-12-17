package main.java.server.proxies.entityinfo;

import java.util.ArrayList;
import java.util.List;

import main.java.server.entities.users.User;
import main.java.server.proxies.entityinfo.UserInfo;

public class SilverUserInfo extends UserInfo {
	
	public SilverUserInfo(User user){
		super(user);
	}
	
	public SilverUserInfo(int id){
		super(id);
	}
	
}
