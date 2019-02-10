package Approach;


import Approach.Router.RouterILP;
import Index.AEI;
import Index.AEIJ;
import Other.RoutingProbSol;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.RecursiveTask;

public class Routing_PL_worker extends RecursiveTask<Double> {
    private LRH lrh;
    //
    private ArrayList<RoutingProbSol> subprobsols;
    //
//    private static final int UTILIZED_NUM_PROCESSORS = LRH.AVAILABLE_NUM_PROCESSORS - 1;
//    private static final int UTILIZED_NUM_PROCESSORS = 10;

    private boolean forkOrNot;
    private int wid;
    //

    Routing_PL_worker(LRH lrh) {
        this.lrh = lrh;
        //
        forkOrNot = true;
        wid = -1;
    }

    private Routing_PL_worker(LRH lrh, int wid, ArrayList<RoutingProbSol> subprobsols) {
        this.lrh = lrh;
        //
        forkOrNot = false;
        this.wid = wid;
        this.subprobsols = subprobsols;
    }

    private Double processing() {
        double totalVal = 0.0;
        for (RoutingProbSol rProbSol: subprobsols) {
            lrh.router.solve(rProbSol);
            totalVal += rProbSol.objV;
        }
        return totalVal;
    }

    @Override
    protected Double compute() {
        double totalVal = 0.0;
        if (forkOrNot) {
            ArrayList aE;
            int numSubProb = 0;
            for (int a : lrh.prmt.A) {
                aE = lrh.prmt.E_a.get(a);
                numSubProb += aE.size();
            }
            //
            int numWorkers;
            if (lrh.router instanceof RouterILP) {
                if (LRH.AVAILABLE_NUM_PROCESSORS < 32) {
                    // CPLEX use cores up to 32
                    numWorkers = Math.min(numSubProb, LRH.AVAILABLE_NUM_PROCESSORS);
                } else {
                    numWorkers = Math.min(numSubProb, LRH.AVAILABLE_NUM_PROCESSORS / 32);
                }
            } else {
                numWorkers = Math.min(numSubProb, LRH.AVAILABLE_NUM_PROCESSORS);
//                numWorkers = Math.min(numSubProb / 2, LRH.AVAILABLE_NUM_PROCESSORS);
            }
            ArrayList[] dividedTasks = new ArrayList[numWorkers];
            for (int i = 0; i < dividedTasks.length; i++) {
                dividedTasks[i] = new ArrayList<RoutingProbSol>();
            }
            //
            int target_wid;
            int idSubProb = 0;
            for (int a : lrh.prmt.A) {
                aE = lrh.prmt.E_a.get(a);
                for (Object e : aE) {
                    target_wid = idSubProb % numWorkers;
                    dividedTasks[target_wid].add(new RoutingProbSol(lrh.prmt, lrh.etc,
                                                                    lrh.lm_aek,
                                                                    a, (Integer) e, lrh.numIters));
                    idSubProb += 1;
                }
            }
            //
            Routing_PL_worker worker;
            ArrayList<Routing_PL_worker> workers = new ArrayList<>();
            for (int i = 0; i < numWorkers; i++) {
                worker = new Routing_PL_worker(lrh, i, dividedTasks[i]);
                workers.add(worker);
                worker.fork();
            }
            for (Iterator<Routing_PL_worker> iterator = workers.iterator(); iterator.hasNext();) {
                Routing_PL_worker worker1 = iterator.next();
                worker = worker1;
                totalVal += worker.join();
                //
                for (RoutingProbSol rProbSol : worker.subprobsols) {
                    //
                    for (AEIJ key : rProbSol.x_aeij.keySet()) {
                        lrh.x_aeij.put(key, rProbSol.x_aeij.get(key));
                    }
                    for (AEI key : rProbSol.mu_aei.keySet()) {
                        lrh.mu_aei.put(key, rProbSol.mu_aei.get(key));
                    }
                }
            }

//            Routing_PL_worker[] workers = new Routing_PL_worker[UTILIZED_NUM_PROCESSORS];
//            for (int i = 0; i < workers.length; i++) {
//                worker = new Routing_PL_worker(lrh, i, dividedTasks[i]);
//                workers[i] = worker;
//                worker.fork();
//            }
//            for (Routing_PL_worker worker1 : workers) {
//                worker = worker1;
//                totalVal += worker.join();
//                //
//                for (RoutingProbSol rProbSol : worker.subprobsols) {
//                    //
//                    for (AEIJ key : rProbSol.x_aeij.keySet()) {
//                        lrh.x_aeij.put(key, rProbSol.x_aeij.get(key));
//                    }
//                    for (AEI key : rProbSol.mu_aei.keySet()) {
//                        lrh.mu_aei.put(key, rProbSol.mu_aei.get(key));
//                    }
//                }
//            }


        } else {
            totalVal += processing();
        }
        return totalVal;
    }
}
