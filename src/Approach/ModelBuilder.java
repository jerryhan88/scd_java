package Approach;

import Index.*;
import Other.Parameter;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.HashMap;

public class ModelBuilder {

    static void def_dvs_yzxm(Parameter prmt, IloCplex cplex,
                             HashMap<AK, IloNumVar> y_ak,
                             HashMap<AEK, IloNumVar> z_aek,
                             HashMap<AEIJ, IloNumVar> x_aeij,
                             HashMap<AEI, IloNumVar> mu_aei){
        AK ak;
        AEK aek;
        AEIJ aeij;
        AEI aei;
        ArrayList aE;
        ArrayList aeN;
        try {
            for (int a : prmt.A) {
                for (int k : prmt.K) {
                    ak = prmt.get_AK(a, k);
                    y_ak.put(ak, cplex.boolVar(String.format("y(%s)", ak.get_label())));
                }
                aE = prmt.E_a.get(a);
                for (Object e : aE) {
                    for (int k : prmt.K) {
                        aek = prmt.get_AEK(a, e, k);
                        z_aek.put(aek, cplex.boolVar(String.format("z(%s)", aek.get_label())));
                    }
                    aeN = prmt.N_ae.get(prmt.get_AE(a, e));
                    for (Object i: aeN) {
                        for (Object j: aeN) {
                            aeij = prmt.get_AEIJ(a, e, i, j);
                            x_aeij.put(aeij, cplex.boolVar(String.format("x(%s)", aeij.get_label())));
                        }
                        aei = prmt.get_AEI(a, e, i);
                        mu_aei.put(aei, cplex.numVar(0.0, Double.MAX_VALUE, String.format("mu(%s)", aei.get_label())));
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    static void def_dvs_yzxm_LR(Parameter prmt, IloCplex cplex,
                             HashMap<AK, IloNumVar> y_ak,
                             HashMap<AEK, IloNumVar> z_aek,
                             HashMap<AEIJ, IloNumVar> x_aeij,
                             HashMap<AEI, IloNumVar> mu_aei){
        AK ak;
        AEK aek;
        AEIJ aeij;
        AEI aei;
        ArrayList aE;
        ArrayList aeN;
        try {
            for (int a : prmt.A) {
                for (int k : prmt.K) {
                    ak = prmt.get_AK(a, k);
                    y_ak.put(ak, cplex.numVar(0.0, 1.0, String.format("y(%s)", ak.get_label())));
                }
                aE = prmt.E_a.get(a);
                for (Object e : aE) {
                    for (int k : prmt.K) {
                        aek = prmt.get_AEK(a, e, k);
                        z_aek.put(aek, cplex.numVar(0.0, 1.0, String.format("z(%s)", aek.get_label())));
                    }
                    aeN = prmt.N_ae.get(prmt.get_AE(a, e));
                    for (Object i: aeN) {
                        for (Object j: aeN) {
                            aeij = prmt.get_AEIJ(a, e, i, j);
                            x_aeij.put(aeij, cplex.numVar(0.0, 1.0, String.format("x(%s)", aeij.get_label())));

                        }
                        aei = prmt.get_AEI(a, e, i);
                        mu_aei.put(aei, cplex.numVar(0.0, Double.MAX_VALUE, String.format("mu(%s)", aei.get_label())));
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
        ArrayList aE;
        IloNumVar y, z;
        IloLinearNumExpr cnst;
        AK ak;
        AEK aek;
        // Task assignment and accomplishment (Q1)
        try {
            for (int k : prmt.K) {
                cnst = cplex.linearNumExpr();
                for (int a : prmt.A) {
                    ak = prmt.get_AK(a, k);
                    y = y_ak.get(ak);
                    cnst.addTerm(1, y);
                }
                cplex.addLe(cnst, 1, String.format("TA(%d)", k));
            }
            for (int a : prmt.A) {
                cnst = cplex.linearNumExpr();
                for (int k : prmt.K) {
                    ak = prmt.get_AK(a, k);
                    y = y_ak.get(ak);

                    cnst.addTerm(prmt.v_k.get(k), y);
                }
                cplex.addLe(cnst, prmt.v_a.get(a), String.format("V(%d)", a));
            }
            for (int a : prmt.A) {
                cnst = cplex.linearNumExpr();
                for (int k : prmt.K) {
                    ak = prmt.get_AK(a, k);
                    y = y_ak.get(ak);
                    cnst.addTerm(prmt.w_k.get(k), y);
                }
                cplex.addLe(cnst, prmt.w_a.get(a), String.format("W(%d)", a));
            }
            //
            for (int k : prmt.K) {
                for (int a : prmt.A) {
                    ak = prmt.get_AK(a, k);
                    y = y_ak.get(ak);
                    aE = prmt.E_a.get(a);
                    for (Object e : aE) {
                        aek = prmt.get_AEK(a, e, k);
                        z = z_aek.get(aek);
                        cplex.addLe(z, y, String.format("TC(%s)", aek.get_label()));
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

        ArrayList aE;
        // Routing (Q2)
        try {
            for (int a : prmt.A) {
                aE = prmt.E_a.get(a);
                for (Object e : aE) {
                    def_FC_cnsts_aeGiven(prmt, a, (Integer) e, cplex, x_aeij);
                    def_AT_cnsts_aeGiven(prmt, a, (Integer) e, cplex, x_aeij, mu_aei);
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
        AEK aek;
        AEI aei;
        AEIJ aeij;
        //
        AE ae = prmt.get_AE(a, e);
        ArrayList aeS = prmt.S_ae.get(ae);
        ArrayList aeN = prmt.N_ae.get(ae);
        ArrayList aeF = prmt.F_ae.get(ae);
        ArrayList ae_iF = prmt.iF_ae.get(ae);
        String o_ae = String.format("s0_%d_%d", a, e);
        String d_ae = String.format("s%d_%d_%d", aeS.size() - 1, a, e);
        // Flow conservation given agent a and routine route r
        try {
            // Initiate flow
            cnst = cplex.linearNumExpr();
            for (Object j: aeN) {
                aeij = prmt.get_AEIJ(a, e, o_ae, j);
                x = x_aeij.get(aeij);
                cnst.addTerm(1, x);
            }
            cplex.addEq(cnst, 1, String.format("iFO(%s)", ae.get_label()));
            cnst = cplex.linearNumExpr();
            for (Object j: aeN) {
                aeij = prmt.get_AEIJ(a, e, j, d_ae);
                x = x_aeij.get(aeij);
                cnst.addTerm(1, x);
            }
            cplex.addEq(cnst, 1, String.format("iFD(%s)", ae.get_label()));
            for (Object i: aeS) {
                if (i.equals(o_ae) || i.equals(d_ae)) continue;
                aei = prmt.get_AEI(a, e, i);
                cnst = cplex.linearNumExpr();
                for (Object j: aeN) {
                    if (j.equals(i)) continue;
                    aeij = prmt.get_AEIJ(a, e, i, j);
                    x = x_aeij.get(aeij);
                    cnst.addTerm(1, x);
                }
                cplex.addEq(cnst, 1, String.format("iFS1(%s)", aei.get_label()));
                cnst = cplex.linearNumExpr();
                for (Object j: aeN) {
                    if (j.equals(i)) continue;
                    aeij = prmt.get_AEIJ(a, e, j, i);
                    x = x_aeij.get(aeij);
                    cnst.addTerm(1, x);
                }
                cplex.addEq(cnst, 1, String.format("iFS2(%s)", aei.get_label()));
            }
            // No flow
            cnst = cplex.linearNumExpr();
            for (Object j: aeN) {
                aeij = prmt.get_AEIJ(a, e, j, o_ae);
                x = x_aeij.get(aeij);
                cnst.addTerm(1, x);
            }
            cplex.addEq(cnst, 0, String.format("xFO(%s)", ae.get_label()));
            cnst = cplex.linearNumExpr();
            for (Object j: aeN) {
                aeij = prmt.get_AEIJ(a, e, d_ae, j);
                x = x_aeij.get(aeij);
                cnst.addTerm(1, x);
            }
            cplex.addEq(cnst, 0, String.format("xFD(%s)", ae.get_label()));
            for (Object k: ae_iF) {
                aek = prmt.get_AEK(a, e, k);
                cnst = cplex.linearNumExpr();
                for (Object j: aeN) {
                    aeij = prmt.get_AEIJ(a, e, prmt.n_k.get((Integer) k), j);
                    x = x_aeij.get(aeij);
                    cnst.addTerm(1, x);
                }
                cplex.addEq(cnst, 0, String.format("xFN(%s)", aek.get_label()));
            }
            // Flow about delivery nodes; only when the warehouse visited
            for (Object k: aeF) {
                aek = prmt.get_AEK(a, e, k);
                cnst = cplex.linearNumExpr();
                for (Object j: aeN) {
                    aeij = prmt.get_AEIJ(a, e, prmt.n_k.get((Integer) k), j);
                    x = x_aeij.get(aeij);
                    cnst.addTerm(1, x);
                }
                for (Object j: aeN) {
                    aeij = prmt.get_AEIJ(a, e, j, prmt.h_k.get((Integer) k));
                    x = x_aeij.get(aeij);
                    cnst.addTerm(-1, x);
                }
                cplex.addLe(cnst, 0, String.format("tFC(%s)", aek.get_label()));
            }
            // Flow conservation
            for (String i: prmt.N) {
                aei = prmt.get_AEI(a, e, i);
                cnst = cplex.linearNumExpr();
                for (Object j: aeN) {
                    aeij = prmt.get_AEIJ(a, e, i, j);
                    x = x_aeij.get(aeij);
                    cnst.addTerm(1, x);
                }
                for (Object j: aeN) {
                    aeij = prmt.get_AEIJ(a, e, j, i);
                    x = x_aeij.get(aeij);
                    cnst.addTerm(-1, x);
                }
                cplex.addEq(cnst, 0, String.format("FC(%s)", aei.get_label()));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void def_AT_cnsts_aeGiven(Parameter prmt, int a, int e,
                                            IloCplex cplex,
                                            HashMap<AEIJ, IloNumVar> x_aeij,
                                            HashMap<AEI, IloNumVar> mu_aei) {
        IJ ij;
        AEK aek;
        AEI aei;
        AEIJ aeij;
        IloNumVar x, mu;
        IloLinearNumExpr cnst;
        //
        AE ae = prmt.get_AE(a, e);
        ArrayList aeS = prmt.S_ae.get(ae);
        ArrayList aeN = prmt.N_ae.get(ae);
        ArrayList aeF = prmt.F_ae.get(ae);
        // Arrival time calculation
        try {
            // Time Window
            for (Object i: aeN) {
                aei = prmt.get_AEI(a, e, i);
                mu = mu_aei.get(aei);
                cplex.addLe(prmt.al_i.get(i), mu, String.format("TW_L(%s)", aei.get_label()));
                cplex.addLe(mu, prmt.be_i.get(i), String.format("TW_U(%s)", aei.get_label()));
            }
            // Warehouse and Delivery Sequence
            for (Object k: aeF) {
                aek = prmt.get_AEK(a, e, k);
                cplex.addLe(mu_aei.get(prmt.get_AEI(a, e, prmt.h_k.get((Integer) k))),
                        mu_aei.get(prmt.get_AEI(a, e, prmt.n_k.get((Integer) k))),
                        String.format("WD_S(%s)", aek.get_label()));
            }
            // Routine route preservation
            for (Object i: aeS) {
                for (Object j: aeS) {
                    aeij = prmt.get_AEIJ(a, e, i, j);
                    cnst = cplex.linearNumExpr();
                    cnst.addTerm(prmt.c_aeij.get(aeij), mu_aei.get(prmt.get_AEI(a, e, i)));
                    cnst.addTerm(-1, mu_aei.get(prmt.get_AEI(a, e, j)));
                    cplex.addLe(cnst, 0, String.format("RR_P(%s)", aeij.get_label()));
                }
            }
            // Arrival time calculation
            for (Object i: aeN) {
                for (Object j: aeN) {
                    ij = prmt.get_IJ(i, j);
                    aeij = prmt.get_AEIJ(a, e, i, j);
                    cnst = cplex.linearNumExpr();
                    cnst.addTerm(1, mu_aei.get(prmt.get_AEI(a, e, i)));
                    cnst.addTerm(prmt.M, x_aeij.get(aeij));
                    cnst.addTerm(-1, mu_aei.get(prmt.get_AEI(a, e, j)));
                    cplex.addLe(cnst,
                            prmt.M - prmt.ga_i.get(i) - prmt.t_ij.get(ij),
                            String.format("AT(%s)", aeij.get_label()));
                }
            }
            // Detour Limit
            cnst = cplex.linearNumExpr();
            for (Object i: aeN) {
                for (Object j: aeN) {
                    x = x_aeij.get(prmt.get_AEIJ(a, e, i, j));
                    ij = prmt.get_IJ(i,j);
                    cnst.addTerm(prmt.t_ij.get(ij), x);
                }
            }
            cplex.addLe(cnst, prmt.l_ae.get(ae) + prmt.u_ae.get(ae),
                        String.format("DL(%s)", ae.get_label()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    static HashMap<String, IloRange> def_complicating_cnsts(Parameter prmt, IloCplex cplex,
                                                            HashMap<AK, IloNumVar> y_ak,
                                                            HashMap<AEK, IloNumVar> z_aek,
                                                            HashMap<AEIJ, IloNumVar> x_aeij) {
        ArrayList aE;
        ArrayList aeN;
        IloNumVar y, z, x;
        IloLinearNumExpr cnst;
        AK ak;
        AE ae;
        AEK aek;
        AEIJ aeij;
        String cnst_lable;
        // Complicated and Combined constraints Q3
        HashMap<String, IloRange> constraints = new HashMap<>();
        try {
            for (int a : prmt.A) {
                aE = prmt.E_a.get(a);
                for (Object e : aE) {
                    ae = prmt.get_AE(a, e);
                    aeN = prmt.N_ae.get(ae);
                    for (int k: prmt.K) {
                        ak = prmt.get_AK(a, k);
                        aek = prmt.get_AEK(a, e, k);
                        cnst = cplex.linearNumExpr();
                        y = y_ak.get(ak);
                        cnst.addTerm(1, y);
                        for (Object j: aeN) {
                            aeij = prmt.get_AEIJ(a, e, j, prmt.n_k.get(k));
                            x = x_aeij.get(aeij);
                            cnst.addTerm(-1, x);
                        }
                        z = z_aek.get(aek);
                        cnst_lable = String.format("CC(%s)", aek.get_label());
                        constraints.put(cnst_lable, (IloRange) cplex.addLe(cnst, z, cnst_lable));
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return constraints;
    }
}
