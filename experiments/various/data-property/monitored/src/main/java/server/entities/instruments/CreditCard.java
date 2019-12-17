package main.java.server.entities.instruments;

import main.java.server.entities.accounts.Account;
import main.java.server.entities.instruments.ReloadableCard;
import main.java.server.entities.users.User;

public class CreditCard extends ReloadableCard {
	public double credit;

	public CreditCard(User user, Double credit, int id){
		super(user, id);
		this.account.credit = credit;
	}

	public CreditCard(User user, Account account, Double credit, int id){
		super(user, account, id);
		this.account.credit = credit;
	}
	
	public boolean hasEnough(Double amount){
		return this.account.balance + credit >= amount;
	}
}
