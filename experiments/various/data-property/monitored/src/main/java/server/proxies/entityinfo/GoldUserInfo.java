package main.java.server.proxies.entityinfo;

import java.util.ArrayList;
import java.util.List;

import main.java.server.entities.users.User;
import main.java.server.proxies.entityinfo.UserInfo;

public class GoldUserInfo extends UserInfo {

	public GoldUserInfo(User user){
		super(user);
	}
	
	public GoldUserInfo(int id){
		super(id);
	}
	
	@Override
	public void blacklist(){
		whitelisted = true;
	}
	
	@Override
	public void greylist(){
		whitelisted = true;
	}
}
