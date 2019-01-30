package Approach;


import Index.AEI;
import Index.AEIJ;
import Other.RoutingProbSol;

import java.util.ArrayList;
import java.util.concurrent.RecursiveTask;

public class Routing_PL_worker extends RecursiveTask<Double> {
    private LRH rlh;
    //
    private ArrayList<RoutingProbSol> subprobsols;
    //
    private static final int UTILIZED_NUM_PROCESSORS = LRH.AVAILABLE_NUM_PROCESSORS - 1;
//    private static final int UTILIZED_NUM_PROCESSORS = 10;

    private boolean forkOrNot;
    private int wid;
    //

    Routing_PL_worker(LRH rlh) {
        this.rlh = rlh;
        //
        forkOrNot = true;
        wid = -1;
    }

    private Routing_PL_worker(LRH rlh, int wid, ArrayList<RoutingProbSol> subprobsols) {
        this.rlh = rlh;
        //
        forkOrNot = false;
        this.wid = wid;
        this.subprobsols = subprobsols;
    }

    private Double processing() {
        double totalVal = 0.0;
        for (RoutingProbSol rProbSol: subprobsols) {
            rlh.router.solve(rProbSol);
            totalVal += rProbSol.objV;
        }
        return totalVal;
    }

    @Override
    protected Double compute() {
        double totalVal = 0.0;
        if (forkOrNot) {
            ArrayList[] dividedTasks = new ArrayList[UTILIZED_NUM_PROCESSORS];
            for (int i = 0; i < dividedTasks.length; i++) {
                dividedTasks[i] = new ArrayList<RoutingProbSol>();
            }
            int target_wid;
            int numTasks = 0;
            ArrayList aE;
            for (int a : rlh.prmt.A) {
                aE = rlh.prmt.E_a.get(a);
                for (Object e : aE) {
                    target_wid = numTasks % UTILIZED_NUM_PROCESSORS;
                    dividedTasks[target_wid].add(new RoutingProbSol(rlh.prmt, rlh.etc,
                                                                    rlh.lm_aek,
                                                                    a, (Integer) e, rlh.numIters));
                    numTasks += 1;
                }
            }
            //
            Routing_PL_worker worker;
            Routing_PL_worker[] workers = new Routing_PL_worker[UTILIZED_NUM_PROCESSORS];
            for (int i = 0; i < workers.length; i++) {
                worker = new Routing_PL_worker(rlh, i, dividedTasks[i]);
                workers[i] = worker;
                worker.fork();
            }
            for (Routing_PL_worker worker1 : workers) {
                worker = worker1;
                totalVal += worker.join();
                //
                for (RoutingProbSol rProbSol : worker.subprobsols) {
                    //
                    for (AEIJ key : rProbSol.x_aeij.keySet()) {
                        rlh.x_aeij.put(key, rProbSol.x_aeij.get(key));
                    }
                    for (AEI key : rProbSol.mu_aei.keySet()) {
                        rlh.mu_aei.put(key, rProbSol.mu_aei.get(key));
                    }
                }
            }
        } else {
            totalVal += processing();
        }
        return totalVal;
    }
}
