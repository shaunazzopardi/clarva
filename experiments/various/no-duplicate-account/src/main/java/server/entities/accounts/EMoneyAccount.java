package main.java.server.entities.accounts;

import main.java.server.entities.accounts.Account;
import main.java.server.entities.instruments.VirtualCard;

public class EMoneyAccount extends Account {

	public VirtualCard associatedCard;
	
	public EMoneyAccount(Integer uid, Integer anumber, Double credit, Double limit) {
		super(uid, anumber, credit, limit);
	}

	public EMoneyAccount(Integer uid, Integer anumber, Double credit) {
		super(uid, anumber, credit);
	}
	
	public EMoneyAccount(Integer uid, Integer anumber) {
		super(uid, anumber);
	}
	
	public boolean addCard(VirtualCard card){
		if(this.cards.size() == 0){
			this.cards.add(card);
			return true;
		}
		else{
			return false;
		}
	}

}
