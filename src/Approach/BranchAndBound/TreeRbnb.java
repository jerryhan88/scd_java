package Approach.BranchAndBound;

import Index.AE;
import Index.AEI;
import Index.AEIJ;
import Index.AEK;
import Other.Parameter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;

public class TreeRbnb extends TreeBnB{

    public TreeRbnb(Parameter _prmt, int _a, int _e, HashMap<AEK, Double> _lm_aek) {
        super(_prmt, _a, _e, _lm_aek);
        //
        AE ae = new AE(_a, _e);
        ArrayList<String> Sn = new ArrayList<>(prmt.S_ae.get(ae));
        ArrayList<Integer> KnM = new ArrayList<>();
        ArrayList<Integer> aeF = prmt.F_ae.get(ae);
        for (int k: aeF) {
            if (_lm_aek.get(new AEK(a, e, k)) > 0) {
                KnM.add(k);
                lm_k.put(k, _lm_aek.get(new AEK(a, e, k)));
            }
        }
        pq = new PriorityQueue<>(new Comparator<NodeBnB>() {
            @Override
            public int compare(NodeBnB o1, NodeBnB o2) {
                if (o1.upperBound < o2.upperBound)
                    return 1;
                else if (o1.upperBound > o2.upperBound)
                    return -1;
                else {
                    if (o1.tLB < o2.tLB)
                        return 1;
                    else if (o1.tLB > o2.tLB)
                        return -1;
                    else
                        return 0;
                }
            }
        });
        pq.add(new NodeBnB(this, KnM, Sn));
    }

    private void branch() {
        NodeBnB tn = pq.poll();
        if (incumbent != null && tn.tLB <= incumbent.lowerBound)
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
}


