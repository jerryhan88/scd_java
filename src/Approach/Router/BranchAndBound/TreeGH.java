package Approach.Router.BranchAndBound;

import Index.AEK;
import Other.Parameter;

import java.util.ArrayList;
import java.util.HashMap;

public class TreeGH extends TreeBNB {
    public TreeGH(Parameter prmt, int a, int e, HashMap<AEK, Double> lm_aek) {
        super(prmt, a, e, lm_aek);
    }

    private boolean branch() {
        NodeBnB tn = pq.poll();
        assert tn != null;
        if (incumbent != null && tn.upperBound <= incumbent.lowerBound)
            return false;
        if (tn.tLB == tn.upperBound) {
            tn.lowerBound = tn.g();
            update_incumbent(tn);
            return true;
        } else {
            ArrayList<NodeBnB> children = tn.gen_children_or_calc_lowerBound();
            if (children == null) {
                update_incumbent(tn);
                return true;
            } else {
                pq.addAll(children);
                return false;
            }
        }
    }

    public void solve() {
        boolean isFinished;
        while (pq.size() != 0) {
            isFinished = branch();
            if (isFinished)
                break;
        }
        update_dvs();
    }
}
