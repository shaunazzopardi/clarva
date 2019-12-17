package main.java.server.interfaces;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import main.java.adminapp.RandomResponsiveAdmin;
import main.java.server.TransactionServer;
import main.java.server.entities.accounts.Account;
import main.java.server.entities.users.User;

public class AdminInterface {
	public static AdminInterface adminInterface;
	public RandomResponsiveAdmin admin;
	
	public Map<User,Map<Integer,Account>> accountsToApprove = new HashMap<User,Map<Integer,Account>>();;
	public Map<Integer,User> usersToActivate = new HashMap<Integer,User>();;

	public AdminInterface(TransactionServer ts){
		TransactionServer.ts = ts;
	}
	
	public void userCreationRequest(int uid, User user){
//		if(admin.yesOrNo()){
			this.activateUser(uid);
			
//			if(admin.yesOrNo()){
//				if(admin.yesOrNo()){
//					this.adminInterface.setGoldUser(uid);
//				}
//				else{
//					this.adminInterface.setSilverUser(uid);
//				}
//			}
//		}
//		else usersToActivate.put(uid, user);
		
		this.accountsToApprove.put(user, new HashMap<Integer,Account>());
	}
	
	public void activateUser(Integer uid){	
		User user = TransactionServer.ts.getUserInfo(uid);
		if(user != null){
			user.makeActive();
				
//			int typeOfUser = admin.rand.nextInt(7);
//			if(typeOfUser == 0) this.setGoldUser(uid);
//			else if(typeOfUser < 3) this.setSilverUser(uid);
			
			usersToActivate.remove(uid);
		}
	}
	
	public Collection<User> usersToActivate(){
		return usersToActivate.values();
	}
	
	public void disableUser(int uid){
		if(TransactionServer.ts.getUserInfo(uid) != null) TransactionServer.ts.getUserInfo(uid).makeDisabled();		
	}
	
	public Collection<Account> accountsToApprove(){
		List<Account> accounts = new ArrayList<Account>();
		for(User user : accountsToApprove.keySet()){
			accounts.addAll(accountsToApprove.get(user).values());
		}
			
		return accounts;
	}
	
	public void approveAccount(Integer uid, Integer acc_no){
		accountsToApprove.get(uid).get(acc_no).activateAccount();
	}

	public void accountApprovalRequest(Integer uid, Integer acc_no){
		
//		if(admin.yesOrNo()){
			TransactionServer.ts.getUserInfo(uid).getAccount(acc_no).activateAccount();
//		}
//		else{
//			Map<Integer, Account> accounts = accountsToApprove.get(uid);
//			
//			User user = TransactionServer.ts.getUserInfo(uid);
//	
//			if(accounts == null && user != null){
//				accounts = new HashMap<Integer, Account>();
//				accountsToApprove.put(user, accounts);
//				accounts.put(acc_no, TransactionServer.ts.getUserInfo(uid).getAccount(acc_no));
//			}
//		}
	}
	
	public void maybeBlacklist(int uid){
		if(admin.yesOrNo()){
			this.blacklistUser(uid);
		}
	}
	
	public void maybeGreylist(int uid){
		if(admin.yesOrNo()) this.greylistUser(uid);
	}
	
	public void maybeWhitelist(int uid){
		if(admin.yesOrNo()) this.whitelistUser(uid);
	}
	
//	public void maybeGold(int uid){
//		if(admin.yesOrNo()) this.setGoldUser(uid);
//	}
//	
//	public void maybeSilver(int uid){
//		if(admin.yesOrNo()) this.setSilverUser(uid);
//	}
	
	public void blacklistUser(int uid){
		if(TransactionServer.ts.getUserInfo(uid) != null) TransactionServer.ts.getUserInfo(uid).blacklist();
	}
	
	public void greylistUser(int uid){
		if(TransactionServer.ts.getUserInfo(uid) != null) TransactionServer.ts.getUserInfo(uid).greylist();
	}
	
	public void whitelistUser(int uid){
		if(TransactionServer.ts.getUserInfo(uid) != null) TransactionServer.ts.getUserInfo(uid).whitelist();
	}
//	
//	public void setGoldUser(int uid){
//		if(TransactionServer.ts.getUserInfo(uid) != null) TransactionServer.ts.getUserInfo(uid).makeGoldUser();;
//	}
//	
//	public void setSilverUser(int uid){
//		if(TransactionServer.ts.getUserInfo(uid) != null) TransactionServer.ts.getUserInfo(uid).makeSilverUser();;
//	}
	
	public boolean isBlacklistUser(int uid){
		if(TransactionServer.ts.getUserInfo(uid) == null) return false; else return TransactionServer.ts.getUserInfo(uid).isBlacklisted();
	}
	
	public boolean isGreylistUser(int uid){
		if(TransactionServer.ts.getUserInfo(uid) == null) return false; else return TransactionServer.ts.getUserInfo(uid).isGreylisted();
	}
	
	public boolean isWhitelistUser(int uid){
		if(TransactionServer.ts.getUserInfo(uid) == null) return false; else return TransactionServer.ts.getUserInfo(uid).isWhitelisted();
	}
	
	public void adminsTurn(int uid){
		int randInt = admin.rand.nextInt(40);
		
		boolean doThis = true;
		
		if(TransactionServer.ts.getUserInfo(uid).isBlacklisted()){
			doThis = admin.rand.nextBoolean() && admin.rand.nextBoolean();
		}
		
		if(doThis){
			if(!TransactionServer.ts.getUserInfo(uid).isWhitelisted()
					&& randInt < 1){
				this.whitelistUser(uid);
			}
			else if(!TransactionServer.ts.getUserInfo(uid).isGreylisted()
					&& randInt < 20){
				this.greylistUser(uid);
			}
			else if(!TransactionServer.ts.getUserInfo(uid).isBlacklisted()
					&& randInt < 30){
				this.blacklistUser(uid);
			}
		}
//		else if(randInt < 20){
//			this.maybeGold(uid);
//		}
//		else //if(randInt < 25)
//			{
//			this.maybeSilver(uid);
//		}
		
		
	}
}
