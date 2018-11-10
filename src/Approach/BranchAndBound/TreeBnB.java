package Approach.BranchAndBound;

import Index.AE;
import Index.AEIJ;
import Index.AEI;
import Index.AEK;
import Other.Parameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

public class TreeBnB implements Runnable{
    Parameter prmt;
    int a, e;
    double a_v, a_w, ae_l, ae_u;
    HashMap<Integer, Double> lm_k;
    //
    public double objV;
    public HashMap<AEIJ, Double> x_aeij;
    public HashMap<AEI, Double> mu_aei;
    //
    private Stack<NodeBnB> stackOfNodes;
    private NodeBnB incumbent = null;

    public TreeBnB(Parameter _prmt, int _a, int _e, HashMap<AEK, Double> _lm_aek) {
        AE ae = new AE(_a, _e);
        prmt = _prmt;
        a = _a;
        e = _e;
        a_v = _prmt.v_a.get(_a);
        a_w = _prmt.w_a.get(_a);
        ae_l = prmt.l_ae.get(ae);
        ae_u = prmt.u_ae.get(ae);
        lm_k = new HashMap<>();
        ArrayList<Integer> KnM = new ArrayList<>(prmt.F_ae.get(ae));
        ArrayList<String> Sn = new ArrayList<>(prmt.S_ae.get(ae));
        for (int k: KnM) {
            lm_k.put(k, _lm_aek.get(new AEK(a, e, k)));
        }
        KnM.sort((Integer o1, Integer o2) -> Double.compare(lm_k.get(o1), lm_k.get(o2)));
        //
        stackOfNodes = new Stack<>();
        stackOfNodes.push(new NodeBnB(this, KnM, Sn));
    }

    private void branch() {
        NodeBnB tn = stackOfNodes.pop();
        ArrayList<NodeBnB> children = tn.gen_children_and_calc_bounds();
        if (children == null) {
            assert tn.lowerBound != -Double.MAX_VALUE;
            if (incumbent == null || incumbent.lowerBound < tn.lowerBound) {
                incumbent = tn;
            }
        } else if (incumbent == null){
            for (NodeBnB cn: children) {
                stackOfNodes.push(cn);
            }
        } else if (incumbent.lowerBound < tn.upperBound) {
            for (NodeBnB cn: children) {
                stackOfNodes.push(cn);
            }
        }
    }

    public void solve() {
        while (!stackOfNodes.empty())
            branch();
        //
        x_aeij = new HashMap<>();
        mu_aei = new HashMap<>();
        ArrayList<String> krN = prmt.N_ae.get(new AE(a, e));
        for (String i: krN) {
            for (String j: krN) {
                x_aeij.put(new AEIJ(a, e, i, j), 0.0);
            }
            mu_aei.put(new AEI(a, e, i), 0.0);
        }
        objV = incumbent.lowerBound;
        String n0, n1;
        for (int s = 0; s < incumbent.Sn.size() - 1; s++) {
            n0 = incumbent.Sn.get(s);
            n1 = incumbent.Sn.get(s + 1);
            x_aeij.put(new AEIJ(a, e, n0, n1), 1.0);
        }
        HashMap<String, Double> arrivalTime = incumbent.get_arrivalTime();
        for (String i: arrivalTime.keySet()) {
            mu_aei.put(new AEI(a, e, i), arrivalTime.get(i));
        }
    }

    @Override
    public void run() {
        solve();
    }
}


