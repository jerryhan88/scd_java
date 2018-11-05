package Approach.BranchAndBound;

import Index.kriN;
import Index.kriT;
import Other.Parameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

public class TreeBnB {
    Parameter prmt;
    int k, r;
    double k_v, kr_l, kr_u;
    HashMap<Integer, Double> lm_iT;
    //
    public double objV;
    public HashMap<Index.krij, Double> x_krij;
    public HashMap<kriN, Double> a_kriN;
    //
    private Stack<NodeBnB> stackOfNodes;
    private NodeBnB incumbent = null;

    public TreeBnB(Parameter _prmt, int _k, int _r, HashMap<kriT, Double> _lm_kriT) {
        Index.kr kr = new Index.kr(_k, _r);
        prmt = _prmt;
        k = _k;
        r = _r;
        k_v = _prmt.v_k.get(_k);
        kr_l = prmt.l_kr.get(kr);
        kr_u = prmt.u_kr.get(kr);
        lm_iT = new HashMap<>();
        ArrayList<Integer> initialFeasibleTasks = new ArrayList<>(prmt.F_kr.get(kr));
        ArrayList<String> initialRoutineRoute = new ArrayList<>(prmt.C_kr.get(kr));
        for (int i: initialFeasibleTasks) {
            lm_iT.put(i, _lm_kriT.get(new Index.kriT(_k, _r, i)));
        }
        initialFeasibleTasks.sort((Integer o1, Integer o2) -> (int) (lm_iT.get(o1) - lm_iT.get(o2)));
        //
        stackOfNodes = new Stack<>();
        stackOfNodes.push(new NodeBnB(this, initialRoutineRoute, initialFeasibleTasks));
    }

    private void branch() {
        NodeBnB tn = stackOfNodes.pop();
        ArrayList<NodeBnB> children = tn.gen_children_and_calc_bounds();
        if (children == null) {
            assert tn.lowerBound != -Double.MAX_VALUE;
            if (incumbent == null || incumbent.lowerBound < tn.lowerBound) {
                incumbent = tn;
            }
        } else if (incumbent.lowerBound < tn.upperBound) {
            for (NodeBnB cn: children) {
                stackOfNodes.push(cn);
            }
        }
        //
        if (!stackOfNodes.empty())
            branch();
    }

    public void solve() {
        branch();
        //
        x_krij = new HashMap<>();
        a_kriN = new HashMap<>();
        ArrayList<String> krN = prmt.N_kr.get(new Index.kr(k, r));
        for (String i: krN) {
            for (String j: krN) {
                x_krij.put(new Index.krij(k, r, i, j), 0.0);
            }
            a_kriN.put(new Index.kriN(k, r, i), 0.0);
        }
        objV = incumbent.lowerBound;
        String n0, n1;
        for (int s = 0; s < incumbent.partialSequence.size() - 1; s++) {
            n0 = incumbent.partialSequence.get(s);
            n1 = incumbent.partialSequence.get(s + 1);
            x_krij.put(new Index.krij(k, r, n0, n1), 1.0);
        }
        HashMap<String, Double> arrivalTime = incumbent.get_arrivalTime();
        for (String i: arrivalTime.keySet()) {
            a_kriN.put(new Index.kriN(k, r, i), arrivalTime.get(i));
        }
    }
}


