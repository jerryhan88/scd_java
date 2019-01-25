package Approach.Router.BranchAndBound;

import Index.AE;
import Index.AEIJ;
import Index.AEI;
import Index.AEK;
import Other.Etc;
import Other.Parameter;

import java.util.*;

public class TreeBNB {
    Parameter prmt;
    Etc etc;
    private int a, e;
    double a_v, a_w, ae_l, ae_u;
    HashMap<Integer, Double> lm_k;
    //
    public double objV;
    public HashMap<AEIJ, Double> x_aeij;
    public HashMap<AEI, Double> mu_aei;
    //
    PriorityQueue<NodeBnB> pq;
    NodeBnB incumbent = null;
    //
    public boolean isTerminated;

    public TreeBNB(Parameter prmt, Etc etc,
                   int a, int e, HashMap<AEK, Double> lm_aek) {
        this.prmt = prmt;
        this.etc = etc;
        this.a = a;
        this.e = e;
        //
        AE ae = new AE(a, e);
        a_v = prmt.v_a.get(a);
        a_w = prmt.w_a.get(a);
        ae_l = this.prmt.l_ae.get(ae);
        ae_u = this.prmt.u_ae.get(ae);
        lm_k = new HashMap<>();
        ArrayList aeF = this.prmt.F_ae.get(ae);
        ArrayList<Integer> KnM = new ArrayList<>();
        AEK aek;
        for (Object k: aeF) {
            aek = new AEK(this.a, this.e, k);
            if (lm_aek.get(aek) > 0) {
                KnM.add((Integer) k);
                lm_k.put((Integer) k, lm_aek.get(aek));
            }
        }
        ArrayList Sn = new ArrayList<>(this.prmt.S_ae.get(ae));
        KnM.sort(Comparator.comparingDouble((Integer o) -> lm_k.get(o)));
        //
        pq = new PriorityQueue<>((o1, o2) -> {
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
        });
        pq.add(new NodeBnB(this, KnM, Sn));
        //
        isTerminated = false;
    }

    void update_incumbent(NodeBnB tn) {
        assert tn.lowerBound != -Double.MAX_VALUE;
        if (incumbent == null || incumbent.lowerBound < tn.lowerBound) {
            incumbent = tn;
        }
    }

    private void branch() {
        NodeBnB tn = pq.poll();
        assert tn != null;
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
            if (etc.trigerTermCondition && etc.getCpuTime() > (etc.getSavedTimestamp() * 2)) {
                break;
            }
        }
        update_dvs();
    }

    void update_dvs() {
        x_aeij = new HashMap<>();
        mu_aei = new HashMap<>();
        ArrayList aeN = prmt.N_ae.get(new AE(a, e));
        for (Object i: aeN) {
            for (Object j: aeN) {
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
}


