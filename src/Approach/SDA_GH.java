package Approach;

import Approach.BranchAndBound.TreeGH;
import Index.AE;
import Index.AEI;
import Index.AEIJ;
import Other.Etc;
import Other.Parameter;

import java.util.ArrayList;
import java.util.HashMap;

public class SDA_GH extends SubgradientDescentAlgorithm {
    private boolean USE_THREAD = true;

    public SDA_GH(Parameter _prmt, Etc _etc) {
        super(_prmt, _etc);
    }

    // Override
    public void solve_Routing() {
        if (USE_THREAD) {
            HashMap<AE, TreeGH> trees = new HashMap<>();
            HashMap<AE, Thread> threads = new HashMap<>();
            TreeGH tree;
            Thread t;
            //
            ArrayList<Integer> aE;
            for (int a : prmt.A) {
                aE = prmt.E_a.get(a);
                for (int e : aE) {
                    AE kr = new AE(a, e);
                    tree = new TreeGH(prmt, a, e, _lm_aek);
                    t = new Thread(tree, String.format("%d, %d", a, e));
                    trees.put(kr, tree);
                    threads.put(kr, t);
                    t.start();
                }
            }
            for (AE ae: threads.keySet()) {
                try {
                    threads.get(ae).join();
                    tree = trees.get(ae);
                    objV_Routing += tree.objV;
                    for (AEIJ key: tree.x_aeij.keySet()) {
                        _x_aeij.put(key, tree.x_aeij.get(key));
                    }
                    for (AEI key: tree.mu_aei.keySet()) {
                        _mu_aei.put(key, tree.mu_aei.get(key));
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            logging("solveDuals", "solve_Routing");
        } else {
            ArrayList<Integer> aE;
            for (int a : prmt.A) {
                aE = prmt.E_a.get(a);
                for (int e : aE) {
                    TreeGH tree = new TreeGH(prmt, a, e, _lm_aek);
                    tree.solve();
                    objV_Routing += tree.objV;
                    for (AEIJ key: tree.x_aeij.keySet()) {
                        _x_aeij.put(key, tree.x_aeij.get(key));
                    }
                    for (AEI key: tree.mu_aei.keySet()) {
                        _mu_aei.put(key, tree.mu_aei.get(key));
                    }
                    logging("solveDuals", String.format("solve_Routing (%d&%d)", a, e));
                }
            }
        }
    }
}
