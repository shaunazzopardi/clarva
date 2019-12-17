package main.java.server.entities.users;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import main.java.server.TransactionServer;
import main.java.server.entities.IndividualEntity;
import main.java.server.entities.UserSession;
import main.java.server.entities.accounts.Account;
import main.java.server.entities.companies.Company;
import main.java.server.entities.instruments.CreditCard;
import main.java.server.entities.instruments.DebitCard;
import main.java.server.entities.instruments.GiftCard;
import main.java.server.entities.instruments.VirtualCard;
import main.java.server.proxies.entityinfo.UserInfo;

public abstract class User extends IndividualEntity{

//	public enum UserType {
//		GOLD, SILVER, NORMAL
//	}
	
//	public Integer uid;
//	public String name;
//	public UserType type;
	private ArrayList<UserSession> sessions;
	private ArrayList<Account> accounts;
	private ArrayList<VirtualCard> cards;
	public Integer next_session_id;
	public Integer next_account;
	public Integer next_card;
	public String country;
	public IndividualEntity owner = this;
	
	public enum PrivacyLevel{OnlyWithinSystem, ThirdPartiesOK, ThirdPartiesIfAnonimised};

	public Map<User, Integer> noOfTransactionsWithUser; 
	
	public User(Integer uid, String name, String country) {
		super(uid, name, country);
		
		noOfTransactionsWithUser = new HashMap<User, Integer>();
		
		makeDisabled();
		whitelist();
//		makeNormalUser();
		
		sessions = new ArrayList<UserSession>();
		accounts = new ArrayList<Account>();
		
		next_session_id = 0;
		next_account = 1;
		next_card = 1;
		
		this.country = country;
		
		cards = new ArrayList<VirtualCard>();
	}
	
	// Basic information
	public Integer getId() 
	{
		return id;
	}
	public String getName()
	{
		return name;
	}
	public String getCountry()
	{
		return country;
	}
	public ArrayList<Account> getAccounts()
	{
		accounts.remove(null);
		return new ArrayList<Account>(accounts);
	}
	public ArrayList<UserSession> getSessions()
	{
		sessions.remove(null);
		return new ArrayList<UserSession>(sessions);
	}
	public ArrayList<VirtualCard> getCards()
	{
		cards.remove(null);
		return new ArrayList<VirtualCard>(cards);
	}
	
	// User type
	public abstract boolean isGoldUser();// { return (type==UserType.GOLD); }
	public abstract boolean isSilverUser();// { return (type==UserType.SILVER); }
	public abstract boolean isNormalUser();// { return (type==UserType.NORMAL); }


	// Sessions
	public UserSession getSession(Integer sid) 
	{
		UserSession s;
		
		Iterator<UserSession> iterator = getSessions().iterator();
		while (iterator.hasNext()) {
		    s = iterator.next();
		    if (s.getId().equals(sid)) return s;
		}
		return null;
	}
	public Integer openSession() 
	{
		Integer sid = next_session_id;
		
		UserSession session = new UserSession(id, sid);
		session.openSession();
		sessions.add(session);

		next_session_id++;

		return(sid);
	}
	public void closeSession(Integer sid) 
	{
		UserSession s = getSession(sid);

		s.closeSession();
	}

	// Accounts
	public Account getAccount(Integer account_number) 
	{
		Account a;
		
		Iterator<Account> iterator = getAccounts().iterator();
		while (iterator.hasNext()) {
		    a = iterator.next();
		    if (a.getAccountNumber().equals(account_number)) return a;
		}
		return null;
	} 
	public Integer createAccount() 
	{
		Integer account_number = next_account;
		next_account++;
		Account a = new Account(id, account_number);
		accounts.add(a);
		
		if(this.defaultIncomingAccount == null) this.defaultIncomingAccount = a;
		if(this.defaultOutgoingAccount == null) this.defaultOutgoingAccount = a;

		return account_number;
	}
	public Integer createCreditCard(int acc_no) 
	{
		Integer card_number = next_card;
		next_card++;
		CreditCard card = new CreditCard(this, this.getAccount(acc_no), 250.0, card_number);
		cards.add(card);
		return card_number;
	}
	public Integer createDebitCard(int acc_no) 
	{
		Integer card_number = next_card;
		next_card++;
		DebitCard card = new DebitCard(this, this.getAccount(acc_no), card_number);
		card.deposit(700000.0);
		cards.add(card);
		return card_number;
	}
	public GiftCard createGiftCard(int uid, Company company, double amount) 
	{
		User user = TransactionServer.ts.getUserInfo(uid);
		
		if(user != null){
			Integer card_number = user.next_card;
			user.next_card++;
			GiftCard card = new GiftCard(user, this, company, card_number, amount);
			card.deposit(700000.0);
			user.cards.add(card);
			return card;
		}
		else return null;
	}
	
	public void closeCard(Integer card_no){
		VirtualCard card = getCard(card_no);
		if(card != null){
			card.close();
		}
	}
	
	public VirtualCard getCard(int card_no){
		for(VirtualCard c : getCards()){
			if(c.id == card_no){
				return c;
			}
		}
		
		return null;
	}
	
	public void removeCard(Integer card_no) {
		cards.remove(getCard(card_no));
	}
	
	public void deleteAccount(Integer account_number) 
	{
		Account a = getAccount(account_number);
		a.closeAccount();
	}

	public void withdrawFrom(Integer account_number, double amount)
	{
		getAccount(account_number).withdraw(amount);
	}
	public void depositTo(Integer account_number, double amount)
	{
		getAccount(account_number).deposit(amount);
	}
	
}
