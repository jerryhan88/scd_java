package Approach.Router.TreeSearch;

import Approach.GH_helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

class Node {
    int nid;
    private Tree tree;
    private Node pn;  // Parent node
    //
    private HashSet<String> Hn;  // Visited warehouses
    private ArrayList<Integer> KnP;  // Set of included tasks
    private ArrayList<Integer> KnM;  // Set of candidate tasks
    ArrayList<String> Sn;  // Partial sequence
    private int seqIndex4Search, n_tk;
    private double currentVolume, currentWeight;
    //
    double lowerBound = -Double.MAX_VALUE;
    double upperBound;
    double tLB;  // temporal lower bound = g()
    //
    private HashMap<Integer, ArrayList> KnM_Sn;
    //
    Node(Tree tree,
         ArrayList<Integer> KnM,
         ArrayList<String> Sn) {
        this.tree = tree;
        nid = this.tree.genNodeID();
        KnP = new ArrayList<>();
        this.KnM = KnM;
        this.Sn = Sn;
        seqIndex4Search = 1;
        currentVolume = 0.0;
        currentWeight = 0.0;
        //
        Hn = new HashSet<>();
        set_KnM_bestSeqence();
        upperBound = g() + h();
        tLB = g();
    }

    private Node(Node pn, int n_tk, ArrayList<String> Sn) {
        tree = pn.tree;
        nid = tree.genNodeID();
        this.pn = pn;
        this.n_tk = n_tk;
        this.Sn = Sn;
        seqIndex4Search = this.Sn.indexOf(tree.prmt.n_k.get(n_tk)) + 1;
        //
        KnP = new ArrayList<>(this.pn.KnP);
        KnP.add(this.n_tk);
        //
        currentVolume = 0.0;
        currentWeight = 0.0;
        Hn = new HashSet<>();
        for (int tid: KnP) {
            Hn.add(tree.prmt.h_k.get(tid));
            currentVolume += tree.prmt.v_k.get(tid);
            currentWeight += tree.prmt.w_k.get(tid);
        }
        set_KnM_bestSeqence();
        upperBound = g() + h();
        tLB = g();
    }

    public String toString() {
        return String.format("tLB %.4f  UB %.4f  %s", tLB, upperBound, String.join(",", Sn));
    }

    double g() {  // return the sum of the lambda values of KnP
        double v = 0.0;
        for (int tid: KnP)
            v += tree.lm_k.get(tid);
        return v;
    }

    private double h() {  // return the sum of the lambda values of KnM
        double v = 0.0;
        for (int tid: KnM)
            v += tree.lm_k.get(tid);
        return v;
    }

    private void set_KnM_bestSeqence() {
        ArrayList<Integer> _KnM;
        if (pn == null){
            _KnM = new ArrayList<>(KnM);
        } else {
            _KnM = new ArrayList<>(pn.KnM);
            _KnM.remove((Integer) n_tk);
        }
        KnM_Sn = new HashMap<>();
        for (int tid: _KnM) {
            if (tree.a_v < currentVolume + tree.prmt.v_k.get(tid))
                continue;
            else if (tree.a_w < currentWeight + tree.prmt.w_k.get(tid))
                continue;
            else {
                ArrayList<String> best_sequence = GH_helper.get_best_sequence(tree.prmt, Hn, Sn, seqIndex4Search, tid);
                if (best_sequence == null ||
                        GH_helper.get_travelTime(tree.prmt, best_sequence) - tree.ae_l > tree.ae_u) {
                    continue;
                } else {
                    KnM_Sn.put(tid, best_sequence);
                }
            }
        }
        KnM = new ArrayList<>(KnM_Sn.keySet());
    }

    ArrayList<Node> gen_children_or_calc_lowerBound() {
        if (KnM.size() == 0) {
            lowerBound = g();
            return null;
        } else {
            ArrayList<Node> children = new ArrayList<>();
            for (int tid: KnM_Sn.keySet()) {
                children.add(new Node(this, tid, KnM_Sn.get(tid)));
            }
            if (children.size() != 0) {
                return children;
            } else {
                lowerBound = g();
                return null;
            }
        }
    }
}
