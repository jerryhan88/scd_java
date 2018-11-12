package Approach.BranchAndBound;

import Index.AE;
import Index.AEIJ;
import Index.AEI;
import Index.AEK;
import Other.Parameter;

import java.util.*;

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
    PriorityQueue<NodeBnB> pq;
    NodeBnB incumbent = null;

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
        ArrayList<Integer> aeF = prmt.F_ae.get(ae);
        ArrayList<Integer> KnM = new ArrayList<>();
        for (int k: aeF) {
            if (_lm_aek.get(new AEK(a, e, k)) > 0) {
                KnM.add(k);
                lm_k.put(k, _lm_aek.get(new AEK(a, e, k)));
            }
        }
        ArrayList<String> Sn = new ArrayList<>(prmt.S_ae.get(ae));
        KnM.sort(Comparator.comparingDouble((Integer o) -> lm_k.get(o)));
        //
        pq = new PriorityQueue<>(new Comparator<NodeBnB>() {
            @Override
            public int compare(NodeBnB o1, NodeBnB o2) {
                if (o1.tLB < o2.tLB)
                    return 1;
                else if (o1.tLB > o2.tLB)
                    return -1;
                else {
                    if (o1.upperBound < o2.upperBound)
                        return 1;
                    else if (o1.upperBound > o2.upperBound)
                        return -1;
                    else
                        return 0;
                }
            }
        });
        pq.add(new NodeBnB(this, KnM, Sn));
    }

    void update_incumbent(NodeBnB tn) {
        assert tn.lowerBound != -Double.MAX_VALUE;
        if (incumbent == null || incumbent.lowerBound < tn.lowerBound) {
            incumbent = tn;
        }

    }

    private void branch() {
        NodeBnB tn = pq.poll();
        if (incumbent != null && tn.upperBound <= incumbent.lowerBound)
            return;
        if (tn.tLB == tn.upperBound) {
            tn.lowerBound = tn.g();
            update_incumbent(tn);
        } else {
            ArrayList<NodeBnB> children = tn.gen_children_or_calc_lowerBound();
            if (children == null) {
                update_incumbent(tn);
            } else {
                pq.addAll(children);
            }
        }
    }

    public void solve() {
        while (pq.size() != 0) {
            branch();
        }
        update_dvs();
    }

    void update_dvs() {
        x_aeij = new HashMap<>();
        mu_aei = new HashMap<>();
        ArrayList<String> aeN = prmt.N_ae.get(new AE(a, e));
        for (String i: aeN) {
            for (String j: aeN) {
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


