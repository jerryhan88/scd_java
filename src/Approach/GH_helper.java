package Approach;

import Index.IJ;
import Other.Parameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class GH_helper {


    static ArrayList<String> get_best_sequence(Parameter prmt,
                                               HashSet<String> visitedWH,
                                               ArrayList<String> partialSequence,
                                               int seqBeginIndex4Search,
                                               int tid) {
        double min_tt = Double.MAX_VALUE;
        ArrayList<String> best_sequence = null;
        if (visitedWH.contains(prmt.h_k.get(tid))) {
            // Insert the delivery node only
            for (int s1 = seqBeginIndex4Search; s1 < partialSequence.size(); s1++) {
                ArrayList<String> sequence1 = new ArrayList<>(partialSequence);
                sequence1.add(s1, prmt.n_k.get(tid));
                if (check_TW_violation(prmt, sequence1))
                    continue;
                double tt = get_travelTime(prmt, sequence1);
                if (tt < min_tt) {
                    min_tt = tt;
                    best_sequence = sequence1;
                }
            }
        } else {
            // Insert both the warehouse and delivery nodes
            // The beginning index for the warehouse starts from the 1 (0 is the origin)
            // but, in the case of the delivery node, it starts from max(s0, seqBeginIndex4Search)
            //      where s0 = the index of the warehouse
            for (int s0 = 1; s0 < partialSequence.size(); s0++) {
                for (int s1 = Integer.max(s0, seqBeginIndex4Search); s1 < partialSequence.size(); s1++) {
                    ArrayList<String> sequence1 = new ArrayList<>(partialSequence);
                    sequence1.add(s0, prmt.h_k.get(tid));
                    sequence1.add(s1 + 1, prmt.n_k.get(tid));
                    if (check_TW_violation(prmt, sequence1))
                        continue;
                    double tt = get_travelTime(prmt, sequence1);
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

    private static boolean check_TW_violation(Parameter prmt,
                                              ArrayList<String> sequence) {
        String n0, n1;
        n0 = sequence.get(0);
        double erest_deptTime = prmt.al_i.get(n0) + prmt.ga_i.get(n0);
        for (int i = 1; i < sequence.size(); i++) {
            n1 = sequence.get(i);
            double erest_arrvTime = erest_deptTime + prmt.t_ij.get(new IJ(n0, n1));
            if (prmt.be_i.get(n1) < erest_arrvTime) {
                return true;
            } else {
                erest_deptTime = Math.max(erest_arrvTime, prmt.al_i.get(n1)) + prmt.ga_i.get(n1);
            }
            n0 = n1;
        }
        return false;
    }

    static double get_travelTime(Parameter prmt, ArrayList<String> sequence) {
        String n0, n1;
        double tt = 0.0;
        for (int i = 0; i < sequence.size() - 1; i++) {
            n0 = sequence.get(i);
            n1 = sequence.get(i + 1);
            tt += prmt.t_ij.get(new IJ(n0, n1));
        }
        return tt;
    }

    static HashMap<String, Double> get_arrivalTime(Parameter prmt, ArrayList<String> sequence) {
        String n0, n1;
        n0 = sequence.get(0);
        HashMap<String, Double> arrivalTime = new HashMap<>();
        arrivalTime.put(n0, prmt.al_i.get(n0));
        double erest_deptTime = prmt.al_i.get(n0) + prmt.ga_i.get(n0);
        for (int i = 1; i < sequence.size(); i++) {
            n1 = sequence.get(i);
            //
            double erest_arrvTime = erest_deptTime + prmt.t_ij.get(new IJ(n0, n1));
            double actual_arrvTime = Math.max(erest_arrvTime, prmt.al_i.get(n1));
            arrivalTime.put(n1, actual_arrvTime);
            erest_deptTime = actual_arrvTime + prmt.ga_i.get(n1);
            n0 = n1;
        }
        //
        return arrivalTime;
    }
}
