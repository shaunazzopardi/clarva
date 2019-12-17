package main.java.server.entities.instruments;

import main.java.server.entities.accounts.Account;
import main.java.server.entities.instruments.PrePaidCard;
import main.java.server.entities.users.User;

public abstract class NonReloadableCard extends PrePaidCard {

	public boolean loaded;
	
	public NonReloadableCard(User user, int id) {
		super(user, id);
		this.balance = 0.0;
		loaded = false;
	}
	public NonReloadableCard(User user, int id, double loadedMoney) {
		super(user, id, loadedMoney);
		loaded = true;
	}

	
	public boolean deposit(Double amount){
		if(!this.loaded){
			return this.increaseBalanceBy(amount);
		}
		else return false;
	}
	
	public boolean withdraw(Double amount){
		if(super.withdraw(amount)
				&& this.balance == 0) {
			this.close();
			return true;
		}
		else return false;
	}

	protected boolean increaseBalanceBy(Double amount){
		this.balance += amount;
		return true;
	}

}
