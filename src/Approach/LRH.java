package Approach;

import Approach.Router.RouterBNB;
import Approach.Router.RouterGH;
import Approach.Router.RouterILP;
import Approach.Router.RouterSup;
import Index.*;
import Other.Etc;
import Other.Parameter;
import Other.Solution;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
//
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ForkJoinPool;


public class LRH extends ApproachSupClass {
    public static final int AVAILABLE_NUM_PROCESSORS = Runtime.getRuntime().availableProcessors();
    RouterSup router;
    //
    private IloCplex cplex;
    //
    int numIters;
    private int noUpdateCounter_L, LRH_NUM_ITER;
    private double at, ut, L_lm_star, F_star, F1;
    private double L_lm1;
    private double L1V, L2V;
    private Solution bestSol;
    private HashMap<AK, Double> y_ak;
    private HashMap<AEK, Double> z_aek;
    HashMap<AEIJ, Double> x_aeij;
    HashMap<AEI, Double> mu_aei;
    HashMap<AEK, Double> lm_aek;
    //
    private int NO_IMPROVEMENT_LIMIT;
    private double LRH_DUAL_GAP, STEP_DECREASE_RATE, SUB_DUAL_GAP_LIMIT;
    private double STEP_SIZE_LIMIT = 1e-4;
    private int NUM_LAMBDA = 0;
    //
    public LRH(Parameter prmt, Etc etc) {
        super(prmt, etc);
        y_ak = new HashMap<>();
        z_aek = new HashMap<>();
        x_aeij = new HashMap<>();
        mu_aei = new HashMap<>();
        lm_aek = new HashMap<>();
        //
        at = 0.0;
        ut = 2.0;
        L_lm_star = Double.MAX_VALUE;
        F_star = -Double.MAX_VALUE;
        numIters = 0;
        noUpdateCounter_L = 0;
        //
        init_lm_dualValue();
    }

    private void init_lm_dualValue() {
        try {
            double r, p;
            ArrayList aE;
            IloNumVar y, z;
            IloLinearNumExpr obj;
            AK ak;
            AE ae;
            AEK aek;
            cplex = new IloCplex();
            //
            HashMap<AK, IloNumVar> y_ak = new HashMap<>();
            HashMap<AEK, IloNumVar> z_aek = new HashMap<>();
            HashMap<AEIJ, IloNumVar> x_aeij = new HashMap<>();
            HashMap<AEI, IloNumVar> mu_aei = new HashMap<>();
            //
            ModelBuilder.def_dvs_yzxm_LR(prmt, cplex, y_ak, z_aek, x_aeij, mu_aei);
            //
            obj = cplex.linearNumExpr();
            for (int k : prmt.K) {
                for (int a : prmt.A) {
                    ak = prmt.get_AK(a, k);
                    r = prmt.r_k.get(k);
                    y = y_ak.get(ak);
                    obj.addTerm(r, y);
                    aE = prmt.E_a.get(a);
                    for (Object e : aE) {
                        ae = prmt.get_AE(a, e);
                        aek = prmt.get_AEK(a, e, k);
                        p = prmt.p_ae.get(ae);
                        z = z_aek.get(aek);
                        obj.addTerm(-(r * p), z);
                    }
                }
            }
            cplex.addMaximize(obj);
            //
            HashMap<String, IloRange> complicating_constraints;
            ModelBuilder.def_TAA_cnsts(prmt, cplex, y_ak, z_aek); //Q1
            ModelBuilder.def_Routing_cnsts(prmt, cplex, x_aeij, mu_aei); //Q2
            complicating_constraints = ModelBuilder.def_complicating_cnsts(prmt, cplex, y_ak, z_aek, x_aeij);  // Q3
            //
            cplex.setOut(null);
            cplex.solve();
            //
            String cnst_lable;
            double lm;
            for (int a : this.prmt.A) {
                aE = this.prmt.E_a.get(a);
                for (Object e : aE) {
                    for (int k : this.prmt.K) {
                        aek = prmt.get_AEK(a, e, k);
                        cnst_lable = String.format("CC(%s)", aek.get_label());
                        //
                        lm = cplex.getDual(complicating_constraints.get(cnst_lable));
                        if (lm > 0.0) {
                            lm_aek.put(aek, lm);
                        } else {
                            lm_aek.put(aek, 0.0);
                        }
                        NUM_LAMBDA += 1;
                    }
                }
            }
        } catch (IloException e) {
            e.printStackTrace();
        }

    }

    public void set_router(String router) {
        switch (router) {
            case "ILP":
                this.router = new RouterILP();
                ((RouterILP) this.router).setDualGapLimit(SUB_DUAL_GAP_LIMIT);
                break;
            case "BNB":
                this.router = new RouterBNB();
                break;
            case "GH":
                this.router = new RouterGH();
                break;
        }
    }

    public void set_parameters(double LRH_DUAL_GAP, int LRH_NUM_ITER,
                               double STEP_DECREASE_RATE,
                               int NO_IMPROVEMENT_LIMIT, double SUB_DUAL_GAP_LIMIT) {
        this.LRH_DUAL_GAP = LRH_DUAL_GAP;
        this.LRH_NUM_ITER = LRH_NUM_ITER;
        //
        this.STEP_DECREASE_RATE = STEP_DECREASE_RATE;
        this.NO_IMPROVEMENT_LIMIT = NO_IMPROVEMENT_LIMIT;
        this.SUB_DUAL_GAP_LIMIT = SUB_DUAL_GAP_LIMIT;
    }

    private void logging(String funcName,
                         String _doV0, String _doV1, String _FS0, String _FS1,
                         String note) {
        String[] row = {String.format("%f", etc.getWallTime()),
                        String.format("%f", etc.getCpuTime()),
                        String.format("%d", numIters),
                        funcName,
                        _doV0, _doV1, _FS0, _FS1,
                        note};
        try {
            CSVPrinter csvPrinter;
            if (numIters == 0 && funcName.equals("Initialization")) {
                csvPrinter = new CSVPrinter(new FileWriter(etc.logPath.toString()), CSVFormat.DEFAULT
                        .withHeader("wallT", "cpuT", "Iteration",
                                    "Function",
                                    "L_lm*", "L_lm1", "F*", "F1",
                                    "Note"));
            } else {
                csvPrinter = new CSVPrinter(new FileWriter(etc.logPath.toString(), true), CSVFormat.DEFAULT);
            }
            csvPrinter.printRecord(row);
            csvPrinter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void lmLogging(String[] row) {
        try {
            CSVPrinter csvPrinter;
            if (numIters == 0) {
                ArrayList<Integer> aE;
                AEK aek;
                String [] header = new String[NUM_LAMBDA];
                int lmIndex = 0;
                for (int a: prmt.A) {
                    aE = prmt.E_a.get(a);
                    for (int e: aE) {
                        for (int k: prmt.K) {
                            aek = prmt.get_AEK(a, e, k);
                            header[lmIndex] = aek.get_label();
                            lmIndex += 1;
                        }
                    }
                }
                csvPrinter = new CSVPrinter(new FileWriter(etc.lmLogPath.toString()), CSVFormat.DEFAULT
                        .withHeader(header));
            } else {
                csvPrinter = new CSVPrinter(new FileWriter(etc.lmLogPath.toString(), true), CSVFormat.DEFAULT);
            }
            csvPrinter.printRecord(row);
            csvPrinter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        logging("Initialization",
                " ", " ", " ", " ",
                " ");
        while (etc.getWallTime() < etc.getTimeLimit()) {
            if (numIters > 1) {
                etc.trigerTermCondition = true;
                etc.saveTimestamp();
            }
            //
            solveDuals();
            solvePrimalNupdateLM();
            if (checkTerminalConditions())
                break;
            if (numIters == LRH_NUM_ITER)
                break;
            numIters += 1;
        }
        //
        bestSol.saveSolCSV(etc.solPathCSV);
//        bestSol.saveSolTXT(etc.solPathTXT);
//        bestSol.saveSolJSN(etc.solPathJSN);
//        bestSol.saveSolSER(etc.solPathSER);
    }

    private boolean checkTerminalConditions() {
        if (bestSol.dualG <= LRH_DUAL_GAP)
            return true;
        else if (at <= STEP_SIZE_LIMIT)
            return true;
        else
            return false;

    }

    private void solvePrimalNupdateLM() {
        primalExtraction();
        logging("primalExtraction",
                " ", " ",
                String.format("%.4f", F_star), String.format("%.4f", F1),
                " ");
        UpdateLM();
        logging("UpdateLM",
                " ", " ", " ", " ",
                String.format("at: %f  ut: %f", at, ut));
        bestSol.cpuT = etc.getCpuTime();
        bestSol.wallT = etc.getWallTime();
        double dualG = (L_lm_star - F_star) / F_star;
        if (dualG < bestSol.dualG)
            bestSol.dualG = dualG;
        //
        logging("IterSummary",
                " ", " ", " ", " ",
                String.format("Dual Gap: %f", bestSol.dualG));
        System.out.println(String.format("#%d : dualG  %.4f  cpuT %f  wallT %f",
                numIters, bestSol.dualG, etc.getCpuTime(), etc.getWallTime()));
    }

    private void solveDuals() {
        L1V = 0.0;
        L2V = 0.0;
        //
        solve_TAA();
        logging("dual_solve_TAA",
                " ", " ", " ", " ",
                String.format("L1V: %.4f", L1V));
        solve_Routing();
        logging("dual_solve_Routing",
                " ", " ", " ", " ",
                String.format("L2V: %.4f", L2V));
        L_lm1 = L1V + L2V;
        if (numIters == 0) {
            logging("solveDuals",
                    "inf", String.format("%.4f", L_lm1), " ", " ",
                    " ");
        } else {
            logging("solveDuals",
                    String.format("%.4f", L_lm_star), String.format("%.4f", L_lm1), " ", " ",
                    " ");
        }
    }

    private void solve_TAA() {
        double lm;
        double r, p;
        ArrayList aE;
        IloNumVar y, z;
        IloLinearNumExpr obj;
        AK ak;
        AE ae;
        AEK aek;
        //
        HashMap<AK, IloNumVar> y_ak = new HashMap<>();
        HashMap<AEK, IloNumVar> z_aek = new HashMap<>();
        // Solve an task assignment and accomplishment problem
        try {
            cplex = new IloCplex();
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
                }
            }
            //
            obj = cplex.linearNumExpr();
            for (int k : prmt.K) {
                r = prmt.r_k.get(k);
                for (int a : prmt.A) {
                    aE = prmt.E_a.get(a);
                    for (Object e : aE) {
                        ae = prmt.get_AE(a, e);
                        aek = prmt.get_AEK(a, e, k);
                        p = prmt.p_ae.get(ae);
                        z = z_aek.get(aek);
                        obj.addTerm(r * p, z);
                    }
                    ak = prmt.get_AK(a, k);
                    y = y_ak.get(ak);
                    obj.addTerm(-r, y);
                }
            }
            for (int k : prmt.K) {
                for (int a : prmt.A) {
                    ak = prmt.get_AK(a, k);
                    y = y_ak.get(ak);
                    aE = prmt.E_a.get(a);
                    for (Object e : aE) {
                        aek = prmt.get_AEK(a, e, k);
                        lm = lm_aek.get(aek);
                        obj.addTerm(lm, y);
                        z = z_aek.get(aek);
                        obj.addTerm(-lm, z);
                    }
                }
            }
            cplex.addMinimize(obj);
            //
            ModelBuilder.def_TAA_cnsts(prmt, cplex, y_ak, z_aek); //Q1
            //
            cplex.setOut(null);
//            cplex.setOut(new FileOutputStream(String.format("TAA_%s.txt", prmt.problemName)));
            cplex.solve();
            if (cplex.getStatus() == IloCplex.Status.Optimal) {
//                cplex.exportModel(String.format("TAA_%s.lp", prmt.problemName));
                L1V = -cplex.getObjValue();
                for (AK key: y_ak.keySet()) {
                    this.y_ak.put(key, cplex.getValue(y_ak.get(key)));
                }
                for (AEK key: z_aek.keySet()) {
                    this.z_aek.put(key, cplex.getValue(z_aek.get(key)));
                }
            } else {
                cplex.output().println("Other.Solution status = " + cplex.getStatus());
            }
            cplex.end();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void solve_Routing() {
        ForkJoinPool pool = ForkJoinPool.commonPool();
        Routing_PL_worker routingPLhandler = new Routing_PL_worker(this);
        L2V = pool.invoke(routingPLhandler);
    }

    private void primalExtraction() {
        double r, p;
        ArrayList aE;
        ArrayList aeN;
        IloLinearNumExpr obj, cnst;
        IloNumVar y, z;
        AK ak;
        AE ae;
        AEK aek;
        AEIJ aeij;
        HashMap<AK, IloNumVar> y_ak = new HashMap<>();
        HashMap<AEK, IloNumVar> z_aek = new HashMap<>();
        try {
            cplex = new IloCplex();
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
                }
            }
            //
            obj = cplex.linearNumExpr();
            for (int k : prmt.K) {
                for (int a : prmt.A) {
                    ak = prmt.get_AK(a, k);
                    r = prmt.r_k.get(k);
                    y = y_ak.get(ak);
                    obj.addTerm(r, y);
                    aE = prmt.E_a.get(a);
                    for (Object e : aE) {
                        ae = prmt.get_AE(a, e);
                        aek = prmt.get_AEK(a, e, k);
                        p = prmt.p_ae.get(ae);
                        z = z_aek.get(aek);
                        obj.addTerm(-(r * p), z);
                    }
                }
            }
            cplex.addMaximize(obj);
            //
            ModelBuilder.def_TAA_cnsts(prmt, cplex, y_ak, z_aek); //Q1
            // Complicated and Combined constraints Q3
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
                        double sumX = 0;
                        for (Object j: aeN) {
                            aeij = prmt.get_AEIJ(a, e, j, prmt.n_k.get(k));
                            sumX += x_aeij.get(aeij);
                        }
                        z = z_aek.get(aek);
                        cnst.addTerm(-1, z);
                        cplex.addLe(cnst, sumX, String.format("CC(%s)", aek.get_label()));
                    }
                }
            }
            //
            cplex.setOut(null);
            cplex.solve();
            if (cplex.getStatus() == IloCplex.Status.Optimal) {
                F1 = cplex.getObjValue();
                if (F1 > F_star) {
                    F_star = F1;
                    //
                    bestSol = new Solution();
                    bestSol.prmt = prmt;
                    bestSol.objV = F_star;
                    bestSol.dualG = ((L_lm1 > L_lm_star ? L_lm_star : L_lm1) - F_star) / F_star;
                    bestSol.cpuT = etc.getCpuTime();
                    bestSol.wallT = etc.getWallTime();
                    bestSol.y_ak = new HashMap<>();
                    for (AK key: y_ak.keySet()) {
                        bestSol.y_ak.put(key, cplex.getValue(y_ak.get(key)));
                    }
                    bestSol.z_aek = new HashMap<>(this.z_aek);
                    for (AEK key: z_aek.keySet()) {
                        bestSol.z_aek.put(key, cplex.getValue(z_aek.get(key)));
                    }
                    bestSol.mu_aei = new HashMap<>(mu_aei);
                    bestSol.x_aeij = new HashMap<>(x_aeij);
                }
            } else {
                cplex.output().println("Other.Solution status = " + cplex.getStatus());
            }
            cplex.end();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void update_step_size() {
        double y, z;
        ArrayList aE;
        ArrayList aeN;
        AK ak;
        AE ae;
        AEK aek;
        AEIJ aeij;
        //   L_lm1: the new value
        //   L_lm_star: the old one
        if (L_lm1 < L_lm_star) {
            // Better solution
            L_lm_star = L_lm1;
            noUpdateCounter_L = 0;
        } else {
            // No improvement
            noUpdateCounter_L += 1;
            if (noUpdateCounter_L == NO_IMPROVEMENT_LIMIT) {
                ut *= STEP_DECREASE_RATE;
                noUpdateCounter_L = 0;
            }
        }
        // Update at (Scale for the movement)
        double numerator = ut * (L_lm_star - F_star);
        double denominator2 = 0.0;
        for (int k: prmt.K) {
            for (int a: prmt.A) {
                ak = prmt.get_AK(a, k);
                y = y_ak.get(ak);
                aE = prmt.E_a.get(a);
                for (Object e : aE) {
                    ae = prmt.get_AE(a, e);
                    aek = prmt.get_AEK(a, e, k);
                    z = z_aek.get(aek);
                    aeN = prmt.N_ae.get(ae);
                    double sumX = 0.0;
                    for (Object j: aeN) {
                        aeij = prmt.get_AEIJ(a, e, j, prmt.n_k.get(k));
                        sumX += x_aeij.get(aeij);
                    }
                    denominator2 += Math.pow(y - z - sumX, 2);
                }
            }
        }
        at = numerator / denominator2;
    }

    private void UpdateLM() {
        double y, z;
        double lm;
        ArrayList aE;
        ArrayList aeN;
        AK ak;
        AE ae;
        AEK aek;
        AEIJ aeij;
        // Update the step size, at, first
        update_step_size();
        // Update multipliers
//        String [] row = new String[NUM_LAMBDA];
        int lmIndex = 0;
        for (int a: prmt.A) {
            aE = prmt.E_a.get(a);
            for (Object e: aE) {
                ae = prmt.get_AE(a, e);
                aeN = prmt.N_ae.get(ae);
                for (int k: prmt.K) {
                    ak = prmt.get_AK(a, k);
                    y = y_ak.get(ak);
                    aek = prmt.get_AEK(a, e, k);
                    z = z_aek.get(aek);
                    double sumX = 0.0;
                    for (Object j: aeN) {
                        aeij = prmt.get_AEIJ(a, e, j, prmt.n_k.get(k));
                        sumX += x_aeij.get(aeij);
                    }
                    lm = lm_aek.get(aek) + at * (y - z - sumX);
                    if (lm < 0.0) {
                        lm_aek.put(aek, 0.0);
                    } else {
                        lm_aek.put(aek, lm);
                    }
//                    row[lmIndex] = String.format("%f", lm_aek.get(aek));
                    lmIndex += 1;
                }
            }
        }
//        lmLogging(row);
    }

}
