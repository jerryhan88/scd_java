package Approach;

import Approach.BranchAndBound.TreeBnB;
import Index.AE;
import Index.AEI;
import Index.AEIJ;
import Other.Etc;
import Other.Parameter;

import java.util.ArrayList;
import java.util.HashMap;

public class SDA_BnB extends SubgradientDescentAlgorithm{
    private boolean USE_THREAD = true;
    public SDA_BnB(Parameter _prmt, Etc _etc) {
        super(_prmt, _etc);
    }

    // Override
    public void solve_Routing() {
        if (USE_THREAD) {
            HashMap<AE, TreeBnB> trees = new HashMap<>();
            HashMap<AE, Thread> threads = new HashMap<>();
            TreeBnB treeBnB;
            Thread t;
            //
            ArrayList<Integer> aE;
            for (int a : prmt.A) {
                aE = prmt.E_a.get(a);
                for (int e : aE) {
                AE ae = new AE(a, e);
                treeBnB = new TreeBnB(prmt, a, e, _lm_aek);
                t = new Thread(treeBnB, String.format("%d, %d", a, e));
                trees.put(ae, treeBnB);
                threads.put(ae, t);
                t.start();
                }
            }
            for (AE ae: threads.keySet()) {
                try {
                    threads.get(ae).join();
                    treeBnB = trees.get(ae);
                    objV_Routing += treeBnB.objV;
                    for (AEIJ key: treeBnB.x_aeij.keySet()) {
                        _x_aeij.put(key, treeBnB.x_aeij.get(key));
                    }
                    for (AEI key: treeBnB.mu_aei.keySet()) {
                        _mu_aei.put(key, treeBnB.mu_aei.get(key));
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else {
            ArrayList<Integer> aE;
            for (int a : prmt.A) {
                aE = prmt.E_a.get(a);
                for (int e : aE) {
                    TreeBnB treeBnB = new TreeBnB(prmt, a, e, _lm_aek);
                    treeBnB.solve();
                    objV_Routing += treeBnB.objV;
                    for (AEIJ key: treeBnB.x_aeij.keySet()) {
                        _x_aeij.put(key, treeBnB.x_aeij.get(key));
                    }
                    for (AEI key: treeBnB.mu_aei.keySet()) {
                        _mu_aei.put(key, treeBnB.mu_aei.get(key));
                    }
                }
            }
        }
    }
}
