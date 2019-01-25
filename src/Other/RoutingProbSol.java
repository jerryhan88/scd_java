package Other;

import Index.AEI;
import Index.AEIJ;
import Index.AEK;

import java.util.HashMap;

public class RoutingProbSol {
    private Parameter prmt;
    private Etc etc;
    private HashMap<AEK, Double> lm_aek;
    private int a, e, numIter;
    //
    public double objV;
    public HashMap<AEIJ, Double> x_aeij;
    public HashMap<AEI, Double> mu_aei;
    //
    public boolean isTerminated;

    public RoutingProbSol(Parameter prmt, Etc etc,
                          HashMap<AEK, Double> lm_aek,
                          int a, int e, int numIter) {
        this.prmt = prmt;
        this.etc = etc;
        this.lm_aek = lm_aek;
        this.a = a;
        this.e = e;
        this.numIter = numIter;
        //
        x_aeij = new HashMap<>();
        mu_aei = new HashMap<>();
        //
        isTerminated = false;
    }

    public int get_aid() {
        return a;
    }

    public int get_eid() {
        return e;
    }

    public int get_numIter() {
        return numIter;
    }

    public Parameter get_prmt() {
        return prmt;
    }

    public Etc get_etc() {
        return etc;
    }

    public HashMap<AEK, Double> get_lm_aek() {
        return lm_aek;
    }

}
