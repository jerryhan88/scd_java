package Approach;

import Index.ki;
import Other.Parameter;
import Other.Etc;

import Index.kriT;
import Other.Solution;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class SubgradientDescentAlgorithm {
    static int NUM_ITERS_LIMIT = 1000;
    static double DUAL_GAP_LIMIT = 0.001;
    //
    Parameter prmt;
    Etc etc;
    IloCplex cplex;
    //
    double a, u, dualObjV0, F_star;
    double dualObjV1, ObjV_TAA, objV_Routing;
    int numIters, noImpvLimits, noUpdateCounter;
    HashMap<kriT, Double> _lm_kriT = new HashMap<>();
    HashMap<Index.ki, Double> _y_ki = new HashMap<>();
    HashMap<Index.kriT, Double> _z_kriT = new HashMap<>();
    HashMap<Index.kriN, Double> _a_kriN = new HashMap<>();
    public HashMap<Index.krij, Double> _x_krij = new HashMap<>();

    public SubgradientDescentAlgorithm(Parameter _prmt, Etc _etc) {
        prmt = _prmt;
        etc = _etc;
        a = 0.0;
        u = 2.0;
        dualObjV0 = Double.MAX_VALUE;
        F_star = -Double.MAX_VALUE;
        numIters = 0;
        noImpvLimits = 5;
        noUpdateCounter = 0;
        ArrayList<Integer> R;
        for (int k : prmt.K) {
            R = prmt.R_k.get(k);
            for (int r : R) {
                for (int i : prmt.T) {
                    _lm_kriT.put(new Index.kriT(k, r, i), 0.0);
                }
            }
        }
    }

    public void run() {
        while (numIters < NUM_ITERS_LIMIT) {
            solveDuals();
            primalExtraction();
            UpdateLM();
            if ((dualObjV0 - F_star) <= DUAL_GAP_LIMIT) break;
            numIters += 1;
        }
        Solution sol = new Solution();
        sol.prmt = prmt;
        sol.objV = F_star;
        sol.dualG = dualObjV0 - F_star;
        sol.cpuT = etc.getCpuTime();
        sol.wallT = etc.getWallTime();
        sol.y_ki = _y_ki;
        sol.z_kriT = _z_kriT;
        sol.a_kriN = _a_kriN;
        sol.x_krij = _x_krij;
        //
        sol.saveSolJSN(etc.solPathJSN);
        sol.saveSolSER(etc.solPathSER);
        sol.saveSolCSV(etc.solPathCSV);
        sol.saveSolTXT(etc.solPathTXT);
    }
    private void solveDuals() {
        dualObjV1 = 0.0;
        ObjV_TAA = 0.0;
        objV_Routing = 0.0;
        //
        solve_TAA();
        solve_Routing();
        dualObjV1 = ObjV_TAA + objV_Routing;
    }

    private void solve_TAA() {
        double lm;
        double w, gamma;
        ArrayList<Integer> R;
        IloNumVar y, z;
        IloLinearNumExpr obj;
        Index.ki ki;
        Index.kr kr;
        Index.kriT kriT;
        //
        HashMap<ki, IloNumVar> y_ki = new HashMap<>();
        HashMap<Index.kriT, IloNumVar> z_kriT = new HashMap<>();
        // Solve an task assignment and accomplishment problem
        try {
            cplex = new IloCplex();
            for (int k : prmt.K) {
                for (int i : prmt.T) {
                    y_ki.put(new Index.ki(k, i), cplex.boolVar(String.format("y(%d,%d)", k, i)));
                }
                R = prmt.R_k.get(k);
                for (int r : R) {
                    for (int i : prmt.T) {
                        z_kriT.put(new Index.kriT(k, r, i), cplex.boolVar(String.format("z(%d,%d,%d)", k, r, i)));
                    }
                }
            }
            //
            obj = cplex.linearNumExpr();
            for (int i : prmt.T) {
                for (int k : prmt.K) {
                    ki = new Index.ki(k, i);
                    w = prmt.w_i.get(i);
                    y = y_ki.get(ki);
                    obj.addTerm(w, y);
                    R = prmt.R_k.get(k);
                    for (int r : R) {
                        kr = new Index.kr(k, r);
                        kriT = new Index.kriT(k, r, i);
                        gamma = prmt.gamma_kr.get(kr);
                        z = z_kriT.get(kriT);
                        obj.addTerm(-(w * gamma), z);
                    }
                }
            }
            for (int i : prmt.T) {
                for (int k : prmt.K) {
                    ki = new Index.ki(k, i);
                    y = y_ki.get(ki);
                    R = prmt.R_k.get(k);
                    for (int r : R) {
                        kriT = new Index.kriT(k, r, i);
                        lm = _lm_kriT.get(kriT);
                        z = z_kriT.get(kriT);
                        obj.addTerm(lm, z);
                        obj.addTerm(-lm, y);
                    }
                }
            }
            cplex.addMaximize(obj);
            //
            ModelBuildingHelper.def_TAA_cnsts(prmt, cplex, y_ki, z_kriT); //Q1
            //
            cplex.setOut(new FileOutputStream(etc.logFile));
            cplex.solve();
            if (cplex.getStatus() == IloCplex.Status.Optimal) {
                ObjV_TAA = cplex.getObjValue();
                for (Index.ki key: y_ki.keySet()) {
                    _y_ki.put(key, cplex.getValue(y_ki.get(key)));
                }
                for (Index.kriT key: z_kriT.keySet()) {
                    _z_kriT.put(key, cplex.getValue(z_kriT.get(key)));
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
        String iP;
        double lm;
        ArrayList<Integer> R;
        ArrayList<String> krN;
        IloNumVar x;
        IloLinearNumExpr obj;
        Index.kr kr;
        Index.kriT kriT;
        Index.krij krij;
        try {
            for (int k : prmt.K) {
                R = prmt.R_k.get(k);
                for (int r : R) {
                    kr = new Index.kr(k, r);
                    krN = prmt.N_kr.get(kr);
                    HashMap<Index.kriN, IloNumVar> a_kriN = new HashMap<>();
                    HashMap<Index.krij, IloNumVar> x_krij = new HashMap<>();
                    //
                    cplex = new IloCplex();
                    for (String i: krN) {
                        for (String j: krN) {
                            x_krij.put(new Index.krij(k, r, i, j), cplex.boolVar(String.format("x(%d,%d,%s,%s)", k, r, i, j)));
                        }
                        a_kriN.put(new Index.kriN(k, r, i), cplex.numVar(0.0, Double.MAX_VALUE, String.format("a(%d,%d,%s)", k, r, i)));
                    }
                    //
                    obj = cplex.linearNumExpr();
                    for (int i : prmt.T) {
                        iP = String.format("p%d", i);
                        kriT = new Index.kriT(k, r, i);
                        lm = _lm_kriT.get(kriT);
                        for (String j: krN) {
                            krij = new Index.krij(k, r, iP, j);
                            x = x_krij.get(krij);
                            obj.addTerm(lm, x);
                        }
                    }
                    cplex.addMaximize(obj);
                    //
                    ModelBuildingHelper.def_FC_cnsts_krGiven(prmt, k, r, cplex, x_krij);
                    ModelBuildingHelper.def_AT_cnsts_krGiven(prmt, k, r, cplex, a_kriN, x_krij);
                    //
                    cplex.setOut(new FileOutputStream(etc.logFile));
                    cplex.solve();
                    if (cplex.getStatus() == IloCplex.Status.Optimal) {
                        try {
                            objV_Routing += cplex.getObjValue();
                            for (Index.krij key: x_krij.keySet()) {
                                _x_krij.put(key, cplex.getValue(x_krij.get(key)));
                            }
                            for (Index.kriN key: a_kriN.keySet()) {
                                _a_kriN.put(key, cplex.getValue(a_kriN.get(key)));
                            }
                        } catch (IloException e) {
                            e.printStackTrace();
                        }
                    } else {
                        cplex.output().println("Other.Solution status = " + cplex.getStatus());
                    }
                    cplex.end();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void primalExtraction() {
        String iP;
        double w, gamma, objV;
        ArrayList<Integer> R;
        ArrayList<String> krN;
        IloLinearNumExpr obj, cnst;
        IloNumVar y, z;
        Index.ki ki;
        Index.kr kr;
        Index.kriT kriT;
        Index.krij krij;
        HashMap<Index.ki, IloNumVar> y_ki = new HashMap<>();
        HashMap<Index.kriT, IloNumVar> z_kriT = new HashMap<>();
        try {
            cplex = new IloCplex();
            for (int k : prmt.K) {
                for (int i : prmt.T) {
                    y_ki.put(new Index.ki(k, i), cplex.boolVar(String.format("y(%d,%d)", k, i)));
                }
                R = prmt.R_k.get(k);
                for (int r : R) {
                    for (int i : prmt.T) {
                        z_kriT.put(new Index.kriT(k, r, i), cplex.boolVar(String.format("z(%d,%d,%d)", k, r, i)));
                    }
                }
            }
            //
            obj = cplex.linearNumExpr();
            for (int i : prmt.T) {
                for (int k : prmt.K) {
                    ki = new Index.ki(k, i);
                    w = prmt.w_i.get(i);
                    y = y_ki.get(ki);
                    obj.addTerm(w, y);
                    R = prmt.R_k.get(k);
                    for (int r : R) {
                        kr = new Index.kr(k, r);
                        kriT = new Index.kriT(k, r, i);
                        gamma = prmt.gamma_kr.get(kr);
                        z = z_kriT.get(kriT);
                        obj.addTerm(-(w * gamma), z);
                    }
                }
            }
            cplex.addMaximize(obj);
            //
            ModelBuildingHelper.def_TAA_cnsts(prmt, cplex, y_ki, z_kriT); //Q1
            // Complicated and Combined constraints Q3
            for (int i: prmt.T) {
                iP = String.format("p%d", i);
                for (int k : prmt.K) {
                    ki = new Index.ki(k, i);
                    y = y_ki.get(ki);
                    R = prmt.R_k.get(k);
                    for (int r : R) {
                        kr = new Index.kr(k, r);
                        kriT = new Index.kriT(k, r, i);
                        z = z_kriT.get(kriT);
                        //
                        cnst = cplex.linearNumExpr();
                        cnst.addTerm(1, y);
                        cnst.addTerm(-1, z);
                        double sumX = 0;
                        krN = prmt.N_kr.get(kr);
                        for (String j: krN) {
                            krij = new Index.krij(k, r, iP, j);
                            sumX += _x_krij.get(krij);
                        }
                        cplex.addLe(cnst, sumX, String.format("CC(%d,%d,%d)", i, k, r));
                    }
                }
            }
            //
            cplex.setOut(new FileOutputStream(etc.logFile));
            cplex.solve();
            if (cplex.getStatus() == IloCplex.Status.Optimal) {
                objV = cplex.getObjValue();
                if (objV > F_star) {
                    F_star = objV;
                }
            } else {
                cplex.output().println("Other.Solution status = " + cplex.getStatus());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void UpdateLM() {
        String iP;
        double y, z;
        double lm;
        ArrayList<Integer> R;
        ArrayList<String> krN;
        Index.ki ki;
        Index.kr kr;
        Index.kriT kriT;
        Index.krij krij;

        // Update u (kind of temperature)
        //   dualObjV1: a new value
        //   dualObjV0: the old one
        if (dualObjV1 < dualObjV0) {
            // Better solution
            dualObjV0 = dualObjV1;
        } else {
            // No improvement
            noUpdateCounter += 1;
            if (noUpdateCounter == noImpvLimits) {
                u *= 0.5;
                noUpdateCounter = 0;
            }
        }

        // Update a (Scale for the movement)
        double denominator = u * (dualObjV0 - F_star);
        double numerator2 = 0.0;
        for (int i: prmt.T) {
            iP = String.format("p%d", i);
            for (int k: prmt.K) {
                ki = new Index.ki(k, i);
                y = _y_ki.get(ki);
                R = prmt.R_k.get(k);
                for (int r : R) {
                    kr = new Index.kr(k, r);
                    kriT = new Index.kriT(k, r, i);
                    z = _z_kriT.get(kriT);
                    krN = prmt.N_kr.get(kr);
                    double sumX = 0.0;
                    for (String j: krN) {
                        krij = new Index.krij(k, r, iP, j);
                        sumX += _x_krij.get(krij);
                    }
                    numerator2 += Math.pow(sumX + z - y, 2);
                }
            }
        }
        a = denominator / Math.sqrt(numerator2);

        // Update multipliers
        for (int i : prmt.T) {
            iP = String.format("p%d", i);
            for (int k : prmt.K) {
                ki = new Index.ki(k, i);
                y = _y_ki.get(ki);
                R = prmt.R_k.get(k);
                for (int r : R) {
                    kr = new Index.kr(k, r);
                    kriT = new Index.kriT(k, r, i);
                    z = _z_kriT.get(kriT);
                    krN = prmt.N_kr.get(kr);
                    double sumX = 0.0;
                    for (String j: krN) {
                        krij = new Index.krij(k, r, iP, j);
                        sumX += _x_krij.get(krij);
                    }
                    lm = _lm_kriT.get(kriT) - a * (sumX + z - y);
                    if (lm < 0.0) {
                        _lm_kriT.put(kriT, 0.0);
                    } else {
                        _lm_kriT.put(kriT, lm);
                    }
                    _lm_kriT.put(kriT, lm);
                }
            }
        }
    }
}
