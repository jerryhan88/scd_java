package Approach;

import Index.*;
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
    HashMap<AK, IloNumVar> y_ae;
    HashMap<AEK, IloNumVar> z_aek;
    HashMap<AEIJ, IloNumVar> x_aeij;
    HashMap<AEI, IloNumVar> mu_aei;

    public ILP(Parameter _prmt, Etc _etc) {
        prmt = _prmt;
        etc = _etc;
        //
        y_ae = new HashMap<>();
        z_aek = new HashMap<>();
        mu_aei = new HashMap<>();
        x_aeij = new HashMap<>();
    }

    public void buildModel() {
        try {
            double r, p;
            ArrayList<Integer> aE;
            ArrayList<String> aeN;
            IloNumVar y, z, x;
            IloLinearNumExpr obj, cnst;
            AK ak;
            AE ae;
            AEK aek;
            AEIJ aeij;
            //
            cplex = new IloCplex();
            ModelBuilder.def_dvs_yzax(prmt, cplex, y_ae, z_aek, x_aeij, mu_aei);
            //
            obj = cplex.linearNumExpr();
            for (int k : prmt.K) {
                for (int a : prmt.A) {
                    ak = new AK(a, k);
                    r = prmt.r_k.get(k);
                    y = y_ae.get(ak);
                    obj.addTerm(r, y);
                    aE = prmt.E_a.get(a);
                    for (int e : aE) {
                        ae = new AE(a, e);
                        aek = new AEK(a, e, k);
                        p = prmt.p_ae.get(ae);
                        z = z_aek.get(aek);
                        obj.addTerm(-(r * p), z);
                    }
                }
            }
            cplex.addMaximize(obj);
            //
            ModelBuilder.def_TAA_cnsts(prmt, cplex, y_ae, z_aek); //Q1
            ModelBuilder.def_Routing_cnsts(prmt, cplex, x_aeij, mu_aei); //Q2
            // Complicated and Combined constraints Q3
            for (int a : prmt.A) {
                aE = prmt.E_a.get(a);
                for (int e : aE) {
                    ae = new AE(a, e);
                    aeN = prmt.N_ae.get(ae);
                    for (int k: prmt.K) {
                        ak = new AK(a, k);
                        aek = new AEK(a, e, k);
                        cnst = cplex.linearNumExpr();
                        y = y_ae.get(ak);
                        cnst.addTerm(1, y);
                        for (String j: aeN) {
                            aeij = new AEIJ(a, e, j, prmt.n_k.get(k));
                            x = x_aeij.get(aeij);
                            cnst.addTerm(-1, x);
                        }
                        z = z_aek.get(aek);
                        cplex.addLe(cnst, z, String.format("CC(%d,%d,%d)", a, e, k));
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
                for (AK key: y_ae.keySet()) {
                    sol.y_ak.put(key, cplex.getValue(y_ae.get(key)));
                }
                for (AEK key: z_aek.keySet()) {
                    sol.z_aek.put(key, cplex.getValue(z_aek.get(key)));
                }
                for (AEIJ key: x_aeij.keySet()) {
                    sol.x_aeij.put(key, cplex.getValue(x_aeij.get(key)));
                }
                for (AEI key: mu_aei.keySet()) {
                    sol.mu_aei.put(key, cplex.getValue(mu_aei.get(key)));
                }
                sol.saveSolJSN(etc.solPathJSN);
                sol.saveSolCSV(etc.solPathCSV);
                sol.saveSolTXT(etc.solPathTXT);
//                sol.saveSolSER(etc.solPathSER);
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
