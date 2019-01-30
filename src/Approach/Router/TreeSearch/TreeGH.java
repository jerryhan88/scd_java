package Approach.Router.TreeSearch;

import Index.AEK;
import Other.Etc;
import Other.Parameter;

import java.util.ArrayList;
import java.util.HashMap;

public class TreeGH extends Tree {
    public TreeGH(Parameter prmt, Etc etc, int a, int e, HashMap<AEK, Double> lm_aek) {
        super(prmt, etc, a, e, lm_aek);
    }

    private void branch() {
        Node tn = popNode();
        assert tn != null;
        if (incumbent != null
                && tn.upperBound <= incumbent.lowerBound)
            return ;
        if (tn.tLB == tn.upperBound) {
            tn.lowerBound = tn.g();
            update_incumbent(tn);
            isSearchFinished = true;
        } else {
            ArrayList<Node> children = tn.gen_children_or_calc_lowerBound();
            if (children == null) {
                update_incumbent(tn);
                isSearchFinished = true;
            } else {
                pushNodes(children);
            }
        }
    }

    public void solve() {
        while (pq.size() != 0) {
            branch();
            if (isSearchFinished)
                break;
        }
        update_dvs();
    }
}
