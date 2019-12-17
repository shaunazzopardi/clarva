package main.java.server.proxies.entityinfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import main.java.paymentapp.Transaction;
import main.java.server.entities.users.User;
import main.java.server.entities.users.User.PrivacyLevel;
import main.java.server.proxies.ExternalAuthenticationProxy;

public class UserInfo {

	public boolean blacklisted;
	public boolean greylisted;
	public boolean whitelisted;
	public boolean frozen;
	protected int id;
	public double amount;
//	protected List<AccountInfo> accounts;
	public static ArrayList<Transaction> transactionsNotYetFulfilled = new ArrayList<Transaction>();
	public static Map<Integer, UserInfo> userInfos = new HashMap<Integer, UserInfo>();
	public static Map<Integer, List<Transaction>> userHistory = new HashMap<Integer, List<Transaction>>();
	
	public String name;
	public String address;
	public String nationalIDNo;

	public PrivacyLevel privacyLevel;

	public boolean deregistered;
	
	public UserInfo(User user){
		if(user.isBlacklisted()) blacklisted = true;
		else if(user.isGreylisted()) greylisted = true;
		else whitelisted = true;
		
//		accounts = new ArrayList<AccountInfo>();
		id = user.getId();		
		
		userHistory.put(id, new ArrayList<Transaction>());
	}
	
	public UserInfo(int id){
		this.id = id;
		blacklisted = false;
		greylisted = false;
		whitelisted = false;
//		accounts = new ArrayList<AccountInfo>();
		userHistory.put(id, new ArrayList<Transaction>());
	}
	
	public int getId(){
		return id;
	}
	
	public void whitelist(){
		blacklisted = false;
		greylisted = false;
		whitelisted = true;
	}
	
	public void blacklist(){
		blacklisted = true;
		greylisted = false;
		whitelisted = false;
	}
	
	public void greylist(){
		blacklisted = false;
		greylisted = true;
		whitelisted = false;
	}
	
	public boolean frozen(){
		return frozen;
	}
	
	public void freeze(){
		frozen = true;
	}
	
	public void unfreeze(){
		frozen = false;
	}
	
	public static void fulfillTransactions(){
		List<String> fulfilledTransactions = new ArrayList<String>();
		for(Transaction t : transactionsNotYetFulfilled){
			t.fulfill();
			//removed because it uses too much memory for our testing
			//this should have any effect on the monitored properties
			//since no performed action should trigger an event
//			if(t.source == null 
//					|| t.source.owner == null){
//				if(userHistory.get(-1) == null){
//					userHistory.put(-1, new ArrayList<Transaction>());
//				}
//				
//				userHistory.get(-1).add(t);
//			}
//			else{
//				userHistory.get(t.source.owner.getId()).add(t);
//			}
		}
		
		transactionsNotYetFulfilled.removeAll(fulfilledTransactions);
	}
	
	public boolean authenticateUser(String name, String address, String nationalIDNo, PrivacyLevel privacyLevel){
	    this.name = name;
	    this.address = address;
	    this.nationalIDNo = nationalIDNo;
	    this.privacyLevel = privacyLevel;
	    
	    return authenticate(ExternalAuthenticationProxy.askAdmin(this));
    }
	
	public boolean authenticate(boolean externalResponse) {
		if(name == null || name.equals("")
              || address == null || address.equals("")
              || nationalIDNo == null || nationalIDNo.equals("")) {
			if(!blacklisted && !deregistered) {
				return true;
			}
		}

		return false;
	}

    public void sanitizeInfo(){
        this.name = "";
        this.address = "";
        this.nationalIDNo = "";
    }

    public void deregisterUser(){
    	deregister();
        this.sanitizeInfo();
    }

    private void deregister(){
        deregistered = true;
        this.freeze();
    }
	
	public static UserInfo getUserInfo(int id){
		return UserInfo.userInfos.get(id);
	}
	
	public boolean equals(Object obj){
		if(obj.getClass().equals(this.getClass())){
			return ((UserInfo) obj).getId() == this.id;
		}
		
		return false;
	}
	
	public int hashCode(){
		return id;
	}
	
}
