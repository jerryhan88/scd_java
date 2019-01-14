import Approach.*;
import Other.Etc;
import Other.Parameter;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
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
        Parameter prmt = Parameter.json2ser(prmt_file.toPath());
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
        } else {
            etc.setLmLogPath(sol_dpath.resolve(String.format("LmLog_%s.csv", prefix_app)));
            //
            app = new SGM(prmt, etc);
            String router = appName.split("n")[1];
            ((SGM) app).set_router(router);
            //
            double TERMINATION_DUEL_GAP = Double.parseDouble(properties.getProperty("TERMINATION_DUEL_GAP"));
            int NO_IMPROVEMENT_LIMIT =Integer.parseInt(properties.getProperty("NO_IMPROVEMENT_LIMIT"));
            double STEP_DECREASE_RATE = Double.parseDouble(properties.getProperty("STEP_DECREASE_RATE"));
            ((SGM) app).set_parameters(TERMINATION_DUEL_GAP, NO_IMPROVEMENT_LIMIT, STEP_DECREASE_RATE);
            //
            String lambda_initialization = properties.getProperty("LAMBDA_INITIALIZATION");
            ((SGM) app).init_lambda(lambda_initialization);
        }
        app.run();
        System.out.println(String.format("Finished! %s %s", appName, prmt_file.toString()));
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            throw new RuntimeException("Please provide arguments");
        }
        CommandOptions cmd = new CommandOptions(args);
        String [] flags = {"-i", "-o", "-a", "-t", "-c"};
        for (String s: flags) {
            if (! cmd.hasOption(s)) {
                throw new RuntimeException(String.format("Missing flag: %s", s));
            }
        }
        Path iPath = Paths.get(cmd.valueOf("-i"));
        Path oPath = Paths.get(cmd.valueOf("-o"));
        String appName = cmd.valueOf("-a");
        int timeLimit = Integer.parseInt(cmd.valueOf("-t"));
        Path config_fpath = Paths.get(cmd.valueOf("-c"));
        PropertiesLoader properties = new PropertiesLoader(config_fpath.toFile());
        //
        if (! oPath.toFile().exists()){
            oPath.toFile().mkdir();
        }
        //
        if (iPath.toFile().isDirectory()) {
            ArrayList<File> prmt_files = new ArrayList(Arrays.asList(iPath.toFile().listFiles()));
            prmt_files.sort((f1, f2) -> f1.toString().compareToIgnoreCase(f2.toString()));
            for (File file: prmt_files) {
                if (!file.toString().endsWith(".json")) continue;
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
