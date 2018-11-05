package Approach;

import Approach.BranchAndBound.TreeBnB;
import Other.Etc;
import Other.Parameter;

import java.util.ArrayList;

public class SDA_BnB extends SubgradientDescentAlgorithm{
    public SDA_BnB(Parameter _prmt, Etc _etc) {
        super(_prmt, _etc);
    }

    // Override
    public void solve_Routing() {
        ArrayList<Integer> kR;
        for (int k : prmt.K) {
            kR = prmt.R_k.get(k);
            for (int r : kR) {
                TreeBnB treeBnB = new TreeBnB(prmt, k, r, _lm_kriT);
                treeBnB.solve();
                objV_Routing += treeBnB.objV;
                for (Index.krij key: treeBnB.x_krij.keySet()) {
                    _x_krij.put(key, treeBnB.x_krij.get(key));
                }
                for (Index.kriN key: treeBnB.a_kriN.keySet()) {
                    _a_kriN.put(key, treeBnB.a_kriN.get(key));
                }
                logging(new String[] {String.format("%f", etc.getCpuTime()), String.format("%d", numIters),
                        "solveDuals", String.format("solve_Routing (%d&%d)", k, r)});
            }
        }
    }
}
