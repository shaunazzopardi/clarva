package main.java.server.entities.instruments;

import main.java.server.entities.accounts.Account;
import main.java.server.entities.instruments.ReloadableCard;
import main.java.server.entities.users.User;

public class DebitCard extends ReloadableCard {

	public DebitCard(User user, int id){
		super(user, id);
	}
	
	public DebitCard(User user, Account account, int id){
		super(user, account, id);
	}
}
