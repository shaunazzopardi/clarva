package main.java.server.entities.instruments;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import main.java.server.entities.IndividualEntity;
import main.java.server.entities.instruments.PrePaidCard;
import main.java.server.entities.users.User;

public class EMoneyCard extends PrePaidCard {
	
//	public OneShotCard(User user){
//		super(user);
//		this.account.limit = 100;
//	}

	public EMoneyCard(User user, int id){
		super(user, id);
	}
	
//	public OneShotCard(User user, Account account, Double limit){
//		super(user, account);
//		this.account.limit = limit;
//	}
	
//	public boolean increaseBalanceBy(Double amount){
//		if(this.loaded){
//			this.account.limit = amount;
//			return this.account.deposit(amount);
//		}
//		else return false;
//	}

//	if()

	public boolean withdraw(Double amount){
//		if(this.expiryDate.isEqual(LocalDateTime.now())
//				|| this.expiryDate.isBefore(LocalDateTime.now())){
//			this.expired();
//			
//			if(ChronoUnit.MONTHS.between(expiryDate, LocalDateTime.now()) < 12){
//				return super.withdraw(amount);
//			}
//			else return false;
//		}
//		else{
			return super.withdraw(amount);
//		}
	}

}
