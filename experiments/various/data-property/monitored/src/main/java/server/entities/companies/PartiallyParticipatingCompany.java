package main.java.server.entities.companies;

import main.java.server.entities.PaymentEntity;
import main.java.server.entities.companies.Company;
import main.java.server.entities.instruments.CreditCard;

public class PartiallyParticipatingCompany extends Company {

	public PartiallyParticipatingCompany(Integer id, String name, String country) {
		super(id, name, country);
		// TODO Auto-generated constructor stub
	}
	
	public boolean handleIncomingPayment(PaymentEntity entity, Double amount){
		if(!entity.getClass().isAssignableFrom(CreditCard.class)){
		
			if(this.defaultIncomingAccount != null){
				if(this.defaultIncomingAccount.balance + this.defaultIncomingAccount.credit >= amount){
					this.defaultIncomingAccount.deposit(amount);
					return true;
				}
				else{
					return false;
				}
			}
			else{
				return false;
			}
		}
		else{
			return false;
		}
	}

	@Override
	public double outgoingTransactionFee(double amount) {
		return 1*amount/100;
	}
}
