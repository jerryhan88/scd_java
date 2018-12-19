package Approach;


import Other.RoutingProbSol;

import java.util.ArrayList;
import java.util.concurrent.RecursiveTask;

public class Routing_PL_worker extends RecursiveTask<Double> {
    SGM sgm;
    //
    ArrayList<RoutingProbSol> subprobsols;
    //
    private static final int UTILIZED_NUM_PROCESSORS = Runtime.getRuntime().availableProcessors() - 1;
    boolean forkOrNot;
    int wid;
    //



    public Routing_PL_worker(SGM sgm) {
        this.sgm = sgm;
        //
        forkOrNot = true;
        wid = -1;
    }

    public Routing_PL_worker(SGM sgm, int wid, ArrayList<RoutingProbSol> subprobsols) {
        this.sgm = sgm;
        //
        forkOrNot = false;
        this.wid = wid;
        this.subprobsols = subprobsols;
    }

    public Double processing() {
        double totalVal = 0.0;
        for (RoutingProbSol rProbSol: subprobsols) {
            totalVal += sgm.router.solve(rProbSol);
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
            ArrayList<Integer> aE;
            for (int a : sgm.prmt.A) {
                aE = sgm.prmt.E_a.get(a);
                for (int e : aE) {
                    target_wid = numTasks % UTILIZED_NUM_PROCESSORS;
                    dividedTasks[target_wid].add(new RoutingProbSol(sgm.prmt, sgm._lm_aek,
                                                                    a, e));
                    numTasks += 1;
                }
            }
            //
            Routing_PL_worker worker;
            Routing_PL_worker[] workers = new Routing_PL_worker[UTILIZED_NUM_PROCESSORS];
            for (int i = 0; i < workers.length; i++) {
                worker = new Routing_PL_worker(sgm, i, dividedTasks[i]);
                workers[i] = worker;
                worker.fork();
            }
            for (int i = 0; i < workers.length; i++) {
                worker = workers[i];
                totalVal += worker.join();
            }




        } else {
            totalVal += processing();
        }

        return totalVal;
    }
}
