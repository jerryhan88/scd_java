package Other;

import java.io.*;
import java.nio.file.Path;

import org.json.simple.*;
import org.json.simple.parser.*;
import java.util.*;


public class ParameterParser {
    JSONParser parser = new JSONParser();
    JSONObject jsonObjWhole;
    Parameter prmt;

    public ParameterParser(Path fpath) {
        try {
            Object obj = parser.parse(new FileReader(fpath.toFile()));
            jsonObjWhole = (JSONObject) obj;

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void handleJSONObject(HashMap prmtRef, String prmtName) {
        JSONObject obj = (JSONObject) jsonObjWhole.get(prmtName);
        for (String key: (Set<String>) obj.keySet()) {
            prmtRef.put(key, obj.get(key));
        }
    }

    public Parameter getPrmt() {
        prmt = new Parameter();
        prmt.problemName = (String) jsonObjWhole.get("problemName");
        //
        JSONArray array;
        int k, r;
        //
        array = (JSONArray) jsonObjWhole.get("T");
        for (Object i : array) {
            prmt.T.add(((Long) i).intValue());
        }
        array = (JSONArray) jsonObjWhole.get("w_i");
        for (Object i : array) {
            prmt.w_i.add((Double) i);
        }
        //
        array = (JSONArray) jsonObjWhole.get("N");
        for (Object i : array) {
            prmt.N.add((String) i);
        }
        handleJSONObject(prmt.alpha_i, "alpha_i");
        handleJSONObject(prmt.beta_i, "beta_i");
        handleJSONObject(prmt.c_i, "c_i");
        //
        array = (JSONArray) jsonObjWhole.get("K");
        for (Object i : array) {
            prmt.K.add(((Long) i).intValue());
        }
        JSONArray arrayOut = (JSONArray) jsonObjWhole.get("R_k");
        for (Object anArrayOut : arrayOut) {
            JSONArray arrayIn = (JSONArray) anArrayOut;
            ArrayList<Integer> arrayInNew = new ArrayList<>();
            for (Object anArrayIn : arrayIn) {
                arrayInNew.add(((Long) anArrayIn).intValue());
            }
            prmt.R_k.add(arrayInNew);
        }
        //
        JSONObject _C_kr = (JSONObject) jsonObjWhole.get("C_kr");
        JSONObject _N_kr = (JSONObject) jsonObjWhole.get("N_kr");
        JSONObject _gamma_kr = (JSONObject) jsonObjWhole.get("gamma_kr");
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
            prmt.C_kr.put(kr, krC);
            ArrayList<String> krN = new ArrayList<>();
            arrayJ = (JSONArray) _N_kr.get(key);
            for (Object anArrayJ : arrayJ) {
                krN.add((String) anArrayJ);
            }
            prmt.N_kr.put(kr, krN);
            prmt.gamma_kr.put(kr, (double) _gamma_kr.get(key));
            prmt.u_kr.put(kr, (double) _u_kr.get(key));
        }
        //
        JSONObject _t_ij = (JSONObject) jsonObjWhole.get("t_ij");
        for (String key: (Set<String>) _t_ij.keySet()) {
            String [] ij = key.split("&");
            prmt.t_ij.put(new Index.ij(ij[0], ij[1]), (double) _t_ij.get(key));
        }
        //
        JSONObject _p_krij = (JSONObject) jsonObjWhole.get("p_krij");
        for (String key: (Set<String>) _p_krij.keySet()) {
            String[] _key = key.split("&");
            k = Integer.parseInt(_key[0]);
            r = Integer.parseInt(_key[1]);
            String i = _key[2];
            String j = _key[3];
            prmt.p_krij.put(new Index.krij(k, r, i, j), (long) _p_krij.get(key));
        }
        //
        return prmt;
    }
}
