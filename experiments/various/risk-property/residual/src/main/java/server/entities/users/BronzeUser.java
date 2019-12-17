package main.java.server.entities.users;

import main.java.server.entities.users.User;
import main.java.server.proxies.entityinfo.BronzeUserInfo;
import main.java.server.proxies.entityinfo.SilverUserInfo;
import main.java.server.proxies.entityinfo.UserInfo;

public class BronzeUser extends User {

	public BronzeUser(Integer uid, String name, String country) {
		super(uid, name, country);
		UserInfo.userInfos.put(uid, new BronzeUserInfo(uid));
	}

	@Override
	public boolean isGoldUser() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSilverUser() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isNormalUser() {
		// TODO Auto-generated method stub
		return true;
	}

	public double incomingTransactionFee(double amount){
		return 2*amount/100;
	}

	@Override
	public double outgoingTransactionFee(double amount) {
		// TODO Auto-generated method stub
		return 2*amount/100;
	}
}
