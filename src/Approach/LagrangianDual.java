package Approach;

import Index.ki;
import Other.Etc;
import Other.Parameter;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloQuadNumExpr;
import ilog.cplex.IloCplex;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class LagrangianDual {
    Parameter prmt;
    Etc etc;
    //
    IloCplex cplex;
    HashMap<ki, IloNumVar> y_ki;
    HashMap<Index.kriT, IloNumVar> z_kriT;
    HashMap<Index.kriN, IloNumVar> a_kriN;
    HashMap<Index.krij, IloNumVar> x_krij;
    HashMap<Index.kriT, IloNumVar> lm_kriT;
    IloNumVar l;

    public LagrangianDual(Parameter _prmt, Etc _etc) {
        prmt = _prmt;
        etc = _etc;
        //
        y_ki = new HashMap<>();
        z_kriT = new HashMap<>();
        a_kriN = new HashMap<>();
        x_krij = new HashMap<>();
        lm_kriT = new HashMap<>();
    }
    public void buildModel() {
        try {
            String iP;
            double w, gamma;
            ArrayList<Integer> R;
            ArrayList<String> krN;
            IloNumVar y, z, x, lm;
            IloLinearNumExpr cnst;
            IloQuadNumExpr cnstQ;
            Index.ki ki;
            Index.kr kr;
            Index.kriT kriT;
            Index.krij krij;
            //
            cplex = new IloCplex();
            ModelBuilder.def_dvs_yzax(prmt, cplex, y_ki, z_kriT, a_kriN, x_krij);
            for (int k : prmt.K) {
                R = prmt.R_k.get(k);
                for (int r : R) {
                    for (int i : prmt.T) {
                        lm_kriT.put(new Index.kriT(k, r, i), cplex.numVar(0.0, Double.MAX_VALUE, String.format("lm(%d,%d,%d)", k, r, i)));
                    }
                }
            }
            l = cplex.numVar(0.0, Double.MAX_VALUE, "l");
            //
            cplex.addMaximize(l);
            //
            cnst = cplex.linearNumExpr();
            for (int i : prmt.T) {
                for (int k : prmt.K) {
                    ki = new Index.ki(k, i);
                    w = prmt.w_i.get(i);
                    y = y_ki.get(ki);
                    cnst.addTerm(w, y);
                    R = prmt.R_k.get(k);
                    for (int r : R) {
                        kr = new Index.kr(k, r);
                        kriT = new Index.kriT(k, r, i);
                        gamma = prmt.r_kr.get(kr);
                        z = z_kriT.get(kriT);
                        cnst.addTerm(-(w * gamma), z);
                    }
                }
            }
            cnstQ = cplex.quadNumExpr();
            for (int i : prmt.T) {
                iP = String.format("p%d", i);
                for (int k : prmt.K) {
                    ki = new Index.ki(k, i);
                    y = y_ki.get(ki);
                    R = prmt.R_k.get(k);
                    for (int r : R) {
                        kr = new Index.kr(k, r);
                        kriT = new Index.kriT(k, r, i);
                        krN = prmt.N_kr.get(kr);
                        lm = lm_kriT.get(kriT);
                        for (String j: krN) {
                            krij = new Index.krij(k, r, iP, j);
                            x = x_krij.get(krij);
                            cnstQ.addTerm(1.0, lm, x);
                        }
                        z = z_kriT.get(kriT);
                        cnstQ.addTerm(1.0, lm, z);
                        cnstQ.addTerm(-1.0, lm, y);
                    }
                }
            }
            cplex.addGe(l, cplex.sum(cnst, cnstQ), "epigraphForm");
            //
            ModelBuilder.def_TAA_cnsts(prmt, cplex, y_ki, z_kriT); //Q1
            ModelBuilder.def_Routing_cnsts(prmt, cplex, a_kriN, x_krij); //Q2
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void solveModel() {
        try {
            cplex.exportModel("LD.lp");
            cplex.setOut(new FileOutputStream(etc.logPath.toFile()));
            cplex.solve();
            System.out.println("hi2");
            cplex.end();
        } catch (Exception ex) {
        ex.printStackTrace();
        }
    }
}
