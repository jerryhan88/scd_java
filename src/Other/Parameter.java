package Other;

import Index.AE;
import Index.AEIJ;
import Index.IJ;
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
            JSONObject _S_ae = (JSONObject) jsonBase.get("S_ae");
            JSONObject _N_ae = (JSONObject) jsonBase.get("N_ae");
            JSONObject _p_ae = (JSONObject) jsonBase.get("p_ae");
            JSONObject _l_kr = (JSONObject) jsonBase.get("l_ae");
            JSONObject _u_kr = (JSONObject) jsonBase.get("u_ae");
            JSONObject _F_ae = (JSONObject) jsonBase.get("F_ae");
            //
            int a, e;
            AE ae;
            JSONArray arrayJ;
            for (String key: (Set<String>) _S_ae.keySet()) {
                String[] _key = key.split("&");
                a = Integer.parseInt(_key[0]);
                e = Integer.parseInt(_key[1]);
                ae = new AE(a, e);
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
                _prmt.c_aeij.put(new AEIJ(a, e, i, j), (long) _c_aeij.get(key));
            }
            parseJsonObject(jsonBase, _prmt.al_i, "al_i");
            parseJsonObject(jsonBase, _prmt.be_i, "be_i");
            parseJsonObject(jsonBase, _prmt.ga_i, "ga_i");
            JSONObject _t_ij = (JSONObject) jsonBase.get("t_ij");
            for (String key: (Set<String>) _t_ij.keySet()) {
                String [] ij = key.split("&");
                _prmt.t_ij.put(new IJ(ij[0], ij[1]), (double) _t_ij.get(key));
            }
            //
            AE _ae;
            ArrayList<Integer> aE;
            HashSet<Integer> aeF;
            for (int _a : _prmt.A) {
                aE = _prmt.E_a.get(_a);
                for (int _e : aE) {
                    _ae = new AE(_a, _e);
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
}
