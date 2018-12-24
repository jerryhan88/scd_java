package Approach.Router;

import Approach.Router.BranchAndBound.TreeGH;
import Index.AEI;
import Index.AEIJ;
import Index.AEK;
import Other.Parameter;
import Other.RoutingProbSol;

import java.util.HashMap;

//

public class RouterGH extends RouterSup {

    public void solve(RoutingProbSol rProbSol) {
        int a = rProbSol.get_aid();
        int e = rProbSol.get_eid();
        Parameter prmt = rProbSol.get_prmt();
        HashMap<AEK, Double> lm_aek = rProbSol.get_lm_aek();
        //
        TreeGH treeGH = new TreeGH(prmt, a, e, lm_aek);
        treeGH.solve();
        //
        rProbSol.objV = treeGH.objV;
        for (AEIJ key: treeGH.x_aeij.keySet()) {
            rProbSol.x_aeij.put(key, treeGH.x_aeij.get(key));
        }
        for (AEI key: treeGH.mu_aei.keySet()) {
            rProbSol.mu_aei.put(key, treeGH.mu_aei.get(key));
        }
    }
}
