import clarva.java.PropertyTransformer;
import com.google.common.io.Files;
import fj.data.Option;
import soot.*;
import soot.JastAddJ.Opt;
import soot.options.Options;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static soot.options.Options.output_format_class;
import static soot.options.Options.output_format_d;

public class Main {
    public static String rootOutputDir = "./output-files/";
    public static String outputDir = "./output-files/dava/classes";

    public static void main(String[] args){
        Thread clarva = new Thread(new clarva.Main(args));
        clarva.start();
        while(clarva.isAlive()){

        }
//        G.reset();
//        soot.Main.main(new String[]{"-cp", "C:\\Program Files\\Java\\jre1.8.0_161\\lib\\jce.jar;" + outputDir, "-f", "dava", "-process-dir", outputDir});

//
//        G.v().reset();
//
//		  Options.v().setPhaseOption("db", "enabled:true");
//		  Options.v().setPhaseOption("db.transformation", "enabled:true");
//
//        Options.v().set_no_bodies_for_excluded(true);
//
//        List<String> packagesToExclude = new ArrayList<String>();
//        packagesToExclude.add("java.*");
//        packagesToExclude.add("jdk.*");
//        packagesToExclude.add("sun.*");
//        Options.v().set_exclude(packagesToExclude);
//
//        List<String> packagesToConsider = new ArrayList<>();
//        packagesToConsider.add(outputDir);
//        Options.v().set_include(new ArrayList<String>(packagesToConsider));
//
//
//        Options.v().set_soot_classpath(outputDir);
//        Options.v().set_prepend_classpath(true);
//        Options.v().set_allow_phantom_refs(true);
//
//        Options.v().set_output_format(output_format_d);
//        Options.v().set_process_dir(Arrays.asList(new String[]{rootOutputDir}));
//
//        Options.v().set_whole_program(true);
//
//        Scene.v().loadNecessaryClasses();
//
//        PackManager.v().runPacks();
//
//            try {
//
//                PackManager.v().writeOutput();
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
    }
}
