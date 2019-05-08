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
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import fj.data.Option;
import fsm.helper.Pair;
import soot.*;
import soot.JastAddJ.Opt;
import soot.options.Options;
import soot.util.Chain;

import static soot.options.Options.*;

public class Main {

	public static String rootOutputDir = "./output-files/";
	public static String outputDir = "./output-files/dava/classes";


	public static void main(String[] args) {
		if(args.length < 3) {
			System.out.println("Arguments must specify: (0) language to be analysed (options: java) " +
					"(1) property file followed by the " +
					"(2) program directory, " +
					"(3) the main class, and " +
					"finally any other (4) entry methods to the application (e.g. the run method of any Thread, or the methods offered by an API).");
			return;
		}

		if(args[0].equals("java")) {
			String property = args[1];//Arrays.asList(Arrays.copyOfRange(args, 1, args.length - 2));
//		properties.remove(properties.size()-1);
			List<String> properties = new ArrayList<>();
			properties.add(property);

			String programPath = args[2];
			String mainClass = args[3];
			List<String> entryPoints = new ArrayList<>();
			for(int i = 4; i < args.length; i++) {
				entryPoints.add(args[args.length - 1]);
			}

			PropertyTransformer.generateFiniteStateMachines(properties);

			Set<String> packagesToConsider = new HashSet<String>();

			for (fsm.date.Global global : PropertyTransformer.dateFSMHierarchy) {
				packagesToConsider.addAll(Arrays.asList(global.imports.replaceAll(";", "").replaceAll("import ", "").replaceAll("\r", "").replaceAll(" ", "").split("\n")));
			}
			packagesToConsider.remove("");
			//must add all libraries imported by class files
//			packagesToConsider.add("java.lang.invoke.LambdaMetafactory");
		//	packagesToConsider.add("java.util.*");
//			//	packagesToConsider.add("java.io.*");
		   	initializeSoot(mainClass, entryPoints, programPath, packagesToConsider);

//			G.reset();

//			//this generates java files
//			if(!PropertyTransformer.allSatisfied) {
//				soot.Main.main(new String[]{"-output-dir", rootOutputDir, "-cp", outputDir, "-allow-phantom-refs", "-f", "dava", "-process-dir", outputDir});
//			}

			System.exit(0);
		}
		else{
			System.out.println("Option " + args[0] + " not supported.");
		}

	}
	
	  @SuppressWarnings("static-access")
	  private static void initializeSoot(String mainClass, List<String> entryPoints, String sootCp, Set<String> packagesToConsider) {
	    G.v().reset();
	    Options.v().set_whole_program(true);
//	    Options.v().setPhaseOption("jtp", "enabled:true");
	    Options.v().setPhaseOption("jb", "use-original-names:true");

//		  Options.v().setPhaseOption("cg.cha", "enabled:true");

		  Options.v().setPhaseOption("cg.spark", "enabled:true");
		Options.v().setPhaseOption("cg.spark", "on-fly-cg:false");
//		Options.v().setPhaseOption("cg.spark", "vta:true");
//		  Options.v().setPhaseOption("cg.spark", "verbose:true");

//		Options.v().setPhaseOption("cg.spark", "rta:true");
//		Options.v().setPhaseOption("cg.spark", "geom-pta:true");
//		Options.v().setPhaseOption("cg.spark", "geom-runs:3");
//		Options.v().setPhaseOption("cg.spark", "simulate-natives:true");

//		Options.v().setPhaseOption("cg.spark", "apponly:true");
//		Options.v().setPhaseOption("cg.spark", "simple-edges-bidirectional:true");
//		Options.v().setPhaseOption("cg.cha", "enabled:true");

//		Options.v().setPhaseOption("cg.spark", "cs-demand:true");
//		Options.v().setPhaseOption("cg.spark", "passes:20");
	////	  Options.v().setPhaseOption("cg.spark", "geom-pta:true");
//		  Options.v().setPhaseOption("cg.spark", "geom-runs:2");
	 ////   Options.v().setPhaseOption("cg.spark", "on-fly-cg:true");
//	    Options.v().s/etPhaseOption("cg.spark", "apponly:true");
	//    String userdir = System.getProperty("user.dir");
	//    String sootCp = userdir + "/targets";

		  //analysis is sound as long as methods before an event triggering point is included
		  //this is used to reduce time and memory needed
	    Options.v().set_no_bodies_for_excluded(true);
	    
	    List<String> packagesToExclude = new ArrayList<String>();
	    packagesToExclude.add("java.*");
	    packagesToExclude.add("jdk.*");
	    packagesToExclude.add("sun.*");
	    Options.v().set_exclude(packagesToExclude);
	    
	    Options.v().set_include(new ArrayList<String>(packagesToConsider));


	    Options.v().set_soot_classpath(sootCp);
	    Options.v().set_prepend_classpath(true);
	    Options.v().set_allow_phantom_refs(true);
	    Options.v().keep_line_number();

//	    String mainClass = mainMethod.replaceAll("\\.[^\\.]+$", "");
	    Options.v().set_main_class(mainClass);

	    Options.v().set_output_dir(outputDir);

		  try {
			  Files.createParentDirs(new File(outputDir));
		  } catch (IOException e) {
			  e.printStackTrace();
		  }

//	    Options.v().out();
	    Options.v().set_output_format(output_format_class);
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

//	    String mainEntryMethodName = mainMethod.replaceAll(".+\\.(?=[^\\.]+$)","");

	    SootMethod methodByName = c.getMethodByName("main");
	    List<SootMethod> ePoints = new LinkedList<>();
	    ePoints.add(methodByName);

	    for(String entryPoint : entryPoints){
	    	String className = entryPoint.replaceAll("\\.[^\\.]+$", "");
	    	String methodName = entryPoint.replaceAll(".+\\.(?=[^\\.]+$)", "");
	    	SootClass classs = Scene.v().forceResolve(className, SootClass.BODIES);
	    	ePoints.add(classs.getMethodByName(methodName));
		}
//
//		  ePoints.add(cc.getMethodByName("run"));
	    Scene.v().setEntryPoints(ePoints);
	    // Add a transformer
	    PackManager.v().getPack("wjtp")
	        .add(new Transform("wjtp.PropertyTransformer", new PropertyTransformer()));

//	    PackManager.v().getPack("cg").apply();
//	    PackManager.v().getPack("wjtp").apply();
		PackManager.v().runPacks();

//		  PhaseOptions.v().setPhaseOption("bb", "enabled");
//		  PhaseOptions.v().setPhaseOption("db", "enabled");
//
//		  soot.Main.main(Options.v().dump_body().toArray(new String[]{}));

	    if(!PropertyTransformer.allSatisfied) {
			try {

//				PackManager.v().getPack("bb").apply();
//				PackManager.v().getPack("db").apply();
				PackManager.v().writeOutput();

//				Options.v().set_output_format(output_format_class);
//				Options.v().set_output_dir(rootOutputDir);
//
//				Set<Pair<SootClass, SootMethod>> toRemove = new HashSet<>();
//
//				Chain<SootClass> classes = Scene.v().getApplicationClasses();
//				for(SootClass sootClass : classes) {
//					for (SootMethod method : sootClass.getMethods()) {
//						if (!method.hasActiveBody()) {
//							try {
//								method.retrieveActiveBody();
//							}catch(Exception e){
//								toRemove.add(new Pair<>(sootClass, method));
//							}
//						}
//					}
//				}
//
//				for(Pair<SootClass, SootMethod> pair : toRemove){
//					pair.first.removeMethod(pair.second);
//					if(pair.first.getMethods().size() == 0){
//						Scene.v().getApplicationClasses().remove(pair.first);
//					}
//				}
//				PackManager.v().writeOutput();

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	  }
	
}
