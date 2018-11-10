package Approach.BranchAndBound;

import Index.IJ;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

class NodeBnB {
    private TreeBnB tree;
    //
    private HashSet<String> Hn;  // Visited warehouses
    private ArrayList<Integer> KnP;  // Set of included tasks
    private ArrayList<Integer> KnM;  // Set of candidate tasks
    ArrayList<String> Sn;  // Partial sequence
    private int seqIndex4Search;
    double currentVolume, currentWeight;
    //
    double lowerBound = -Double.MAX_VALUE;
    double upperBound = Double.MAX_VALUE;
    //
    NodeBnB(TreeBnB _tree,
            ArrayList<Integer> _KnM,
            ArrayList<String> _Sn) {
        tree = _tree;
        KnP = new ArrayList<>();
        KnM = _KnM;
        Sn = _Sn;
        seqIndex4Search = 1;
        currentVolume = 0.0;
        currentWeight = 0.0;
        //
        Hn = new HashSet<>();
    }

    private NodeBnB(NodeBnB pn,
                    ArrayList<Integer> _KnP,
                    ArrayList<Integer> _KnM,
                    ArrayList<String> _Sn, int _seqIndex4Search) {
        tree = pn.tree;
        //
        KnP = _KnP;
        KnM = _KnM;
        Sn = _Sn;
        seqIndex4Search = _seqIndex4Search;
        currentVolume = 0.0;
        currentWeight = 0.0;
        //
        Hn = new HashSet<>();
        for (int tid: KnP) {
            Hn.add(tree.prmt.h_k.get(tid));
            currentVolume += tree.prmt.v_k.get(tid);
            currentWeight += tree.prmt.w_k.get(tid);
        }
    }

    public String toString() {
        return String.join(",", Sn);
    }

    double g() {  // return the sum of the lambda values of KnP
        double v = 0.0;
        for (int tid: KnP)
            v += tree.lm_k.get(tid);
        return v;
    }

    double h() {  // return the sum of the lambda values of KnM
        double v = 0.0;
        for (int tid: KnM)
            v += tree.lm_k.get(tid);
        return v;
    }

    ArrayList<NodeBnB> gen_children_and_calc_bounds() {
        if (KnM.size() == 0) {
            lowerBound = g();
            return null;
        } else {
            ArrayList<NodeBnB> children = new ArrayList<>();
            for (int tid: KnM) {
                if (tree.a_v < currentVolume + tree.prmt.v_k.get(tid))
                    continue;
                if (tree.a_w < currentWeight + tree.prmt.w_k.get(tid))
                    continue;
                ArrayList<String> best_sequence = get_best_sequence(tid, seqIndex4Search);
                if (get_travelTime(best_sequence) - tree.ae_l <= tree.ae_u) {
                    ArrayList<Integer> KnP1 = new ArrayList<>(KnP);
                    KnP1.add(tid);
                    ArrayList<Integer> KnM1 = new ArrayList<>(KnM);
                    KnM1.remove((Integer) tid);
                    int seqIndex4Search1 = best_sequence.indexOf(tree.prmt.n_k.get(tid)) + 1;
                    children.add(new NodeBnB(this, KnP1, KnM1,
                                             best_sequence, seqIndex4Search1));
                }
            }
            if (children.size() != 0) {
                upperBound = g() + h();
                return children;
            } else {
                lowerBound = g();
                return null;
            }
        }
    }

    private ArrayList<String> get_best_sequence(int tid, int firstIndex) {
        double min_tt = Double.MAX_VALUE;
        ArrayList<String> best_sequence = null;
        if (Hn.contains(tree.prmt.h_k.get(tid))) {
            // Insert the delivery node only
            for (int s1 = firstIndex; s1 < Sn.size(); s1++) {
                ArrayList<String> sequence1 = new ArrayList<>(Sn);
                sequence1.add(s1, tree.prmt.n_k.get(tid));
                if (check_TW_violation(sequence1))
                    continue;
                double tt = get_travelTime(sequence1);
                if (tt < min_tt) {
                    min_tt = tt;
                    best_sequence = sequence1;
                }
            }
        } else {
            // Insert both the warehouse and delivery nodes
            for (int s0 = firstIndex; s0 < Sn.size(); s0++) {
                for (int s1 = s0; s1 < Sn.size(); s1++) {
                    ArrayList<String> sequence1 = new ArrayList<>(Sn);
                    sequence1.add(s0, tree.prmt.h_k.get(tid));
                    sequence1.add(s1 + 1, tree.prmt.n_k.get(tid));
                    if (check_TW_violation(sequence1))
                        continue;
                    double tt = get_travelTime(sequence1);
                    if (tt < min_tt) {
                        min_tt = tt;
                        best_sequence = sequence1;
                    }
                }
            }
        }
        //
        return best_sequence;
    }

    private double get_travelTime(ArrayList<String> sequence) {
        String n0, n1;
        double tt = 0.0;
        for (int i = 0; i < sequence.size() - 1; i++) {
            n0 = sequence.get(i);
            n1 = sequence.get(i + 1);
            tt += tree.prmt.t_ij.get(new IJ(n0, n1));
        }
        return tt;
    }

    private boolean check_TW_violation(ArrayList<String> sequence) {
        String n0, n1;
        n0 = sequence.get(0);
        double erest_deptTime = tree.prmt.al_i.get(n0) + tree.prmt.ga_i.get(n0);
        for (int i = 1; i < sequence.size(); i++) {
            n1 = sequence.get(i);
            double erest_arrvTime = erest_deptTime + tree.prmt.t_ij.get(new IJ(n0, n1));
            if (tree.prmt.be_i.get(n1) < erest_arrvTime) {
                return true;
            } else {
                erest_deptTime = Math.max(erest_arrvTime, tree.prmt.al_i.get(n1)) + tree.prmt.ga_i.get(n1);
            }
            n0 = n1;
        }
        return false;
    }

    HashMap<String, Double> get_arrivalTime() {
        assert lowerBound != -Double.MAX_VALUE;
        //
        String n0, n1;
        n0 = Sn.get(0);
        HashMap<String, Double> arrivalTime = new HashMap<>();
        arrivalTime.put(n0, tree.prmt.al_i.get(n0));
        double erest_deptTime = tree.prmt.al_i.get(n0) + tree.prmt.ga_i.get(n0);
        for (int i = 1; i < Sn.size(); i++) {
            n1 = Sn.get(i);
            //
            double erest_arrvTime = erest_deptTime + tree.prmt.t_ij.get(new IJ(n0, n1));
            double actual_arrvTime = Math.max(erest_arrvTime, tree.prmt.al_i.get(n1));
            arrivalTime.put(n1, actual_arrvTime);
            erest_deptTime = actual_arrvTime + tree.prmt.ga_i.get(n1);
            n0 = n1;
        }
        //
        return arrivalTime;
    }
}
