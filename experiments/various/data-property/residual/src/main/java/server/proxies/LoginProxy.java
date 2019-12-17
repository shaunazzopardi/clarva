package main.java.server.proxies;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import main.java.paymentapp.UserUI;
import main.java.server.entities.users.User.PrivacyLevel;
import main.java.server.interfaces.UserInterface;
import main.java.server.proxies.BronzeUserProxy;
import main.java.server.proxies.GoldUserProxy;
import main.java.server.proxies.SilverUserProxy;
import main.java.server.proxies.entityinfo.BronzeUserInfo;
import main.java.server.proxies.entityinfo.GoldUserInfo;
import main.java.server.proxies.entityinfo.SilverUserInfo;
import main.java.server.proxies.entityinfo.UserInfo;

public class LoginProxy {

//	public static LoginProxy proxy;
	public static UserInfo external = new UserInfo(-1);
	public static UserInterface intrf = UserInterface.ui;
	
//	public LoginProxy(){
//		intrf;
//		proxy = this;
//	}
	
	public static synchronized void initialMenu(UserUI userUI){//, long end){
		boolean exit = false;

		List<String> argNames = new ArrayList<String>();
		List<Class> argTypes = new ArrayList<Class>();
		
		List<List<Object>> acceptableValues = new ArrayList<List<Object>>();
		
		List<Object> args;
		
		while(!exit){// && (end == -1 || System.currentTimeMillis() < end)){			
			List<String> options = new ArrayList<String>();
			options.add("(1) to create new user");
			options.add("(2) to login");
			
			int choice = userUI.currentOptions(0,options);
			if(choice == -1) return;
			
			acceptableValues.clear();
			argNames.clear();
			argTypes.clear();
			
			//added for randomized users
			if(userUI.uid != null) {
				choice = 2;
			}
			
			switch(choice){
			case 1: 
					argNames.clear();
					argNames.add("Name");
					argNames.add("Country");
					
					argTypes.clear();
					argTypes.add(String.class);
					argTypes.add(String.class);
					
					List<Object> names = new ArrayList<Object>();
					names.add("GenericName");
					
					acceptableValues.clear();
					acceptableValues.add(names);
					acceptableValues.add(UserInterface.countries);					
					
				//	synchronized(userUI){
						args = userUI.argsNeeded(argNames, argTypes, acceptableValues);
					//}					intrf.createUser((String) args.get(0), (String) args.get(1));
						
					if(args.size() >= 2){
						try {
							userUI.uid = intrf.createUser((String) args.get(0), (String) args.get(1)).getId();
							if(userUI.uid != null)
								System.out.println("User " + userUI.uid + " created.");
						} catch(Exception e) {
							
						}
					}
					break;
			case 2: 
				
					argNames.clear();
//					argNames.add("User ID");
//					
//					argTypes.clear();
//					argTypes.add(Integer.class);
//					
//					acceptableValues.clear();
//					
//					List<Object> userIDs = new ArrayList<Object>();
//					for(int i = 0 ; i < intrf.ts.getUsers().size(); i++) {
//						userIDs.add(intrf.ts.getUsers().get(i).getId());
//					}
//					acceptableValues.add(userIDs);					
					
//				if(userIDs.size() > 0){
					if(userUI.uid != null){
				///	synchronized(userUI){
//						args = userUI.argsNeeded(argNames, argTypes, acceptableValues);
					//}
						System.out.println("User " + userUI.uid + " logged in.");

//						if(args.size() > 0){
							int uid = userUI.uid;
							UserInfo user = UserInfo.getUserInfo(uid);
							if(user.getClass().equals(GoldUserInfo.class)){
								GoldUserInfo u = (GoldUserInfo) user;

								int sid = intrf.login(u);
								//u = new UserInfo(uid);
								if(sid != -1){
									GoldUserProxy.loggedInMenu(userUI, u, sid);
								}
							}
							else if(user.getClass().equals(SilverUserInfo.class)){
								SilverUserInfo u = (SilverUserInfo) user;

								int sid = intrf.login(u);
								//u = new UserInfo(uid);
								if(sid != -1){
									SilverUserProxy.loggedInMenu(userUI, u, sid);
								}
							}
							else if(user.getClass().equals(BronzeUserInfo.class)){
								BronzeUserInfo u = (BronzeUserInfo) user;

								int sid = intrf.login(u);
								//u = new UserInfo(uid);
								if(sid != -1){
									BronzeUserProxy.loggedInMenu(userUI, u, sid);
								}
							}
						}
//					}
					break;
			default:
			}
		
		}
	}
}
