import Approach.*;
import Other.Etc;
import Other.Parameter;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;


public class Main {
    private static void runInstance(File prmt_file, Path sol_dpath,
                                    String appName, int timeLimit, PropertiesLoader properties) {
        String fn = FilenameUtils.getBaseName(prmt_file.toString());
        String prefix = fn.split("_")[1];
        String prefix_app =  String.format("%s_%s", prefix, appName);
        Parameter prmt;
        if (prmt_file.toString().endsWith(".json")) {
            prmt = Parameter.json2ser(prmt_file.toPath());
        } else {
            prmt = Parameter.loadPrmt(prmt_file.toPath());
        }
        //
        Etc etc = new Etc(
                sol_dpath.resolve(String.format("sol_%s.json", prefix_app)),
                sol_dpath.resolve(String.format("sol_%s.ser", prefix_app)),
                sol_dpath.resolve(String.format("sol_%s.csv", prefix_app)),
                sol_dpath.resolve(String.format("sol_%s.txt", prefix_app)));
        etc.setLogPath(sol_dpath.resolve(String.format("log_%s.csv", prefix_app)));
        etc.setTimeLimit(timeLimit);
        ApproachSupClass app;
        if (appName.equals("ILP")) {
            app = new ILP(prmt, etc);
        } else if (appName.equals("PureGH")) {
            app = new PureGH(prmt, etc);
        } else {
            assert appName.startsWith("LRH");
            assert appName.contains("n");
            //
            etc.setLmLogPath(sol_dpath.resolve(String.format("LmLog_%s.csv", prefix_app)));
            //
            app = new LRH(prmt, etc);
            double LRH_DUAL_GAP = Double.parseDouble(properties.getProperty("LRH_DUAL_GAP"));
            int LRH_NUM_ITER =Integer.parseInt(properties.getProperty("LRH_NUM_ITER"));
            double STEP_DECREASE_RATE = Double.parseDouble(properties.getProperty("STEP_DECREASE_RATE"));
            int NO_IMPROVEMENT_LIMIT =Integer.parseInt(properties.getProperty("NO_IMPROVEMENT_LIMIT"));
            double SUB_DUAL_GAP_LIMIT =Double.parseDouble(properties.getProperty("SUB_DUAL_GAP_LIMIT"));
            ((LRH) app).set_parameters(LRH_DUAL_GAP, LRH_NUM_ITER,
                    STEP_DECREASE_RATE, NO_IMPROVEMENT_LIMIT, SUB_DUAL_GAP_LIMIT);
            String router = appName.split("n")[1];
            ((LRH) app).set_router(router);
        }
        app.run();
        System.out.println(String.format("Finished! %s %s", appName, prmt_file.toString()));
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            throw new RuntimeException("Please provide arguments");
        }
        CommandOptions cmd = new CommandOptions(args);
        String [] basicFlags = {"-i", "-o"};
        for (String s: basicFlags) {
            if (! cmd.hasOption(s)) {
                throw new RuntimeException(String.format("Missing flag: %s", s));
            }
        }
        Path iPath = Paths.get(cmd.valueOf("-i"));
        Path oPath = Paths.get(cmd.valueOf("-o"));
        if (! oPath.toFile().exists()){
            oPath.toFile().mkdir();
        }
        //
        if (cmd.hasOption("-s")) {
            if (iPath.toFile().isDirectory()) {
                ArrayList<File> prmt_files = new ArrayList(Arrays.asList(iPath.toFile().listFiles()));
                prmt_files.sort((f1, f2) -> f1.toString().compareToIgnoreCase(f2.toString()));
                for (File prmt_file: prmt_files) {
                    if (!prmt_file.toString().endsWith(".json")) continue;
                    Parameter prmt = Parameter.json2ser(prmt_file.toPath());
                    //
                    String fn = FilenameUtils.getBaseName(prmt_file.toString());
                    Path ofPath = oPath.resolve(String.format("%s.ser", fn));
                    System.out.println(ofPath.toString());
                    prmt.savePrmt(ofPath);
                }
            } else {
                File prmt_file = iPath.toFile();
                Parameter prmt = Parameter.json2ser(prmt_file.toPath());
                //
                String fn = FilenameUtils.getBaseName(prmt_file.toString());
                Path ofPath = oPath.resolve(String.format("%s.ser", fn));
                System.out.println(ofPath.toString());
                prmt.savePrmt(ofPath);
            }
        } else {
            String [] algoFlags = {"-a", "-t", "-c"};
            for (String s: algoFlags) {
                if (! cmd.hasOption(s)) {
                    throw new RuntimeException(String.format("Missing flag: %s", s));
                }
            }
            String appName = cmd.valueOf("-a");
            int timeLimit = Integer.parseInt(cmd.valueOf("-t"));
            Path config_fpath = Paths.get(cmd.valueOf("-c"));
            PropertiesLoader properties = new PropertiesLoader(config_fpath.toFile());
            //
            if (iPath.toFile().isDirectory()) {
                ArrayList<File> prmt_files = new ArrayList(Arrays.asList(iPath.toFile().listFiles()));
                prmt_files.sort((f1, f2) -> f1.toString().compareToIgnoreCase(f2.toString()));
                for (File file: prmt_files) {
                    if (! (file.toString().endsWith(".json")
                            || file.toString().endsWith(".ser")) ) continue;
                    runInstance(file, oPath,
                            appName, timeLimit, properties);
                }
            } else {
                File prmt_file = iPath.toFile();
                runInstance(prmt_file, oPath,
                        appName, timeLimit, properties);
            }
        }
    }
}
