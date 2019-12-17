package main.java.server.entities.instruments;

import main.java.server.entities.accounts.Account;
import main.java.server.entities.instruments.VirtualCard;
import main.java.server.entities.users.User;

public class ReloadableCard extends VirtualCard {

	public Account account;

	public ReloadableCard(User user, Account account, int id) {
		super(user, id);
		this.account = account;
		account.addCard(this);

		// TODO Auto-generated constructor stub
	}
	
	public ReloadableCard(User user, int id) {
		super(user, id);
		int acc_no = user.createAccount();
		this.account = user.getAccount(acc_no);
		account.addCard(this);

		// TODO Auto-generated constructor stub
	}

	public boolean deposit(Double amount){
		return this.increaseBalanceBy(amount);
	}
	
	@Override
	protected boolean reduceBalanceBy(Double amount){
		return this.account.withdraw(amount);
	}

	
	@Override
	public boolean increaseBalanceBy(Double amount){
		return this.account.deposit(amount);
	}
	
	public void close(){
		account.removeCard(this.id);
		
		if(account.getCards().size() == 0){
			owner.defaultOutgoingAccount.balance += this.account.balance;
			((User)owner).deleteAccount(this.account.account_number);
		}
		
		((User )owner).removeCard(this.id);
	}

	@Override
	public double getBalance() {
		return this.account.balance;
	}
}
