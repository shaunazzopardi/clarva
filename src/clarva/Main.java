package clarva;

import clarva.java.PropertyTransformer;
import com.google.common.io.Files;
import soot.*;
import soot.options.Options;

import javax.swing.text.html.Option;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static soot.options.Options.output_format_class;

public class Main implements Runnable {

    public static String rootOutputDir = "./output-files/";
    public static String outputDir = "./output-files/dava/classes";

    public String[] args;

    public Main(String[] args){
        this.args = args;
    }

    public void run() {
        if (args == null || args.length == 0 || args.length < 3) {
            System.out.println("Arguments must specify: (0) the mode of the analysis: fast or intensive" +
                    "(1) property file followed by the " +
                    "(2) program directory, " +
                    "(3) the main class, and " +
                    "finally any other (4) entry methods to the application (e.g. the run method of any Thread, or the methods offered by an API).");
            return;
        }

        {
            Boolean fast = args[0].equals("fast") ? true : false;
            String property = args[1];//Arrays.asList(Arrays.copyOfRange(args, 1, args.length - 2));
//		properties.remove(properties.size()-1);
            List<String> properties = new ArrayList<>();
            properties.add(property);

            String programPath = args[2];
            String mainClass = args[3];
            List<String> entryPoints = new ArrayList<>();
            for (int i = 4; i < args.length; i++) {
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
            initializeSoot(fast, mainClass, entryPoints, programPath, packagesToConsider);

//			G.reset();

//			//this generates java files
//			if(!PropertyTransformer.allSatisfied) {
//				soot.Main.main(new String[]{"-output-dir", rootOutputDir, "-cp", outputDir, "-allow-phantom-refs", "-f", "dava", "-process-dir", outputDir});
//			}

//            System.exit(0);
        }
//        else {
//            System.out.println("Option " + args[0] + " not supported.");
//        }

    }

    @SuppressWarnings("static-access")
    private static void initializeSoot(boolean fast, String mainClass, List<String> entryPoints, String sootCp, Set<String> packagesToConsider) {
        G.v().reset();

        List<String> args = new ArrayList<>();

//        Options.v().set_whole_program(true);
        args.add("-w");

//        Options.v().setPhaseOption("jb", "use-original-names:true");
        args.add("-p");
        args.add("jb");
        args.add("enabled:true,use-original-names:true");
      //  args.add("");

//		  Options.v().setPhaseOption("cg.cha", "enabled:true");

        if(!fast) {
////        Options.v().setPhaseOption("cg.spark", "enabled:true");
            args.add("-p");
            args.add("cg.spark");
            args.add("enabled:true,verbose:true");
        } else {
            args.add("-no-bodies-for-excluded");
        }
        //analysis is sound as long as methods before an event triggering point is included
        //this is used to reduce time and memory needed
//        Options.v().set_no_bodies_for_excluded(true);

//        List<String> packagesToExclude = new ArrayList<String>();
//        packagesToExclude.add("java.*");
//        packagesToExclude.add("jdk.*");
//        packagesToExclude.add("sun.*");
//        Options.v().set_exclude(packagesToExclude);

        args.add("-exclude");
        args.add("java.*");
        args.add("-exclude");
        args.add("jdk.*");
        args.add("-exclude");
        args.add("sun.*");


//        Options.v().set_include(new ArrayList<String>(packagesToConsider));

        if(packagesToConsider.size() > 0) {


            List<String> pkgsList = new ArrayList<>(packagesToConsider);
            for (int i = 0; i < pkgsList.size(); i++) {
                args.add("-include");
                args.add(pkgsList.get(i));
            }
        }


//        Options.v().set_soot_classpath(sootCp);
        args.add("-cp");
        args.add(sootCp);
//        args.add("./FitsWithDataProtection/bin");

//        Options.v().set_prepend_classpath(true);
        args.add("-pp");


//        Options.v().set_allow_phantom_refs(true);
        args.add("-allow-phantom-refs");

//        Options.v().keep_line_number();
        args.add("-keep-line-number");

//	    String mainClass = mainMethod.replaceAll("\\.[^\\.]+$", "");
//        Options.v().set_main_class(mainClass);

        args.add("-main-class");
        args.add(mainClass);

//        args.add("-permissive-resolving");

//        Options.v().set_output_dir(outputDir);
        args.add("-output-dir");
        args.add(outputDir);

        try {
            Files.createParentDirs(new File(outputDir));
        } catch (IOException e) {
            e.printStackTrace();
        }

//	    Options.v().out();
//        Options.v().set_output_format(output_format_class);
        args.add("-f");
        args.add("c");

//        Options.v().set_process_dir(Arrays.asList(new String[]{sootCp}));
        args.add("-process-dir");
        args.add(sootCp);

//	    Options.v().set_oaat(true);
//	    Options.v().set_output_format(Options.output_format_dava);
//	    Options.v().set_output_format(Options.output_format_class);

//        Options.v().output_jar();

//        Scene.v().setSootClassPath(sootCp);
//        Scene.v().addBasicClass(mainClass, SootClass.BODIES);
//        Scene.v().loadNecessaryClasses();
//
//        SootClass c = Scene.v().forceResolve(mainClass, SootClass.BODIES);
//        if (c != null) {
//            c.setApplicationClass();
//        }
//
////	    String mainEntryMethodName = mainMethod.replaceAll(".+\\.(?=[^\\.]+$)","");
//
//        SootMethod methodByName = c.getMethodByName("main");
//        List<SootMethod> ePoints = new LinkedList<>();
//        ePoints.add(methodByName);
//
//        for (String entryPoint : entryPoints) {
//            String className = entryPoint.replaceAll("\\.[^\\.]+$", "");
//            String methodName = entryPoint.replaceAll(".+\\.(?=[^\\.]+$)", "");
//            SootClass classs = Scene.v().forceResolve(className, SootClass.BODIES);
//            ePoints.add(classs.getMethodByName(methodName));
//        }
////
////		  ePoints.add(cc.getMethodByName("run"));
//        Scene.v().setEntryPoints(ePoints);
        // Add a transformer
        PackManager.v().getPack("wjtp")
                .add(new Transform("wjtp.PropertyTransformer", new PropertyTransformer()));

//        PackManager.v().getPack("cg").apply();
//	    PackManager.v().getPack("wjtp").apply();
//        PackManager.v().runPacks();

//		  PhaseOptions.v().setPhaseOption("bb", "enabled");
//		  PhaseOptions.v().setPhaseOption("db", "enabled");
//
		  soot.Main.main(args.toArray(new String[]{}));

//        if (!PropertyTransformer.allSatisfied) {
//            try {
//
//                PackManager.v().writeOutput();
//
////				Options.v().set_output_format(output_format_class);
////				Options.v().set_output_dir(rootOutputDir);
////
////				Set<Pair<SootClass, SootMethod>> toRemove = new HashSet<>();
////
////				Chain<SootClass> classes = Scene.v().getApplicationClasses();
////				for(SootClass sootClass : classes) {
////					for (SootMethod method : sootClass.getMethods()) {
////						if (!method.hasActiveBody()) {
////							try {
////								method.retrieveActiveBody();
////							}catch(Exception e){
////								toRemove.add(new Pair<>(sootClass, method));
////							}
////						}
////					}
////				}
////
////				for(Pair<SootClass, SootMethod> pair : toRemove){
////					pair.first.removeMethod(pair.second);
////					if(pair.first.getMethods().size() == 0){
////						Scene.v().getApplicationClasses().remove(pair.first);
////					}
////				}
////				PackManager.v().writeOutput();
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
    }

}
