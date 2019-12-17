package main.java.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import main.java.paymentapp.Transaction;
import main.java.server.entities.companies.Company;
import main.java.server.entities.companies.FullyParticipatingCompany;
import main.java.server.entities.companies.PartiallyParticipatingCompany;
import main.java.server.entities.users.*;
import main.java.server.interfaces.UserInterface;

public class TransactionServer {
	public static TransactionServer ts;
	
	protected boolean initialised;
	protected ArrayList<User> users;
	protected ArrayList<Company> companies;
	protected Integer next_user_id;
	protected Integer next_company_id;
	Random random;

	public boolean online;
	
	// Constructor
	public TransactionServer() 
	{
		users = new ArrayList<User>();
		initialised = false;
		next_user_id = 1;
		next_company_id = 1;
		users = new ArrayList<User> ();
		companies = new ArrayList<Company>();
		ts = this;
		random = new Random(this.hashCode());
		
		online = true;
	}
	
	public void online(){
		this.online = true;
	}
	
	public void offline(){
		this.online = false;
	}
	
	public boolean isOnline(){
		return online;
	}
	
	public ArrayList<User> getUsers() 
	{
		users.remove(null);
		return users;
	}
	
	public ArrayList<Company> getCompanies() 
	{
		companies.remove(null);
		return companies;
	}
	
	// Initialise the transaction system
	public void initialise() 
	{
		initialised = !initialised;

//		User admin = new User(0,"Clark Kent","Malta");
//		admin.makeSilverUser();
//		admin.makeActive();
//		users.add(admin);
	}
	
	// Lookup a user by user-id
	public synchronized User getUserInfo(Integer uid) 
	{
		User u;
			Iterator<User> iterator = getUsers().iterator();
			while (iterator.hasNext()) {
			    u = iterator.next();
			    if (u != null && u.getId().equals(uid)) return u;
			}
			return null;
	}
	public Company getCompanyInfo(Integer cid) 
	{
		Company c;
		
			Iterator<Company> iterator = getCompanies().iterator();
			while (iterator.hasNext()) {
			    c = iterator.next();
			    if (c != null && c.getId().equals(cid)) return c;
			}
			return null;
	}

	// Add a user to the system
	public Integer addUser(String name, String country) 
	{
		Integer uid = next_user_id;
		next_user_id++;
	
		User user;
		if(random.nextBoolean()){
			user = new BronzeUser(uid, name, country);
		}
		else if(random.nextBoolean()){
			user = new SilverUser(uid, name, country);
		}
		else{
			user = new BronzeUser(uid, name, country);
		}
		
		users.add(user);
		return uid;
	}

	// Add a user to the system
	public Integer addCompany(String name, String country) 
	{
		Integer cid = next_company_id;
		next_company_id++;
	
		Company company;
		if(random.nextBoolean()){
			company = new PartiallyParticipatingCompany(cid, name, country);
		}
		else{
			company = new FullyParticipatingCompany(cid, name, country);
		}
		
		companies.add(company);
		
		return cid;
	}

//	// Calculate the charges when a particular user makes a transfer
//	public double charges(Integer uid, double amount) 
//	{
//		User u = getUserInfo(uid);
//		if (u.isGoldUser()) {
////			if (amount <= 100) return 0;				// no charges
////			if (amount <= 1000) return (amount * 0.02); // 2% charges
//			return (amount * 0.2);						// 1% charges
//		}
//		if (u.isSilverUser()) {
////			if (amount <= 1000) return (amount * 0.03); // 3% charges
//			return (amount * 0.4);						// 2% charges
//		}
//		if (u.isNormalUser()) {
////			if (amount*0.05 > 2.0) {
//				return (amount*0.5); 
////			} else {
////				return 2.0;
////			}											// 5% charges, minimum of $2
//		}
//		return 0;
//	}
}
