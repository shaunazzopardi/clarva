package main.java.test.java;

import main.java.adminapp.RandomResponsiveAdmin;
import main.java.paymentapp.RandomizedUserUI;
import main.java.server.TransactionServer;
import main.java.server.interfaces.AdminInterface;
import main.java.server.interfaces.UserInterface;
import main.java.server.proxies.BronzeUserProxy;
import main.java.server.proxies.GoldUserProxy;
import main.java.server.proxies.LoginProxy;
import main.java.server.proxies.SilverUserProxy;
import main.java.server.proxies.entityinfo.UserInfo;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class Main {

	static List<RandomizedUserUI> userUis;
	static List<Thread> threads;
	public static Thread mainThread;
	public static Object lock;
	public static int noOfTimesOffline = 0;
	
	public static void main(String[] args){
		int noOfUsers = Integer.parseInt(args[0]);

		long now = System.currentTimeMillis();

		try {
			Main.runExperiment(noOfUsers);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		stopAllThreads(threads);
//		System.gc();

//		long taken = System.currentTimeMillis() - now;

//		System.out.println(taken);
//		System.out.println(SC.riskc);
//		System.out.println(SC.trans1);
//		System.out.println(SC.trans2);
//		System.out.println(SC.trans3);
//
//		System.out.println("taken");
//		for(Integer key : _cls_BlacklistingWithOfflineOrig0.transCheckedAndTaken.keySet()){
//			System.out.println(key + " --> " + _cls_BlacklistingWithOfflineOrig0.transCheckedAndTaken.get(key));
//		}
//
//		System.out.println("not taken");
//		for(Integer key : _cls_BlacklistingWithOfflineOrig0.transCheckedButNotTaken.keySet()){
//			System.out.println(key + " --> " + _cls_BlacklistingWithOfflineOrig0.transCheckedButNotTaken.get(key));
//		}
//		System.out.println(noOfTimesOffline);
	}
	
	public static void runExperiment(int noOfUsers) throws InterruptedException{

		lock = new Object();

		mainThread = Thread.currentThread();
		Random rand = new Random(1323);
		List<Integer> noOfActions = new ArrayList<Integer>();

		TransactionServer ts = new TransactionServer();
		//UserProxy proxy = new UserProxy();
		UserInterface ui = new UserInterface();

		for(int i = 0; i < 100; i++){
			ts.addCompany(rand.nextInt() + "", (String) UserInterface.countries.get(rand.nextInt(ui.countries.size())));

			if(i % 9 == 0) ts.getCompanies().get(i).greylist();
		}

		RandomResponsiveAdmin admin = new RandomResponsiveAdmin(37, ts);
        AdminInterface.adminInterface = new AdminInterface(TransactionServer.ts);
        AdminInterface.adminInterface.admin = admin;

		int seed = 0;

		userUis = new ArrayList<RandomizedUserUI>();
		threads = new ArrayList<Thread>();

//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = System.out;

//		new LoginProxy();
//		new GoldUserProxy();
//		new SilverUserProxy();
//		new BronzeUserProxy();
		//PrintStream ps = System.out;

		while(seed < noOfUsers){//StartJMH.noOfUsers){
			RandomizedUserUI user = new RandomizedUserUI(seed, ps, rand.nextInt(5000) + 400);
			Thread thread = new Thread(user);
			threads.add(thread);
			synchronized(user){
				thread.start();
				//user.wait();
				userUis.add(user);
				seed++;
			}
		}

		boolean stop = false;

		int counterOffline = 0;


		
		while(!stop){
//				System.gc();
				int userUi;

				//do{
					userUi = rand.nextInt(1000) % userUis.size();
					
				//}while(userUi >= userUis.size());

				boolean adminsTurn = rand.nextInt(10) < 3;

				if(adminsTurn){
					try {
						AdminInterface.adminInterface.adminsTurn(userUis.get(userUi).uid);
					} catch (Exception e) {
						
					}
				}


				if(ts.online){
					boolean transactionserveroffline = rand.nextInt(20) < 3;
					if(transactionserveroffline) {
						ts.online = false;
						noOfTimesOffline++;
					}
				}
				else{
					counterOffline++;
					if(counterOffline > 50 && rand.nextBoolean()){
						counterOffline = 0;
						ts.online = true;
//						int c = 0;

						try {
							UserInfo.fulfillTransactions();
						} catch (Exception e) {
							
						}
//								c++;

//						System.out.println(c);
					}
				}

				{
					if(new HashSet<Integer>(noOfActions).size() == 1 && noOfActions.contains(0)){
						ps.println("End");
						stop = true;
						stopAllThreads(threads);
					}
					else{
//						while(noOfActions.get(userUi) <= 0){
//							userUi = rand.nextInt(userUis.size());
//						}

//						do{
//							userUi = rand.nextInt(1300) % userUis.size();
//						}while(noOfActions.get(userUi) <= 0);
//
//						int noOfActionsNow;
//						if(noOfActions.get(userUi) > 5)
//							noOfActionsNow = rand.nextInt(5) + 100;
//						else noOfActionsNow = rand.nextInt(noOfActions.get(userUi)) + 1;
//
//						int left = noOfActions.get(userUi) - noOfActionsNow;
//						
//						if(left <= 0) {
//							noOfActions.set(userUi, 0);
//						}
//						else noOfActions.set(userUi, noOfActions.get(userUi) - noOfActionsNow);
//
//						while(noOfActionsNow > 0){
//							try {
//								synchronized(userUis.get(userUi)){
////									threads.get(userUi).notify();
//									userUis.get(userUi).notifyAll();
//									Thread.State state = threads.get(userUi).getState();
//									while(!threads.get(userUi).getState().equals(Thread.State.BLOCKED)) {
//										
//									}
//								}
//							} catch (Exception e) {
//								
//							}
//						//		synchronized(mainThread){
//								//mainThread.wait();
//							//	}
//							noOfActionsNow--;
//
//						}
						
//						if(noOfActions.get(userUi) <= 0) {
//							while(!threads.get(userUi).getState().equals(Thread.State.WAITING)) {
//								
//							}
//							threads.get(userUi).interrupt();
//						}
					}
				}
				
				boolean oneStillRunning = false;
				
				for(RandomizedUserUI userUI : userUis){
					if(userUI.noOfActions > 0) {
						oneStillRunning = true;
						continue;
					}
				}
				
				if(!oneStillRunning) {
					stop = true;
				}
			}

		}

	public static void stopAllThreads(List<Thread> threads){
		for(Thread thread : threads){
//			while(!thread.getState().equals(Thread.State.BLOCKED)) {
//				
//			}
			thread.stop();
		}
	}

//	public static void waitThis(UserUI ui){
//		try {
//			threads.get(userUis.indexOf(ui)).wait();
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
}
