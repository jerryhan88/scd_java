package Other;

import ilog.cplex.IloCplex;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.json.simple.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Solution implements Serializable {
    public Parameter prmt;
    public Double objV, dualG;
    public Double cpuT, wallT;
    public HashMap<Index.ki, Double> y_ki = new HashMap<>();
    public HashMap<Index.kriT, Double> z_kriT = new HashMap<>();
    public HashMap<Index.kriN, Double> a_kriN = new HashMap<>();
    public HashMap<Index.krij, Double> x_krij = new HashMap<>();

    public void saveSolSER(Path fpath) {
        try {
            ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(fpath.toFile()));
            os.writeObject(this);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void saveSolJSN(Path fpath) {
        JSONObject base = new JSONObject();
        JSONObject _y_ki = new JSONObject();
        JSONObject _z_kriT = new JSONObject();
        JSONObject _a_kriN = new JSONObject();
        JSONObject _x_krij = new JSONObject();
        //
        for (Index.ki key: y_ki.keySet()) {
            _y_ki.put(key.get_label(), y_ki.get(key));
        }
        for (Index.kriT key: z_kriT.keySet()) {
            _z_kriT.put(key.get_label(), z_kriT.get(key));
        }
        for (Index.kriN key: a_kriN.keySet()) {
            _a_kriN.put(key.get_label(), a_kriN.get(key));
        }
        for (Index.krij key: x_krij.keySet()) {
            _x_krij.put(key.get_label(), x_krij.get(key));
        }
        base.put("objV", objV);
        base.put("y_ki", _y_ki);
        base.put("z_kriT", _z_kriT);
        base.put("a_kriN", _a_kriN);
        base.put("x_krij", _x_krij);
        //
        try {
            FileWriter file = new FileWriter(fpath.toFile());
            file.write(base.toJSONString());
            file.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveSolCSV(Path fpath) {
        try {
            CSVPrinter csvPrinter = new CSVPrinter(new FileWriter(fpath.toString()), CSVFormat.DEFAULT
                    .withHeader("objV", "Gap", "eliCpuTime", "eliWallTime"));
            String[] res = {String.format("%f", objV),
                    String.format("%f", dualG),
                    String.format("%f", cpuT),
                    String.format("%f", wallT)};
            csvPrinter.printRecord(res);
            csvPrinter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveSolTXT(Path fpath) {
        try {
            BufferedWriter bw = Files.newBufferedWriter(Paths.get(fpath.toString()));
            String logContents = "Summary\n";
            logContents += String.format("\t Cpu Time: %f\n", cpuT);
            logContents += String.format("\t Wall Time: %f\n", wallT);
            logContents += String.format("\t ObjV: %.3f\n", objV);
            logContents += String.format("\t Gap: %.3f\n", dualG);
            logContents += "\n";
            //
            logContents += "Details\n";
            Index.ki ki;
            Index.kr kr;
            Index.kriN kriN;
            Index.krij krij;
            ArrayList<Integer> kR;
            ArrayList<String> krC, krN;
            String o_kr, d_kr;
            for (int k: prmt.K) {
                ArrayList<Integer> assignedTasks = new ArrayList<>();
                ArrayList<String> _assignedTasks = new ArrayList<>();
                Set<String> meaninglessNodes = new HashSet<>();
                for (int i: prmt.T) {
                    ki = new Index.ki(k, i);
                    if (y_ki.get(ki) > 0.5) {
                        assignedTasks.add(i);
                        _assignedTasks.add(String.format("%d", i));
                    } else {
                        meaninglessNodes.add(prmt.h_i.get(i));
                        meaninglessNodes.add(prmt.n_i.get(i));
                    }
                }
                logContents += String.format("A%d: ", k);
                logContents += "[" + String.join(",", _assignedTasks) + "]\n";
                kR = prmt.R_k.get(k);
                for (int r : kR) {
                    kr = new Index.kr(k, r);
                    krC = prmt.C_kr.get(kr);
                    krN = prmt.N_kr.get(kr);
                    o_kr = String.format("s0_%d_%d", k, r);
                    d_kr = String.format("s%d_%d_%d", krC.size() - 1, k, r);
                    HashMap<String, String> _route = new HashMap<>();
                    for (String i: krN) {
                        for (String j: krN) {
                            krij = new Index.krij(k, r, i, j);
                            if (x_krij.get(krij) > 0.5) {
                                _route.put(i, j);
                            }
                        }
                    }
                    String i = o_kr;
                    String route = "";
                    ArrayList<String> _accomplishedTasks = new ArrayList<>();
                    while (!i.equals(d_kr)) {
                        if (!meaninglessNodes.contains(i)) {
                            kriN = new Index.kriN(k, r, i);
                            route += String.format("%s(%.2f)-", i, a_kriN.get(kriN));
                            if (i.startsWith("n")) {
                                _accomplishedTasks.add(i.substring("n".length()));
                            }
                        }
                        i = _route.get(i);
                    }
                    kriN = new Index.kriN(k, r, i);
                    route += String.format("%s(%.2f)", i, a_kriN.get(kriN));
                    logContents += String.format("\t R%d%s: %s\n", r,
                            "[" + String.join(",", _accomplishedTasks) + "]"
                            , route);
                }
            }
            bw.write(logContents);
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Solution loadSol(Path fpath) {
        Solution _sol = null;
        try {
            ObjectInputStream os = new ObjectInputStream(new FileInputStream(fpath.toFile()));
            _sol = (Solution) os.readObject();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return _sol;
    }
}
