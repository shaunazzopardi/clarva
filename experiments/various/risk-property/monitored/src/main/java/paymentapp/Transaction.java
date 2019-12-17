package main.java.paymentapp;

import main.java.server.TransactionServer;
import main.java.server.entities.IndividualEntity;
import main.java.server.entities.PaymentEntity;

public class Transaction {

	public PaymentEntity source;
	
	public IndividualEntity destination;
	
	public double amount;
	
	public boolean fulfilled;
	
	public boolean success;
	
	public Transaction(PaymentEntity source, IndividualEntity destination, double amount){
		this.source = source;
		this.destination = destination;
		this.amount = amount;
		this.fulfilled = false;
	}
	
	public boolean fulfill(){
		if(!this.fulfilled && TransactionServer.ts.isOnline()){
			this.fulfilled = true;
			success = source.paymentTo(destination, amount);
			return success;
		}
		else return false;
	}
	
	public boolean isFulfilled(){
		return this.fulfilled;
	}
}
