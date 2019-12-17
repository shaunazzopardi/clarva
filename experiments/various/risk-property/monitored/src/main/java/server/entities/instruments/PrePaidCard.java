package main.java.server.entities.instruments;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;

import main.java.server.entities.IndividualEntity;
import main.java.server.entities.instruments.VirtualCard;
import main.java.server.entities.users.User;;

public abstract class PrePaidCard extends VirtualCard {

	protected boolean expired;
//	protected LocalDateTime expiryDate;
	public Double balance;
	
	public PrePaidCard(User user, int id, double loadedMoney){
		super(user, id);
		this.id = id;
		this.balance = loadedMoney;		
		
//		this.expiryDate = LocalDateTime.now().plusDays(365);
	}
	
	public PrePaidCard(User user, int id){
		super(user, id);
		this.id = id;
		this.balance = 0.0;
		
//		this.expiryDate = LocalDateTime.now().plusDays(365);
	}
	
	public boolean paymentTo(IndividualEntity entity, Double amount){
		if(this.enabled){
			return super.paymentTo(entity, amount);
		}
		else return false;
	}	

	public boolean deposit(Double amount){
//		if(this.expiryDate.isEqual(LocalDateTime.now())
//				|| this.expiryDate.isBefore(LocalDateTime.now())){
//			this.expired();
//			return false;
//		}
//		else{
			return this.increaseBalanceBy(amount);
//		}
	}
	
	public void expired(){
		this.expired = true;
	}
	
	@Override
	protected boolean reduceBalanceBy(Double amount) {
		this.balance -= amount;
		return true;
	}


	@Override
	protected boolean increaseBalanceBy(Double amount) {
		this.balance += amount;
		return true;
	}
	
	public double getBalance(){
		return this.balance;
	}
}
