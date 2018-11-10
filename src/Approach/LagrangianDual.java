package Approach;

import Index.*;
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
    HashMap<AK, IloNumVar> y_ki;
    HashMap<AEK, IloNumVar> z_kriT;
    HashMap<AEI, IloNumVar> a_kriN;
    HashMap<AEIJ, IloNumVar> x_krij;
    HashMap<AEK, IloNumVar> lm_kriT;
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
            AK AK;
            AE kr;
            AEK AEK;
            AEIJ AEIJ;
            //
            cplex = new IloCplex();
            ModelBuilder.def_dvs_yzax(prmt, cplex, y_ki, z_kriT, x_krij, a_kriN);
            for (int k : prmt.A) {
                R = prmt.E_a.get(k);
                for (int r : R) {
                    for (int i : prmt.K) {
                        lm_kriT.put(new AEK(k, r, i), cplex.numVar(0.0, Double.MAX_VALUE, String.format("lm(%d,%d,%d)", k, r, i)));
                    }
                }
            }
            l = cplex.numVar(0.0, Double.MAX_VALUE, "l");
            //
            cplex.addMaximize(l);
            //
            cnst = cplex.linearNumExpr();
            for (int i : prmt.K) {
                for (int k : prmt.A) {
                    AK = new AK(k, i);
                    w = prmt.r_k.get(i);
                    y = y_ki.get(AK);
                    cnst.addTerm(w, y);
                    R = prmt.E_a.get(k);
                    for (int r : R) {
                        kr = new AE(k, r);
                        AEK = new AEK(k, r, i);
                        gamma = prmt.p_ae.get(kr);
                        z = z_kriT.get(AEK);
                        cnst.addTerm(-(w * gamma), z);
                    }
                }
            }
            cnstQ = cplex.quadNumExpr();
            for (int i : prmt.K) {
                iP = String.format("p%d", i);
                for (int k : prmt.A) {
                    AK = new AK(k, i);
                    y = y_ki.get(AK);
                    R = prmt.E_a.get(k);
                    for (int r : R) {
                        kr = new AE(k, r);
                        AEK = new AEK(k, r, i);
                        krN = prmt.N_ae.get(kr);
                        lm = lm_kriT.get(AEK);
                        for (String j: krN) {
                            AEIJ = new AEIJ(k, r, iP, j);
                            x = x_krij.get(AEIJ);
                            cnstQ.addTerm(1.0, lm, x);
                        }
                        z = z_kriT.get(AEK);
                        cnstQ.addTerm(1.0, lm, z);
                        cnstQ.addTerm(-1.0, lm, y);
                    }
                }
            }
            cplex.addGe(l, cplex.sum(cnst, cnstQ), "epigraphForm");
            //
            ModelBuilder.def_TAA_cnsts(prmt, cplex, y_ki, z_kriT); //Q1
            ModelBuilder.def_Routing_cnsts(prmt, cplex, x_krij, a_kriN); //Q2
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
