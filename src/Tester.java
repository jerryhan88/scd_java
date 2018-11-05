import Approach.ApproachSupClass;
import Approach.ILP;
import Approach.SDA_BnB;
import Approach.SubgradientDescentAlgorithm;
import Other.Etc;
import Other.Parameter;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


public class Tester {
    public static void testSmallExample() {
//        /*

        Path dpath = Paths.get("_temp");
        String prefix = "ED_Ex0";
//        String appName = "ILP";
//        String appName = "SDA";
        String appName = "SDAbnb";

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
        }


//        */
    }

    public static void main(String[] args) {
        CommandOptions cmd = new CommandOptions(args);
        String appName = null;
        Path prmt_dpath = null;
        Path sol_dpath = null;

        if (args.length == 0) {
            testSmallExample();
        } else {
            if (cmd.hasOption("-a")) {
                appName = cmd.valueOf("-a");
            }
            if (cmd.hasOption("-i")) {
                prmt_dpath = Paths.get(cmd.valueOf("-i"));
            }
            if (cmd.hasOption("-o")) {
                sol_dpath = Paths.get(cmd.valueOf("-o"));
                if (!sol_dpath.toFile().exists()){
                    sol_dpath.toFile().mkdir();
                }
            }
            String fn, prefix;
            Parameter prmt;
            ApproachSupClass app;
            //
            for (File file: prmt_dpath.toFile().listFiles()) {
                if (!file.toString().endsWith(".json")) continue;
                prmt = Parameter.json2ser(file.toPath());
                //
                fn = FilenameUtils.getBaseName(file.toString());
                prefix = fn.split("_")[1];
                Etc etc = new Etc(
                        sol_dpath.resolve(String.format("sol_%s_%s.json", prefix, appName)),
                        sol_dpath.resolve(String.format("sol_%s_%s.ser", prefix, appName)),
                        sol_dpath.resolve(String.format("sol_%s_%s.csv", prefix, appName)),
                        sol_dpath.resolve(String.format("sol_%s_%s.txt", prefix, appName)));
                if (appName.equals("ILP")) {
                    etc.setLogPath(sol_dpath.resolve(String.format("log_%s_%s.log", prefix, appName)));
                    app = new ILP(prmt, etc);
                } else if (appName.equals("SDA")) {
                    etc.setLogPath(sol_dpath.resolve(String.format("log_%s_%s.csv", prefix, appName)));
                    app = new SubgradientDescentAlgorithm(prmt, etc);
                } else {
                    throw new RuntimeException("Type proper an approach name, ILP or SDA");
                }
                app.run();
                System.out.println("testing!");
            }
        }
    }
}
