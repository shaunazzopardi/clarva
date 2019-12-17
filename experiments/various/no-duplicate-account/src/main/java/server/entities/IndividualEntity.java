package main.java.server.entities;

import java.util.ArrayList;

import main.java.server.entities.PaymentEntity;
import main.java.server.entities.accounts.Account;

public abstract class IndividualEntity extends PaymentEntity {

	public String name;
	public String country;
	protected ArrayList<Account> accounts;
	public Account defaultIncomingAccount;
	public Account defaultOutgoingAccount;
	public UserStatus status;

	public enum UserStatus {
		WHITELISTED, GREYLISTED, BLACKLISTED;
	}
	
	public boolean isWhitelisted() { return (status==UserStatus.WHITELISTED); }
	public boolean isGreylisted() { return (status==UserStatus.GREYLISTED); }
	public boolean isBlacklisted() { return (status==UserStatus.BLACKLISTED); }
	
	public void blacklist() 
	{ 
		status=UserStatus.BLACKLISTED; 
	}
	public void greylist() 
	{
		status=UserStatus.GREYLISTED; 
	}
	public void whitelist()
	{ 
		status=UserStatus.WHITELISTED; 
	}

	protected enum UserMode {
		ACTIVE, DISABLED, FROZEN;
	}

	public UserMode mode;
	public boolean isActive() { return (mode==UserMode.ACTIVE); }
	public boolean isFrozen() { return (mode==UserMode.FROZEN); }
	public boolean isDisabled() { return (mode==UserMode.DISABLED); }

	// Mode

	public void makeActive() 
	{ 
		mode=UserMode.ACTIVE; 
	}
	public void makeFrozen() 
	{ 
		mode=UserMode.FROZEN; 
	}
	public void makeDisabled() 
	{ 
		mode=UserMode.DISABLED; 
	}
	public void makeUnfrozen() 
	{ 
		mode=UserMode.ACTIVE; 
	}
	public IndividualEntity(Integer id, String name, String country){
		this.id = id;
		this.name = name;
		this.country = country;
		accounts = new ArrayList<Account>();
	}
	
	public boolean setDefaultIncomingAccount(Account account){
		if(accounts.contains(account)){
			this.defaultIncomingAccount = account;
			return true;
		}
		else return false;
	}
	
	public boolean setDefaultOutgoingAccount(Account account){
		if(accounts.contains(account)){
			this.defaultOutgoingAccount = account;
			return true;
		}
		else return false;
	}
	
	public ArrayList<Account> getAccounts(){
		return accounts;
	}
	
	public int createAccount(Integer uid, Integer anumber, Double credit, Double limit){
		Account acc = new Account(uid, anumber, credit, limit);
		accounts.add(acc);
		return acc.getAccountNumber();
	}
	
	public boolean handleIncomingPayment(PaymentEntity entity, Double amount){
		if(this.defaultIncomingAccount != null){
			if(this.defaultIncomingAccount.balance + this.defaultIncomingAccount.credit >= amount){
				this.defaultIncomingAccount.deposit(amount);
				return true;
			}
			else{
				return false;
			}
		}
		else{
			return false;
		}
	}
	
	public boolean withdraw(Double amount){
		return this.reduceBalanceBy(amount);
	}
	
	public boolean deposit(Double amount){
		return this.increaseBalanceBy(amount);
	}
	
	@Override
	protected boolean reduceBalanceBy(Double amount){
		if(this.defaultOutgoingAccount == null) return false;
		else{
			return this.defaultOutgoingAccount.withdraw(amount);
		}
	}
	
	@Override
	protected boolean increaseBalanceBy(Double amount){
		if(this.defaultIncomingAccount == null) return false;
		else{
			return this.defaultIncomingAccount.deposit(amount);
		}
	}
	
	public boolean hasEnough(Double amount){
		if(this.defaultOutgoingAccount == null) return false;
		else{
			return this.defaultOutgoingAccount.balance + this.defaultOutgoingAccount.credit > amount;
		}
	}
	
	public abstract double incomingTransactionFee(double amount);


}
