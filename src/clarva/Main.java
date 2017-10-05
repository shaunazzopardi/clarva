package clarva;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import soot.G;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.options.Options;

public class Main {
	

	public static void main(String[] args) {
		if(args.length < 3) {
			System.out.println("Arguments must specify: (1) property files followed by the (2) program directory, and finally the (3) main class.");
			return;
		}
		List<String> properties = Arrays.asList(Arrays.copyOfRange(args, 0, args.length - 2));
//		properties.remove(properties.size()-1);

		String programPath = args[args.length - 2];
		String mainClass = args[args.length - 1];

		ClaraTransformer.generateFiniteStateMachines(properties);
	
		Set<String> packagesToConsider = new HashSet<String>();
		
		for(fsm.date.Global global : ClaraTransformer.dateFSMHierarchy){
			packagesToConsider.addAll(Arrays.asList(global.imports.replaceAll(";","").replaceAll("import ", "").replaceAll("\r", "").replaceAll(" ", "").split("\n")));
		}
		packagesToConsider.remove("");
		//must add all libraries imported by class files
	//	packagesToConsider.add("transactionsystem.*");
		packagesToConsider.add("java.lang.*");
		packagesToConsider.add("java.util.*");
	//	packagesToConsider.add("java.io.*");
		initializeSoot(mainClass, programPath, packagesToConsider);
		
		System.exit(0);
	}
	
	  @SuppressWarnings("static-access")
	  private static void initializeSoot(String mainClass, String sootCp, Set<String> packagesToConsider) {
	    G.v().reset();
	    Options.v().set_whole_program(true);
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
	    
	    Options.v().set_include(new ArrayList<String>(packagesToConsider));
	    
	    Options.v().setPhaseOption("cg.spark", "geom-pta:true");
	    Options.v().setPhaseOption("cg.spark", "geom-runs:2");
	    Options.v().set_soot_classpath(sootCp);
	    Options.v().set_prepend_classpath(true);
	    Options.v().set_allow_phantom_refs(true);
	    Options.v().keep_line_number();
	    Options.v().set_main_class(mainClass);

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
	        .add(new Transform("wjtp.ClaraTransformer", new ClaraTransformer()));
	    PackManager.v().getPack("cg").apply();
	    PackManager.v().getPack("wjtp").apply();
	    
	  }
	
}
