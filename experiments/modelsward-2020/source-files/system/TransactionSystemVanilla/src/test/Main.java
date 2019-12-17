package test;

import java.util.List;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import main.Menu;

public class Main {
	static String actions = "";
	
	public static void main(String[] args) throws IOException {
		actions = String.join("\\n", Files.readAllLines(new File(args[0]).toPath()));		
      //  InputStream inputStream = new ByteArrayInputStream(actions.getBytes(Charset.forName("UTF-8")));
        
        Menu.menu(new Scanner(new StringReader(actions)));
	}
	
	
}
