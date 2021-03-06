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
import main.java.server.proxies.entityinfo.BronzeUserInfo;
import main.java.server.proxies.entityinfo.UserInfo;

public class BronzeUserProxy {

//	public static BronzeUserProxy proxy;
	public static BronzeUserInfo external = new BronzeUserInfo(-1);
	public static UserInterface intrf = UserInterface.ui;
		
//	public BronzeUserProxy(){
//		intrf = UserInterface.ui;
//		proxy = this;
//	}

	public static synchronized void loggedInMenu(UserUI userUI, BronzeUserInfo u, int sid){//, long end){
		boolean exit = false;

		List<String> argNames = new ArrayList<String>();
		List<Class> argTypes = new ArrayList<Class>();
		
		List<List<Object>> acceptableValues = new ArrayList<List<Object>>();

		List<Object> args;
		
		int acts = 0;
		
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
			
			int uid;
			
			acceptableValues.clear();
			argNames.clear();
			argTypes.clear();
			switch(choice){
				//create account
				case 1: accountMenu(userUI, u, sid); //break;
						//}
				case 2: cardMenu(userUI, u, sid);// break;
				//Transaction Menu
				case 3: if(!intrf.checkIfFrozen(u)){
							
								u.whitelist(); 
							
								transactionMenu(userUI, u, sid);//, end);

						}
						break;
//				case 4: intrf.freezeUserAccount(u, sid); u.freeze(); break;
//				case 5: intrf.unfreezeUserAccount(u, sid); u.unfreeze(); break;
				case 4: //de-register
					argNames.clear();
					argNames.add("User ID");

					argTypes.clear();
					argTypes.add(Integer.class);

					acceptableValues.clear();
					
					
					List<Object> userIDs = new ArrayList<Object>();
					List<User> users = intrf.ts.getUsers();
					for(int i = 0 ; i < users.size(); i++) {
						userIDs.add(users.get(i).getId());
					}
					acceptableValues.add(userIDs);					

					if(userIDs.size() > 0){
						///	synchronized(userUI){
						args = userUI.argsNeeded(argNames, argTypes, acceptableValues);
						//}
						if(args.size() > 0){
							uid = (Integer) args.get(0);

							UserInfo.getUserInfo(uid).deregisterUser();
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
				users = intrf.ts.getUsers();
				for(int i = 0 ; i < users.size(); i++) {
					userIDs.add(users.get(i).getId());
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
					}
				}
				break;
//				default: intrf.logout(u, sid); exit = true; acts = 0; acts = 0; break;
			}
			
			//intrf.adminsTurn(uid);
		}
	}

	public static synchronized void accountMenu(UserUI userUI, BronzeUserInfo u, int sid){//, long end){
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
							System.out.println("Account requested with ID: " + intrf.requestAccount(u, sid)); break;
						//}
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
//						}
//						exit = true; acts++;
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
						exit = true; acts ++;
						break;
				default: exit = true; acts = 0; break;
			}
			
			//intrf.adminsTurn(uid);
		}
	}

	public static synchronized void cardMenu(UserUI userUI, BronzeUserInfo u, int sid){//, long end){
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
			options.add("(1) to request gift card for user");
//			options.add("(2) to request anonymous prepaid un-reloadable card");
//			options.add("(3) to freeze a card");
//			options.add("(4) to unfreeze a card");
//			options.add("(5) to close a card");
			options.add("(2) to check balance on card");
//			options.add("(7) to go back to main menu");
			
			int choice = userUI.currentOptions(2, options);
			if(choice == -1) return;

			acceptableValues.clear();
			argNames.clear();
			argTypes.clear();
			switch(choice){
				//create account
				case 1: argNames.clear();
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
						if(companyIDs.size() > 0){
					//	synchronized(userUI){
							args = userUI.argsNeeded(argNames, argTypes, acceptableValues);
						//}
							
							if(args.size() > 0){
								System.out.println("Gift Card requested for user with ID " + (Integer) args.get(0) 
									+ "with ID: " + intrf.requestGiftCard(u, sid, (Integer) args.get(0),  (Integer) args.get(1),  (Double) args.get(2))); 
							}
						}
						
						break;
//				case 2: argNames.clear();
//						argNames.add("Account ID");
//						argNames.add("Company ID");
//						argNames.add("Amount to pay");
//		
//						argTypes.clear();
//						argTypes.add(Integer.class);
//						argTypes.add(Integer.class);
//						argTypes.add(Double.class);
//		
//						ArrayList<Object> accIDs = new ArrayList<Object>();
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
//						if(accIDs.size() > 0 && companyIDs.size() > 0){
//						//synchronized(userUI){
//							args = userUI.argsNeeded(argNames, argTypes, acceptableValues);
//						//}			
//								
//							if(args.size() > 1){
//								if(intrf.payToCompany(u, sid, (int) args.get(0), (int) args.get(1), (Double) args.get(2))){
//									System.out.println("Payment from user " + u.getId() + " " + sid);
//								}
//								exit = true; acts = 0;
//							}
//						}
//						break;
//				case 3: argNames.clear();
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
//				case 4: argNames.clear();
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
//				case 5: argNames.clear();
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
				case 2:	argNames.clear();
						argNames.add("Card ID");
						
						argTypes.clear();
						argTypes.add(Integer.class);
						
						acceptableValues.clear();
						ArrayList<Object> cardIDs = new ArrayList<Object>();
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
			
			//intrf.adminsTurn(uid);3
		}
	}
	
	public static synchronized void transactionMenu(UserUI userUI, BronzeUserInfo u, int sid){//, long end){
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
//			options.add("(2) to go back to the main menu");
			
			int choice = userUI.currentOptions(2, options);
			if(choice == -1) return;
			
			acceptableValues.clear();
			argNames.clear();
			argTypes.clear();
			switch(choice){
			//payment from card
			case 1: argNames.clear();
					argNames.add("Card ID");
					argNames.add("Company ID");
//					argNames.add("Amount to pay");
	
					argTypes.clear();
					argTypes.add(Integer.class);
					argTypes.add(Integer.class);
//					argTypes.add(Double.class);				
	
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

//					acceptableValues.add(new ArrayList<Object>());					
	
					if(cardIDs.size() > 0 && companyIDs.size() > 0){
					//synchronized(userUI){
						args = userUI.argsNeeded(argNames, argTypes, acceptableValues);
					//}		
						Double totalAmountAvailable = intrf.checkCard(u, sid, (int) args.get(1));
						Double transactionAmount = new java.util.Random((int) ((totalAmountAvailable.intValue()))).nextDouble();	
						
						if(args.size() > 1){
							if(intrf.payToCompanyFromCard(u, sid, (Integer) args.get(0), (Integer) args.get(1), transactionAmount)){
								System.out.println("Payment from user " + u.getId() + ".");
							}
						}
					}
					break;
				default: exit = true; acts = 0; break;
			}
			
			//intrf.adminsTurn(uid);
		}
	}

}
