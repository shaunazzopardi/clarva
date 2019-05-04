package clarva;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import clarva.java.PropertyTransformer;
import com.google.common.io.Files;
import soot.G;
import soot.JastAddJ.Opt;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.options.Options;

import javax.swing.text.html.Option;

import static soot.options.Options.*;

public class Main {

	public static String rootOutputDir = "./output-files/";
	public static String outputDir = "./output-files/dava/classes";


	public static void main(String[] args) {
		if(args.length < 3) {
			System.out.println("Arguments must specify: (0) language to be analysed (options: java) " +
					"(1) property files followed by the " +
					"(2) program directory, and finally the (3) main class.");
			return;
		}

		if(args[0].equals("java")) {
			List<String> properties = Arrays.asList(Arrays.copyOfRange(args, 1, args.length - 2));
//		properties.remove(properties.size()-1);

			String programPath = args[args.length - 2];
			String mainClass = args[args.length - 1];

			PropertyTransformer.generateFiniteStateMachines(properties);

			Set<String> packagesToConsider = new HashSet<String>();

			for (fsm.date.Global global : PropertyTransformer.dateFSMHierarchy) {
				packagesToConsider.addAll(Arrays.asList(global.imports.replaceAll(";", "").replaceAll("import ", "").replaceAll("\r", "").replaceAll(" ", "").split("\n")));
			}
			packagesToConsider.remove("");
			//must add all libraries imported by class files
			packagesToConsider.add("java.lang.Thread");
//			packagesToConsider.add("java.util.*");
//			//	packagesToConsider.add("java.io.*");
		   	initializeSoot(mainClass, programPath, packagesToConsider);

			G.reset();

			//this generates java files
			if(!PropertyTransformer.allSatisfied) {
				soot.Main.main(new String[]{"-output-dir", rootOutputDir, "-cp", outputDir, "-allow-phantom-refs", "-f", "dava", "-process-dir", outputDir});
			}

			System.exit(0);
		}
		else{
			System.out.println("Option " + args[0] + " not supported.");
		}

	}
	
	  @SuppressWarnings("static-access")
	  private static void initializeSoot(String mainClass, String sootCp, Set<String> packagesToConsider) {
	    G.v().reset();
	    Options.v().set_whole_program(true);
//	    Options.v().setPhaseOption("jtp", "enabled:true");
	    Options.v().setPhaseOption("jb", "use-original-names:true");
	    Options.v().setPhaseOption("cg.spark", "enabled:true");
	//    Options.v().setPhaseOption("cg.spark", "on-fly-cg:true");
	//    Options.v().setPhaseOption("cg.spark", "apponly:true");
	//    String userdir = System.getProperty("user.dir");
	//    String sootCp = userdir + "/targets";

	    Options.v().set_no_bodies_for_excluded(true);
	    
	    List<String> packagesToExclude = new ArrayList<String>();
	    packagesToExclude.add("java.*");
	    packagesToExclude.add("jdk.*");
	    Options.v().set_exclude(packagesToExclude);
	    
//	    Options.v().set_include(new ArrayList<String>(packagesToConsider));
	    
	    Options.v().setPhaseOption("cg.spark", "geom-pta:true");
	    Options.v().setPhaseOption("cg.spark", "geom-runs:2");
//	    Options.v().set_soot_classpath(sootCp);
//	    Options.v().set_prepend_classpath(true);
	    Options.v().set_allow_phantom_refs(true);
	    Options.v().keep_line_number();
	    Options.v().set_main_class(mainClass);

	    Options.v().set_output_dir(outputDir);

		  try {
			  Files.createParentDirs(new File(outputDir));
		  } catch (IOException e) {
			  e.printStackTrace();
		  }

//	    Options.v().out();
	    Options.v().set_output_format(output_format_class);
	    Options.v().dump_body();
		  Options.v().set_process_dir(Arrays.asList(new String[]{sootCp}));
//	    Options.v().set_oaat(true);
//	    Options.v().set_output_format(Options.output_format_dava);
//	    Options.v().set_output_format(Options.output_format_class);
		  Options.v().set_whole_program(true);

	    Scene.v().addBasicClass(mainClass, SootClass.BODIES);
	    Scene.v().loadNecessaryClasses();

	    SootClass c = Scene.v().forceResolve(mainClass, SootClass.BODIES);
	    if (c != null) {
	      c.setApplicationClass();
	    }
	    SootMethod methodByName = c.getMethodByName("main");
	    List<SootMethod> ePoints = new LinkedList<>();
	    ePoints.add(methodByName);
	    Scene.v().setEntryPoints(ePoints);
	    // Add a transformer
	    PackManager.v().getPack("wjtp")
	        .add(new Transform("wjtp.PropertyTransformer", new PropertyTransformer()));

//	    soot.Main.main(new String[]{});

	    PackManager.v().getPack("cg").apply();
	    PackManager.v().getPack("wjtp").apply();


	    if(!PropertyTransformer.allSatisfied) {
			try {
				PackManager.v().writeOutput();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	  }
	
}
