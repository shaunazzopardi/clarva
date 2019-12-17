package main.java.server.entities.instruments;

import java.time.Duration;
import java.time.LocalDateTime;

import main.java.server.entities.IndividualEntity;
import main.java.server.entities.companies.Company;
import main.java.server.entities.instruments.NonReloadableCard;
import main.java.server.entities.users.User;

public class GiftCard extends NonReloadableCard {

	User creator;
	Company company;
//	public OneShotCard(User user){
//		super(user);
//		this.account.limit = 100;
//	}

	public GiftCard(User user, User creator, Company company, int id, double giftedMoney){
		super(user, id, giftedMoney);
		this.creator = creator;
		this.deposit(giftedMoney);
		this.company = company;
	}

	//anonymous card
	public GiftCard(User creator, Company company, int id, double giftedMoney){
		super(null, id, giftedMoney);
		this.creator = creator;
		this.deposit(giftedMoney);
		this.company = company;
	}
	
//	public OneShotCard(User user, Account account, Double limit){
//		super(user, account);
//		this.account.limit = limit;
//	}
	
	public boolean paymentTo(IndividualEntity entity, Double amount){
		if(this.enabled && company.equals(entity)){
			return super.paymentTo(entity, amount);
		}
		else return false;
	}	

	@Override
	public double outgoingTransactionFee(double amount) {
		return 0;
	}
	
	public boolean withdraw(Double amount){
//		if(this.expiryDate.isEqual(LocalDateTime.now())
//				|| this.expiryDate.isBefore(LocalDateTime.now())){
//			this.expired();
//			return false;
//		}
//		else{
			return super.withdraw(amount);
//		}
	}
	
}
