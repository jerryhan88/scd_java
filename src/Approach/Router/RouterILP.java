package Approach.Router;

import Approach.ModelBuilder;
import Index.AE;
import Index.AEI;
import Index.AEIJ;
import Index.AEK;
import Other.Parameter;
import Other.RoutingProbSol;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.HashMap;

public class RouterILP extends RouterSup {

    public double solve(RoutingProbSol rProbSol) {
        int a = rProbSol.get_aid();
        int e = rProbSol.get_eid();
        Parameter prmt = rProbSol.get_prmt();
        HashMap<AEK, Double> lm_aek = rProbSol.get_lm_aek();
        //
        AE ae = new AE(a, e);
        ArrayList<String> aeN = prmt.N_ae.get(ae);
        try {
            double lm;
            AEK aek;
            AEIJ aeij;
            IloNumVar x;
            //
            IloCplex cplex = new IloCplex();
            HashMap<AEIJ, IloNumVar> x_aeij = new HashMap<>();
            HashMap<AEI, IloNumVar> mu_aei = new HashMap<>();
            for (String i: aeN) {
                for (String j: aeN) {
                    x_aeij.put(new AEIJ(a, e, i, j), cplex.boolVar(String.format("x(%d,%d,%s,%s)", a, e, i, j)));
                }
                mu_aei.put(new AEI(a, e, i), cplex.numVar(0.0, Double.MAX_VALUE, String.format("mu(%d,%d,%s)", a, e, i)));
            }
            //
            IloLinearNumExpr obj = cplex.linearNumExpr();
            for (int k : prmt.K) {
                aek = new AEK(a, e, k);
                lm = lm_aek.get(aek);
                for (String j: aeN) {
                    aeij = new AEIJ(a, e, prmt.n_k.get(k), j);
                    x = x_aeij.get(aeij);
                    obj.addTerm(-lm, x);
                }
            }
            cplex.addMinimize(obj);
            //
            ModelBuilder.def_FC_cnsts_aeGiven(prmt, a, e, cplex, x_aeij);
            ModelBuilder.def_AT_cnsts_aeGiven(prmt, a, e, cplex, x_aeij, mu_aei);
            //
            cplex.setOut(null);
            cplex.solve();
            if (cplex.getStatus() == IloCplex.Status.Optimal) {
                // TODO
                // Update this part!!  save solution in routingProbSol

//                try {
//                    objV_Routing += cplex.getObjValue();
//                    for (AEIJ key: x_aeij.keySet()) {
//                        _x_aeij.put(key, cplex.getValue(x_aeij.get(key)));
//                    }
//                    for (AEI key: mu_aei.keySet()) {
//                        _mu_aei.put(key, cplex.getValue(mu_aei.get(key)));
//                    }
//                } catch (IloException ex) {
//                    ex.printStackTrace();
//                }


            } else {
                cplex.output().println("Other.Solution status = " + cplex.getStatus());
            }
            cplex.end();
        } catch (Exception ex) {
        ex.printStackTrace();
        }

        return 0.0;
    }
}
