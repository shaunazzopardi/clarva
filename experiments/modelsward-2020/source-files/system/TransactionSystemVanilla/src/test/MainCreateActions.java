package test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import main.Menu;

public class MainCreateActions {
	static String actions = "";
	static List<Integer> userNoOfActions = new ArrayList<Integer>();
	static List<List<String>> bunches = new ArrayList<List<String>>();

	static int noUsers = 0;
	
	static int seed = 1492746;
	static Random rand = new Random(seed);
	
	public static void main(String[] args) throws IOException {
		for(int i = 0; i < args.length; i++) {
			noUsers = Integer.parseInt(args[i]);
			
			createNumberOfUsers();
			doActions();
			
			Files.write(new File(args[i] + ".txt").toPath(), actions.getBytes(), new OpenOption[0]);
			
			actions = "";
		}
	}
	
	public static void createNumberOfUsers() {
		int no = noUsers;
		while(no > 0) {
			//create user
			actions += "5 ";
			userNoOfActions.add(5000 + rand.nextInt(1));
			no--;
		}
	}
	
	public static void doActions() {
			for(int i = 0; i < noUsers; i++) {
				if(userNoOfActions.get(i) > 0) {
					int bunch = rand.nextInt(500) + 500;
					userNoOfActions.set(i, userNoOfActions.get(i) - bunch);
					//login
					actions += "0 " + i + " ";

					while(bunch > 0) {
						int act = rand.nextInt(2) + 2;
						actions += act + " ";
						if(act == 2) {
							actions += rand.nextDouble() + " " + rand.nextInt(noUsers) + " ";
						} else if(act == 3) {
							actions += rand.nextDouble()*1000 + " ";
						}
						bunch--;
					}

					//logout
					actions += "1 ";

				}
			}


		for(int i = 0; i < noUsers; i++) {
			if(rand.nextBoolean()) {
				//login
				actions += "0 " + i + " ";

				//delete user
				actions += "4 ";
			}
		}
		
		actions += 6;
	}
}
