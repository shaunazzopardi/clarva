package main.java.paymentapp;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import main.java.adminapp.RandomResponsiveAdmin;
import main.java.server.TransactionServer;
import main.java.server.interfaces.AdminInterface;
import main.java.server.interfaces.UserInterface;
import main.java.server.proxies.BronzeUserProxy;
import main.java.server.proxies.GoldUserProxy;
import main.java.server.proxies.LoginProxy;
import main.java.server.proxies.SilverUserProxy;

public class UserUI implements Runnable{

	public Thread t;
	public Integer uid;
	public int uiID;
	static int uiIDCounter = 0;
	static Scanner in;
	PrintStream ps;
	//UserInfo u;
	long start;
	long end;
	
	public UserUI(PrintStream ps){				
		in = new Scanner(System.in);
		
		this.ps = ps;
		
		uiID = uiIDCounter;
		uiIDCounter++;
	}

	@Override
	public void run(){
	//	start = System.currentTimeMillis();
		//this.end = start + end*1000; // to turn into milliseconds	
		LoginProxy.initialMenu(this);//, this.end);
	}
	

	
	public void start(){
		if(t == null){
			t = new Thread(this);
			t.start();
		}
	}
	
	public synchronized int currentOptions(int depth, List<String> options){
		ps.println("Enter: ");

		for(int i = 0; i < options.size(); i++){
			ps.println(options.get(i) + "\n");
		}
			
		return in.nextInt();
	}
	
	public List<Object> argsNeeded(List<String> argNames, List<Class> argTypes, List<List<Object>> acceptableValues){
		List<Object> args = new ArrayList<Object>();
		
		for(int i = 0; i < argTypes.size(); i++){
			if(argTypes.get(i).isPrimitive()){
				ps.println("UI ID: " + this.uiID + " | Enter the " + argNames.get(i) + ":");

				if(argTypes.get(i).getClass().equals(Integer.class)){
					int input = in.nextInt();
					if(acceptableValues.get(i).contains(input)){
						args.add(input);
					}
					else{
						i--;
					}
				}
				else if(argTypes.get(i).getClass().equals(Double.class)){
					args.add(in.nextDouble());
				}
				else if(argTypes.get(i).getClass().equals(String.class)){
					String input = in.nextLine();
					if(acceptableValues.get(i).contains(input)){
						args.add(input);
					}
					else{
						i--;
					}
				}
				else if(argTypes.get(i).getClass().equals(float.class)){
					args.add(in.nextFloat());
				}
			}
		}
		
		return args;
	}

	public void output(String output){
		ps.println(output);
	}


}
