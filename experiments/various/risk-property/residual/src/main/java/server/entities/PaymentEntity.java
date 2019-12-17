package main.java.server.entities;

import main.java.server.entities.Entity;
import main.java.server.entities.IndividualEntity;

public abstract class PaymentEntity extends Entity {
	
	public IndividualEntity owner;
	
	public boolean paymentTo(IndividualEntity entity, Double amount){
		if(this.hasEnough(amount + this.outgoingTransactionFee(amount))){
			if(this.withdraw(amount + this.outgoingTransactionFee(amount))){
				entity.handleIncomingPayment(this, amount);
				return true;
			}
			else return false;
		}
		else return false;
	}	
	
	public abstract boolean withdraw(Double amount);

	public abstract boolean deposit(Double amount);

	protected abstract boolean reduceBalanceBy(Double amount);
	
	protected abstract boolean increaseBalanceBy(Double amount);
	
	public abstract boolean hasEnough(Double amount);
		
	public abstract double outgoingTransactionFee(double amount);
	
}
