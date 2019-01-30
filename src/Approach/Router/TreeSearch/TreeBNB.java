package Approach.Router.TreeSearch;

import Approach.LRH;
import Index.AEK;
import Other.Etc;
import Other.Parameter;

import java.util.ArrayList;
import java.util.HashMap;

enum WorkerStatus {
    BUSY,
    WAITING
}

class Worker extends Thread {

    int wid;
    private TreeBNB tree;
    private WorkerStatus status;


    Worker(int wid, TreeBNB tree) {
        this.wid = wid;
        this.tree = tree;
    }

    public String toString() {
        return String.format("%d (a%d,e%d)", wid, tree.a, tree.e);
    }

    @Override
    public void run() {
        while (!tree.isSearchFinished) {
            status = WorkerStatus.BUSY;
            while (tree.pq.size() != 0) {
                tree.branch();
            }
            status = WorkerStatus.WAITING;
            boolean allFinished = true;
            for (Worker worker: tree.workers) {
                if (worker.status == WorkerStatus.BUSY) {
                    allFinished = false;
                    break;
                }
            }
            if (allFinished)
                tree.isSearchFinished = true;
        }
    }
}

public class TreeBNB extends Tree {
    private int numLeafTracker;
    ArrayList<Worker> workers;

    public TreeBNB(Parameter prmt, Etc etc,
                   int a, int e, HashMap<AEK, Double> lm_aek) {

        super(prmt, etc,
                a, e, lm_aek);
        workers = new ArrayList<>();
        workers.add(new Worker(workers.size(), this));
        numLeafTracker = pq.size();
    }

    synchronized void pushNodes(ArrayList<Node> nodes) {
        super.pushNodes(nodes);
        if (workers.size() < LRH.AVAILABLE_NUM_PROCESSORS
                && numLeafTracker * 2 < pq.size()) {
            numLeafTracker = pq.size();
            Worker worker = new Worker(workers.size(), this);
            workers.add(worker);
            worker.start();
        }
    }

    void branch() {
        Node tn = popNode();
        if (tn != null) {
            if (incumbent != null
                    && tn.upperBound <= incumbent.lowerBound)
                return;
            if (incumbent != null
                    && tn.nid > incumbent.nid * lm_k.size()) {
                isSearchFinished = true;
                return;
            }
            if (tn.tLB == tn.upperBound) {
                tn.lowerBound = tn.g();
                update_incumbent(tn);
            } else {
                ArrayList<Node> children = tn.gen_children_or_calc_lowerBound();
                if (children == null) {
                    update_incumbent(tn);
                } else {
                    pushNodes(children);
                }
            }
        }
    }

    public void solve() throws InterruptedException {
        Worker firstWorker = workers.get(0);
        firstWorker.start();
        firstWorker.join();
        for (Worker worker: workers) {
            if (worker.wid == 0)
                continue;
            worker.join();
        }
        update_dvs();
    }

}