package Other;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class Parameter implements Serializable {
    public String problemName;
    public ArrayList<Integer> T = new ArrayList<>();;
    public ArrayList<Double> w_i = new ArrayList<>();;
    //
    public ArrayList<String> N = new ArrayList<>();
    public HashMap<String, Double> alpha_i = new HashMap<>();
    public HashMap<String, Double> beta_i = new HashMap<>();
    public HashMap<String, Double> c_i = new HashMap<>();
    //
    public ArrayList<Integer> K = new ArrayList<>();
    public ArrayList<ArrayList> R_k = new ArrayList<>();
    public HashMap<Index.kr, ArrayList> C_kr = new HashMap<>();
    public HashMap<Index.kr, ArrayList> N_kr = new HashMap<>();
    public HashMap<Index.kr, Double> gamma_kr = new HashMap<>();
    public HashMap<Index.kr, Double> l_kr = new HashMap<>();
    public HashMap<Index.kr, Double> u_kr = new HashMap<>();
    //
    public HashMap<Index.ij, Double> t_ij = new HashMap<>();
    public HashMap<Index.krij, Long> p_krij = new HashMap<>();


    public void savePrmt(Path fpath) {
        try {
            ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(fpath.toFile()));
            os.writeObject(this);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void handleJSONObject(JSONObject jsonObjWhole, HashMap prmtRef, String prmtName) {
        JSONObject obj = (JSONObject) jsonObjWhole.get(prmtName);
        for (String key: (Set<String>) obj.keySet()) {
            prmtRef.put(key, obj.get(key));
        }
    }

    public static Parameter json2ser(Path fpath) {
        Parameter _prmt = new Parameter();
        try {
            JSONParser parser = new JSONParser();
            JSONObject jsonObjWhole = (JSONObject) parser.parse(new FileReader(fpath.toFile()));
            _prmt.problemName = (String) jsonObjWhole.get("problemName");
            JSONArray array;
            int k, r;
            //
            array = (JSONArray) jsonObjWhole.get("T");
            for (Object i : array) {
                _prmt.T.add(((Long) i).intValue());
            }
            array = (JSONArray) jsonObjWhole.get("w_i");
            for (Object i : array) {
                _prmt.w_i.add((Double) i);
            }
            //
            array = (JSONArray) jsonObjWhole.get("N");
            for (Object i : array) {
                _prmt.N.add((String) i);
            }
            handleJSONObject(jsonObjWhole, _prmt.alpha_i, "alpha_i");
            handleJSONObject(jsonObjWhole, _prmt.beta_i, "beta_i");
            handleJSONObject(jsonObjWhole, _prmt.c_i, "c_i");
            //
            array = (JSONArray) jsonObjWhole.get("K");
            for (Object i : array) {
                _prmt.K.add(((Long) i).intValue());
            }
            JSONArray arrayOut = (JSONArray) jsonObjWhole.get("R_k");
            for (Object anArrayOut : arrayOut) {
                JSONArray arrayIn = (JSONArray) anArrayOut;
                ArrayList<Integer> arrayInNew = new ArrayList<>();
                for (Object anArrayIn : arrayIn) {
                    arrayInNew.add(((Long) anArrayIn).intValue());
                }
                _prmt.R_k.add(arrayInNew);
            }
            //
            JSONObject _C_kr = (JSONObject) jsonObjWhole.get("C_kr");
            JSONObject _N_kr = (JSONObject) jsonObjWhole.get("N_kr");
            JSONObject _gamma_kr = (JSONObject) jsonObjWhole.get("gamma_kr");
            JSONObject _l_kr = (JSONObject) jsonObjWhole.get("l_kr");
            JSONObject _u_kr = (JSONObject) jsonObjWhole.get("u_kr");
            //
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
                _prmt.gamma_kr.put(kr, (double) _gamma_kr.get(key));
                _prmt.l_kr.put(kr, (double) _l_kr.get(key));
                _prmt.u_kr.put(kr, (double) _u_kr.get(key));
            }
            //
            JSONObject _t_ij = (JSONObject) jsonObjWhole.get("t_ij");
            for (String key: (Set<String>) _t_ij.keySet()) {
                String [] ij = key.split("&");
                _prmt.t_ij.put(new Index.ij(ij[0], ij[1]), (double) _t_ij.get(key));
            }
            //
            JSONObject _p_krij = (JSONObject) jsonObjWhole.get("p_krij");
            for (String key: (Set<String>) _p_krij.keySet()) {
                String[] _key = key.split("&");
                k = Integer.parseInt(_key[0]);
                r = Integer.parseInt(_key[1]);
                String i = _key[2];
                String j = _key[3];
                _prmt.p_krij.put(new Index.krij(k, r, i, j), (long) _p_krij.get(key));
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
