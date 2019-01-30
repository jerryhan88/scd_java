package Approach.Router;

import Approach.ModelBuilder;
import Index.*;
import Other.Etc;
import Other.Parameter;
import Other.RoutingProbSol;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
//
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

public class RouterILP extends RouterSup {
    private static double dualGapLimit;

    static class CallbackTimeLimit extends IloCplex.MIPInfoCallback {
        Etc etc;
        HashMap<AEIJ, IloNumVar> x_aeij;
        HashMap<AEI, IloNumVar> mu_aei;
        RoutingProbSol rProbSol;

        CallbackTimeLimit(Etc etc,
                          HashMap<AEIJ, IloNumVar> x_aeij,
                          HashMap<AEI, IloNumVar> mu_aei,
                          RoutingProbSol rProbSol) {
            this.etc = etc;
            this.x_aeij = x_aeij;
            this.mu_aei = mu_aei;
            this.rProbSol = rProbSol;
        }

        @Override
        protected void main() throws IloException {
            if (etc.trigerTermCondition && etc.getCpuTime() > (etc.getSavedTimestamp() * 2)) {
                rProbSol.objV = -getIncumbentObjValue();
                for (AEIJ key: x_aeij.keySet()) {
                    rProbSol.x_aeij.put(key, getIncumbentValue(x_aeij.get(key)));
                }
                for (AEI key: mu_aei.keySet()) {
                    rProbSol.mu_aei.put(key, getIncumbentValue(mu_aei.get(key)));
                }
                abort();
            }
        }
    }

    static class CallbackDualGapLimit extends IloCplex.MIPInfoCallback {
        Etc etc;
        HashMap<AEIJ, IloNumVar> x_aeij;
        HashMap<AEI, IloNumVar> mu_aei;
        RoutingProbSol rProbSol;

        CallbackDualGapLimit(Etc etc,
                             HashMap<AEIJ, IloNumVar> x_aeij,
                             HashMap<AEI, IloNumVar> mu_aei,
                             RoutingProbSol rProbSol) {
            this.etc = etc;
            this.x_aeij = x_aeij;
            this.mu_aei = mu_aei;
            this.rProbSol = rProbSol;
        }

        @Override
        protected void main() throws IloException {
            if (hasIncumbent() && getMIPRelativeGap() < dualGapLimit) {
                rProbSol.objV = -getIncumbentObjValue();
                for (AEIJ key: x_aeij.keySet()) {
                    rProbSol.x_aeij.put(key, getIncumbentValue(x_aeij.get(key)));
                }
                for (AEI key: mu_aei.keySet()) {
                    rProbSol.mu_aei.put(key, getIncumbentValue(mu_aei.get(key)));
                }
                abort();
            }
        }
    }

    static class CallbackIncumbentTreeSize extends IloCplex.MIPInfoCallback {
        Etc etc;
        HashMap<AEIJ, IloNumVar> x_aeij;
        HashMap<AEI, IloNumVar> mu_aei;
        RoutingProbSol rProbSol;
        //
        double _lastIncumbentObjV;
        long _lastIncumbentNodeID = -1;

        CallbackIncumbentTreeSize(Etc etc,
                             HashMap<AEIJ, IloNumVar> x_aeij,
                             HashMap<AEI, IloNumVar> mu_aei,
                             RoutingProbSol rProbSol) {
            this.etc = etc;
            this.x_aeij = x_aeij;
            this.mu_aei = mu_aei;
            this.rProbSol = rProbSol;
        }

        void recordSol() throws IloException {
            rProbSol.objV = -getIncumbentObjValue();
            for (AEIJ key: x_aeij.keySet()) {
                rProbSol.x_aeij.put(key, getIncumbentValue(x_aeij.get(key)));
            }
            for (AEI key: mu_aei.keySet()) {
                rProbSol.mu_aei.put(key, getIncumbentValue(mu_aei.get(key)));
            }
        }

        @Override
        protected void main() throws IloException {
            if (hasIncumbent()) {
                long bnbNodeID = getNnodes64();
                if ( Math.abs(_lastIncumbentObjV - getIncumbentObjValue())
                            > 1e-5 * (1.0 + Math.abs(getIncumbentObjValue())) ){
                    _lastIncumbentObjV = getIncumbentObjValue();
                    _lastIncumbentNodeID = bnbNodeID;
                } else {
                    if (_lastIncumbentNodeID == 0) {
                        if (bnbNodeID > 1e4) {
                            recordSol();
                            abort();
                        }
                    } else {
                        if (_lastIncumbentNodeID != -1 &&
                            bnbNodeID > _lastIncumbentNodeID * 2 &&
                            Math.abs(_lastIncumbentObjV - getIncumbentObjValue())
                                                < 1e-5 * (1.0 + Math.abs(getIncumbentObjValue()))) {
                            recordSol();
                            abort();
                        }
                    }
                }
            }
        }
    }

    public void setDualGapLimit(double dualGapLimit) {
        this.dualGapLimit = dualGapLimit;
    }

    public void solve(RoutingProbSol rProbSol) {
        int a = rProbSol.get_aid();
        int e = rProbSol.get_eid();
        Parameter prmt = rProbSol.get_prmt();
        Etc etc = rProbSol.get_etc();
        HashMap<AEK, Double> lm_aek = rProbSol.get_lm_aek();
        //
        AE ae = new AE(a, e);
        ArrayList aeN = prmt.N_ae.get(ae);
        ArrayList aeF = prmt.F_ae.get(ae);
        //
        try {
            double lm;
            AEK aek;
            AEIJ aeij;
            AEI aei;
            IloNumVar x;
            //
            IloCplex cplex = new IloCplex();
            HashMap<AEIJ, IloNumVar> x_aeij = new HashMap<>();
            HashMap<AEI, IloNumVar> mu_aei = new HashMap<>();
            for (Object i: aeN) {
                for (Object j: aeN) {
                    aeij = new AEIJ(a, e, i, j);
                    x_aeij.put(aeij, cplex.boolVar(String.format("x(%s)", aeij.get_label())));
                }
                aei = new AEI(a, e, i);
                mu_aei.put(aei, cplex.numVar(0.0, Double.MAX_VALUE, String.format("mu(%s)", aei.get_label())));
            }
            //
            IloLinearNumExpr obj = cplex.linearNumExpr();
            for (Object k : aeF) {
                aek = new AEK(a, e, k);
                lm = lm_aek.get(aek);
                for (Object j: aeN) {
                    aeij = new AEIJ(a, e, prmt.n_k.get((Integer) k), j);
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
//            cplex.setOut(new FileOutputStream(String.format("%s_%d_%d_%d.txt", prmt.problemName, numIter, a, e)));
//            cplex.use(new CallbackTimeLimit(etc, x_aeij, mu_aei, rProbSol));
//            cplex.use(new CallbackDualGapLimit(etc, x_aeij, mu_aei, rProbSol));
            cplex.use(new CallbackIncumbentTreeSize(etc, x_aeij, mu_aei, rProbSol));

//            cplex.exportModel(String.format("%s_%d_%d_%d.lp", prmt.problemName, numIter, a, e));
            cplex.solve();
            if (cplex.getStatus() == IloCplex.Status.Optimal) {
                rProbSol.objV = -cplex.getObjValue();
                for (AEIJ key: x_aeij.keySet()) {
                    rProbSol.x_aeij.put(key, cplex.getValue(x_aeij.get(key)));
                }
                for (AEI key: mu_aei.keySet()) {
                    rProbSol.mu_aei.put(key, cplex.getValue(mu_aei.get(key)));
                }
            } else if (cplex.getStatus() == IloCplex.Status.InfeasibleOrUnbounded) {
                cplex.exportModel(String.format("%s.lp", prmt.problemName));
            }
            cplex.end();
        } catch (Exception ex) {
        ex.printStackTrace();
        }
    }
}
