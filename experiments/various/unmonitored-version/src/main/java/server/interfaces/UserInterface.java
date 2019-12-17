package main.java.server.interfaces;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import main.java.paymentapp.Transaction;
import main.java.server.TransactionServer;
import main.java.server.entities.UserSession;
import main.java.server.entities.accounts.Account;
import main.java.server.entities.companies.Company;
import main.java.server.entities.instruments.GiftCard;
import main.java.server.entities.instruments.VirtualCard;
import main.java.server.entities.users.User;
import main.java.server.interfaces.AdminInterface;
import main.java.server.proxies.entityinfo.GoldUserInfo;
import main.java.server.proxies.entityinfo.UserInfo;

public class UserInterface {
	public TransactionServer ts;
	public static List<Object> countries;
	public static UserInterface ui;
	
	public UserInterface()
	{
		this.ts = TransactionServer.ts;
		
		countries = new ArrayList<Object>();

		String[] locales = Locale.getISOCountries();

		for (String countryCode : locales) {

			Locale obj = new Locale("", countryCode);

			countries.add(obj.getDisplayCountry());
		}
		
		ui = this;
	}

	public void adminsTurn(Integer uid){
//		ts.getUserInfo(uid).blacklist();
		AdminInterface.adminInterface.adminsTurn(uid);
	}
	
	// * Create a new user
	public UserInfo createUser(String name, String country)
	{
		Integer uid = ts.addUser(name,country);

		ts.getUserInfo(uid).makeDisabled();
		AdminInterface.adminInterface.userCreationRequest(uid, ts.getUserInfo(uid));
		
		
		return UserInfo.getUserInfo(uid);
	}
	
	// USER methods
	// * Login into the system (allows only ACTIVE users to login)
	public Integer login(UserInfo user) 
	{
		Integer uid = user.getId();
		
		User u = ts.getUserInfo(uid);

		if (u != null && u.isActive()) {
			return (u.openSession());
		} else {
			return -1;
		}
	}
	// * Logout of the chosen session
	public void logout(UserInfo u, Integer sid)
	{
		Integer uid = u.getId();
		ts.getUserInfo(uid).closeSession(sid);
	}
	
	
	// * Freeze his/her own user account
	public boolean freezeUserAccount(UserInfo user, Integer sid)
	{
		Integer uid = user.getId();

		User u = ts.getUserInfo(uid);
		u.getSession(sid).log("Freeze user");
		u.makeFrozen();		
		return true;
	}
	// * Unfreeze his/her own user account
	public boolean unfreezeUserAccount(UserInfo user, Integer sid)
	{
		Integer uid = user.getId();

		User u = ts.getUserInfo(uid);
		UserSession s = u.getSession(sid);
		if (u.isFrozen()) {
			System.out.println("Unfreeze user");
			u.makeUnfrozen();
			return true;
		} 
		System.out.println("FAILED (user account not frozen): Unfreeze account");
		return false;
	}
	// * Open a new money account
	public Integer requestAccount(UserInfo user, Integer sid)
	{
		Integer uid = user.getId();

		User u = ts.getUserInfo(uid);
		UserSession s = u.getSession(sid);
		Integer account_number = u.createAccount();
		System.out.println("Request new account with number <"+account_number+">");
		AdminInterface.adminInterface.accountApprovalRequest(uid, account_number);
		return (account_number);
	}
	// * Open a new money account
	public boolean checkIfAccountApproved(UserInfo user, Integer acc_no)
	{
		Integer uid = user.getId();

		User u = ts.getUserInfo(uid);
		return u.getAccount(acc_no).opened;
	}
	// * Close an existing money account
	public void closeAccount(UserInfo user, Integer sid, Integer account_number)
	{
		Integer uid = user.getId();

		User u = ts.getUserInfo(uid);
		UserSession s = u.getSession(sid);
		System.out.println("Close account number <"+account_number+">");
		u.deleteAccount(account_number);
	}
	
	public double checkAccount(UserInfo u, Integer account_number){
		Integer uid = u.getId();
		User user = ts.getUserInfo(uid);
		
		if(user != null){
			Account acc = user.getAccount(account_number);
			
			if(acc != null){
				return acc.balance;
			}
		}
		
		return -1;
	}
	
	// * Deposit money from an external source (e.g. from a credit card)
	public boolean depositFromExternal(UserInfo user, Integer sid, Integer to_account_number, double amount)
	{
		Integer uid = user.getId();

		User u = ts.getUserInfo(uid);
		UserSession s = u.getSession(sid);
		System.out.println("Deposit $"+amount+"to account <"+to_account_number+">");
		u.depositTo(to_account_number,amount);
		user.amount += amount;
		return true;
	}
	// * Pay a bill (i.e. an external money account) - charges apply
	public boolean payToCompany(UserInfo ui, Integer sid, Integer from_account, Integer to_company_number, double amount)
	{
		Integer uid = ui.getId();
		User u = ts.getUserInfo(uid);
		UserSession s = u.getSession(sid);
		
		if (s == null) return false;

		//double total_amount = amount;// + ts.charges(uid, amount);
		Account acc = u.getAccount(from_account);
		Company company = ts.getCompanyInfo(to_company_number);
		
		if (ts.isOnline() && acc != null && company != null && acc.getBalance() >= amount) {
			System.out.println("Payment of $"+amount+" from account <"+from_account+">");
			
		//	u.withdrawFrom(from_card_number, total_amount);
			ui.amount -= acc.getBalance();
			
			Transaction t = new Transaction(u, company, amount);
			t.success = true;
			t.fulfilled = true;
			
			UserInfo.userHistory.get(uid).add(t);

			return true;
		}
		else if(!ts.isOnline() && acc != null && company != null){
			Transaction t = new Transaction(u, company, amount);
			UserInfo.transactionsNotYetFulfilled.add(t);
		}
		else System.out.println("FAILED: Payment of $"+amount+" from account <"+from_account+">");
		return false;
	}
	// * Transfer money to another user's account - charges apply
	public boolean transferToOtherAccount(UserInfo from_user, UserInfo to_user, Integer sid, Integer from_account_number, Integer to_account_number, double amount)
	{
		Integer from_uid = from_user.getId();
		
		User from_u = ts.getUserInfo(from_uid);
		UserSession s = from_u.getSession(sid);

		int to_uid = to_user.getId();
		
		if (s == null) return false;
		
		double total_amount = amount;// + ts.charges(from_uid, amount);
		
		if (from_u.getAccount(from_account_number).getBalance() >= total_amount) {
			from_u.withdrawFrom(from_account_number, total_amount);
			ts.getUserInfo(to_uid).depositTo(to_account_number, amount);
			System.out.println("Payment of $"+amount+" from account <"+from_account_number+"> to account "+
					"<"+to_account_number+" of user "+to_uid);
			
			User to_u = ts.getUserInfo(to_uid);
						
			Integer no = from_u.noOfTransactionsWithUser.get(to_u);
			if(no == null){
				from_u.noOfTransactionsWithUser.put(to_u, 0); 
			}
			else{
				from_u.noOfTransactionsWithUser.put(to_u, no + 1);
			}
			if(from_user.getId() != to_uid){
				from_user.amount -= total_amount;
			}
			return true;
		}
		System.out.println("FAILED (not enough funds): "+
				"Payment of $"+amount+" from account <"+from_account_number+"> to account "+
				"<"+to_account_number+" of user "+to_uid);
		return false;
	}
	// * Transfer money across own accounts - charges do not apply
	public boolean transferOwnAccounts(UserInfo ui, Integer sid, Integer from_account_number, Integer to_account_number, double amount)
	{
		Integer uid = ui.getId();
		User u = ts.getUserInfo(uid);
		UserSession s = u.getSession(sid);
		Account 
			from_a = ts.getUserInfo(uid).getAccount(from_account_number),
			to_a   = ts.getUserInfo(uid).getAccount(to_account_number);
		
		if (from_a.getBalance() >= amount) {
			from_a.withdraw(amount);
			to_a.deposit(amount);
			System.out.println("Transfer of $"+amount+" from account <"+from_account_number+"> to own account <"+to_account_number);
			return true;
		}
		System.out.println("FAILED (not enough funds)"+
				"Transfer of $"+amount+" from account <"+from_account_number+"> to own account <"+to_account_number);
		return false;
	}

	
	public boolean checkIfActivated(UserInfo u){
		return ts.getUserInfo(u.getId()).isActive();
	}

	public boolean checkIfFrozen(UserInfo u){
		return ts.getUserInfo(u.getId()).isFrozen();
	}
	
	public boolean checkIfGreylisted(UserInfo u){
		return ts.getUserInfo(u.getId()).isGreylisted();
	}
	
	public boolean checkIfBlacklisted(UserInfo u){
		return ts.getUserInfo(u.getId()).isBlacklisted();
	}
	
	public boolean checkIfWhitelisted(UserInfo u){
		return ts.getUserInfo(u.getId()).isWhitelisted();
	}
		
	public boolean checkIfGoldUser(int uid){
		return ts.getUserInfo(uid).isGoldUser();
	}
	
	public boolean checkIfSilverUser(int uid){
		return ts.getUserInfo(uid).isSilverUser();
	}
	
	public boolean checkIfBronzeUser(int uid){
		return !checkIfGoldUser(uid) & !checkIfSilverUser(uid);
	}

	public Integer requestDebitCard(UserInfo u, int sid, int acc_no) {
		User user = this.ts.getUserInfo(u.getId());
		
		if(user != null){
			return user.createDebitCard(acc_no);
		}
		
		return -1;
	}

	public Integer requestCreditCard(UserInfo u, int sid, int acc_no) {
		User user = this.ts.getUserInfo(u.getId());
		
		if(user != null){
			return user.createCreditCard(acc_no);
		}
		
		return -1;
	}

	public Integer requestGiftCard(UserInfo u, int sid, int to_uid, Integer cid, double amount) {
		User user = this.ts.getUserInfo(u.getId());
		Company company = ts.getCompanyInfo(cid);

		if(user != null
				&& this.ts.getUserInfo(to_uid) != null
				&& company != null){
			GiftCard card = user.createGiftCard(to_uid, company, amount);
			if(card != null){
				return card.getId();
			}
			else return -1;
		}
		
		return -1;
	}

	public void closeCard(UserInfo u, int sid, int card_no) {
		int uid = u.getId();

		User user = this.ts.getUserInfo(uid);
		
		if(user != null
				&& this.ts.getUserInfo(uid) != null){
			user.closeCard(card_no);
		}
		
	}

	public void freezeCard(UserInfo u, int sid, int card_no) {
		int uid = u.getId();

		User user = this.ts.getUserInfo(uid);
		
		if(user != null
				&& this.ts.getUserInfo(uid) != null){
			user.getCard(card_no).freeze();
		}
		
	}


	public void unFreezeCard(UserInfo u, int sid, int card_no) {
		int uid = u.getId();

		User user = this.ts.getUserInfo(uid);
		
		if(user != null
				&& this.ts.getUserInfo(uid) != null){
			user.getCard(card_no).unFreeze();
		}
		
	}


	public double checkCard(UserInfo u, int sid, int card_no) {
		int uid = u.getId();

		User user = this.ts.getUserInfo(uid);
		
		if(user != null
				&& this.ts.getUserInfo(uid) != null){
			if(user.getCard(card_no) != null) {
				return user.getCard(card_no).getBalance();
			} else {
				return -1;
			}
		}
		
		else return -1;
		
	}

	public boolean payToCompanyFromCard(UserInfo ui, int sid, int to_company_number, int cardID, Double amount) {
		Integer uid = ui.getId();
		User u = ts.getUserInfo(uid);
//		UserSession s = u.getSession(sid);
//		
//		if (s == null) return false;

		//double total_amount = amount;// + ts.charges(uid, amount);
		VirtualCard card = u.getCard(cardID);
		Company company = ts.getCompanyInfo(to_company_number);
		
		if(u != null && company != null
				&& ((u.isWhitelisted() && !company.isBlacklisted())
						|| (u.isGreylisted() && company.isWhitelisted())
						|| (u.isBlacklisted() && company.isWhitelisted()))){
						
			if (ts.isOnline() && card != null && company != null && card.getBalance() >= amount) {
				System.out.println("Payment of $"+amount+" from card <"+cardID+">");
				
			//	u.withdrawFrom(from_card_number, total_amount);
				ui.amount -= card.getBalance();
				
				Transaction t = new Transaction(card, company, amount);
				t.success = true;
				t.fulfilled = true;
				
				if(UserInfo.userHistory.containsKey(uid)) {
					UserInfo.userHistory.get(uid).add(t);
				} else {
					List<Transaction> trans = new ArrayList<Transaction>();
					trans.add(t);
					UserInfo.userHistory.put(uid,  trans);
				}
				
				return true;
			}
			else if (!ts.isOnline() && card != null && company != null && card.getBalance() >= amount) {
				ui.transactionsNotYetFulfilled.add(new Transaction(card, company, amount));
				System.out.println("OFFLINE: Payment of $"+amount+" from card <"+cardID+"> postponed until server is back online");
			}	
			else System.out.println("FAILED: Payment of $"+amount+" from card <"+cardID+">");
		}
		return false;

	}


}


