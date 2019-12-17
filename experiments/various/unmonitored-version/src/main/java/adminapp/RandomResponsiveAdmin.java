package main.java.adminapp;

import java.util.Random;

import main.java.server.TransactionServer;
import main.java.server.interfaces.AdminInterface;

public class RandomResponsiveAdmin{

	static AdminInterface adminInterface;
	public Random rand;
	
	public RandomResponsiveAdmin(int seed, TransactionServer ts){
		rand = new Random(seed);
		adminInterface = new AdminInterface(ts);
		adminInterface.admin = this;
	}
	
	public boolean yesOrNo(){
		return rand.nextBoolean();
	}
}
