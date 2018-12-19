package Approach;

import Approach.Router.RouterILP;
import Approach.Router.RouterSup;
import Index.*;
import Other.Etc;
import Other.Parameter;
import Other.Solution;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ForkJoinPool;


public class SGM extends ApproachSupClass {
    RouterSup router;
    //
    IloCplex cplex;
    //
    private int numIters, noUpdateCounter;
    private double at, ut, dualObjV0, F_star, F1;
    private double dualObjV1;
    private double ObjV_TAA;
    double objV_Routing;
    Solution bestSol;
    HashMap<AEK, Double> _lm_aek = new HashMap<>();
    HashMap<AK, Double> _y_ak = new HashMap<>();
    HashMap<AEK, Double> _z_aek = new HashMap<>();
    HashMap<AEIJ, Double> _x_aeij = new HashMap<>();
    HashMap<AEI, Double> _mu_aei = new HashMap<>();
    //
    private int TERMINATION_NUM_ITERS, NO_IMPROVEMENT_LIMIT;
    private double TERMINATION_DUEL_GAP, STEP_DECREASE_RATE;
    //
    private double COMPENSATION_LIMIT = -1.0;
    private double COMPENSATION_RATE = -1.0;
    private boolean isCompensationMode = false;
    //
    private double INIT_LAMBDA_MULIPLYER = 2.0;
    private int NUM_LAMBDA = 0;
    //
    public SGM(Parameter prmt, Etc etc) {
        super(prmt, etc);
        at = 0.0;
        ut = 2.0;
        dualObjV0 = -Double.MAX_VALUE;
        F_star = Double.MAX_VALUE;
        numIters = 0;
        noUpdateCounter = 0;
        ArrayList<Integer> aE;
        for (int a : this.prmt.A) {
            aE = this.prmt.E_a.get(a);
            for (int e : aE) {
                for (int k : this.prmt.K) {
                    _lm_aek.put(new AEK(a, e, k), this.prmt.r_k.get(k) * INIT_LAMBDA_MULIPLYER);
                    NUM_LAMBDA += 1;
                }
            }
        }
    }

    public void set_router(String router) {
        if (router.equals("ILP")) {
            this.router = new RouterILP();
        }
    }

    public void set_parameters(int TERMINATION_NUM_ITERS, double TERMINATION_DUEL_GAP,
                               int NO_IMPROVEMENT_LIMIT, double STEP_DECREASE_RATE) {
        this.TERMINATION_NUM_ITERS = TERMINATION_NUM_ITERS;
        this.TERMINATION_DUEL_GAP = TERMINATION_DUEL_GAP;
        this.NO_IMPROVEMENT_LIMIT = NO_IMPROVEMENT_LIMIT;
        this.STEP_DECREASE_RATE = STEP_DECREASE_RATE;
    }

    public void set_parameters(int TERMINATION_NUM_ITERS, double TERMINATION_DUEL_GAP,
                               int NO_IMPROVEMENT_LIMIT, double STEP_DECREASE_RATE,
                               double COMPENSATION_LIMIT, double COMPENSATION_RATE) {
        this.TERMINATION_NUM_ITERS = TERMINATION_NUM_ITERS;
        this.TERMINATION_DUEL_GAP = TERMINATION_DUEL_GAP;
        this.NO_IMPROVEMENT_LIMIT = NO_IMPROVEMENT_LIMIT;
        this.STEP_DECREASE_RATE = STEP_DECREASE_RATE;
        this.COMPENSATION_LIMIT = COMPENSATION_LIMIT;
        this.COMPENSATION_RATE = COMPENSATION_RATE;
        this.isCompensationMode = true;
    }

    public void logging(String funcName,
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
                                    "DualObjV0", "DualObjV1", "F*", "F1",
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


    public void lmLogging(String [] row) {
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
                            aek = new AEK(a, e, k);
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
        double dualG;
        while (numIters < TERMINATION_NUM_ITERS) {
            solveDuals();
            if (numIters == 0) {
                logging("solveDuals",
                        "-inf", String.format("%.4f", dualObjV1), " ", " ",
                        " ");
            } else {
                logging("solveDuals",
                        String.format("%.4f", dualObjV0), String.format("%.4f", dualObjV1), " ", " ",
                        " ");
            }
            primalExtraction();
            logging("primalExtraction",
                    " ", " ",
                    String.format("%.4f", F_star), String.format("%.4f", F1),
                    " ");
            UpdateLM();
            logging("UpdateLM",
                    " ", " ", " ", " ",
                    String.format("at: %f  ut: %f", at, ut));
            dualG = (dualObjV0 - F_star) / F_star;
            if (dualG <= TERMINATION_DUEL_GAP) break;
            numIters += 1;
            System.out.println(String.format("#%d : dualG  %.4f  cpuT %f", numIters, dualG, etc.getCpuTime()));
        }
        //
        bestSol.saveSolCSV(etc.solPathCSV);
        bestSol.saveSolTXT(etc.solPathTXT);
//        bestSol.saveSolJSN(etc.solPathJSN);
//        bestSol.saveSolSER(etc.solPathSER);
    }
    private void solveDuals() {
        dualObjV1 = 0.0;
        ObjV_TAA = 0.0;
        objV_Routing = 0.0;
        //
        solve_TAA();
        logging("dual_solve_TAA",
                " ", " ", " ", " ",
                String.format("ObjV_TAA: %.4f", ObjV_TAA));
        solve_Routing();
        logging("dual_solve_Routing",
                " ", " ", " ", " ",
                String.format("objV_Routing: %.4f", objV_Routing));
        dualObjV1 = ObjV_TAA + objV_Routing;
    }

    private void solve_TAA() {
        double lm;
        double r, p;
        ArrayList<Integer> aE;
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
                    y_ak.put(new AK(a, k), cplex.boolVar(String.format("y(%d,%d)", a, k)));
                }
                aE = prmt.E_a.get(a);
                for (int e : aE) {
                    for (int k : prmt.K) {
                        z_aek.put(new AEK(a, e, k), cplex.boolVar(String.format("z(%d,%d,%d)", a, e, k)));
                    }
                }
            }
            //
            obj = cplex.linearNumExpr();
            for (int k : prmt.K) {
                r = prmt.r_k.get(k);
                for (int a : prmt.A) {
                    aE = prmt.E_a.get(a);
                    for (int e : aE) {
                        ae = new AE(a, e);
                        aek = new AEK(a, e, k);
                        p = prmt.p_ae.get(ae);
                        z = z_aek.get(aek);
                        obj.addTerm(r * p, z);
                    }
                    ak = new AK(a, k);
                    y = y_ak.get(ak);
                    obj.addTerm(-r, y);
                }
            }
            for (int k : prmt.K) {
                for (int a : prmt.A) {
                    ak = new AK(a, k);
                    y = y_ak.get(ak);
                    aE = prmt.E_a.get(a);
                    for (int e : aE) {
                        aek = new AEK(a, e, k);
                        lm = _lm_aek.get(aek);
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
            cplex.solve();
            if (cplex.getStatus() == IloCplex.Status.Optimal) {
                ObjV_TAA = cplex.getObjValue();
                for (AK key: y_ak.keySet()) {
                    _y_ak.put(key, cplex.getValue(y_ak.get(key)));
                }
                for (AEK key: z_aek.keySet()) {
                    _z_aek.put(key, cplex.getValue(z_aek.get(key)));
                }
            } else {
                cplex.output().println("Other.Solution status = " + cplex.getStatus());
            }
            cplex.end();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void primalExtraction() {
        double r, p;
        ArrayList<Integer> aE;
        ArrayList<String> aeN;
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
                    y_ak.put(new AK(a, k), cplex.boolVar(String.format("y(%d,%d)", a, k)));
                }
                aE = prmt.E_a.get(a);
                for (int e : aE) {
                    for (int k : prmt.K) {
                        z_aek.put(new AEK(a, e, k), cplex.boolVar(String.format("z(%d,%d,%d)", a, e, k)));
                    }
                }
            }
            //
            obj = cplex.linearNumExpr();
            for (int k : prmt.K) {
                for (int a : prmt.A) {
                    ak = new AK(a, k);
                    r = prmt.r_k.get(k);
                    y = y_ak.get(ak);
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
            ModelBuilder.def_TAA_cnsts(prmt, cplex, y_ak, z_aek); //Q1
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
                        y = y_ak.get(ak);
                        cnst.addTerm(1, y);
                        double sumX = 0;
                        for (String j: aeN) {
                            aeij = new AEIJ(a, e, j, prmt.n_k.get(k));
                            sumX += _x_aeij.get(aeij);
                        }
                        z = z_aek.get(aek);
                        cnst.addTerm(-1, z);
                        cplex.addLe(cnst, sumX, String.format("CC(%d,%d,%d)", a, e, k));
                    }
                }
            }
            //
            cplex.setOut(null);
            cplex.solve();
            if (cplex.getStatus() == IloCplex.Status.Optimal) {
                F1 = -cplex.getObjValue();
                if (F1 < F_star) {
                    F_star = F1;
                    //
                    bestSol = new Solution();
                    bestSol.prmt = prmt;
                    bestSol.objV = -F_star;
                    bestSol.dualG = ((dualObjV1 > dualObjV0 ? dualObjV1 : dualObjV0) - F_star) / F_star;
                    bestSol.cpuT = etc.getCpuTime();
                    bestSol.wallT = etc.getWallTime();
                    bestSol.y_ak = new HashMap<>();
                    for (AK key: y_ak.keySet()) {
                        bestSol.y_ak.put(key, cplex.getValue(y_ak.get(key)));
                    }
                    bestSol.z_aek = new HashMap<>(_z_aek);
                    for (AEK key: z_aek.keySet()) {
                        bestSol.z_aek.put(key, cplex.getValue(z_aek.get(key)));
                    }
                    bestSol.mu_aei = new HashMap<>(_mu_aei);
                    bestSol.x_aeij = new HashMap<>(_x_aeij);
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
        ArrayList<Integer> aE;
        ArrayList<String> aeN;
        AK ak;
        AE ae;
        AEK aek;
        AEIJ aeij;
        //   dualObjV1: at new value
        //   dualObjV0: the old one
        if (isCompensationMode && numIters !=0 && COMPENSATION_LIMIT < (dualObjV0 - dualObjV1) / dualObjV0) {
            at *= COMPENSATION_RATE;
        } else {
            if (dualObjV1 > dualObjV0) {
                // Better solution
                dualObjV0 = dualObjV1;
                noUpdateCounter = 0;
            } else {
                // No improvement
                noUpdateCounter += 1;
                if (noUpdateCounter == NO_IMPROVEMENT_LIMIT) {
                    ut *= STEP_DECREASE_RATE;
                    noUpdateCounter = 0;
                }
            }
            // Update at (Scale for the movement)
            double denominator = ut * (F_star - dualObjV0);
            double numerator2 = 0.0;
            for (int k: prmt.K) {
                for (int a: prmt.A) {
                    ak = new AK(a, k);
                    y = _y_ak.get(ak);
                    aE = prmt.E_a.get(a);
                    for (int e : aE) {
                        ae = new AE(a, e);
                        aek = new AEK(a, e, k);
                        z = _z_aek.get(aek);
                        aeN = prmt.N_ae.get(ae);
                        double sumX = 0.0;
                        for (String j: aeN) {
                            aeij = new AEIJ(a, e, j, prmt.n_k.get(k));
                            sumX += _x_aeij.get(aeij);
                        }
                        numerator2 += Math.pow(y - z - sumX, 2);
                    }
                }
            }
            at = denominator / numerator2;
        }
    }

    private void UpdateLM() {
        double y, z;
        double lm;
        ArrayList<Integer> aE;
        ArrayList<String> aeN;
        AK ak;
        AE ae;
        AEK aek;
        AEIJ aeij;
        // Update the step size, at, first
        update_step_size();


        // Update multipliers
        String [] row = new String[NUM_LAMBDA];
        int lmIndex = 0;
        for (int a: prmt.A) {
            aE = prmt.E_a.get(a);
            for (int e: aE) {
                ae = new AE(a, e);
                aeN = prmt.N_ae.get(ae);
                for (int k: prmt.K) {
                    ak = new AK(a, k);
                    y = _y_ak.get(ak);
                    aek = new AEK(a, e, k);
                    z = _z_aek.get(aek);
                    double sumX = 0.0;
                    for (String j: aeN) {
                        aeij = new AEIJ(a, e, j, prmt.n_k.get(k));
                        sumX += _x_aeij.get(aeij);
                    }
                    lm = _lm_aek.get(aek) + at * (y - z - sumX);
                    if (lm < 0.0) {
                        _lm_aek.put(aek, 0.0);
                    } else {
                        _lm_aek.put(aek, lm);
                    }
                    row[lmIndex] = String.format("%f", _lm_aek.get(aek));
                    lmIndex += 1;
                }
            }
        }
        lmLogging(row);
    }

    public void solve_Routing() {
        ForkJoinPool pool = ForkJoinPool.commonPool();
        Routing_PL_worker routingPLhandler = new Routing_PL_worker(this);
        objV_Routing = pool.invoke(routingPLhandler);
    }
}
