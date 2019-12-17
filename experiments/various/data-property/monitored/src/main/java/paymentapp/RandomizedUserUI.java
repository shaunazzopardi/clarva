package main.java.paymentapp;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Timer;

import main.java.adminapp.RandomResponsiveAdmin;
import main.java.paymentapp.UserUI;
import main.java.server.TransactionServer;
import main.java.server.entities.companies.Company;
import main.java.server.entities.users.User;
import main.java.server.interfaces.AdminInterface;
import main.java.server.interfaces.UserInterface;
import main.java.server.proxies.BronzeUserProxy;
import main.java.server.proxies.GoldUserProxy;
import main.java.server.proxies.LoginProxy;
import main.java.server.proxies.SilverUserProxy;


public class RandomizedUserUI extends UserUI {

	public Timer timer;
	public List<Integer> accountsRequested;
	public List<Integer> accountsApproved;
	
	public Integer noOfActions;
	
	static List<Integer> userIds = new ArrayList<Integer>();
	static Random rand;
	//UserInfo u;
	
	public static List<Object> nationalIDs = new ArrayList<Object>();
	
	static {
		for(int i = 0; i < 2000; i++) {
			nationalIDs.add("" + i);
		}
	}	
	
	public static List<Object> addresses = new ArrayList<Object>();
	
	static {
		addresses.add("White Street");
		addresses.add("Blue Street");
		addresses.add("Black Street");
		addresses.add("Red Street");
	}
	
	public static List<Object> names = new ArrayList<Object>();
	
	static {
		names.add("Jack");
		names.add("Jill");
		names.add("John");
		names.add("Jane");
		names.add("Erin");
		names.add("Nico");
	}

//	//for analysis
//	public static void main(String[] args){
//	//	start = System.currentTimeMillis();
//		//this.end = start + end*1000; // to turn into milliseconds
//		Random rand = new Random(1323);
//
//		List<Integer> noOfActions = new ArrayList<Integer>();
//
//		TransactionServer ts = new TransactionServer();
//		//UserProxy proxy = new UserProxy();
//		UserInterface ui = new UserInterface();
//
//		for(int i = 0; i < 100; i++){
//			ts.addCompany(rand.nextInt() + "", (String) UserInterface.countries.get(rand.nextInt(ui.countries.size())));
//
//			if(i % 9 == 0) ts.companies.get(i).greylist();
//		}
//
//		RandomResponsiveAdmin admin = new RandomResponsiveAdmin(37, ts);
//        AdminInterface.adminInterface = new AdminInterface(TransactionServer.ts);
//        AdminInterface.adminInterface.admin = admin;
//
//		int seed = 0;
//
//		List<RandomizedUserUI> userUis = new ArrayList<RandomizedUserUI>();
//
//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
//		PrintStream ps = new PrintStream(baos);
//
//		new LoginProxy();
//		new GoldUserProxy();
//		new SilverUserProxy();
//		new BronzeUserProxy();		
//		
//		RandomizedUserUI user = new RandomizedUserUI(seed, ps);
//		
//		while(rand.nextInt() != 78) {
//			user.run();
//			AdminInterface.adminInterface.adminsTurn(user.uid);
//
//		}
//	}
//	
//	
	static int userId = -1;

	public RandomizedUserUI(int seed, PrintStream ps, Integer noOfActions){
		super(ps);
		
		rand = new Random(seed);
				
		this.noOfActions = noOfActions;
	}
	
	boolean createUser = true;
	boolean loginUser = true;
	boolean createAcc = true;
	boolean openAccMenu = true;
	boolean createCard = true;
	boolean openCardMenu = true;
	
	@Override
	public synchronized int currentOptions(int depth, List<String> options){
	//	ps.println("UI ID: " + this.uiID + "| Enter: ");

//		for(int i = 0; i < options.size(); i++){
//			ps.println(options.get(i));
//		}
//		try {
//			this.wait();
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		if(noOfActions <= 0) {
//			try {
//				this.wait();
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			return -1;
		} else{
			noOfActions--;
		}
		
		
		if(!createUser && !createAcc && !loginUser && !openAccMenu && !createCard && !openCardMenu){
			synchronized(this){
//				this.notifyAll();
//				try {
//					this.wait();
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
			
				int choice;
				
				//if logged in menu
				
				if(options.size() > 6){
					//9/10 chance to choose transaction menu
					if(depth == 1 && rand.nextInt(10) < 9){
						choice = 3;
					}
					else choice = rand.nextInt(options.size()) + 1;
				}
				else{ 
					choice = rand.nextInt(options.size()) + 1;
				}
				
				return choice;
			}
		}
//		else if(this.uid == null) {
//			return 1;
//		}
		else if(createUser){
			createUser = false;
			return 1;
		}
		else if(loginUser){
			loginUser = false;
			return 2;
		}
		else if(openAccMenu){
			openAccMenu = false;
			return 1;
		}
		else if(createAcc){
			if(rand.nextInt(20) < 15){
				createAcc = false;
				return 1;
			}
			return 1;
		}
		else if(openCardMenu){
			openCardMenu = false;
			return 2;
		}
		else{
//			System.out.println("here");
			
//			if(rand.nextInt(20) < 15){
//				createCard = false;
//				return -1;
//			}
			
			return rand.nextInt(options.size()) + 1;
		}
		
	//	return choice;
	}
	
	public List<Object> argsNeeded(List<String> argNames, List<Class> argTypes, List<List<Object>> acceptableValues){
		List<Object> args = new ArrayList<Object>();
		
		for(int i = 0; i < argTypes.size(); i++){
		//	if(argTypes.get(i).isPrimitive()){
	//			ps.println("Enter the " + argNames.get(i) + ":");

				int bound = acceptableValues.get(i).size();
				int index;
				if(bound > 0) index = rand.nextInt(bound);
				else index = 0;
				
				if(argTypes.get(i).equals(int.class)
						|| argTypes.get(i).equals(Integer.class)){
					if(argNames.get(i).equals("User ID")
							&& uid != null) args.add(uid);
					else args.add(acceptableValues.get(i).get(index));
				}
				else if(argTypes.get(i).equals(double.class)
						|| argTypes.get(i).equals(Double.class)){
					double rangeMin = 1;
					double rangeMax = 1000;
					double randomValue = rangeMin + (rangeMax - rangeMin) * rand.nextDouble();

					args.add(randomValue);
				}
				else if(argTypes.get(i).equals(String.class)){
					args.add(acceptableValues.get(i).get(index));
				}
				else if(argTypes.get(i).equals(float.class)
						|| argTypes.get(i).equals(Float.class)){
					args.add(acceptableValues.get(i).get(index));
				}
		//	}
		}
		
		for(int i = 0; i < args.size(); i++){
	//		ps.println(args.get(i));
		}
		
		return args;
	}

	public void output(String output){
//		ps.println(output);
	}
	
}
