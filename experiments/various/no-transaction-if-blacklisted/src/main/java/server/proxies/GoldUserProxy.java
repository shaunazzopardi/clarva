package main.java.server.proxies;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import main.java.paymentapp.RandomizedUserUI;
import main.java.paymentapp.UserUI;
import main.java.server.entities.accounts.Account;
import main.java.server.entities.companies.Company;
import main.java.server.entities.instruments.VirtualCard;
import main.java.server.entities.users.User;
import main.java.server.entities.users.User.PrivacyLevel;
import main.java.server.interfaces.UserInterface;
import main.java.server.proxies.entityinfo.GoldUserInfo;
import main.java.server.proxies.entityinfo.UserInfo;

public class GoldUserProxy {

//	public static GoldUserProxy proxy;
	public static GoldUserInfo external = new GoldUserInfo(-1);
	public static UserInterface intrf = UserInterface.ui;
		
//	public GoldUserProxy(){
//		intrf
//		proxy = this;
//	}

	public static synchronized void loggedInMenu(UserUI userUI, GoldUserInfo u, int sid){//, long end){
		boolean exit = false;

		List<String> argNames = new ArrayList<String>();
		List<Class> argTypes = new ArrayList<Class>();
		
		List<List<Object>> acceptableValues = new ArrayList<List<Object>>();

		List<Object> args;
		
		int uid = u.getId();
		
		int acts = 0;
//		
		while(true){
//			acts++;			
			List<String> options = new ArrayList<String>();
			options.add("(1) to enter the account menu");
			options.add("(2) to enter the card menu");
			options.add("(3) to enter the transaction menu");
//			options.add("(4) to freeze your account");
//			options.add("(5) to unfreeze your account");
			options.add("(6) to de-register user");
			options.add("(7) to authenticate");
//			options.add("(8) logout");
			
			int choice = userUI.currentOptions(1, options);
			if(choice == -1) return;

			acceptableValues.clear();
			argNames.clear();
			argTypes.clear();
			switch(choice){
				//create account
				case 1: accountMenu(userUI, u, sid); //break;
						//}
				case 2: cardMenu(userUI, u, sid); //break;
				//Transaction Menu
				case 3: if(!intrf.checkIfFrozen(u)){
							boolean blacklisted = intrf.checkIfBlacklisted(u);
							boolean greylisted = intrf.checkIfGreylisted(u);
				
							if(blacklisted){
								u.blacklist();
								System.out.println(u.getId() + "black");
								transactionBlacklistedMenu(userUI, u, sid);
							}
							else if(greylisted){
								u.greylist();
								System.out.println(u.getId() + "grey");
								transactionGreylistedMenu(userUI, u, sid);
							}
							else{
								u.whitelist(); 
								System.out.println(u.getId() + " white");
								transactionWhitelistedMenu(userUI, u, sid);
							}
						}
						break;
//				case 4: {
//					intrf.freezeUserAccount(u, sid); u.freeze();
//					System.out.println("User " + u + " frozen");
//					 break;
//				}
//				case 5: intrf.unfreezeUserAccount(u, sid); 
//						u.unfreeze(); 
//						System.out.println("User " + u + " unfrozen");
//						 break;
				case 4: //de-register
					argNames.clear();
					argNames.add("User ID");

					argTypes.clear();
					argTypes.add(Integer.class);

					acceptableValues.clear();
					List<Object> userIDs = new ArrayList<Object>();
					for(int i = 0 ; i < intrf.ts.getUsers().size(); i++) {
						userIDs.add(intrf.ts.getUsers().get(i).getId());
					}
					acceptableValues.add(userIDs);					

					if(userIDs.size() > 0){
						///	synchronized(userUI){
						args = userUI.argsNeeded(argNames, argTypes, acceptableValues);
						//}
						if(args.size() > 0){
							uid = (Integer) args.get(0);

							UserInfo.getUserInfo(uid).deregisterUser();
							System.out.println("User " + u + " deregistered");

						}
					}
					break;
			case 5: //authenticate
				argNames.clear();
				argNames.add("User ID");
				argNames.add("Name");
				argNames.add("Address");
				argNames.add("ID");
				argNames.add("PrivacyLevel");

				argTypes.clear();
				argTypes.add(Integer.class);
				argTypes.add(String.class);
				argTypes.add(String.class);
				argTypes.add(String.class);
				argTypes.add(Integer.class);

				acceptableValues.clear();

				userIDs = new ArrayList<Object>();
				for(int i = 0 ; i < intrf.ts.getUsers().size(); i++) {
					userIDs.add(intrf.ts.getUsers().get(i).getId());
				}
				acceptableValues.add(userIDs);
				
				acceptableValues.add(RandomizedUserUI.names);
				
				acceptableValues.add(RandomizedUserUI.addresses);
				
				acceptableValues.add(RandomizedUserUI.nationalIDs);
				
				acceptableValues.add(Arrays.asList(PrivacyLevel.values()));

				if(userIDs.size() > 0){
					///	synchronized(userUI){
					args = userUI.argsNeeded(argNames, argTypes, acceptableValues);
					//}
					if(args.size() > 0){
						uid = (Integer) args.get(0);
						String name = (String) args.get(1);
						String address = (String) args.get(2);
						String id = (String) args.get(3);
						PrivacyLevel privacyLevel = (PrivacyLevel) args.get(4);

						UserInfo.getUserInfo(uid).authenticateUser(name, address, id, privacyLevel);
						System.out.println("User " + u + " authenticated");

					}
				}
				break;
				default: {intrf.logout(u, sid); exit = true; acts = 0; 
						System.out.println("User " + u + " logged out.");

						break;
				
				}
			}
			
			//intrf.adminsTurn(uid);
		}
	}

	public static synchronized void accountMenu(UserUI userUI, GoldUserInfo u, int sid){//, long end){
		boolean exit = false;

		List<String> argNames = new ArrayList<String>();
		List<Class> argTypes = new ArrayList<Class>();
		
		List<List<Object>> acceptableValues = new ArrayList<List<Object>>();

		List<Object> args;
		
		int uid = u.getId();
		
		int acts = 0;
		
		while(acts < 5){
			acts++;			
			List<String> options = new ArrayList<String>();
			options.add("(1) to request new account");
//			options.add("(2) to close an account");
			options.add("(3) to check balance on account");
			options.add("(4) to go to the main menu");

			
			int choice = userUI.currentOptions(2, options);
			if(choice == -1) return;

			acceptableValues.clear();
			argNames.clear();
			argTypes.clear();
			switch(choice){
				//create account
				case 1:// synchronized(userUI){
						if(intrf.checkIfActivated(u)) {
							System.out.println("Account requested with ID: " + intrf.requestAccount(u, sid)); break;
						}
//				case 2: argNames.clear();
//						argNames.add("Account ID");
//						
//						argTypes.clear();
//						argTypes.add(Integer.class);
//						
//						List<Object> accIDs = new ArrayList<Object>();
//						for(Account acc : intrf.ts.getUserInfo(uid).getAccounts()) {
//							accIDs.add(acc.getAccountNumber());
//						}
//						acceptableValues.add(accIDs);					
//						
//						if(accIDs.size() > 0){
//				    //		synchronized(userUI){
//				 			args = userUI.argsNeeded(argNames, argTypes, acceptableValues);
//					//	}
//							
//							if(args.size() > 0)
//								intrf.closeAccount(u, sid, (int) args.get(0)); 
//							System.out.println("Account closed ID: " + sid); break;
//
//						}
//						break;
				case 2: argNames.clear();
						argNames.add("Account ID");
						
						argTypes.clear();
						argTypes.add(Integer.class);
						
						List<Object> accIDs = new ArrayList<Object>();
						for(Account acc : intrf.ts.getUserInfo(uid).getAccounts()) {
							accIDs.add(acc.getAccountNumber());
						}
						acceptableValues.add(accIDs);					
						
						if(accIDs.size() > 0){
					//	synchronized(userUI){
							args = userUI.argsNeeded(argNames, argTypes, acceptableValues);
						//}
							
							if(args.size() > 0)
								intrf.checkAccount(u, (int) args.get(0)); 
						}
						break;
				default: exit = true; acts = 0; break;
			}
			
			//intrf.adminsTurn(uid);
		}
	}

	public static synchronized void cardMenu(UserUI userUI, GoldUserInfo u, int sid){//, long end){
		boolean exit = false;

		List<String> argNames = new ArrayList<String>();
		List<Class> argTypes = new ArrayList<Class>();
		
		List<List<Object>> acceptableValues = new ArrayList<List<Object>>();

		List<Object> args;
		
		int uid = u.getId();
		
		int acts = 0;
		
		while(acts < 5){
			acts++;			
			List<String> options = new ArrayList<String>();
			options.add("(1) to request debit card");
			options.add("(2) to request credit card");
			options.add("(3) to request gift card for user");
//			options.add("(4) to request anonymous prepaid un-reloadable card");
//			options.add("(5) to freeze a card");
//			options.add("(6) to unfreeze a card");
//			options.add("(7) to close a card");
			options.add("(8) to check balance on card");
//			options.add("(9) to go back to main menu");
			
			int choice = userUI.currentOptions(2, options);
			if(choice == -1) return;

			acceptableValues.clear();
			argNames.clear();
			argTypes.clear();
			switch(choice){
				//create account
				case 1: argNames.clear();
						argNames.add("With Account ID");
						
						argTypes.clear();
						argTypes.add(Integer.class);
					
						List<Object> accIDs = new ArrayList<Object>();
						for(Account acc : intrf.ts.getUserInfo(uid).getAccounts()) {
							accIDs.add(acc.getAccountNumber());
						}
						acceptableValues.add(accIDs);					
						
						if(accIDs.size() > 0){
					//	synchronized(userUI){
							args = userUI.argsNeeded(argNames, argTypes, acceptableValues);
						//}
							
							if(args.size() > 0){
								System.out.println("Debit Card requested with ID: " + intrf.requestDebitCard(u, sid, (int) args.get(0)));
							}
						}
						
						break;
				case 2: argNames.clear();
						argNames.add("With Account ID");
						
						argTypes.clear();
						argTypes.add(Integer.class);
						
						acceptableValues.clear();
						accIDs = new ArrayList<Object>();
						for(Account acc : intrf.ts.getUserInfo(uid).getAccounts()) {
							accIDs.add(acc.getAccountNumber());
						}
						acceptableValues.add(accIDs);					
						
						if(accIDs.size() > 0){
					//	synchronized(userUI){
							args = userUI.argsNeeded(argNames, argTypes, acceptableValues);
						//}
							
							if(args.size() > 0){
								System.out.println("Debit Card requested with ID: " + intrf.requestCreditCard(u, sid, (int) args.get(0)));
							}
						}
						
						break;
				case 3: argNames.clear();
						argNames.add("User ID");
						argNames.add("Company ID");
						argNames.add("Amount");
						
						argTypes.clear();
						argTypes.add(Integer.class);
						argTypes.add(Integer.class);
						argTypes.add(Double.class);
					
						acceptableValues.clear();
						List<Object> userIDs = new ArrayList<Object>();
						for(User usr : intrf.ts.getUsers()) {
							userIDs.add(usr.getId());
						}
						acceptableValues.add(userIDs);					
						
						List<Object> companyIDs = new ArrayList<Object>();
						for(Company company : intrf.ts.getCompanies()) {
							companyIDs.add(company.getId());
						}
						acceptableValues.add(companyIDs);					

						acceptableValues.add(new ArrayList<Object>());

						if(userIDs.size() > 0 && companyIDs.size() > 0){
					//	synchronized(userUI){
							args = userUI.argsNeeded(argNames, argTypes, acceptableValues);
						//}
							
							if(args.size() > 0){
								System.out.println("Gift Card requested for user with ID " + (Integer) args.get(0) 
									+ "with ID: " + intrf.requestGiftCard(u, sid, (Integer) args.get(0),  (Integer) args.get(1),  (Double) args.get(2))); 
							}
						}
						
						break;
//				case 4: argNames.clear();
//						argNames.add("Account ID");
//						argNames.add("Company ID");
//						argNames.add("Amount to pay");
//		
//						argTypes.clear();
//						argTypes.add(Integer.class);
//						argTypes.add(Integer.class);
//						argTypes.add(Double.class);
//		
//						accIDs = new ArrayList<Object>();
//						for(Account acc : intrf.ts.getUserInfo(uid).getAccounts()){
//							if(acc.opened) accIDs.add(acc.getAccountNumber());
//						}
//		
//						acceptableValues.add(accIDs);					
//		
//						companyIDs = new ArrayList<Object>();
//						for(Company company : intrf.ts.getCompanies()) {
//							companyIDs.add(company.getId());
//						}
//						acceptableValues.add(companyIDs);					
//
//						acceptableValues.add(new ArrayList<Object>());					
//		
//						if(accIDs.size() > 0){
//						//synchronized(userUI){
//							args = userUI.argsNeeded(argNames, argTypes, acceptableValues);
//						//}			
//								
//							Double totalAmountAvailable = intrf.checkCard(u, sid, (int) args.get(0));
//							Double transactionAmount = new java.util.Random((int) (Math.floor(totalAmountAvailable))).nextDouble();	
//							
//							if(args.size() > 1){
//								if(intrf.payToCompanyFromCard(u, sid, (int) args.get(0), (int) args.get(1), transactionAmount)){
//						//			external.amount -= (Double) args.get(1);
//								System.out.println("Payment from user " + u.getId() + " " + sid);
//								}
//								exit = true; acts = 0;
//							}
//						}
//						break;
//				case 5: argNames.clear();
//						argNames.add("Card ID");
//						
//						argTypes.clear();
//						argTypes.add(Integer.class);
//					
//						acceptableValues.clear();
//						ArrayList<Object> cardIDs = new ArrayList<Object>();
//						for(VirtualCard card : intrf.ts.getUserInfo(uid).getCards()){
//							if(card.enabled) cardIDs.add(card.getId());
//						}
//						acceptableValues.add(cardIDs);					
//						
//						if(cardIDs.size() > 0){
//					//	synchronized(userUI){
//							args = userUI.argsNeeded(argNames, argTypes, acceptableValues);
//						//}
//							
//							if(args.size() > 0){
//								intrf.freezeCard(u, sid, (Integer) args.get(0));
//								System.out.println("Card with ID" + (Integer) args.get(0) 
//									+ "frozen."); 
//							}
//						}
//						
//						break;
//				case 6: argNames.clear();
//						argNames.add("Card ID");
//						
//						argTypes.clear();
//						argTypes.add(Integer.class);
//					
//						acceptableValues.clear();
//						cardIDs = new ArrayList<Object>();
//						for(VirtualCard card : intrf.ts.getUserInfo(uid).getCards()){
//							if(!card.enabled) cardIDs.add(card.getId());
//						}
//						acceptableValues.add(cardIDs);					
//						
//						if(cardIDs.size() > 0){
//					//	synchronized(userUI){
//							args = userUI.argsNeeded(argNames, argTypes, acceptableValues);
//						//}
//							
//							if(args.size() > 0){
//								intrf.unFreezeCard(u, sid, (Integer) args.get(0));
//								System.out.println("Card with ID" + (Integer) args.get(0) 
//									+ "unfrozen."); 
//							}
//						}
//						
//						break;
//				case 7: argNames.clear();
//						argNames.add("Card ID");
//						
//						argTypes.clear();
//						argTypes.add(Integer.class);
//						
//						acceptableValues.clear();
//						cardIDs = new ArrayList<Object>();
//						for(VirtualCard card : intrf.ts.getUserInfo(uid).getCards()){
//							if(card.enabled) cardIDs.add(card.getId());
//						}
//						acceptableValues.add(cardIDs);						
//						
//						if(cardIDs.size() > 0){
//				//		synchronized(userUI){
//							args = userUI.argsNeeded(argNames, argTypes, acceptableValues);
//					//	}
//							
//							if(args.size() > 0)
//								intrf.closeCard(u, sid, (int) args.get(0)); 
//						}
//						break;
				case 4:	argNames.clear();
						argNames.add("Card ID");
						
						argTypes.clear();
						argTypes.add(Integer.class);
						
						acceptableValues.clear();
						List<Object> cardIDs = new ArrayList<Object>();
						for(VirtualCard card : intrf.ts.getUserInfo(uid).getCards()){
							if(card.enabled) cardIDs.add(card.getId());
						}
						acceptableValues.add(cardIDs);						
						
						if(cardIDs.size() > 0){
					//	synchronized(userUI){
							args = userUI.argsNeeded(argNames, argTypes, acceptableValues);
						//}
							
							if(args.size() > 0)
								System.out.println("Card has the following balance: " + intrf.checkCard(u, sid, (int) args.get(0))); 
						}
						break;
				default: exit = true; acts = 0; break;
			}
			
			//intrf.adminsTurn(uid);
		}
	}
	
	public static synchronized void transactionWhitelistedMenu(UserUI userUI, GoldUserInfo u, int sid){//, long end){
		boolean exit = false;

		List<String> argNames = new ArrayList<String>();
		List<Class> argTypes = new ArrayList<Class>();
		
		List<List<Object>> acceptableValues = new ArrayList<List<Object>>();

		List<Object> args;
		
		int uid = u.getId();
		
		int acts = 0;
		
		while(acts < 5){
			acts++;			
			List<String> options = new ArrayList<String>();
			options.add("(1) to make a card payment");
			options.add("(2) to transfer money between your own accounts");
			options.add("(3) to deposit money into your account");
//			options.add("(4) to transfer money to another user's account");
			
			int choice = userUI.currentOptions(2, options);
			if(choice == -1) return;
			
			acceptableValues.clear();
			argNames.clear();
			argTypes.clear();
			switch(choice){
			//payment from card
			case 1: argNames.clear();
					argNames.clear();
					argNames.add("Card ID");
					argNames.add("Company ID");
		//			argNames.add("Amount to pay");
		
					argTypes.clear();
					argTypes.add(Integer.class);
					argTypes.add(Integer.class);
		//			argTypes.add(Double.class);				
		
					List<Object> companyIDs = new ArrayList<Object>();
					for(Company company : intrf.ts.getCompanies()) {
						companyIDs.add(company.getId());
					}
					acceptableValues.add(companyIDs);	
		
					List<Object> cardIDs = new ArrayList<Object>();
					for(VirtualCard card : intrf.ts.getUserInfo(uid).getCards()){
						if(card.enabled) cardIDs.add(card.getId());
					}
		
					acceptableValues.add(cardIDs);					
		
		//			acceptableValues.add(new ArrayList<Object>());					
		
					if(cardIDs.size() > 0 && companyIDs.size() > 0){
					//synchronized(userUI){
						args = userUI.argsNeeded(argNames, argTypes, acceptableValues);
					//}		
						Double totalAmountAvailable = intrf.checkCard(u, sid, (int) args.get(1));
						Double transactionAmount = new java.util.Random((int) ((totalAmountAvailable.intValue()))).nextDouble();	
						
						if(args.size() > 1){
							if(intrf.payToCompanyFromCard(u, sid, (Integer) args.get(0), (Integer) args.get(1), transactionAmount)){
								System.out.println("Payment from user " + u.getId() + " " + sid);
							}
						}
					}
					break;
//					//payment from account
//			case 2: argNames.clear();
//					argNames.add("Account ID");
//					argNames.add("Company ID");
//					argNames.add("Amount to pay");
//	
//					argTypes.clear();
//					argTypes.add(Integer.class);
//					argTypes.add(Integer.class);
//					argTypes.add(Double.class);
//	
//					List<Object> accIDs = new ArrayList<Object>();
//					for(Account acc : intrf.ts.getUserInfo(uid).getAccounts()){
//						if(acc.opened) accIDs.add(acc.getAccountNumber());
//					}
//	
//					acceptableValues.add(accIDs);					
//	
//					companyIDs = new ArrayList<Object>();
//					intrf.ts.getCompanies().forEach(company -> companyIDs.add(company.getId()));;
//					acceptableValues.add(companyIDs);	
//					acceptableValues.add(new ArrayList<Object>());					
//	
//					if(accIDs.size() > 0){
//					//synchronized(userUI){
//						args = userUI.argsNeeded(argNames, argTypes, acceptableValues);
//					//}			
//							
//						if(args.size() > 1){
//							if(intrf.payToCompany(u, sid, (int) args.get(0), (int) args.get(1), (Double) args.get(2))){
//					//			external.amount -= (Double) args.get(1);
//							}
//							exit = true; acts = 0;
//						}
//					}
//					break;
			//transfer between own accounts
				case 2: argNames.clear();
						argNames.add("First Account ID");
						argNames.add("Second Account ID");
						argNames.add("Amount to pay");
						
						argTypes.clear();
						argTypes.add(Integer.class);
						argTypes.add(Integer.class);
						argTypes.add(Double.class);
						
						List<Object> accIDs = new ArrayList<Object>();
						for(main.java.server.entities.accounts.Account acc : intrf.ts.getUserInfo(uid).getAccounts()){
							if(acc.opened) accIDs.add(acc.getAccountNumber());
						}

						acceptableValues.add(accIDs);					
						acceptableValues.add(accIDs);					
						acceptableValues.add(new ArrayList<Object>());					

						if(accIDs.size() > 0){
						//synchronized(userUI){
							args = userUI.argsNeeded(argNames, argTypes, acceptableValues);
						//}		
							if(args.size() > 2){
								intrf.transferOwnAccounts(u, sid, (int) args.get(0), (int) args.get(1), (Double) args.get(2)); 
								exit = true; acts = 0;
							}
						}
						break;
				//deposit from bank account
				case 3: argNames.clear();
						argNames.add("Account ID");
						argNames.add("Amount to deposit into account");
						
						argTypes.clear();
						argTypes.add(Integer.class);
						argTypes.add(Double.class);
						
						accIDs = new ArrayList<Object>();
						for(main.java.server.entities.accounts.Account acc : intrf.ts.getUserInfo(uid).getAccounts()){
							if(acc.opened) accIDs.add(acc.getAccountNumber());
						}

						acceptableValues.add(accIDs);
						acceptableValues.add(new ArrayList<Object>());
						
						if(accIDs.size() > 0){
						//synchronized(userUI){
							args = userUI.argsNeeded(argNames, argTypes, acceptableValues);
						//}					
							if(args.size() > 1){
								if(intrf.depositFromExternal(u, sid, (int) args.get(0), (Double) args.get(1))){
									//external.amount += (Double) args.get(1);
								}
								exit = true; acts = 0;
							}
						}
						break;
//				//Transfer amount to another user's account
//				case 4: argNames.clear();
//						
//						argNames.add("To User ID");
//						
//						acceptableValues.clear();
//						
//						List<Object> userIDs = new ArrayList<Object>();
//						intrf.ts.getUsers().forEach(user -> userIDs.add(user.getId()));
//						acceptableValues.add(userIDs);
//
//						argTypes.clear();
//						argTypes.add(Integer.class);
//
//						if(userIDs.size() > 1){
//							args = userUI.argsNeeded(argNames, argTypes, acceptableValues);
//						
//							argNames.clear();
//							argNames.add("From Account ID");
//							argNames.add("To Account ID");
//							argNames.add("Amount to pay");
//							
//		
//							argTypes.clear();
//							argTypes.add(Integer.class);
//							argTypes.add(Integer.class);
//							argTypes.add(Double.class);
//							
//							List<Object> from_accIDs = new ArrayList<Object>();
//							for(Account acc : intrf.ts.getUserInfo(uid).getAccounts()){
//								if(acc.opened) from_accIDs.add(acc.getAccountNumber());
//							}
//			
//							acceptableValues.clear();
//							acceptableValues.add(from_accIDs);	
//							
//							List<Object> to_accIDs = new ArrayList<Object>();
//							int to_uid = (Integer) args.get(0);
//							
//							for(Account acc : intrf.ts.getUserInfo(to_uid).getAccounts()){
//								if(acc.opened) to_accIDs.add(acc.getAccountNumber());
//							}
//							
//							acceptableValues.add(to_accIDs);	
//
//							acceptableValues.add(new ArrayList<Object>());					
//			
//							if(from_accIDs.size() > 0
//									&& to_accIDs.size() > 0){
//							//synchronized(userUI){
//								args = userUI.argsNeeded(argNames, argTypes, acceptableValues);
//							//}			
//									
//								if(args.size() > 1){
//									intrf.transferToOtherAccount(u, UserInfo.getUserInfo(to_uid), sid, (int) args.get(0), (int) args.get(1), (Double) args.get(2)); 
//									exit = true; acts = 0;
//								}
//							}
//						}
//						break;

				default: exit = true; acts = 0; break;
			}
			
			//intrf.adminsTurn(uid);
		}
	}


	public static synchronized void transactionGreylistedMenu(UserUI userUI, GoldUserInfo u, int sid){//, long end){
		boolean exit = false;

		List<String> argNames = new ArrayList<String>();
		List<Class> argTypes = new ArrayList<Class>();
		
		List<List<Object>> acceptableValues = new ArrayList<List<Object>>();

		List<Object> args;
		
		int uid = u.getId();
		
		int acts = 0;
		
		while(acts < 5){
			acts++;			
			List<String> options = new ArrayList<String>();
			options.add("(1) to make a card payment");
			options.add("(2) to transfer money between your own accounts");
//			options.add("(3) to transfer money to another user's account");
//			options.add("(3) to go back to the main menu");
			
			int choice = userUI.currentOptions(2, options);
			if(choice == -1) return;
			
			acceptableValues.clear();
			argNames.clear();
			argTypes.clear();
			switch(choice){
			//payment from card
			case 1: argNames.clear();
					argNames.clear();
					argNames.add("Card ID");
					argNames.add("Company ID");
		//			argNames.add("Amount to pay");
		
					argTypes.clear();
					argTypes.add(Integer.class);
					argTypes.add(Integer.class);
		//			argTypes.add(Double.class);				
		
					List<Object> companyIDs = new ArrayList<Object>();
					for(Company company : intrf.ts.getCompanies()) {
						companyIDs.add(company.getId());
					}
					acceptableValues.add(companyIDs);	
		
					List<Object> cardIDs = new ArrayList<Object>();
					for(VirtualCard card : intrf.ts.getUserInfo(uid).getCards()){
						if(card.enabled) cardIDs.add(card.getId());
					}
		
					acceptableValues.add(cardIDs);					
		
		//			acceptableValues.add(new ArrayList<Object>());					
		
					if(cardIDs.size() > 0 && companyIDs.size() > 0){
					//synchronized(userUI){
						args = userUI.argsNeeded(argNames, argTypes, acceptableValues);
					//}		
						Double totalAmountAvailable = intrf.checkCard(u, sid, (int) args.get(1));
						Double transactionAmount = new java.util.Random((int) ((totalAmountAvailable.intValue()))).nextDouble();	
						
						if(args.size() > 1){
							if(intrf.payToCompanyFromCard(u, sid, (Integer) args.get(0), (Integer) args.get(1), transactionAmount)){
								System.out.println("Payment from user " + u.getId() + " " + sid);
							}
						}
					}
					break;
//					//payment from account
//			case 2: argNames.clear();
//					argNames.add("Account ID");
//					argNames.add("Company ID");
//					argNames.add("Amount to pay");
//	
//					argTypes.clear();
//					argTypes.add(Integer.class);
//					argTypes.add(Integer.class);
//					argTypes.add(Double.class);
//	
//					List<Object> accIDs = new ArrayList<Object>();
//					for(Account acc : intrf.ts.getUserInfo(uid).getAccounts()){
//						if(acc.opened) accIDs.add(acc.getAccountNumber());
//					}
//	
//					acceptableValues.add(accIDs);					
//	
//					companyIDs = new ArrayList<Object>();
//					intrf.ts.getCompanies().forEach(company -> companyIDs.add(company.getId()));;
//					acceptableValues.add(companyIDs);	
//					acceptableValues.add(new ArrayList<Object>());					
//	
//					if(accIDs.size() > 0){
//					//synchronized(userUI){
//						args = userUI.argsNeeded(argNames, argTypes, acceptableValues);
//					//}			
//							
//						if(args.size() > 1){
//							if(intrf.payToCompany(u, sid, (int) args.get(0), (int) args.get(1), (Double) args.get(2))){
//					//			external.amount -= (Double) args.get(1);
//							}
//							exit = true; acts = 0;
//						}
//					}
//					break;
			//transfer between own accounts
				case 2: argNames.clear();
						argNames.add("First Account ID");
						argNames.add("Second Account ID");
						argNames.add("Amount to pay");
						
						argTypes.clear();
						argTypes.add(Integer.class);
						argTypes.add(Integer.class);
						argTypes.add(Double.class);
						
						List<Object> accIDs = new ArrayList<Object>();
						for(main.java.server.entities.accounts.Account acc : intrf.ts.getUserInfo(uid).getAccounts()){
							if(acc.opened) accIDs.add(acc.getAccountNumber());
						}

						acceptableValues.add(accIDs);					
						acceptableValues.add(accIDs);					
						acceptableValues.add(new ArrayList<Object>());					

						if(accIDs.size() > 0){
						//synchronized(userUI){
							args = userUI.argsNeeded(argNames, argTypes, acceptableValues);
						//}		
							if(args.size() > 2){
								intrf.transferOwnAccounts(u, sid, (int) args.get(0), (int) args.get(1), (Double) args.get(2)); 
								exit = true; acts = 0;
							}
						}
						break;
//				//deposit from bank account
//				case 3: argNames.clear();
//						argNames.add("Account ID");
//						argNames.add("Amount to deposit into account");
//						
//						argTypes.clear();
//						argTypes.add(Integer.class);
//						argTypes.add(Double.class);
//						
//						accIDs = new ArrayList<Object>();
//						for(Account acc : intrf.ts.getUserInfo(uid).getAccounts()){
//							if(acc.opened) accIDs.add(acc.getAccountNumber());
//						}
//
//						acceptableValues.add(accIDs);
//						acceptableValues.add(new ArrayList<Object>());
//						
//						if(accIDs.size() > 0){
//						//synchronized(userUI){
//							args = userUI.argsNeeded(argNames, argTypes, acceptableValues);
//						//}					
//							if(args.size() > 1){
//								if(intrf.depositFromExternal(u, sid, (int) args.get(0), (Double) args.get(1))){
//									//external.amount += (Double) args.get(1);
//								}
//								exit = true; acts = 0;
//							}
//						}
//						break;
//				//Transfer amount to another user's account
//				case 3: argNames.clear();
//						
//						argNames.add("To User ID");
//						
//						acceptableValues.clear();
//						
//						List<Object> userIDs = new ArrayList<Object>();
//						intrf.ts.getUsers().forEach(user -> userIDs.add(user.getId()));
//						acceptableValues.add(userIDs);
//
//						argTypes.clear();
//						argTypes.add(Integer.class);
//
//						if(userIDs.size() > 1){
//							args = userUI.argsNeeded(argNames, argTypes, acceptableValues);
//						
//							argNames.clear();
//							argNames.add("From Account ID");
//							argNames.add("To Account ID");
//							argNames.add("Amount to pay");
//							
//		
//							argTypes.clear();
//							argTypes.add(Integer.class);
//							argTypes.add(Integer.class);
//							argTypes.add(Double.class);
//							
//							List<Object> from_accIDs = new ArrayList<Object>();
//							for(Account acc : intrf.ts.getUserInfo(uid).getAccounts()){
//								if(acc.opened) from_accIDs.add(acc.getAccountNumber());
//							}
//			
//							acceptableValues.clear();
//							acceptableValues.add(from_accIDs);	
//							
//							List<Object> to_accIDs = new ArrayList<Object>();
//							int to_uid = (Integer) args.get(0);
//							
//							for(Account acc : intrf.ts.getUserInfo(to_uid).getAccounts()){
//								if(acc.opened) to_accIDs.add(acc.getAccountNumber());
//							}
//							
//							acceptableValues.add(to_accIDs);	
//
//							acceptableValues.add(new ArrayList<Object>());					
//			
//							if(from_accIDs.size() > 0
//									&& to_accIDs.size() > 0){
//							//synchronized(userUI){
//								args = userUI.argsNeeded(argNames, argTypes, acceptableValues);
//							//}			
//									
//								if(args.size() > 1){
//									intrf.transferToOtherAccount(u, UserInfo.getUserInfo(to_uid), sid, (int) args.get(0), (int) args.get(1), (Double) args.get(2)); 
//									exit = true; acts = 0;
//								}
//							}
//						}
//						break;

				default: exit = true; acts = 0; break;
			}
			
			//intrf.adminsTurn(uid);
		}
	}


	public static synchronized void transactionBlacklistedMenu(UserUI userUI, GoldUserInfo u, int sid){//, long end){
		boolean exit = false;

		List<String> argNames = new ArrayList<String>();
		List<Class> argTypes = new ArrayList<Class>();
		
		List<List<Object>> acceptableValues = new ArrayList<List<Object>>();

		List<Object> args;
		
		int uid = u.getId();
		
		int acts = 0;
		
		while(acts < 5){
			acts++;			
			List<String> options = new ArrayList<String>();
//			options.add("(1) to make a card payment");
			options.add("(2) to transfer money between your own accounts");
//			options.add("(3) to deposit money into your account");
//			options.add("(4) to transfer money to another user's account");
//			options.add("(2) to go back to the main menu");
			
			int choice = userUI.currentOptions(2, options);
			if(choice == -1) return;
			
			acceptableValues.clear();
			argNames.clear();
			argTypes.clear();
			switch(choice){
			//payment from card
//			case 1: argNames.clear();
//					argNames.add("Card ID");
//					argNames.add("Company ID");
//					argNames.add("Amount to pay");
//	
//					argTypes.clear();
//					argTypes.add(Integer.class);
//					argTypes.add(Integer.class);
//					argTypes.add(Double.class);
//	
//					List<Object> cardIDs = new ArrayList<Object>();
//					for(VirtualCard card : intrf.ts.getUserInfo(uid).getCards()){
//						if(card.enabled) cardIDs.add(card.getId());
//					}
//	
//					acceptableValues.add(cardIDs);					
//	
//					List<Object> companyIDs = new ArrayList<Object>();
//					intrf.ts.getCompanies().forEach(company -> companyIDs.add(company.getId()));;
//					acceptableValues.add(companyIDs);	
//					acceptableValues.add(new ArrayList<Object>());					
//	
//					if(cardIDs.size() > 0 && companyIDs.size() > 0){
//					//synchronized(userUI){
//						args = userUI.argsNeeded(argNames, argTypes, acceptableValues);
//					//}			
//							
//						if(args.size() > 1){
//							if(intrf.payToCompanyFromCard(u, sid, (int) args.get(0), (int) args.get(1), (Double) args.get(2))){
//					//			external.amount -= (Double) args.get(1);
//							}
//							exit = true; acts = 0;
//						}
//					}
//					break;
//					//payment from account
//			case 2: argNames.clear();
//					argNames.add("Account ID");
//					argNames.add("Company ID");
//					argNames.add("Amount to pay");
//	
//					argTypes.clear();
//					argTypes.add(Integer.class);
//					argTypes.add(Integer.class);
//					argTypes.add(Double.class);
//	
//					List<Object> accIDs = new ArrayList<Object>();
//					for(Account acc : intrf.ts.getUserInfo(uid).getAccounts()){
//						if(acc.opened) accIDs.add(acc.getAccountNumber());
//					}
//	
//					acceptableValues.add(accIDs);					
//	
//					companyIDs = new ArrayList<Object>();
//					intrf.ts.getCompanies().forEach(company -> companyIDs.add(company.getId()));;
//					acceptableValues.add(companyIDs);	
//					acceptableValues.add(new ArrayList<Object>());					
//	
//					if(accIDs.size() > 0){
//					//synchronized(userUI){
//						args = userUI.argsNeeded(argNames, argTypes, acceptableValues);
//					//}			
//							
//						if(args.size() > 1){
//							if(intrf.payToCompany(u, sid, (int) args.get(0), (int) args.get(1), (Double) args.get(2))){
//					//			external.amount -= (Double) args.get(1);
//							}
//							exit = true; acts = 0;
//						}
//					}
//					break;
			//transfer between own accounts
				case 1: argNames.clear();
						argNames.add("First Account ID");
						argNames.add("Second Account ID");
						argNames.add("Amount to pay");
						
						argTypes.clear();
						argTypes.add(Integer.class);
						argTypes.add(Integer.class);
						argTypes.add(Double.class);
						
						List<Object> accIDs = new ArrayList<Object>();
						for(main.java.server.entities.accounts.Account acc : intrf.ts.getUserInfo(uid).getAccounts()){
							if(acc.opened) accIDs.add(acc.getAccountNumber());
						}

						acceptableValues.add(accIDs);					
						acceptableValues.add(accIDs);					
						acceptableValues.add(new ArrayList<Object>());					

						if(accIDs.size() > 0){
						//synchronized(userUI){
							args = userUI.argsNeeded(argNames, argTypes, acceptableValues);
						//}		
							if(args.size() > 2){
								intrf.transferOwnAccounts(u, sid, (int) args.get(0), (int) args.get(1), (Double) args.get(2)); 
								exit = true; acts = 0;
							}
						}
						break;
//				//deposit from bank account
//				case 3: argNames.clear();
//						argNames.add("Account ID");
//						argNames.add("Amount to deposit into account");
//						
//						argTypes.clear();
//						argTypes.add(Integer.class);
//						argTypes.add(Double.class);
//						
//						accIDs = new ArrayList<Object>();
//						for(Account acc : intrf.ts.getUserInfo(uid).getAccounts()){
//							if(acc.opened) accIDs.add(acc.getAccountNumber());
//						}
//
//						acceptableValues.add(accIDs);
//						acceptableValues.add(new ArrayList<Object>());
//						
//						if(accIDs.size() > 0){
//						//synchronized(userUI){
//							args = userUI.argsNeeded(argNames, argTypes, acceptableValues);
//						//}					
//							if(args.size() > 1){
//								if(intrf.depositFromExternal(u, sid, (int) args.get(0), (Double) args.get(1))){
//									//external.amount += (Double) args.get(1);
//								}
//								exit = true; acts = 0;
//							}
//						}
//						break;
//				//Transfer amount to another user's account
//				case 4: argNames.clear();
//						
//						argNames.add("To User ID");
//						
//						acceptableValues.clear();
//						
//						List<Object> userIDs = new ArrayList<Object>();
//						intrf.ts.getUsers().forEach(user -> userIDs.add(user.getId()));
//						acceptableValues.add(userIDs);
//
//						argTypes.clear();
//						argTypes.add(Integer.class);
//
//						if(userIDs.size() > 1){
//							args = userUI.argsNeeded(argNames, argTypes, acceptableValues);
//						
//							argNames.clear();
//							argNames.add("From Account ID");
//							argNames.add("To Account ID");
//							argNames.add("Amount to pay");
//							
//		
//							argTypes.clear();
//							argTypes.add(Integer.class);
//							argTypes.add(Integer.class);
//							argTypes.add(Double.class);
//							
//							List<Object> from_accIDs = new ArrayList<Object>();
//							for(Account acc : intrf.ts.getUserInfo(uid).getAccounts()){
//								if(acc.opened) from_accIDs.add(acc.getAccountNumber());
//							}
//			
//							acceptableValues.clear();
//							acceptableValues.add(from_accIDs);	
//							
//							List<Object> to_accIDs = new ArrayList<Object>();
//							int to_uid = (Integer) args.get(0);
//							
//							for(Account acc : intrf.ts.getUserInfo(to_uid).getAccounts()){
//								if(acc.opened) to_accIDs.add(acc.getAccountNumber());
//							}
//							
//							acceptableValues.add(to_accIDs);	
//
//							acceptableValues.add(new ArrayList<Object>());					
//			
//							if(from_accIDs.size() > 0
//									&& to_accIDs.size() > 0){
//							//synchronized(userUI){
//								args = userUI.argsNeeded(argNames, argTypes, acceptableValues);
//							//}			
//									
//								if(args.size() > 1){
//									intrf.transferToOtherAccount(u, UserInfo.getUserInfo(to_uid), sid, (int) args.get(0), (int) args.get(1), (Double) args.get(2)); 
//									exit = true; acts = 0;
//								}
//							}
//						}
//						break;

				default: exit = true; acts = 0; break;
			}
			
			//intrf.adminsTurn(uid);
		}
	}


}
