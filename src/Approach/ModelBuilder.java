package Approach;

import Index.ki;
import Other.Parameter;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class ModelBuilder {

    static void def_dvs_yzax(Parameter prmt, IloCplex cplex,
                             HashMap<ki, IloNumVar> y_ki,
                             HashMap<Index.kriT, IloNumVar> z_kriT,
                             HashMap<Index.kriN, IloNumVar> a_kriN,
                             HashMap<Index.krij, IloNumVar> x_krij){
        ArrayList<Integer> R;
        ArrayList<String> krN;
        try {
            for (int k : prmt.K) {
                for (int i : prmt.T) {
                    y_ki.put(new Index.ki(k, i), cplex.boolVar(String.format("y(%d,%d)", k, i)));
                }
                R = prmt.R_k.get(k);
                for (int r : R) {
                    for (int i : prmt.T) {
                        z_kriT.put(new Index.kriT(k, r, i), cplex.boolVar(String.format("z(%d,%d,%d)", k, r, i)));
                    }
                    krN = prmt.N_kr.get(new Index.kr(k, r));
                    for (String i: krN) {
                        for (String j: krN) {
                            x_krij.put(new Index.krij(k, r, i, j), cplex.boolVar(String.format("x(%d,%d,%s,%s)", k, r, i, j)));
                        }
                        a_kriN.put(new Index.kriN(k, r, i), cplex.numVar(0.0, Double.MAX_VALUE, String.format("a(%d,%d,%s)", k, r, i)));
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    static void def_TAA_cnsts(Parameter prmt, IloCplex cplex,
                              HashMap<ki, IloNumVar> y_ki,
                              HashMap<Index.kriT, IloNumVar> z_kriT) {
        ArrayList<Integer> R;
        IloNumVar y, z;
        IloLinearNumExpr cnst;
        Index.ki ki;
        Index.kriT kriT;
        // Task assignment and accomplishment (Q1)
        try {
            for (int i : prmt.T) {
                cnst = cplex.linearNumExpr();
                for (int k : prmt.K) {
                    ki = new Index.ki(k, i);
                    y = y_ki.get(ki);
                    cnst.addTerm(1, y);
                }
                cplex.addLe(cnst, 1, String.format("TA(%d)", i));
            }
            //
            for (int i : prmt.T) {
                for (int k : prmt.K) {
                    ki = new Index.ki(k, i);
                    y = y_ki.get(ki);
                    R = prmt.R_k.get(k);
                    for (int r : R) {
                        kriT = new Index.kriT(k, r, i);
                        z = z_kriT.get(kriT);
                        cplex.addLe(z, y, String.format("TC(%d,%d,%d)", i, k, r));
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    static void def_AT_cnsts_krGiven(Parameter prmt, int k, int r,
                                     IloCplex cplex,
                                     HashMap<Index.kriN, IloNumVar> a_kriN,
                                     HashMap<Index.krij, IloNumVar> x_krij) {
        String krP, krM;
        String iP, iM;
        ArrayList<String> krC;
        ArrayList<String> krN;
        IloNumVar x, a;
        IloLinearNumExpr cnst;
        Index.kr kr;
        Index.ij ij;
        Index.kriN kriN;
        Index.krij krij;
        // Arrival time calculation
        try {
            double M = prmt.N.size() * Collections.max(prmt.t_ij.values());
            kr = new Index.kr(k, r);
            krN = prmt.N_kr.get(kr);
            krC = prmt.C_kr.get(kr);
            krP = String.format("o_%d_%d", k, r);
            krM = String.format("d_%d_%d", k, r);
            // Initiate arrival time
            a = a_kriN.get(new Index.kriN(k, r, krP));
            cplex.addEq(a, 0, String.format("iAT(%d,%d)", k, r));
            // Arrival time calculation
            for (String i: krN) {
                for (String j: krN) {
                    ij = new Index.ij(i, j);
                    krij = new Index.krij(k, r, i, j);
                    cnst = cplex.linearNumExpr();
                    cnst.addTerm(1, a_kriN.get(new Index.kriN(k, r, i)));
                    cnst.addTerm(M, x_krij.get(krij));
                    cnst.addTerm(-1, a_kriN.get(new Index.kriN(k, r, j)));
                    cplex.addLe(cnst,
                            M - prmt.c_i.get(i) - prmt.t_ij.get(ij),
                            String.format("AT(%d,%d,%s,%s)", k, r, i, j));
                }
            }
            // Time Window
            for (String i: prmt.N) {
                kriN = new Index.kriN(k, r, i);
                a = a_kriN.get(kriN);
                cplex.addLe(prmt.alpha_i.get(i), a, String.format("TW_L(%d,%d,%s)", k, r, i));
                cplex.addLe(a, prmt.beta_i.get(i), String.format("TW_U(%d,%d,%s)", k, r, i));
            }
            // Pickup and Delivery Sequence
            for (int i: prmt.T) {
                iP = String.format("p%d", i);
                iM = String.format("d%d", i);
                cplex.addLe(a_kriN.get(new Index.kriN(k, r, iP)),
                        a_kriN.get(new Index.kriN(k, r, iM)),
                        String.format("PD_S(%d,%d,%d)", k, r, i));
            }
            // Routine route preservation
            for (String i: krC) {
                for (String j: krC) {
                    krij = new Index.krij(k, r, i, j);
                    cnst = cplex.linearNumExpr();
                    cnst.addTerm(prmt.p_krij.get(krij), a_kriN.get(new Index.kriN(k, r, i)));
                    cnst.addTerm(-1, a_kriN.get(new Index.kriN(k, r, j)));
                    cplex.addLe(cnst, 0, String.format("RR_P(%d,%d,%s,%s)", k, r, i, j));
                }
            }
            // Detour Limit
            cnst = cplex.linearNumExpr();
            for (String i: krN) {
                for (String j: krN) {
                    x = x_krij.get(new Index.krij(k, r, i, j));
                    ij = new Index.ij(i,j);
                    cnst.addTerm(prmt.t_ij.get(ij), x);
                }
            }
            cplex.addLe(cnst, prmt.l_kr.get(kr) + prmt.u_kr.get(kr),
                        String.format("DL(%d,%d)", k, r));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    static void def_FC_cnsts_krGiven(Parameter prmt, int k, int r,
                                     IloCplex cplex,
                                     HashMap<Index.krij, IloNumVar> x_krij) {
        String krP, krM;
        String iP, iM;
        ArrayList<String> C;
        ArrayList<String> krN;
        IloNumVar x;
        IloLinearNumExpr cnst;
        Index.kr kr;
        Index.krij krij;
        // Flow conservation given agent k and routine route r
        try {
            kr = new Index.kr(k, r);
            krN = prmt.N_kr.get(kr);
            krP = String.format("o_%d_%d", k, r);
            krM = String.format("d_%d_%d", k, r);
            C = prmt.C_kr.get(kr);
            // Initiate flow
            cnst = cplex.linearNumExpr();
            for (String j: krN) {
                krij = new Index.krij(k, r, krP, j);
                x = x_krij.get(krij);
                cnst.addTerm(1, x);
            }
            cplex.addEq(cnst, 1, String.format("iFO(%d,%d)", k, r));
            cnst = cplex.linearNumExpr();
            for (String j: krN) {
                krij = new Index.krij(k, r, j, krM);
                x = x_krij.get(krij);
                cnst.addTerm(1, x);
            }
            cplex.addEq(cnst, 1, String.format("iFD(%d,%d)", k, r));
            for (String i: C) {
                if (i.equals(krP) || i.equals(krM)) continue;
                cnst = cplex.linearNumExpr();
                for (String j: krN) {
                    if (j.equals(i)) continue;
                    krij = new Index.krij(k, r, i, j);
                    x = x_krij.get(krij);
                    cnst.addTerm(1, x);
                }
                cplex.addEq(cnst, 1, String.format("iFS1(%d,%d,%s)", k, r, i));
                cnst = cplex.linearNumExpr();
                for (String j: krN) {
                    if (j.equals(i)) continue;
                    krij = new Index.krij(k, r, j, i);
                    x = x_krij.get(krij);
                    cnst.addTerm(1, x);
                }
                cplex.addEq(cnst, 1, String.format("iFS2(%d,%d,%s)", k, r, i));
            }
            // No flow
            cnst = cplex.linearNumExpr();
            for (String j: krN) {
                krij = new Index.krij(k, r, j, krP);
                x = x_krij.get(krij);
                cnst.addTerm(1, x);
            }
            cplex.addEq(cnst, 0, String.format("xFO(%d,%d)", k, r));
            cnst = cplex.linearNumExpr();
            for (String j: krN) {
                krij = new Index.krij(k, r, krM, j);
                x = x_krij.get(krij);
                cnst.addTerm(1, x);
            }
            cplex.addEq(cnst, 0, String.format("xFD(%d,%d)", k, r));
            // Flow conservation related to a task
            for (int i: prmt.T) {
                iP = String.format("p%d", i);
                iM = String.format("d%d", i);
                //
                cnst = cplex.linearNumExpr();
                for (String j: krN) {
                    krij = new Index.krij(k, r, iP, j);
                    x = x_krij.get(krij);
                    cnst.addTerm(1, x);
                }
                for (String j: krN) {
                    krij = new Index.krij(k, r, j, iM);
                    x = x_krij.get(krij);
                    cnst.addTerm(-1, x);
                }
                cplex.addEq(cnst, 0, String.format("tFC(%d,%d,%d)", k, r, i));
            }
            // Flow conservation
            for (String i: prmt.N) {
                cnst = cplex.linearNumExpr();
                for (String j: krN) {
                    krij = new Index.krij(k, r, i, j);
                    x = x_krij.get(krij);
                    cnst.addTerm(1, x);
                }
                for (String j: krN) {
                    krij = new Index.krij(k, r, j, i);
                    x = x_krij.get(krij);
                    cnst.addTerm(-1, x);
                }
                cplex.addEq(cnst, 0, String.format("FC(%d,%d,%s)", k, r, i));
            }
        } catch (Exception ex) {
        ex.printStackTrace();
        }
    }

    static void def_Routing_cnsts(Parameter prmt, IloCplex cplex,
                                  HashMap<Index.kriN, IloNumVar> a_kriN,
                                  HashMap<Index.krij, IloNumVar> x_krij) {

        ArrayList<Integer> R;
        // Routing (Q2)
        try {
            for (int k : prmt.K) {
                R = prmt.R_k.get(k);
                for (int r : R) {
                    def_FC_cnsts_krGiven(prmt, k, r, cplex, x_krij);
                    def_AT_cnsts_krGiven(prmt, k, r, cplex, a_kriN, x_krij);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
