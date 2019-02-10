package Approach.Router.TreeSearch;

import Approach.GH_helper;
import Index.AE;
import Index.AEIJ;
import Index.AEI;
import Index.AEK;
import Other.Etc;
import Other.Parameter;

import java.util.*;

public class Tree {
    Parameter prmt;
    private Etc etc;
    public int a, e;
    double a_v, a_w, ae_l, ae_u;
    HashMap<Integer, Double> lm_k;
    //
    public double objV;
    public HashMap<AEIJ, Double> x_aeij;
    public HashMap<AEI, Double> mu_aei;
    //
    private int lastNodeID;
    PriorityQueue<Node> pq;
    Node incumbent = null;
    boolean isSearchFinished;

    Tree(Parameter prmt, Etc etc,
         int a, int e, HashMap<AEK, Double> lm_aek) {
        this.prmt = prmt;
        this.etc = etc;
        this.a = a;
        this.e = e;
        //
        AE ae = prmt.get_AE(a, e);
        a_v = prmt.v_a.get(a);
        a_w = prmt.w_a.get(a);
        ae_l = this.prmt.l_ae.get(ae);
        ae_u = this.prmt.u_ae.get(ae);
        lm_k = new HashMap<>();
        ArrayList aeF = this.prmt.F_ae.get(ae);
        ArrayList<Integer> KnM = new ArrayList<>();
        AEK aek;
        for (Object k: aeF) {
            aek = prmt.get_AEK(this.a, this.e, k);
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
        lastNodeID = -1;
        pq.add(new Node(this, KnM, Sn));
        isSearchFinished = false;
    }

    void update_dvs() {
        x_aeij = new HashMap<>();
        mu_aei = new HashMap<>();
        ArrayList aeN = prmt.N_ae.get(prmt.get_AE(a, e));
        for (Object i: aeN) {
            for (Object j: aeN) {
                x_aeij.put(prmt.get_AEIJ(a, e, i, j), 0.0);
            }
            mu_aei.put(prmt.get_AEI(a, e, i), 0.0);
        }
        objV = incumbent.lowerBound;
        String n0, n1;
        for (int s = 0; s < incumbent.Sn.size() - 1; s++) {
            n0 = incumbent.Sn.get(s);
            n1 = incumbent.Sn.get(s + 1);
            x_aeij.put(prmt.get_AEIJ(a, e, n0, n1), 1.0);
        }
        HashMap<String, Double> arrivalTime = GH_helper.get_arrivalTime(prmt, incumbent.Sn);
        for (String i: arrivalTime.keySet()) {
            mu_aei.put(prmt.get_AEI(a, e, i), arrivalTime.get(i));
        }
    }

    synchronized int genNodeID() {
        lastNodeID += 1;
        return lastNodeID;
    }

    synchronized int getLastNodeID() {
        return lastNodeID;
    }

    synchronized void update_incumbent(Node tn) {
        assert tn.lowerBound != -Double.MAX_VALUE;
        if (incumbent == null || incumbent.lowerBound < tn.lowerBound) {
            incumbent = tn;
        }
    }

    synchronized Node popNode() {
        Node tn = pq.poll();
        return tn;
    }

    synchronized void pushNodes(ArrayList<Node> nodes) {
        pq.addAll(nodes);
    }

    public void solve() throws InterruptedException {
        throw new RuntimeException("Should override the function!!");
    }
}


