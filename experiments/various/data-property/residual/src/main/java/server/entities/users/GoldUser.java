package main.java.server.entities.users;

import main.java.server.entities.users.User;
import main.java.server.proxies.entityinfo.GoldUserInfo;
import main.java.server.proxies.entityinfo.SilverUserInfo;
import main.java.server.proxies.entityinfo.UserInfo;

public class GoldUser extends User {

	public GoldUser(Integer uid, String name, String country) {
		super(uid, name, country);
		// TODO Auto-generated constructor stub
		UserInfo.userInfos.put(uid, new GoldUserInfo(uid));
	}

	@Override
	public boolean isGoldUser() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isSilverUser() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isNormalUser() {
		// TODO Auto-generated method stub
		return false;
	}

	public double incomingTransactionFee(double amount){
		return 1*amount/100;
	}

	@Override
	public double outgoingTransactionFee(double amount) {
		// TODO Auto-generated method stub
		return 1*amount/100;
	}
}
