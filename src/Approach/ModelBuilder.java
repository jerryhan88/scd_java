package Approach;

import Index.*;
import Other.Parameter;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class ModelBuilder {

    static void def_dvs_yzax(Parameter prmt, IloCplex cplex,
                             HashMap<AK, IloNumVar> y_ak,
                             HashMap<AEK, IloNumVar> z_aek,
                             HashMap<AEIJ, IloNumVar> x_aeij,
                             HashMap<AEI, IloNumVar> mu_aei){
        ArrayList<Integer> aE;
        ArrayList<String> aeN;
        try {
            for (int a : prmt.A) {
                for (int k : prmt.K) {
                    y_ak.put(new AK(a, k), cplex.boolVar(String.format("y(%d,%d)", a, k)));
                }
                aE = prmt.E_a.get(a);
                for (int e : aE) {
                    for (int k : prmt.K) {
                        z_aek.put(new AEK(a, e, k), cplex.boolVar(String.format("z(%d,%d,%d)", a, e, k)));
                    }
                    aeN = prmt.N_ae.get(new AE(a, e));
                    for (String i: aeN) {
                        for (String j: aeN) {
                            x_aeij.put(new AEIJ(a, e, i, j), cplex.boolVar(String.format("x(%d,%d,%s,%s)", a, e, i, j)));
                        }
                        mu_aei.put(new AEI(a, e, i), cplex.numVar(0.0, Double.MAX_VALUE, String.format("mu(%d,%d,%s)", a, e, i)));
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    static void def_TAA_cnsts(Parameter prmt, IloCplex cplex,
                              HashMap<AK, IloNumVar> y_ak,
                              HashMap<AEK, IloNumVar> z_aek) {
        ArrayList<Integer> aE;
        IloNumVar y, z;
        IloLinearNumExpr cnst;
        AK ak;
        AEK aek;
        // Task assignment and accomplishment (Q1)
        try {
            for (int k : prmt.K) {
                cnst = cplex.linearNumExpr();
                for (int a : prmt.A) {
                    ak = new AK(a, k);
                    y = y_ak.get(ak);
                    cnst.addTerm(1, y);
                }
                cplex.addLe(cnst, 1, String.format("TA(%d)", k));
            }
            for (int a : prmt.A) {
                cnst = cplex.linearNumExpr();
                for (int k : prmt.K) {
                    ak = new AK(a, k);
                    y = y_ak.get(ak);

                    cnst.addTerm(prmt.v_k.get(k), y);
                }
                cplex.addLe(cnst, prmt.v_a.get(a), String.format("V(%d)", a));
            }
            for (int a : prmt.A) {
                cnst = cplex.linearNumExpr();
                for (int k : prmt.K) {
                    ak = new AK(a, k);
                    y = y_ak.get(ak);
                    cnst.addTerm(prmt.w_k.get(k), y);
                }
                cplex.addLe(cnst, prmt.w_a.get(a), String.format("W(%d)", a));
            }
            //
            for (int k : prmt.K) {
                for (int a : prmt.A) {
                    ak = new AK(a, k);
                    y = y_ak.get(ak);
                    aE = prmt.E_a.get(a);
                    for (int e : aE) {
                        aek = new AEK(a, e, k);
                        z = z_aek.get(aek);
                        cplex.addLe(z, y, String.format("TC(%d,%d,%d)", a, e, k));
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    static void def_Routing_cnsts(Parameter prmt, IloCplex cplex,
                                  HashMap<AEIJ, IloNumVar> x_aeij,
                                  HashMap<AEI, IloNumVar> mu_aei) {

        ArrayList<Integer> aE;
        // Routing (Q2)
        try {
            for (int a : prmt.A) {
                aE = prmt.E_a.get(a);
                for (int e : aE) {
                    def_FC_cnsts_aeGiven(prmt, a, e, cplex, x_aeij);
                    def_AT_cnsts_aeGiven(prmt, a, e, cplex, x_aeij, mu_aei);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void def_FC_cnsts_aeGiven(Parameter prmt, int a, int e,
                                            IloCplex cplex,
                                            HashMap<AEIJ, IloNumVar> x_aeij) {
        IloNumVar x;
        IloLinearNumExpr cnst;
        AEIJ aeij;
        //
        AE ae = new AE(a, e);
        ArrayList<String> aeS = prmt.S_ae.get(ae);
        ArrayList<String> aeN = prmt.N_ae.get(ae);
        ArrayList<Integer> ae_uF = prmt.uF_ae.get(ae);
        String o_ae = String.format("s0_%d_%d", a, e);
        String d_ae = String.format("s%d_%d_%d", aeS.size() - 1, a, e);
        // Flow conservation given agent k and routine route r
        try {
            // Initiate flow
            cnst = cplex.linearNumExpr();
            for (String j: aeN) {
                aeij = new AEIJ(a, e, o_ae, j);
                x = x_aeij.get(aeij);
                cnst.addTerm(1, x);
            }
            cplex.addEq(cnst, 1, String.format("iFO(%d,%d)", a, e));
            cnst = cplex.linearNumExpr();
            for (String j: aeN) {
                aeij = new AEIJ(a, e, j, d_ae);
                x = x_aeij.get(aeij);
                cnst.addTerm(1, x);
            }
            cplex.addEq(cnst, 1, String.format("iFD(%d,%d)", a, e));
            for (String i: aeS) {
                if (i.equals(o_ae) || i.equals(d_ae)) continue;
                cnst = cplex.linearNumExpr();
                for (String j: aeN) {
                    if (j.equals(i)) continue;
                    aeij = new AEIJ(a, e, i, j);
                    x = x_aeij.get(aeij);
                    cnst.addTerm(1, x);
                }
                cplex.addEq(cnst, 1, String.format("iFS1(%d,%d,%s)", a, e, i));
                cnst = cplex.linearNumExpr();
                for (String j: aeN) {
                    if (j.equals(i)) continue;
                    aeij = new AEIJ(a, e, j, i);
                    x = x_aeij.get(aeij);
                    cnst.addTerm(1, x);
                }
                cplex.addEq(cnst, 1, String.format("iFS2(%d,%d,%s)", a, e, i));
            }
            // No flow
            cnst = cplex.linearNumExpr();
            for (String j: aeN) {
                aeij = new AEIJ(a, e, j, o_ae);
                x = x_aeij.get(aeij);
                cnst.addTerm(1, x);
            }
            cplex.addEq(cnst, 0, String.format("xFO(%d,%d)", a, e));
            cnst = cplex.linearNumExpr();
            for (String j: aeN) {
                aeij = new AEIJ(a, e, d_ae, j);
                x = x_aeij.get(aeij);
                cnst.addTerm(1, x);
            }
            cplex.addEq(cnst, 0, String.format("xFD(%d,%d)", a, e));
            for (int i: ae_uF) {
                cnst = cplex.linearNumExpr();
                for (String j: aeN) {
                    aeij = new AEIJ(a, e, prmt.n_k.get(i), j);
                    x = x_aeij.get(aeij);
                    cnst.addTerm(1, x);
                }
                cplex.addEq(cnst, 0, String.format("xFN(%d,%d,%d)", a, e, i));
            }
            // Flow about delivery nodes; only when the warehouse visited
            for (int i: prmt.K) {
                cnst = cplex.linearNumExpr();
                for (String j: aeN) {
                    aeij = new AEIJ(a, e, prmt.n_k.get(i), j);
                    x = x_aeij.get(aeij);
                    cnst.addTerm(1, x);
                }
                for (String j: aeN) {
                    aeij = new AEIJ(a, e, j, prmt.h_k.get(i));
                    x = x_aeij.get(aeij);
                    cnst.addTerm(-1, x);
                }
                cplex.addLe(cnst, 0, String.format("tFC(%d,%d,%d)", a, e, i));
            }
            // Flow conservation
            for (String i: prmt.N) {
                cnst = cplex.linearNumExpr();
                for (String j: aeN) {
                    aeij = new AEIJ(a, e, i, j);
                    x = x_aeij.get(aeij);
                    cnst.addTerm(1, x);
                }
                for (String j: aeN) {
                    aeij = new AEIJ(a, e, j, i);
                    x = x_aeij.get(aeij);
                    cnst.addTerm(-1, x);
                }
                cplex.addEq(cnst, 0, String.format("FC(%d,%d,%s)", a, e, i));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void def_AT_cnsts_aeGiven(Parameter prmt, int a, int e,
                                            IloCplex cplex,
                                            HashMap<AEIJ, IloNumVar> x_aeij,
                                            HashMap<AEI, IloNumVar> mu_aei) {
        IloNumVar x, mu;
        IloLinearNumExpr cnst;
        IJ ij;
        AEI aei;
        AEIJ aeij;
        //
        AE ae = new AE(a, e);
        ArrayList<String> aeS = prmt.S_ae.get(ae);
        ArrayList<String> aeN = prmt.N_ae.get(ae);
        // Arrival time calculation
        try {
            // Time Window
            for (String i: aeN) {
                aei = new AEI(a, e, i);
                mu = mu_aei.get(aei);
                cplex.addLe(prmt.al_i.get(i), mu, String.format("TW_L(%d,%d,%s)", a, e, i));
                cplex.addLe(mu, prmt.be_i.get(i), String.format("TW_U(%d,%d,%s)", a, e, i));
            }
            // Warehouse and Delivery Sequence
            for (int i: prmt.K) {
                cplex.addLe(mu_aei.get(new AEI(a, e, prmt.h_k.get(i))),
                        mu_aei.get(new AEI(a, e, prmt.n_k.get(i))),
                        String.format("WD_S(%d,%d,%d)", a, e, i));
            }
            // Routine route preservation
            for (String i: aeS) {
                for (String j: aeS) {
                    aeij = new AEIJ(a, e, i, j);
                    cnst = cplex.linearNumExpr();
                    cnst.addTerm(prmt.c_aeij.get(aeij), mu_aei.get(new AEI(a, e, i)));
                    cnst.addTerm(-1, mu_aei.get(new AEI(a, e, j)));
                    cplex.addLe(cnst, 0, String.format("RR_P(%d,%d,%s,%s)", a, e, i, j));
                }
            }
            // Arrival time calculation
            for (String i: aeN) {
                for (String j: aeN) {
                    ij = new IJ(i, j);
                    aeij = new AEIJ(a, e, i, j);
                    cnst = cplex.linearNumExpr();
                    cnst.addTerm(1, mu_aei.get(new AEI(a, e, i)));
                    cnst.addTerm(prmt.M, x_aeij.get(aeij));
                    cnst.addTerm(-1, mu_aei.get(new AEI(a, e, j)));
                    cplex.addLe(cnst,
                            prmt.M - prmt.ga_i.get(i) - prmt.t_ij.get(ij),
                            String.format("AT(%d,%d,%s,%s)", a, e, i, j));
                }
            }
            // Detour Limit
            cnst = cplex.linearNumExpr();
            for (String i: aeN) {
                for (String j: aeN) {
                    x = x_aeij.get(new AEIJ(a, e, i, j));
                    ij = new IJ(i,j);
                    cnst.addTerm(prmt.t_ij.get(ij), x);
                }
            }
            cplex.addLe(cnst, prmt.l_ae.get(ae) + prmt.u_ae.get(ae),
                        String.format("DL(%d,%d)", a, e));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
