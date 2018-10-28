import Approach.MILP;
import Approach.SubgradientDescentAlgorithm;
import Other.Etc;
import Other.Parameter;
import Other.ParameterParser;
import Other.Solution;

import java.nio.file.Path;
import java.nio.file.Paths;


public class Tester {
    public static void main(String[] args) {
        Path dpath = Paths.get("_temp");
        String problemName = "euclideanDistEx0";
        Path prmt_fpath = dpath.resolve(String.format("prmt_%s.ser", problemName));
        Parameter prmt;
        if (prmt_fpath.toFile().exists()) {
            prmt = Parameter.loadPrmt(prmt_fpath);
        } else {
            Path json_fpath = dpath.resolve(String.format("prmt_%s.json", problemName));
            prmt = Parameter.json2ser(json_fpath);
            prmt.savePrmt(prmt_fpath);
        }
        //
        String approach = "SDA";

        Etc etc = new Etc(dpath.resolve(String.format("log_%s_%s.log", problemName, approach)),
                        dpath.resolve(String.format("sol_%s_%s.json", problemName, approach)),
                        dpath.resolve(String.format("sol_%s_%s.ser", problemName, approach)),
                        dpath.resolve(String.format("sol_%s_%s.csv", problemName, approach)),
                        dpath.resolve(String.format("sol_%s_%s.txt", problemName, approach)));
        if (approach.equals("EX")) {
            MILP em = new MILP(prmt, etc);
            em.buildModel();
            em.solveModel();
        } else if (approach.equals("SDA")) {
            SubgradientDescentAlgorithm sda = new SubgradientDescentAlgorithm(prmt, etc);
            sda.run();
        }
    }
}
