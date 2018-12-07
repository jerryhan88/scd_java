import Approach.*;
import Other.Etc;
import Other.Parameter;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;


public class Tester {
    public static void testSmallExample() {
//        /*

        Path dpath = Paths.get("_temp");


        String prefix = "na010-nt100-vc10-wc10-sn00";
//        String appName = "ILP";
//        String appName = "SDA";
//        String appName = "SDAbnb";
        String appName = "SDArbnb";
//        String appName = "SDAgh";

        Path prmt_fpath = dpath.resolve(String.format("prmt_%s.ser", prefix));
        Parameter prmt;
        if (prmt_fpath.toFile().exists()) {
            prmt = Parameter.loadPrmt(prmt_fpath);
        } else {
            Path json_fpath = dpath.resolve(String.format("prmt_%s.json", prefix));
            prmt = Parameter.json2ser(json_fpath);
            prmt.savePrmt(prmt_fpath);
        }
        //
        Etc etc = new Etc(
                dpath.resolve(String.format("sol_%s_%s.json", prefix, appName)),
                dpath.resolve(String.format("sol_%s_%s.ser", prefix, appName)),
                dpath.resolve(String.format("sol_%s_%s.csv", prefix, appName)),
                dpath.resolve(String.format("sol_%s_%s.txt", prefix, appName)));
        if (appName.equals("ILP")) {
            etc.setLogPath(dpath.resolve(String.format("log_%s_%s.log", prefix, appName)));
            ILP em = new ILP(prmt, etc);
            em.buildModel();
            em.solveModel();
        } else if (appName.equals("SDA")) {
            etc.setLogPath(dpath.resolve(String.format("log_%s_%s.csv", prefix, appName)));
            SubgradientDescentAlgorithm sda = new SubgradientDescentAlgorithm(prmt, etc);
            sda.run();
        } else if (appName.equals("SDAbnb")) {
            etc.setLogPath(dpath.resolve(String.format("log_%s_%s.csv", prefix, appName)));
            SubgradientDescentAlgorithm sda_bnb = new SDA_BnB(prmt, etc);
            sda_bnb.run();
        } else if (appName.equals("SDArbnb")) {
            etc.setLogPath(dpath.resolve(String.format("log_%s_%s.csv", prefix, appName)));
            SubgradientDescentAlgorithm sda_rbnb = new SDA_rBnB(prmt, etc);
            sda_rbnb.run();
        } else if (appName.equals("SDAgh")) {
            etc.setLogPath(dpath.resolve(String.format("log_%s_%s.csv", prefix, appName)));
            SubgradientDescentAlgorithm sda_bnb = new SDA_GH(prmt, etc);
            sda_bnb.run();
        }


//        */
    }

    public static void main(String[] args) {
        String appName = null;
        Path prmt_dpath = null;
        Path sol_dpath = null;
        Path config_fpath = null;
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
        if (Integer.parseInt(properties.getProperty("executionMode")) == 0) {
            testSmallExample();
        } else {
            appName = properties.getProperty("appName");
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
                    app = new ILP(prmt, etc);
                } else {
                    int TERMINATION_NUM_ITERS = Integer.parseInt(properties.getProperty("TERMINATION_NUM_ITERS"));
                    double TERMINATION_DUEL_GAP = Double.parseDouble(properties.getProperty("TERMINATION_DUEL_GAP"));
                    int NO_IMPROVEMENT_LIMIT =Integer.parseInt(properties.getProperty("NO_IMPROVEMENT_LIMIT"));
                    double STEP_DECREASE_RATE = Double.parseDouble(properties.getProperty("STEP_DECREASE_RATE"));
                    double COMPENSATION_LIMIT = -1.0;
                    double COMPENSATION_RATE = -1.0;
                    String modiAppName = String.format("%s-tni%03d-tdg%03d-nil%03d-sdr%03d",
                                                        appName,
                                                        TERMINATION_NUM_ITERS,
                                                        (int) (TERMINATION_DUEL_GAP * 100),
                                                        NO_IMPROVEMENT_LIMIT,
                                                        (int) (STEP_DECREASE_RATE * 100)
                                                      );
                    int compensationMode = Integer.parseInt(properties.getProperty("compensationMode"));
                    if (compensationMode != 0) {
                        COMPENSATION_LIMIT = Double.parseDouble(properties.getProperty("COMPENSATION_LIMIT"));
                        COMPENSATION_RATE = Double.parseDouble(properties.getProperty("COMPENSATION_RATE"));
                        modiAppName = String.format("%s-cl%03d-cr%03d",
                                modiAppName,
                                (int) (COMPENSATION_LIMIT * 100),
                                (int) (COMPENSATION_RATE * 100)
                        );
                    }
                    Etc etc = new Etc(
                            sol_dpath.resolve(String.format("sol_%s_%s.json", prefix, modiAppName)),
                            sol_dpath.resolve(String.format("sol_%s_%s.ser", prefix, modiAppName)),
                            sol_dpath.resolve(String.format("sol_%s_%s.csv", prefix, modiAppName)),
                            sol_dpath.resolve(String.format("sol_%s_%s.txt", prefix, modiAppName)));
                    etc.setLogPath(sol_dpath.resolve(String.format("log_%s_%s.csv", prefix, modiAppName)));
                    if (appName.equals("SDA")) {
                        app = new SubgradientDescentAlgorithm(prmt, etc);
                    } else if (appName.equals("SDAbnb")) {
                        app = new SDA_BnB(prmt, etc);
                    } else if (appName.equals("SDAgh")) {
                        app = new SDA_GH(prmt, etc);
                    } else {
                        throw new RuntimeException("Type proper an approach name, ILP or SDAs");
                    }
                    if (compensationMode == 0) {
                        ((SubgradientDescentAlgorithm) app).set_parameters(TERMINATION_NUM_ITERS,  TERMINATION_DUEL_GAP,
                                NO_IMPROVEMENT_LIMIT, STEP_DECREASE_RATE);
                    } else {
                        ((SubgradientDescentAlgorithm) app).set_parameters(TERMINATION_NUM_ITERS,  TERMINATION_DUEL_GAP,
                                NO_IMPROVEMENT_LIMIT, STEP_DECREASE_RATE,
                                COMPENSATION_LIMIT, COMPENSATION_RATE);
                    }
                }
                app.run();
                System.out.println(String.format("Finished! %s %s", appName, file.toString()));
            }
        }
    }
}
