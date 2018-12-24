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
    public static void main(String[] args) {
        String appName;
        int timeLimit;
        Path config_fpath, prmt_dpath, sol_dpath;
        PropertiesLoader properties = null;
        //
        if (args.length != 0) {
            CommandOptions cmd = new CommandOptions(args);
            if (cmd.hasOption("-c")) {
                config_fpath = Paths.get(cmd.valueOf("-c"));
                properties = new PropertiesLoader(config_fpath.toFile());
            } else {
                throw new RuntimeException("Use -c flag and set the path about the configuration");
            }
        }
        //
        appName = properties.getProperty("appName");
        timeLimit = Integer.parseInt(properties.getProperty("TimeLimit"));
        prmt_dpath = Paths.get(properties.getProperty("input_dpath"));
        sol_dpath = Paths.get(properties.getProperty("output_dpath"));
        if (!sol_dpath.toFile().exists()){
            sol_dpath.toFile().mkdir();
        }
        String fn, prefix;
        Parameter prmt;
        ApproachSupClass app;
        //
        ArrayList<File> prmt_files = new ArrayList(Arrays.asList(prmt_dpath.toFile().listFiles()));
        prmt_files.sort((f1, f2) -> f1.toString().compareToIgnoreCase(f2.toString()));
        for (File file: prmt_files) {
            if (!file.toString().endsWith(".json")) continue;
            prmt = Parameter.json2ser(file.toPath());
            //
            fn = FilenameUtils.getBaseName(file.toString());
            prefix = fn.split("_")[1];
            if (appName.equals("ILP")) {
                Etc etc = new Etc(
                        sol_dpath.resolve(String.format("sol_%s_%s.json", prefix, appName)),
                        sol_dpath.resolve(String.format("sol_%s_%s.ser", prefix, appName)),
                        sol_dpath.resolve(String.format("sol_%s_%s.csv", prefix, appName)),
                        sol_dpath.resolve(String.format("sol_%s_%s.txt", prefix, appName)));
                etc.setLogPath(sol_dpath.resolve(String.format("log_%s_%s.log", prefix, appName)));
                etc.setTimeLimit(timeLimit);
                app = new ILP(prmt, etc);
            } else {
                double TERMINATION_DUEL_GAP = Double.parseDouble(properties.getProperty("TERMINATION_DUEL_GAP"));
                int NO_IMPROVEMENT_LIMIT =Integer.parseInt(properties.getProperty("NO_IMPROVEMENT_LIMIT"));
                double STEP_DECREASE_RATE = Double.parseDouble(properties.getProperty("STEP_DECREASE_RATE"));
                String modiAppName = String.format("%s-tdg%03d-nil%03d-sdr%03d",
                                                    appName,
                                                    (int) (TERMINATION_DUEL_GAP * 100),
                                                    NO_IMPROVEMENT_LIMIT,
                                                    (int) (STEP_DECREASE_RATE * 100)
                                                  );
                Etc etc = new Etc(
                        sol_dpath.resolve(String.format("sol_%s_%s.json", prefix, modiAppName)),
                        sol_dpath.resolve(String.format("sol_%s_%s.ser", prefix, modiAppName)),
                        sol_dpath.resolve(String.format("sol_%s_%s.csv", prefix, modiAppName)),
                        sol_dpath.resolve(String.format("sol_%s_%s.txt", prefix, modiAppName)));
                etc.setTimeLimit(timeLimit);
                etc.setLogPath(sol_dpath.resolve(String.format("log_%s_%s.csv", prefix, modiAppName)));
                etc.setLmLogPath(sol_dpath.resolve(String.format("LmLog_%s_%s.csv", prefix, modiAppName)));
                //
                app = new SGM(prmt, etc);
                String router = appName.split("n")[1];
                ((SGM) app).set_router(router);
                ((SGM) app).set_parameters(TERMINATION_DUEL_GAP, NO_IMPROVEMENT_LIMIT, STEP_DECREASE_RATE);
            }
            app.run();
            System.out.println(String.format("Finished! %s %s", appName, file.toString()));
        }
    }
}
