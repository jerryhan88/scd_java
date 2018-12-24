package Approach;

import Index.*;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.io.FileOutputStream;
import java.util.*;

import Other.Etc;
import Other.Parameter;
import Other.Solution;

public class ILP extends ApproachSupClass {
    static class TimeLimitCallback extends IloCplex.MIPInfoCallback {
        Parameter prmt;
        Etc etc;
        //
        HashMap<AK, IloNumVar> y_ak;
        HashMap<AEK, IloNumVar> z_aek;
        HashMap<AEIJ, IloNumVar> x_aeij;
        HashMap<AEI, IloNumVar> mu_aei;

        TimeLimitCallback(Parameter prmt, Etc etc,
                          HashMap<AK, IloNumVar> y_ak,
                          HashMap<AEK, IloNumVar> z_aek,
                          HashMap<AEIJ, IloNumVar> x_aeij,
                          HashMap<AEI, IloNumVar> mu_aei) {
            this.prmt = prmt;
            this.etc = etc;
            //
            this.y_ak = y_ak;
            this.z_aek = z_aek;
            this.x_aeij = x_aeij;
            this.mu_aei = mu_aei;
        }

        public void main() throws IloException {
            if (etc.getWallTime() > etc.getTimeLimit()) {
                Solution sol = new Solution();
                sol.prmt = prmt;
                sol.cpuT = etc.getCpuTime();
                sol.wallT = etc.getWallTime();
                if (hasIncumbent()) {
                    sol.objV = getIncumbentObjValue();
                    sol.dualG = getMIPRelativeGap();
                    //
                    ArrayList aE;
                    ArrayList aeN;
                    AK ak;
                    AEK aek;
                    AEIJ aeij;
                    AEI aei;
                    for (int a : prmt.A) {
                        for (int k : prmt.K) {
                            ak = new AK(a, k);
                            sol.y_ak.put(ak, getIncumbentValue(y_ak.get(ak)));
                        }
                        aE = prmt.E_a.get(a);
                        for (Object e : aE) {
                            for (int k : prmt.K) {
                                aek = new AEK(a, e, k);
                                sol.z_aek.put(aek, getIncumbentValue(z_aek.get(aek)));
                            }
                            aeN = prmt.N_ae.get(new AE(a, e));
                            for (Object i: aeN) {
                                for (Object j: aeN) {
                                    aeij = new AEIJ(a, e, i, j);
                                    sol.x_aeij.put(aeij, getIncumbentValue(x_aeij.get(aeij)));
                                }
                                aei = new AEI(a, e, i);
                                sol.mu_aei.put(aei, getIncumbentValue(mu_aei.get(aei)));
                            }
                        }
                    }
                    sol.saveSolCSV(etc.solPathCSV);
                    sol.saveSolTXT(etc.solPathTXT);
                } else {
                    sol.objV = -1.0;
                    sol.dualG = -1.0;
                    //
                    sol.saveSolCSV(etc.solPathCSV);
                }
                abort();
            }
        }
    }
    //
    private IloCplex cplex;
    private HashMap<AK, IloNumVar> y_ak;
    private HashMap<AEK, IloNumVar> z_aek;
    private HashMap<AEIJ, IloNumVar> x_aeij;
    private HashMap<AEI, IloNumVar> mu_aei;

    public ILP(Parameter prmt, Etc etc) {
        super(prmt, etc);
        //
        y_ak = new HashMap<>();
        z_aek = new HashMap<>();
        mu_aei = new HashMap<>();
        x_aeij = new HashMap<>();
    }

    private void buildModel() {
        try {
            double r, p;
            ArrayList aE;
            ArrayList aeN;
            IloNumVar y, z, x;
            IloLinearNumExpr obj, cnst;
            AK ak;
            AE ae;
            AEK aek;
            AEIJ aeij;
            //
            cplex = new IloCplex();
            ModelBuilder.def_dvs_yzax(prmt, cplex, y_ak, z_aek, x_aeij, mu_aei);
            //
            obj = cplex.linearNumExpr();
            for (int k : prmt.K) {
                for (int a : prmt.A) {
                    ak = new AK(a, k);
                    r = prmt.r_k.get(k);
                    y = y_ak.get(ak);
                    obj.addTerm(r, y);
                    aE = prmt.E_a.get(a);
                    for (Object e : aE) {
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
            ModelBuilder.def_TAA_cnsts(prmt, cplex, y_ak, z_aek); //Q1
            ModelBuilder.def_Routing_cnsts(prmt, cplex, x_aeij, mu_aei); //Q2
            // Complicated and Combined constraints Q3
            for (int a : prmt.A) {
                aE = prmt.E_a.get(a);
                for (Object e : aE) {
                    ae = new AE(a, e);
                    aeN = prmt.N_ae.get(ae);
                    for (int k: prmt.K) {
                        ak = new AK(a, k);
                        aek = new AEK(a, e, k);
                        cnst = cplex.linearNumExpr();
                        y = y_ak.get(ak);
                        cnst.addTerm(1, y);
                        for (Object j: aeN) {
                            aeij = new AEIJ(a, e, j, prmt.n_k.get(k));
                            x = x_aeij.get(aeij);
                            cnst.addTerm(-1, x);
                        }
                        z = z_aek.get(aek);
                        cplex.addLe(cnst, z, String.format("CC(%s)", aek.get_label()));
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void solveModel() {
        try {
            cplex.setOut(new FileOutputStream(etc.logPath.toFile()));
            cplex.use(new TimeLimitCallback(prmt, etc,
                    y_ak, z_aek, x_aeij, mu_aei));
            cplex.solve();
            if (cplex.getStatus() == IloCplex.Status.Optimal) {
                Solution sol = new Solution();
                sol.prmt = prmt;
                sol.objV = cplex.getObjValue();
                sol.dualG = cplex.getMIPRelativeGap();
                sol.cpuT = etc.getCpuTime();
                sol.wallT = etc.getWallTime();
                for (AK key: y_ak.keySet()) {
                    sol.y_ak.put(key, cplex.getValue(y_ak.get(key)));
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
                sol.saveSolCSV(etc.solPathCSV);
                sol.saveSolTXT(etc.solPathTXT);
//                sol.saveSolJSN(etc.solPathJSN);
//                sol.saveSolSER(etc.solPathSER);
            } else if (cplex.getStatus() == IloCplex.Status.InfeasibleOrUnbounded) {
                cplex.exportModel(String.format("%s.lp", prmt.problemName));
            }
            else {
                cplex.output().println("Other.Solution status = " + cplex.getStatus());
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
