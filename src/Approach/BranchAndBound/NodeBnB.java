package Approach.BranchAndBound;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

class NodeBnB {
    private TreeBnB tree;
    //
    private HashSet<String> visitedWarehouses;
    private ArrayList<Integer> includedTasks;
    private ArrayList<Integer> candiTasks;
    ArrayList<String> partialSequence;
    private int seqIndex4Search;
    //
    double lowerBound = -Double.MAX_VALUE;
    double upperBound = Double.MAX_VALUE;
    //
    NodeBnB(TreeBnB _tree,
            ArrayList<String> initialRoutineRoute,
            ArrayList<Integer> initialFeasibleTasks) {
        tree = _tree;
        includedTasks = new ArrayList<>();
        candiTasks = initialFeasibleTasks;
        partialSequence = initialRoutineRoute;
        //
        visitedWarehouses = new HashSet<>();
        seqIndex4Search = 1;
    }

    private NodeBnB(NodeBnB pn,
                    ArrayList<Integer> _includedTasks,
                    ArrayList<Integer> _candiTasks,
                    ArrayList<String> _partialSequence, int _seqIndex4Search) {
        tree = pn.tree;
        visitedWarehouses = new HashSet<>(pn.visitedWarehouses);
        //
        includedTasks = _includedTasks;
        candiTasks = _candiTasks;
        partialSequence = _partialSequence;
        seqIndex4Search = _seqIndex4Search;
    }

    ArrayList<NodeBnB> gen_children_and_calc_bounds() {
        if (includedTasks.size() == tree.k_v || candiTasks.size() == 0) {
            lowerBound = 0.0;
            for (int tid: includedTasks)
                lowerBound += tree.lm_iT.get(tid);
            return null;
        } else {
            ArrayList<NodeBnB> children = new ArrayList<>();
            ArrayList<Integer> feasibleTasks = new ArrayList<>();
            for (int tid: candiTasks) {
                ArrayList<String> best_sequence = get_best_sequence(tid, seqIndex4Search);
                if (get_travelTime(best_sequence) - tree.kr_l <= tree.kr_u) {
                    feasibleTasks.add(tid);
                    ArrayList<Integer> includedTasks1 = new ArrayList<>(includedTasks);
                    includedTasks1.add(tid);
                    ArrayList<Integer> candiTasks1 = new ArrayList<>(candiTasks);
                    candiTasks1.remove(tid);
                    int seqIndex4Search1 = best_sequence.indexOf(tree.prmt.n_i.get(tid));
                    children.add(new NodeBnB(this, includedTasks1, candiTasks1,
                                             best_sequence, seqIndex4Search1));
                }
            }
            if (children.size() != 0) {
                upperBound = 0.0;
                for (int tid: includedTasks)
                    upperBound += tree.lm_iT.get(tid);
                for (int tid: feasibleTasks)
                    upperBound += tree.lm_iT.get(tid);
                return children;
            } else {
                lowerBound = 0.0;
                for (int tid: includedTasks)
                    lowerBound += tree.lm_iT.get(tid);
                return null;
            }
        }
    }

    private ArrayList<String> get_best_sequence(int tid, int firstIndex) {
        double min_tt = Double.MAX_VALUE;
        ArrayList<String> best_sequence = null;
        if (visitedWarehouses.contains(tree.prmt.n_i.get(tid))) {
            // Insert the delivery node only
            for (int s1 = firstIndex; s1 < partialSequence.size(); s1++) {
                ArrayList<String> sequence1 = new ArrayList<>(partialSequence);
                sequence1.add(s1, tree.prmt.n_i.get(tid));
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
            visitedWarehouses.add(tree.prmt.n_i.get(tid));
            for (int s0 = firstIndex; s0 < partialSequence.size(); s0++) {
                for (int s1 = s0; s1 < partialSequence.size(); s1++) {
                    ArrayList<String> sequence1 = new ArrayList<>(partialSequence);
                    sequence1.add(s0, tree.prmt.h_i.get(tid));
                    sequence1.add(s1 + 1, tree.prmt.n_i.get(tid));
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
            tt += tree.prmt.t_ij.get(new Index.ij(n0, n1));
        }
        return tt;
    }

    private boolean check_TW_violation(ArrayList<String> sequence) {
        String n0, n1;
        n0 = sequence.get(0);
        double erest_deptTime = tree.prmt.a_i.get(n0) + tree.prmt.c_i.get(n0);
        for (int i = 1; i < sequence.size(); i++) {
            n1 = sequence.get(i);
            double erest_arrvTime = erest_deptTime + tree.prmt.t_ij.get(new Index.ij(n0, n1));
            if (tree.prmt.b_i.get(n1) < erest_arrvTime) {
                return true;
            } else {
                erest_deptTime = Math.max(erest_arrvTime, tree.prmt.a_i.get(n1)) + tree.prmt.c_i.get(n1);
            }
            n0 = n1;
        }
        return false;
    }

    HashMap<String, Double> get_arrivalTime() {
        assert lowerBound != -Double.MAX_VALUE;
        //
        String n0, n1;
        n0 = partialSequence.get(0);
        HashMap<String, Double> arrivalTime = new HashMap<>();
        arrivalTime.put(n0, tree.prmt.a_i.get(n0));
        double erest_deptTime = tree.prmt.a_i.get(n0) + tree.prmt.c_i.get(n0);
        for (int i = 1; i < partialSequence.size(); i++) {
            n1 = partialSequence.get(i);
            //
            double erest_arrvTime = erest_deptTime + tree.prmt.t_ij.get(new Index.ij(n0, n1));
            double actual_arrvTime = Math.max(erest_arrvTime, tree.prmt.a_i.get(n1));
            arrivalTime.put(n1, actual_arrvTime);
            erest_deptTime = actual_arrvTime + tree.prmt.c_i.get(n1);
            n0 = n1;
        }
        //
        return arrivalTime;
    }
}
