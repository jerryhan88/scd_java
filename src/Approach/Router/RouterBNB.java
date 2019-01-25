package Approach.Router;

import Approach.Router.BranchAndBound.TreeBNB;
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
        TreeBNB treeBnB = new TreeBNB(prmt, etc,
                                a, e, lm_aek);
        treeBnB.solve();
        if(!treeBnB.isTerminated) {
            rProbSol.objV = treeBnB.objV;
            for (AEIJ key: treeBnB.x_aeij.keySet()) {
                rProbSol.x_aeij.put(key, treeBnB.x_aeij.get(key));
            }
            for (AEI key: treeBnB.mu_aei.keySet()) {
                rProbSol.mu_aei.put(key, treeBnB.mu_aei.get(key));
            }
        } else {
            rProbSol.objV = -1;
            rProbSol.isTerminated = true;
        }
    }
}
