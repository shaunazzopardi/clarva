package main.java.server.entities.companies;

import main.java.server.entities.companies.Company;

public class FullyParticipatingCompany extends Company {

	public FullyParticipatingCompany(Integer id, String name, String country) {
		super(id, name, country);
		// TODO Auto-generated constructor stub
	}

	@Override
	public double outgoingTransactionFee(double amount) {
		return 0.7*amount/100;
	}

}
