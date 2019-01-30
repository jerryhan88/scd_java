package Approach.Router;

import Approach.Router.TreeSearch.TreeBNB;
import Index.AEI;
import Index.AEIJ;
import Index.AEK;
import Other.Etc;
import Other.Parameter;
import Other.RoutingProbSol;
import java.util.HashMap;

//

public class RouterBNB extends RouterSup {

    public void solve(RoutingProbSol rProbSol) {
        int a = rProbSol.get_aid();
        int e = rProbSol.get_eid();
        Parameter prmt = rProbSol.get_prmt();
        Etc etc = rProbSol.get_etc();
        HashMap<AEK, Double> lm_aek = rProbSol.get_lm_aek();
        //
        TreeBNB treeBNB = new TreeBNB(prmt, etc, a, e, lm_aek);
        try {
            treeBNB.solve();
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        rProbSol.objV = treeBNB.objV;
        for (AEIJ key: treeBNB.x_aeij.keySet()) {
            rProbSol.x_aeij.put(key, treeBNB.x_aeij.get(key));
        }
        for (AEI key: treeBNB.mu_aei.keySet()) {
            rProbSol.mu_aei.put(key, treeBNB.mu_aei.get(key));
        }
    }
}
