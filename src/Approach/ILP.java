package Approach;

import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.io.FileOutputStream;
import java.util.*;

import Other.Etc;
import Other.Parameter;
import Other.Solution;

public class ILP extends ApproachSupClass {
    Parameter prmt;
    Etc etc;
    IloCplex cplex;
    //
    HashMap<Index.ki, IloNumVar> y_ki;
    HashMap<Index.kriT, IloNumVar> z_kriT;
    HashMap<Index.kriN, IloNumVar> a_kriN;
    HashMap<Index.krij, IloNumVar> x_krij;

    public ILP(Parameter _prmt, Etc _etc) {
        prmt = _prmt;
        etc = _etc;
        //
        y_ki = new HashMap<>();
        z_kriT = new HashMap<>();
        a_kriN = new HashMap<>();
        x_krij = new HashMap<>();
    }

    public void buildModel() {
        try {
            double w, gamma;
            ArrayList<Integer> kR;
            ArrayList<String> krN;
            IloNumVar y, z, x;
            IloLinearNumExpr obj, cnst;
            Index.ki ki;
            Index.kr kr;
            Index.kriT kriT;
            Index.krij krij;
            //
            cplex = new IloCplex();
            ModelBuilder.def_dvs_yzax(prmt, cplex, y_ki, z_kriT, a_kriN, x_krij);
            //
            obj = cplex.linearNumExpr();
            for (int i : prmt.T) {
                for (int k : prmt.K) {
                    ki = new Index.ki(k, i);
                    w = prmt.w_i.get(i);
                    y = y_ki.get(ki);
                    obj.addTerm(w, y);
                    kR = prmt.R_k.get(k);
                    for (int r : kR) {
                        kr = new Index.kr(k, r);
                        kriT = new Index.kriT(k, r, i);
                        gamma = prmt.r_kr.get(kr);
                        z = z_kriT.get(kriT);
                        obj.addTerm(-(w * gamma), z);
                    }
                }
            }
            cplex.addMaximize(obj);
            //
            ModelBuilder.def_TAA_cnsts(prmt, cplex, y_ki, z_kriT); //Q1
            ModelBuilder.def_Routing_cnsts(prmt, cplex, a_kriN, x_krij); //Q2
            // Complicated and Combined constraints Q3
            for (int k : prmt.K) {
                kR = prmt.R_k.get(k);
                for (int r : kR) {
                    kr = new Index.kr(k, r);
                    krN = prmt.N_kr.get(kr);
                    for (int i: prmt.T) {
                        ki = new Index.ki(k, i);
                        kriT = new Index.kriT(k, r, i);
                        cnst = cplex.linearNumExpr();
                        y = y_ki.get(ki);
                        cnst.addTerm(1, y);
                        for (String j: krN) {
                            krij = new Index.krij(k, r, prmt.n_i.get(i), j);
                            x = x_krij.get(krij);
                            cnst.addTerm(-1, x);
                        }
                        z = z_kriT.get(kriT);
                        cplex.addLe(cnst, z, String.format("CC(%d,%d,%d)", k, r, i));
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void solveModel() {
        try {
            cplex.setOut(new FileOutputStream(etc.logPath.toFile()));
            cplex.solve();
            if (cplex.getStatus() == IloCplex.Status.Optimal) {
                Solution sol = new Solution();
                sol.prmt = prmt;
                sol.objV = cplex.getObjValue();
                sol.dualG = cplex.getMIPRelativeGap();
                sol.cpuT = etc.getCpuTime();
                sol.wallT = etc.getWallTime();
                for (Index.ki key: y_ki.keySet()) {
                    sol.y_ki.put(key, cplex.getValue(y_ki.get(key)));
                }
                for (Index.kriT key: z_kriT.keySet()) {
                    sol.z_kriT.put(key, cplex.getValue(z_kriT.get(key)));
                }
                for (Index.kriN key: a_kriN.keySet()) {
                    sol.a_kriN.put(key, cplex.getValue(a_kriN.get(key)));
                }
                for (Index.krij key: x_krij.keySet()) {
                    sol.x_krij.put(key, cplex.getValue(x_krij.get(key)));
                }
                sol.saveSolJSN(etc.solPathJSN);
                sol.saveSolSER(etc.solPathSER);
                sol.saveSolCSV(etc.solPathCSV);
                sol.saveSolTXT(etc.solPathTXT);
            } else {
                cplex.output().println("Other.Solution status = " + cplex.getStatus());
                cplex.exportModel(String.format("%s.lp", prmt.problemName));
            }
            cplex.end();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void run() {
        buildModel();
        solveModel();
    }
}
