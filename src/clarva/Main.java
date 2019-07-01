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

    public boolean fast;

    public Main(String[] args){
        this.args = args;
    }

    public static void main(String[] args){
        new Main(args).run();
    }

    public void run() {
        if (args == null || args.length == 0 || args.length < 3) {
            System.out.println("Arguments must specify: (0) the mode of the analysis: fast or intensive" +
                    "(1) property file followed by the " +
                    "(2) program directory, and" +
                    "(3) the main class");
//                    "finally any other (4) entry methods to the application (e.g. the run method of any Thread, or the methods offered by an API).");
            return;
        }

        {
            fast = args[0].equals("fast") ? true : false;
            String property = args[1];//Arrays.asList(Arrays.copyOfRange(args, 1, args.length - 2));
//		properties.remove(properties.size()-1);
            List<String> properties = new ArrayList<>();
            properties.add(property);

            String programPath = args[2];
            String mainClass = args[3];
//            List<String> entryPoints = new ArrayList<>();
//            for (int i = 4; i < args.length; i++) {
//                entryPoints.add(args[args.length - 1]);
//            }

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
            initializeSoot(mainClass, programPath, packagesToConsider);

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
    private void initializeSoot(String mainClass, String sootCp, Set<String> packagesToConsider) {
        G.v().reset();

        List<String> args = new ArrayList<>();

        args.add("-w");

        args.add("-p");
        args.add("jb");
        args.add("enabled:true,use-original-names:true");

        if(!fast) {
            args.add("-p");
            args.add("cg.spark");
            args.add("enabled:true,verbose:true");
        }else{
            //analysis is sound as long as methods before an event triggering point is included
            //this is used to reduce time and memory needed
            args.add("-no-bodies-for-excluded");
        }


        args.add("-exclude");
        args.add("java.*");
        args.add("-exclude");
        args.add("jdk.*");
        args.add("-exclude");
        args.add("sun.*");


        if(packagesToConsider.size() > 0) {


            List<String> pkgsList = new ArrayList<>(packagesToConsider);
            for (int i = 0; i < pkgsList.size(); i++) {
                args.add("-include");
                args.add(pkgsList.get(i));
            }
        }

        args.add("-cp");
        args.add(sootCp);

        args.add("-pp");

        args.add("-allow-phantom-refs");

        args.add("-keep-line-number");

        args.add("-main-class");
        args.add(mainClass);

//        args.add("-permissive-resolving");

        args.add("-output-dir");
        args.add(outputDir);

        try {
            Files.createParentDirs(new File(outputDir));
        } catch (IOException e) {
            e.printStackTrace();
        }

        args.add("-f");
        args.add("c");

        args.add("-process-dir");
        args.add(sootCp);

        // Add a transformer
        PackManager.v().getPack("wjtp")
                .add(new Transform("wjtp.PropertyTransformer", new PropertyTransformer()));


		  soot.Main.main(args.toArray(new String[]{}));
    }

}
