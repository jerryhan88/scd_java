package Approach;

import Index.*;
import Other.Etc;
import Other.Parameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

class Agent extends Thread {

    Parameter prmt;
    Etc etc;
    int aid;
    PureGH gh;
    //
    HashSet<Integer> feasibleTasks;

    public Agent(Parameter prmt, Etc etc, int a, PureGH gh) {
        this.prmt = prmt;
        this.etc = etc;
        this.aid = a;
        this.gh = gh;

        feasibleTasks = new HashSet<>();
        ArrayList aE = this.prmt.E_a.get(a);
        ArrayList aeF;
        for (Object e : aE) {
            aeF = prmt.F_ae.get(new AE(a, e));
            for (Object k: aeF) {
                feasibleTasks.add((Integer) k);
            }
        }
    }

    @Override
    public void run() {
        long curValue;
        for(int i=0; i < 2; i++) {
            try {
                sleep(200);
//                curValue = gh.add(1);
                System.out.println(aid + "-" + feasibleTasks);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

    }
}

public class PureGH extends ApproachSupClass {

    private HashMap<AK, Double> y_ak;
    private HashMap<AEK, Double> z_aek;
    HashMap<AEIJ, Double> x_aeij;
    HashMap<AEI, Double> mu_aei;
    //
    HashSet<Integer> unSelectedTasks;

    public synchronized void updateAssignment() {

    }

    public PureGH(Parameter prmt, Etc etc) {
        super(prmt, etc);
        //
        y_ak = new HashMap<>();
        z_aek = new HashMap<>();
        mu_aei = new HashMap<>();
        x_aeij = new HashMap<>();
        //
        unSelectedTasks = new HashSet<>(prmt.K);
    }

    public void test() {
        Agent agent;
        for (int a: prmt.A) {
            agent = new Agent(prmt, etc, a, this);
            agent.start();
        }
    }

    @Override
    public void run() {
        test();
    }

}
