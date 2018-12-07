package Approach;

import Index.*;
import Other.Parameter;
import Other.Etc;

import Other.Solution;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;


public class SubgradientDescentAlgorithm extends ApproachSupClass {
    Parameter prmt;
    Etc etc;
    IloCplex cplex;
    int numIters, noUpdateCounter;
    double at, ut, dualObjV0, F_star;
    double dualObjV1, ObjV_TAA, objV_Routing;
    Solution bestSol;
    HashMap<AEK, Double> _lm_aek = new HashMap<>();
    HashMap<AK, Double> _y_ak = new HashMap<>();
    HashMap<AEK, Double> _z_aek = new HashMap<>();
    public HashMap<AEIJ, Double> _x_aeij = new HashMap<>();
    HashMap<AEI, Double> _mu_aei = new HashMap<>();
    //
    int TERMINATION_NUM_ITERS, NO_IMPROVEMENT_LIMIT;
    double TERMINATION_DUEL_GAP, STEP_DECREASE_RATE;

    double COMPENSATION_LIMIT = -1.0;
    double COMPENSATION_RATE = -1.0;
    boolean isCompensationMode = false;
    public SubgradientDescentAlgorithm(Parameter _prmt, Etc _etc) {
        prmt = _prmt;
        etc = _etc;
        at = 0.0;
        ut = 2.0;
        dualObjV0 = Double.MAX_VALUE;
        F_star = -Double.MAX_VALUE;
        numIters = 0;
        noUpdateCounter = 0;
        ArrayList<Integer> aE;
        for (int a : prmt.A) {
            aE = prmt.E_a.get(a);
            for (int e : aE) {
                for (int k : prmt.K) {
                    _lm_aek.put(new AEK(a, e, k), 0.0);
                }
            }
        }
    }

    public void set_parameters(int _TERMINATION_NUM_ITERS, double _TERMINATION_DUEL_GAP,
                               int _NO_IMPROVEMENT_LIMIT, double _STEP_DECREASE_RATE) {
        TERMINATION_NUM_ITERS = _TERMINATION_NUM_ITERS;
        TERMINATION_DUEL_GAP = _TERMINATION_DUEL_GAP;
        NO_IMPROVEMENT_LIMIT = _NO_IMPROVEMENT_LIMIT;
        STEP_DECREASE_RATE = _STEP_DECREASE_RATE;
    }

    public void set_parameters(int _TERMINATION_NUM_ITERS, double _TERMINATION_DUEL_GAP,
                               int _NO_IMPROVEMENT_LIMIT, double _STEP_DECREASE_RATE,
                               double _COMPENSATION_LIMIT, double _COMPENSATION_RATE) {
        TERMINATION_NUM_ITERS = _TERMINATION_NUM_ITERS;
        TERMINATION_DUEL_GAP = _TERMINATION_DUEL_GAP;
        NO_IMPROVEMENT_LIMIT = _NO_IMPROVEMENT_LIMIT;
        STEP_DECREASE_RATE = _STEP_DECREASE_RATE;
        COMPENSATION_LIMIT = _COMPENSATION_LIMIT;
        COMPENSATION_RATE = _COMPENSATION_RATE;
        isCompensationMode = true;
    }

    public void logging(String category, String note) {
        String[] row = {String.format("%f", etc.getWallTime()),
                        String.format("%f", etc.getCpuTime()),
                        String.format("%d", numIters),
                        category, note};
        try {
            CSVPrinter csvPrinter;
            if (numIters == 0 && note.equals("Initialization")) {
                csvPrinter = new CSVPrinter(new FileWriter(etc.logPath.toString()), CSVFormat.DEFAULT
                        .withHeader("wallT", "cpuT", "Iteration", "Category", "Note"));
            } else {
                csvPrinter = new CSVPrinter(new FileWriter(etc.logPath.toString(), true), CSVFormat.DEFAULT);
            }
            csvPrinter.printRecord(row);
            csvPrinter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        logging("-", "Initialization");
        double dualG;
        while (numIters < TERMINATION_NUM_ITERS) {
            solveDuals();
            primalExtraction();
            UpdateLM();
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
        solve_Routing();
        dualObjV1 = ObjV_TAA + objV_Routing;
        if (numIters == 0) {
            logging("solveDuals", String.format("DualObjV0: inf  DualObjV1: %.4f", dualObjV1));
        } else {
            logging("solveDuals", String.format("DualObjV0: %.4f  DualObjV1: %.4f", dualObjV0, dualObjV1));
        }

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
            for (int k : prmt.K) {
                for (int a : prmt.A) {
                    ak = new AK(a, k);
                    y = y_ak.get(ak);
                    aE = prmt.E_a.get(a);
                    for (int e : aE) {
                        aek = new AEK(a, e, k);
                        lm = _lm_aek.get(aek);
                        z = z_aek.get(aek);
                        obj.addTerm(lm, z);
                        obj.addTerm(-lm, y);
                    }
                }
            }
            cplex.addMaximize(obj);
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
            logging("solveDuals", "solve_TAA");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void solve_Routing() {
        double lm;
        ArrayList<Integer> aE;
        ArrayList<String> aeN;
        IloNumVar x;
        IloLinearNumExpr obj;
        AE ae;
        AEK aek;
        AEIJ aeij;
        try {
            for (int a : prmt.A) {
                aE = prmt.E_a.get(a);
                for (int e : aE) {
                    ae = new AE(a, e);
                    aeN = prmt.N_ae.get(ae);
                    HashMap<AEIJ, IloNumVar> x_aeij = new HashMap<>();
                    HashMap<AEI, IloNumVar> mu_aei = new HashMap<>();
                    //
                    cplex = new IloCplex();
                    for (String i: aeN) {
                        for (String j: aeN) {
                            x_aeij.put(new AEIJ(a, e, i, j), cplex.boolVar(String.format("x(%d,%d,%s,%s)", a, e, i, j)));
                        }
                        mu_aei.put(new AEI(a, e, i), cplex.numVar(0.0, Double.MAX_VALUE, String.format("mu(%d,%d,%s)", a, e, i)));
                    }
                    //
                    obj = cplex.linearNumExpr();
                    for (int k : prmt.K) {
                        aek = new AEK(a, e, k);
                        lm = _lm_aek.get(aek);
                        for (String j: aeN) {
                            aeij = new AEIJ(a, e, prmt.n_k.get(k), j);
                            x = x_aeij.get(aeij);
                            obj.addTerm(lm, x);
                        }
                    }
                    cplex.addMaximize(obj);
                    //
                    ModelBuilder.def_FC_cnsts_aeGiven(prmt, a, e, cplex, x_aeij);
                    ModelBuilder.def_AT_cnsts_aeGiven(prmt, a, e, cplex, x_aeij, mu_aei);
                    //
                    cplex.setOut(null);
                    cplex.solve();
                    if (cplex.getStatus() == IloCplex.Status.Optimal) {
                        try {
                            objV_Routing += cplex.getObjValue();
                            for (AEIJ key: x_aeij.keySet()) {
                                _x_aeij.put(key, cplex.getValue(x_aeij.get(key)));
                            }
                            for (AEI key: mu_aei.keySet()) {
                                _mu_aei.put(key, cplex.getValue(mu_aei.get(key)));
                            }
                        } catch (IloException ex) {
                            ex.printStackTrace();
                        }
                    } else {
                        cplex.output().println("Other.Solution status = " + cplex.getStatus());
                    }
                    cplex.end();
                    logging("solveDuals", String.format("solve_Routing (%d&%d)", a, e));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void primalExtraction() {
        double r, p, objV;
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
                objV = cplex.getObjValue();
                if (objV > F_star) {
                    F_star = objV;
                    //
                    bestSol = new Solution();
                    bestSol.prmt = prmt;
                    bestSol.objV = F_star;
                    bestSol.dualG = ((dualObjV1 < dualObjV0 ? dualObjV1 : dualObjV0) - F_star) / F_star;
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
            logging("primalExtraction", String.format("F*: %f", F_star));
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
            if (dualObjV1 < dualObjV0) {
                // Better solution
                dualObjV0 = dualObjV1;
            } else {
                // No improvement
                noUpdateCounter += 1;
                if (noUpdateCounter == NO_IMPROVEMENT_LIMIT) {
                    ut *= STEP_DECREASE_RATE;
                    noUpdateCounter = 0;
                }
            }
            // Update at (Scale for the movement)
            double denominator = ut * (dualObjV0 - F_star);
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
                        numerator2 += Math.pow(sumX + z - y, 2);
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
        for (int k: prmt.K) {
            for (int a: prmt.A) {
                ak = new AK(a, k);
                y = _y_ak.get(ak);
                aE = prmt.E_a.get(a);
                for (int e: aE) {
                    ae = new AE(a, e);
                    aek = new AEK(a, e, k);
                    z = _z_aek.get(aek);
                    aeN = prmt.N_ae.get(ae);
                    double sumX = 0.0;
                    for (String j: aeN) {
                        aeij = new AEIJ(a, e, j, prmt.n_k.get(k));
                        sumX += _x_aeij.get(aeij);
                    }
                    lm = _lm_aek.get(aek) - at * (sumX + z - y);
                    if (lm < 0.0) {
                        _lm_aek.put(aek, 0.0);
                    } else {
                        _lm_aek.put(aek, lm);
                    }
                }
            }
        }
        logging("UpdateLM", String.format("at: %f  ut: %f", at, ut));
    }
}
