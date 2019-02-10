package Other;

import Index.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class Parameter implements Serializable {
    public String problemName;
    public ArrayList<Integer> H = new ArrayList<>();
    public ArrayList<Integer> K = new ArrayList<>();
    public ArrayList<String> h_k = new ArrayList<>();
    public ArrayList<String> n_k = new ArrayList<>();
    public ArrayList<Double> v_k = new ArrayList<>();
    public ArrayList<Double> w_k = new ArrayList<>();
    public ArrayList<Double> r_k = new ArrayList<>();
    public ArrayList<String> N = new ArrayList<>();
    public ArrayList<Integer> A = new ArrayList<>();
    public ArrayList<Double> v_a = new ArrayList<>();
    public ArrayList<Double> w_a = new ArrayList<>();
    public ArrayList<ArrayList> E_a = new ArrayList<>();
    public HashMap<AE, Double> p_ae = new HashMap<>();
    public HashMap<AE, Double> l_ae = new HashMap<>();
    public HashMap<AE, Double> u_ae = new HashMap<>();
    public HashMap<AE, ArrayList> S_ae = new HashMap<>();
    public HashMap<AE, ArrayList> N_ae = new HashMap<>();
    public HashMap<AEIJ, Long> c_aeij = new HashMap<>();
    public HashMap<String, Double> al_i = new HashMap<>();
    public HashMap<String, Double> be_i = new HashMap<>();
    public HashMap<String, Double> ga_i = new HashMap<>();
    public HashMap<IJ, Double> t_ij = new HashMap<>();
    public HashMap<AE, ArrayList> F_ae = new HashMap<>();
    public HashMap<AE, ArrayList> iF_ae = new HashMap<>();
    public double M;
    //
    ArrayList<ArrayList> Indices_AE;
    ArrayList<ArrayList> Indices_AK;
    ArrayList<ArrayList> Indices_AEK;
    HashMap<String, AEIJ> Indices_AEIJ;
    HashMap<String, AEI> Indices_AEI;
    HashMap<String, IJ> Indices_IJ;
    //

    public void savePrmt(Path fpath) {
        try {
            ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(fpath.toFile()));
            os.writeObject(this);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void parseJsonArray(JSONObject jsonBase, ArrayList prmtRef, String prmtName, String dataType) {
        JSONArray array = (JSONArray) jsonBase.get(prmtName);
        for (Object i : array) {
            if (dataType.equals("I")) {
                prmtRef.add(((Long) i).intValue());
            } else if (dataType.equals("D")) {
                prmtRef.add((Double) i);
            } else if (dataType.equals("S")) {
                prmtRef.add((String) i);
            }
        }
    }

    private static void parseJsonObject(JSONObject jsonObjWhole, HashMap prmtRef, String prmtName) {
        JSONObject obj = (JSONObject) jsonObjWhole.get(prmtName);
        for (String key: (Set<String>) obj.keySet()) {
            prmtRef.put(key, obj.get(key));
        }
    }

    public static Parameter json2ser(Path fpath) {
        Parameter _prmt = new Parameter();
        try {
            JSONObject jsonBase = (JSONObject) new JSONParser().parse(new FileReader(fpath.toFile()));
            //
            _prmt.problemName = (String) jsonBase.get("problemName");
            parseJsonArray(jsonBase, _prmt.H, "H", "I");
            parseJsonArray(jsonBase, _prmt.K, "K", "I");
            parseJsonArray(jsonBase, _prmt.h_k, "h_k", "S");
            parseJsonArray(jsonBase, _prmt.n_k, "n_k", "S");
            parseJsonArray(jsonBase, _prmt.v_k, "v_k", "D");
            parseJsonArray(jsonBase, _prmt.w_k, "w_k", "D");
            parseJsonArray(jsonBase, _prmt.r_k, "r_k", "D");
            parseJsonArray(jsonBase, _prmt.N, "N", "S");
            parseJsonArray(jsonBase, _prmt.A, "A", "I");
            parseJsonArray(jsonBase, _prmt.v_a, "v_a", "D");
            parseJsonArray(jsonBase, _prmt.w_a, "w_a", "D");
            JSONArray arrayOut = (JSONArray) jsonBase.get("E_a");
            for (Object anArrayOut : arrayOut) {
                JSONArray arrayIn = (JSONArray) anArrayOut;
                ArrayList<Integer> arrayInNew = new ArrayList<>();
                for (Object anArrayIn : arrayIn) {
                    arrayInNew.add(((Long) anArrayIn).intValue());
                }
                _prmt.E_a.add(arrayInNew);
            }
            //
            _prmt.Indices_AE = new ArrayList<>();
            for (int a: _prmt.A) {
                ArrayList<AE> inner_AE = new ArrayList<>();
                for (Object e: _prmt.E_a.get(a)) {
                    inner_AE.add(new AE(a, e));
                }
                _prmt.Indices_AE.add(inner_AE);
            }
            _prmt.Indices_AK = new ArrayList<>();
            for (int a: _prmt.A) {
                ArrayList<AK> inner_AK = new ArrayList<>();
                for (int k: _prmt.K) {
                    inner_AK.add(new AK(a, k));
                }
                _prmt.Indices_AK.add(inner_AK);
            }
            _prmt.Indices_AEK = new ArrayList<>();
            for (int a: _prmt.A) {
                ArrayList<ArrayList> inner_AEK = new ArrayList<>();
                for (Object e: _prmt.E_a.get(a)) {
                    ArrayList<AEK> inner_inner_AEK = new ArrayList<>();
                    for (int k: _prmt.K) {
                        inner_inner_AEK.add(new AEK(a, e, k));
                    }
                    inner_AEK.add(inner_inner_AEK);
                }
                _prmt.Indices_AEK.add(inner_AEK);
            }
            //
            JSONObject _S_ae = (JSONObject) jsonBase.get("S_ae");
            JSONObject _N_ae = (JSONObject) jsonBase.get("N_ae");
            JSONObject _p_ae = (JSONObject) jsonBase.get("p_ae");
            JSONObject _l_kr = (JSONObject) jsonBase.get("l_ae");
            JSONObject _u_kr = (JSONObject) jsonBase.get("u_ae");
            JSONObject _F_ae = (JSONObject) jsonBase.get("F_ae");
            //
            int a, e;
            AE ae;
            AEIJ aeij;
            JSONArray arrayJ;
            _prmt.Indices_AEIJ = new HashMap<>();
            for (String key: (Set<String>) _S_ae.keySet()) {
                String[] _key = key.split("&");
                a = Integer.parseInt(_key[0]);
                e = Integer.parseInt(_key[1]);
                ae = _prmt.get_AE(a, e);
                //
                ArrayList<String> aeS = new ArrayList<>();
                arrayJ = (JSONArray) _S_ae.get(key);
                for (Object anArrayJ : arrayJ) {
                    aeS.add((String) anArrayJ);
                }
                _prmt.S_ae.put(ae, aeS);
                ArrayList<String> aeN = new ArrayList<>();
                arrayJ = (JSONArray) _N_ae.get(key);
                for (Object anArrayJ : arrayJ) {
                    aeN.add((String) anArrayJ);
                }
                _prmt.N_ae.put(ae, aeN);
                //
                ArrayList<Integer> aeF = new ArrayList<>();
                arrayJ = (JSONArray) _F_ae.get(key);
                for (Object anArrayJ : arrayJ) {
                    aeF.add(((Long) anArrayJ).intValue());
                }
                _prmt.F_ae.put(ae, aeF);
                //
                _prmt.p_ae.put(ae, (double) _p_ae.get(key));
                _prmt.l_ae.put(ae, (double) _l_kr.get(key));
                _prmt.u_ae.put(ae, (double) _u_kr.get(key));
            }
            JSONObject _c_aeij = (JSONObject) jsonBase.get("c_aeij");
            for (String key: (Set<String>) _c_aeij.keySet()) {
                String[] _key = key.split("&");
                a = Integer.parseInt(_key[0]);
                e = Integer.parseInt(_key[1]);
                String i = _key[2];
                String j = _key[3];
                aeij = new AEIJ(a, e, i, j);
                _prmt.Indices_AEIJ.put(aeij.get_label(), aeij);
                _prmt.c_aeij.put(_prmt.get_AEIJ(a, e, i, j), (long) _c_aeij.get(key));
            }
            parseJsonObject(jsonBase, _prmt.al_i, "al_i");
            parseJsonObject(jsonBase, _prmt.be_i, "be_i");
            parseJsonObject(jsonBase, _prmt.ga_i, "ga_i");
            //
            IJ ij;
            _prmt.Indices_IJ = new HashMap<>();
            JSONObject _t_ij = (JSONObject) jsonBase.get("t_ij");
            for (String key: (Set<String>) _t_ij.keySet()) {
                String [] _ij = key.split("&");
                ij = new IJ(_ij[0], _ij[1]);
                _prmt.Indices_IJ.put(ij.get_label(), ij);
                _prmt.t_ij.put(_prmt.get_IJ(_ij[0], _ij[1]), (double) _t_ij.get(key));
            }
            //
            AE _ae;
            ArrayList<Integer> aE;
            HashSet<Integer> aeF;
            for (int _a : _prmt.A) {
                aE = _prmt.E_a.get(_a);
                for (int _e : aE) {
                    _ae = _prmt.get_AE(_a, _e);
                    aeF = new HashSet<>(_prmt.F_ae.get(_ae));
                    ArrayList<Integer> ae_uF = new ArrayList<>();
                    for (int _k: _prmt.K) {
                        if (!aeF.contains(_k)) {
                            ae_uF.add(_k);
                        }
                    }
                     _prmt.iF_ae.put(_ae, ae_uF);
                }
            }
            _prmt.M = _prmt.N.size() * (Collections.max(_prmt.t_ij.values()) + Collections.max(_prmt.ga_i.values()));
            //
            ArrayList aeN;
            AEI aei;
            _prmt.Indices_AEI = new HashMap<>();
            for (int _a : _prmt.A) {
                aE = _prmt.E_a.get(_a);
                for (Object _e : aE) {
                    aeN = _prmt.N_ae.get(_prmt.get_AE(_a, _e));
                    for (Object i: aeN) {
                        for (Object j: aeN) {
                            aeij = _prmt.get_AEIJ(_a, _e, i, j);
                            if (aeij == null) {
                                aeij = new AEIJ(_a, _e, i, j);
                                _prmt.Indices_AEIJ.put(aeij.get_label(), aeij);
                            }
                        }
                        aei = new AEI(_a, _e, i);
                        _prmt.Indices_AEI.put(aei.get_label(), aei);
                    }
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return _prmt;
    }

    public static Parameter loadPrmt(Path fpath) {
        Parameter _prmt = null;
        try {
            ObjectInputStream os = new ObjectInputStream(new FileInputStream(fpath.toFile()));
            _prmt = (Parameter) os.readObject();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return _prmt;
    }

    public AE get_AE(int a, Object e) {
        return (AE) Indices_AE.get(a).get((Integer) e);
    }

    public AK get_AK(int a, int k) {
        return (AK) Indices_AK.get(a).get(k);
    }

    public AEK get_AEK(int a, Object e, Object k) {
        return (AEK) ((ArrayList) Indices_AEK.get(a).get((Integer) e)).get((Integer) k);
    }

    public AEIJ get_AEIJ(int a, Object e, Object i, Object j) {
        String label = String.format("%d&%d&%s&%s", a, e, i, j);
        return Indices_AEIJ.get(label);
    }

    public AEI get_AEI(int a, Object e, Object i) {
        String label = String.format("%d&%d&%s", a, e, i);
        return Indices_AEI.get(label);
    }

    public IJ get_IJ(Object i, Object j) {
        String label = String.format("%s&%s", i, j);
        return Indices_IJ.get(label);
    }
}
