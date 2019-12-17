package main.java.adminapp;

import java.util.Map;
import java.util.Map.Entry;

import main.java.server.TransactionServer;
import main.java.server.entities.accounts.Account;
import main.java.server.entities.users.User;
import main.java.server.interfaces.AdminInterface;

import java.util.Scanner;

public class AdminUI {
	protected static Scanner in = new Scanner(System.in);
	static AdminInterface adminInterface;
	
	public static void main(String[] args){
		boolean exit = false;
		
		while(!exit){
			int num = in.nextInt();
			
			switch(num){
				//see users not activated
				case 1: printOutDisabledUsers(); break;
				//print non-approved accounts
				case 2: printOutDisabledAccount(); break;
				//activate user
				case 3: adminInterface.activateUser(in.nextInt()); break;
				//approve account
				case 4: adminInterface.approveAccount(in.nextInt(), in.nextInt()); break;
				//blacklist user
				case 5: adminInterface.blacklistUser(in.nextInt()); break;
				//greylist user
				case 6: adminInterface.greylistUser(in.nextInt()); break;
				//whitelist user
				case 7: adminInterface.whitelistUser(in.nextInt()); break;
			}
		}
	}
	
	public static void printOutDisabledUsers(){		
		for(Entry<Integer, User> entry : adminInterface.usersToActivate.entrySet()){
			System.out.println(entry.getKey() + " - " + entry.getValue());
		}
	}
	
	public static void printOutDisabledAccount(){		
		for(Entry<User, Map<Integer, Account>> entry : adminInterface.accountsToApprove.entrySet()){
			for(Entry<Integer, Account> accountEntry : entry.getValue().entrySet()){
				System.out.println("User" + entry.getKey() + " - Account no " + accountEntry.getKey());
			}
		}
	}
	
	public static void initialise(){
		adminInterface = new AdminInterface(TransactionServer.ts);
		AdminInterface.adminInterface = adminInterface;
	}

}
