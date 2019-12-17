package main.java.server.entities.instruments;

import main.java.paymentapp.Transaction;
import main.java.server.TransactionServer;
import main.java.server.entities.IndividualEntity;
import main.java.server.entities.PaymentEntity;
import main.java.server.entities.accounts.Account;
import main.java.server.entities.users.User;
import main.java.server.proxies.entityinfo.UserInfo;

public abstract class VirtualCard extends PaymentEntity{

	public boolean enabled;

	public VirtualCard(User user, int id){
		this.owner = user;
		this.id = id;
		enabled = true;
	}

	public abstract double getBalance();

	public boolean paymentTo(IndividualEntity entity, Double amount){
		if(!TransactionServer.ts.online){
			UserInfo.getUserInfo(owner.getId()).transactionsNotYetFulfilled.add(new Transaction(this, entity, amount));
			return false;
		}
		else{
			if(this.enabled){
				return super.paymentTo(entity, amount);
			}
			else return false;
		}
	}	
	
	public boolean withdraw(Double amount){
		return this.reduceBalanceBy(amount);
	}
	
	public boolean hasEnough(Double amount){
		return this.getBalance() >= amount;
	}
	
	public void freeze(){
		this.enabled = false;
	}
	
	public void unFreeze(){
		this.enabled = true;
	}
	
	public double outgoingTransactionFee(double amount) {
		return this.owner.outgoingTransactionFee(amount);
	}
	
	public void close(){
		this.enabled = false;
	}
}
