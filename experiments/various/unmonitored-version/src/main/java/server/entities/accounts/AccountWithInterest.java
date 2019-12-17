package main.java.server.entities.accounts;

import main.java.server.entities.accounts.Account;

public class AccountWithInterest extends Account {

	public double interest;
	
	public AccountWithInterest(Integer uid, Integer anumber, Double credit, Double limit, Double interest) {
		super(uid, anumber, credit, limit);
		this.interest = interest;
	}

	public AccountWithInterest(Integer uid, Integer anumber, Double credit, Double interest) {
		super(uid, anumber, credit);
		this.interest = interest;
	}
	
	public AccountWithInterest(Integer uid, Integer anumber, Double interest) {
		super(uid, anumber);
		this.interest = interest;
	}

	public void endOfYear(){
		if(this.balance > 0
				&& this.interest > 0){
			this.balance *= 1 + interest;
		}
	}
	
}
