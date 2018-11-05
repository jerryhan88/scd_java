package Other;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Parameter implements Serializable {
    public String problemName;
    public ArrayList<Integer> H = new ArrayList<>();
    public ArrayList<Integer> T = new ArrayList<>();
    public ArrayList<String> h_i = new ArrayList<>();
    public ArrayList<String> n_i = new ArrayList<>();
    public ArrayList<Double> w_i = new ArrayList<>();
    public ArrayList<String> N = new ArrayList<>();
    public ArrayList<Integer> K = new ArrayList<>();
    public ArrayList<Double> v_k = new ArrayList<>();
    public ArrayList<ArrayList> R_k = new ArrayList<>();
    public HashMap<Index.kr, Double> r_kr = new HashMap<>();
    public HashMap<Index.kr, Double> l_kr = new HashMap<>();
    public HashMap<Index.kr, Double> u_kr = new HashMap<>();
    public HashMap<Index.kr, ArrayList> C_kr = new HashMap<>();
    public HashMap<Index.kr, ArrayList> N_kr = new HashMap<>();
    public HashMap<Index.kr, ArrayList> F_kr = new HashMap<>();
    public HashMap<Index.krij, Long> p_krij = new HashMap<>();
    public HashMap<String, Double> a_i = new HashMap<>();
    public HashMap<String, Double> b_i = new HashMap<>();
    public HashMap<String, Double> c_i = new HashMap<>();
    public HashMap<Index.ij, Double> t_ij = new HashMap<>();
    public HashMap<Index.kr, ArrayList> uF_kr = new HashMap<>();


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
            parseJsonArray(jsonBase, _prmt.T, "T", "I");
            parseJsonArray(jsonBase, _prmt.h_i, "h_i", "S");
            parseJsonArray(jsonBase, _prmt.n_i, "n_i", "S");
            parseJsonArray(jsonBase, _prmt.w_i, "w_i", "D");
            parseJsonArray(jsonBase, _prmt.N, "N", "S");
            parseJsonArray(jsonBase, _prmt.K, "K", "I");
            parseJsonArray(jsonBase, _prmt.v_k, "v_k", "D");
            JSONArray arrayOut = (JSONArray) jsonBase.get("R_k");
            for (Object anArrayOut : arrayOut) {
                JSONArray arrayIn = (JSONArray) anArrayOut;
                ArrayList<Integer> arrayInNew = new ArrayList<>();
                for (Object anArrayIn : arrayIn) {
                    arrayInNew.add(((Long) anArrayIn).intValue());
                }
                _prmt.R_k.add(arrayInNew);
            }
            //
            JSONObject _C_kr = (JSONObject) jsonBase.get("C_kr");
            JSONObject _N_kr = (JSONObject) jsonBase.get("N_kr");
            JSONObject _F_kr = (JSONObject) jsonBase.get("F_kr");
            JSONObject _r_kr = (JSONObject) jsonBase.get("r_kr");
            JSONObject _l_kr = (JSONObject) jsonBase.get("l_kr");
            JSONObject _u_kr = (JSONObject) jsonBase.get("u_kr");
            //
            int k, r;
            Index.kr kr;
            JSONArray arrayJ;
            for (String key: (Set<String>) _C_kr.keySet()) {
                String[] _key = key.split("&");
                k = Integer.parseInt(_key[0]);
                r = Integer.parseInt(_key[1]);
                kr = new Index.kr(k, r);
                //
                ArrayList<String> krC = new ArrayList<>();
                arrayJ = (JSONArray) _C_kr.get(key);
                for (Object anArrayJ : arrayJ) {
                    krC.add((String) anArrayJ);
                }
                _prmt.C_kr.put(kr, krC);
                ArrayList<String> krN = new ArrayList<>();
                arrayJ = (JSONArray) _N_kr.get(key);
                for (Object anArrayJ : arrayJ) {
                    krN.add((String) anArrayJ);
                }
                _prmt.N_kr.put(kr, krN);
                //
                ArrayList<Integer> krF = new ArrayList<>();
                arrayJ = (JSONArray) _F_kr.get(key);
                for (Object anArrayJ : arrayJ) {
                    krF.add(((Long) anArrayJ).intValue());
                }
                _prmt.F_kr.put(kr, krF);
                //
                _prmt.r_kr.put(kr, (double) _r_kr.get(key));
                _prmt.l_kr.put(kr, (double) _l_kr.get(key));
                _prmt.u_kr.put(kr, (double) _u_kr.get(key));
            }
            JSONObject _p_krij = (JSONObject) jsonBase.get("p_krij");
            for (String key: (Set<String>) _p_krij.keySet()) {
                String[] _key = key.split("&");
                k = Integer.parseInt(_key[0]);
                r = Integer.parseInt(_key[1]);
                String i = _key[2];
                String j = _key[3];
                _prmt.p_krij.put(new Index.krij(k, r, i, j), (long) _p_krij.get(key));
            }
            parseJsonObject(jsonBase, _prmt.a_i, "a_i");
            parseJsonObject(jsonBase, _prmt.b_i, "b_i");
            parseJsonObject(jsonBase, _prmt.c_i, "c_i");
            JSONObject _t_ij = (JSONObject) jsonBase.get("t_ij");
            for (String key: (Set<String>) _t_ij.keySet()) {
                String [] ij = key.split("&");
                _prmt.t_ij.put(new Index.ij(ij[0], ij[1]), (double) _t_ij.get(key));
            }
            //
            Index.kr _kr;
            ArrayList<Integer> kR;
            HashSet<Integer> krF;
            for (int _k : _prmt.K) {
                kR = _prmt.R_k.get(_k);
                for (int _r : kR) {
                    _kr = new Index.kr(_k, _r);
                    krF = new HashSet<>(_prmt.F_kr.get(_kr));
                    ArrayList<Integer> kr_uF = new ArrayList<>();
                    for (int _i: _prmt.T) {
                        if (!krF.contains(_i)) {
                            kr_uF.add(_i);
                        }
                    }
                     _prmt.uF_kr.put(_kr, kr_uF);
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
}
