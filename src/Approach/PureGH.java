package Approach;

import Index.*;
import Other.Etc;
import Other.Parameter;
import Other.Solution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

class Agent extends Thread {

    private Parameter prmt;
    private Etc etc;
    private int aid;
    private PureGH platform;
    //
    private final double volumeLimit, weightLimit;
    private final ArrayList<Double> l_e, u_e;
    //
    HashSet<Integer> KnM, KnP;
    HashMap<Integer, HashSet<Integer>> KnM_e, KnP_e;
    HashMap<Integer, ArrayList<String>> Sn_e;
    HashMap<Integer, HashSet<String>> Hn_e;
    ArrayList<Integer> index4Search_e;
    double currentReward, currentVolume, currentWeight;
    //
    int numTrial, best_tid;
    double best_expReward;
    HashMap<Integer, ArrayList> best_tid_eBestSeq;

    Agent(Parameter prmt, Etc etc, int a, PureGH gh) {
        this.prmt = prmt;
        this.etc = etc;
        this.aid = a;
        this.platform = gh;
        //
        volumeLimit = this.prmt.v_a.get(this.aid);
        weightLimit = this.prmt.w_a.get(this.aid);

        //
        KnM = new HashSet<>();
        KnP = new HashSet<>();
        KnM_e = new HashMap<>();
        KnP_e = new HashMap<>();
        Sn_e = new HashMap<>();
        Hn_e = new HashMap<>();
        index4Search_e = new ArrayList<>();
        currentVolume = 0.0;
        currentWeight = 0.0;
        l_e = new ArrayList<>();
        u_e = new ArrayList<>();
        //
        ArrayList aeF;
        ArrayList aeS;
        AE ae;
        ArrayList aE = this.prmt.E_a.get(a);
        for (Object e : aE) {
            ae = this.prmt.get_AE(a, e);
            aeF = this.prmt.F_ae.get(ae);
            aeS = this.prmt.S_ae.get(ae);
            for (Object k: aeF) {
                KnM.add((Integer) k);
            }
            KnM_e.put((Integer) e, new HashSet<Integer>(aeF));
            KnP_e.put((Integer) e, new HashSet<>());
            Sn_e.put((Integer) e, new ArrayList<String>(aeS));
            Hn_e.put((Integer) e, new HashSet<String>());
            index4Search_e.add(1);
            l_e.add(this.prmt.l_ae.get(ae));
            u_e.add(this.prmt.u_ae.get(ae));
        }
        numTrial = 0;
    }

    private void findBestTask() {
        Integer _e;
        double kr, kv, kw, p, expReward;
        boolean isFeasible;
        ArrayList<String> best_sequence;
        //
        best_tid = -1;
        best_expReward = -Double.MAX_VALUE;
        best_tid_eBestSeq = null;
        for (int tid: KnM) {
            kr = prmt.r_k.get(tid);
            kv = prmt.v_k.get(tid);
            kw = prmt.w_k.get(tid);
            //
            HashMap<Integer, ArrayList> tid_eBestSeq = new HashMap<>();
            expReward = 0.0;
            for (Object e: prmt.E_a.get(aid)) {
                p = prmt.p_ae.get(prmt.get_AE(aid, e));
                _e = (Integer) e;
                //
                isFeasible = true;
                if (!KnM_e.get(_e).contains(tid)) {
                    isFeasible = false;
                } else if (volumeLimit < currentVolume + kv) {
                    isFeasible = false;
                } else if (weightLimit < currentWeight + kw) {
                    isFeasible = false;
                } else {
                    best_sequence = GH_helper.get_best_sequence(prmt,
                                                                Hn_e.get(_e),
                                                                Sn_e.get(_e),
                                                                index4Search_e.get(_e),
                                                                tid);
                    if (best_sequence == null ||
                            GH_helper.get_travelTime(prmt, best_sequence) - l_e.get(_e) > u_e.get(_e)) {
                        isFeasible = false;
                    } else {
                        tid_eBestSeq.put(_e, best_sequence);
                    }
                }
                if (isFeasible) {
                    expReward += kr * p;
                }
            }
            if (best_expReward < expReward) {
                best_expReward = expReward;
                best_tid = tid;
                best_tid_eBestSeq = tid_eBestSeq;
            }
        }
    }

    private void updateBestTask() {
        updateFeasibleTasks();
        KnP.add(best_tid);
        //
        currentReward += best_expReward;
        currentVolume += prmt.v_k.get(best_tid);
        currentWeight += prmt.w_k.get(best_tid);
        Integer _e;
        for (Object e: prmt.E_a.get(aid)) {
            _e = (Integer) e;
            if (KnM_e.get(_e).contains(best_tid)) {
                KnM_e.get(_e).remove(best_tid);
                KnP_e.get(_e).add(best_tid);
                if (best_tid_eBestSeq.containsKey(_e)) {
                    Sn_e.put(_e, new ArrayList<>(best_tid_eBestSeq.get(_e)));
                }
                Hn_e.get(_e).add(prmt.h_k.get(best_tid));
                index4Search_e.set(_e, Sn_e.get(_e).indexOf(prmt.n_k.get(best_tid)) + 1);
            }
        }
    }

    private void updateFeasibleTasks() {
        KnM.remove(best_tid);
    }

    @Override
    public void run() {
        while (!KnM.isEmpty()) {
            findBestTask();
            //
            boolean successTaskSelection = platform.confirmTaskSelection(this);
            if (successTaskSelection) {
                updateBestTask();
            } else {
                updateFeasibleTasks();
            }
            //
            numTrial += 1;
//            System.out.println(String.format("%d  agent %d: cT %d(%b), reward %.4f %s",
//                numTrial, aid, best_tid, successTaskSelection, currentReward, Sn_e));
        }
    }
}

public class PureGH extends ApproachSupClass {

    private Solution sol;
    //
    HashSet<Integer> unSelectedTasks;
    ArrayList<Agent> agents;

    public PureGH(Parameter prmt, Etc etc) {
        super(prmt, etc);
        //
        unSelectedTasks = new HashSet<>(prmt.K);
        agents = new ArrayList<>();
        for (int a: prmt.A) {
            agents.add(new Agent(prmt, etc, a, this));
        }
    }

    synchronized boolean confirmTaskSelection(Agent ag) {
        boolean successTaskSelection = false;
        if (unSelectedTasks.contains(ag.best_tid)) {
            unSelectedTasks.remove(ag.best_tid);
            successTaskSelection = true;
        }
        return successTaskSelection;
    }

    private void summarizeSol() {
        AEIJ aeij;
        AEI aei;
        AEK aek;
        AK ak;
        ArrayList aE, aeN;
        Agent agent;
        //
        sol = new Solution();
        sol.prmt = prmt;
        sol.cpuT = etc.getCpuTime();
        sol.wallT = etc.getWallTime();
        sol.y_ak = new HashMap<>();
        sol.z_aek = new HashMap<>();
        sol.mu_aei = new HashMap<>();
        sol.x_aeij = new HashMap<>();
        for (int a : prmt.A) {
            for (int k : prmt.K) {
                ak = prmt.get_AK(a, k);
                sol.y_ak.put(ak, 0.0);
            }
            aE = prmt.E_a.get(a);
            for (Object e : aE) {
                for (int k : prmt.K) {
                    aek = prmt.get_AEK(a, e, k);
                    sol.z_aek.put(aek, 0.0);
                }
                aeN = prmt.N_ae.get(prmt.get_AE(a, e));
                for (Object i: aeN) {
                    for (Object j: aeN) {
                        aeij = prmt.get_AEIJ(a, e, i, j);
                        sol.x_aeij.put(aeij, 0.0);
                    }
                    aei = prmt.get_AEI(a, e, i);
                    sol.mu_aei.put(aei, 0.0);
                }
            }
        }
        //
        String n0, n1;
        sol.objV = 0.0;
        for (int a: prmt.A) {
            agent = agents.get(a);
            sol.objV += agent.currentReward;
            for (int k: prmt.K) {
                ak = prmt.get_AK(a, k);
                if (agent.KnP.contains(k)) {
                    sol.y_ak.replace(ak, 1.0);
                    for (Object e: prmt.E_a.get(a)) {
                        aek = prmt.get_AEK(a, e, k);
                        if (!agent.KnP_e.get(e).contains(k)) {
                            sol.z_aek.replace(aek, 1.0);
                        }
                    }
                }
            }
            for (Object e: prmt.E_a.get(a)) {
                for (int s = 0; s < agent.Sn_e.get(e).size() - 1; s++) {
                    n0 = agent.Sn_e.get(e).get(s);
                    n1 = agent.Sn_e.get(e).get(s + 1);
                    sol.x_aeij.replace(prmt.get_AEIJ(a, e, n0, n1), 1.0);
                }
                HashMap<String, Double> arrivalTime = GH_helper.get_arrivalTime(prmt, agent.Sn_e.get(e));
                for (String i: arrivalTime.keySet()) {
                    sol.mu_aei.replace(prmt.get_AEI(a, e, i), arrivalTime.get(i));
                }
            }
        }
    }

    @Override
    public void run() {
        try {
            for (Agent agent: agents) {
                agent.start();
            }
            for (Agent agent: agents) {
                    agent.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //
        summarizeSol();
        sol.saveSolCSV(etc.solPathCSV);
        sol.saveSolTXT(etc.solPathTXT);
    }
}
