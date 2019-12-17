package main;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class Menu {
	static long idCounter = 0;
	public static Map<Long, User> users = new HashMap<Long, User>();
	static Set<User> loggedIn = new HashSet<User>();

	public static void menu(Scanner input){
		  int option;
		  User u = null;
		  boolean open = true;

//		  input.useDelimiter("\\n");
		  
		  while(open){
			  System.out.println("Choose option: ");
			  //try {
			  option = input.nextInt();//input.nextInt();
//			  } catch(Exception e) {
//				  e.printStackTrace();
//			  }
//			  input = input.skip("\\n");
			  switch(option) {
			      case 0: long id = input.nextLong();
//				  		  input = input.skip("\\n");
			              u = login(id);
			              System.out.println("login " + id);
			              break;
			      case 1: if(u != null){
							 logout(u.id);
							 System.out.println("logout " + u.id);
						   }
			               break;
			      case 2: if(u != null) {
							  double val = input.nextDouble();
		//					  		input = input.skip("\\n");
							  long to = input.nextLong();
		//					  		input = input.skip("\\n");
							  long from = u.id;
							  if (u.bal >= val) {
								  transact(from, to, val);
								  System.out.println("transact " + from + " " + to + " " + val);
							  } else {
								  System.out.println("failed-transact " + from + " " + to + " " + val);
							  }
						  }
			              break; 
			      case 3: if(u != null){
		    	  			double val = input.nextDouble(); 
//					  		input = input.skip("\\n");               
			                deposit(u.id, val);
				            System.out.println("deposit " + u.id + " " + val);
			              }
			              break; 
			      case 4: if(u != null) {
			    	  		delete(u);
				            System.out.println("delete " + u.id);
			    	  		u = null;
			      		  }
	              		  break; 
			      case 5: createUser();
				          System.out.println("createUser");
			    	      break; 
			      case 6: open = false;  
		          		  System.out.println("closed");
			  }
		}
	}

	public static User createUser(){
		User uu = new User();
		uu.id = idCounter;
		users.put(idCounter, uu);
		idCounter++;
		return uu;
	}

	public static void delete(User u){
		 users.remove(u.id);
	}

	public static void transact(long from, long to, double val){
		 if(users.get(from).bal >= val 
		      && users.containsKey(to)){
		   users.get(from).bal -= val;
		   users.get(to).bal += val;
		 }
	}

	public static void deposit(long to, double val){
		 if(users.containsKey(to)){
		   users.get(to).bal += val;
		 }
	}

	public static void withdraw(long from, double val){
		 if(users.containsKey(from)){
		   users.get(from).bal -= val;
		 }
	}
	
	public static User login(long id) {
		if(users.containsKey(id)){
			User u = users.get(id);
			if(u.activated) {
				return u;
			}
		}
		return null;
	}
	
	public static void logout(long id) {
		if(users.containsKey(id)){
			User u = users.get(id);
			loggedIn.remove(u);
		}
	}
}
