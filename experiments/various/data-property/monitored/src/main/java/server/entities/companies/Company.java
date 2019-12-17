package main.java.server.entities.companies;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import main.java.server.TransactionServer;
import main.java.server.entities.IndividualEntity;
import main.java.server.entities.PaymentEntity;
import main.java.server.entities.accounts.Account;
import main.java.server.entities.instruments.GiftCard;
import main.java.server.entities.users.User;
import main.java.server.proxies.entityinfo.UserInfo;

public abstract class Company extends IndividualEntity{

	Map<UserInfo, List<GiftCard>> userGiftCards;
	public List<UserInfo> usersTransactingWithCompany = new ArrayList<UserInfo>();
	
	public Company(Integer id, String name, String country) {
		super(id, name, country);
		
		this.accounts.add(new Account(id, 0));
		this.defaultIncomingAccount = accounts.get(0);
		this.defaultOutgoingAccount = accounts.get(0);
	}

	public double incomingTransactionFee(double amount){
		return 0.3*amount/100;
	}

	public boolean handleIncomingPayment(PaymentEntity entity, Double amount){
		if(super.handleIncomingPayment(entity, amount)
				&& User.class.isAssignableFrom(entity.getClass())){
			usersTransactingWithCompany.add(UserInfo.getUserInfo(entity.id));
			return true;
		}
		else return false;
	}

}
