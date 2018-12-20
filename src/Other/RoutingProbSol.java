package Other;

import Index.AEI;
import Index.AEIJ;
import Index.AEK;

import java.util.HashMap;

public class RoutingProbSol {
    private Parameter prmt;
    private HashMap<AEK, Double> lm_aek;
    private int a, e;
    //
    public double objV;
    public HashMap<AEIJ, Double> x_aeij;
    public HashMap<AEI, Double> mu_aei;

    public RoutingProbSol(Parameter prmt, HashMap<AEK, Double> lm_aek,
                          int a, int e) {
        this.prmt = prmt;
        this.lm_aek = lm_aek;
        this.a = a;
        this.e = e;
        //
        x_aeij = new HashMap<>();
        mu_aei = new HashMap<>();
    }

    public int get_aid() {
        return a;
    }

    public int get_eid() {
        return e;
    }

    public Parameter get_prmt() {
        return prmt;
    }

    public HashMap<AEK, Double> get_lm_aek() {
        return lm_aek;
    }

}
