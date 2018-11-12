package Other;

import Index.*;
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
    public HashMap<AK, Double> y_ak = new HashMap<>();
    public HashMap<AEK, Double> z_aek = new HashMap<>();
    public HashMap<AEI, Double> mu_aei = new HashMap<>();
    public HashMap<AEIJ, Double> x_aeij = new HashMap<>();

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
        for (AK key: y_ak.keySet()) {
            _y_ki.put(key.get_label(), y_ak.get(key));
        }
        for (AEK key: z_aek.keySet()) {
            _z_kriT.put(key.get_label(), z_aek.get(key));
        }
        for (AEI key: mu_aei.keySet()) {
            _a_kriN.put(key.get_label(), mu_aei.get(key));
        }
        for (AEIJ key: x_aeij.keySet()) {
            _x_krij.put(key.get_label(), x_aeij.get(key));
        }
        base.put("objV", objV);
        base.put("y_ak", _y_ki);
        base.put("z_aek", _z_kriT);
        base.put("mu_aei", _a_kriN);
        base.put("x_aeij", _x_krij);
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
            AK ak;
            AE ae;
            AEI aei;
            AEIJ aeij;
            ArrayList<Integer> aE;
            ArrayList<String> aeS, aeN;
            String o_ae, d_ae;
            for (int a: prmt.A) {
                ArrayList<Integer> assignedTasks = new ArrayList<>();
                ArrayList<String> _assignedTasks = new ArrayList<>();
                Set<String> meaninglessNodes = new HashSet<>(prmt.N);
                for (int k: prmt.K) {
                    ak = new AK(a, k);
                    if (y_ak.get(ak) > 0.5) {
                        assignedTasks.add(k);
                        _assignedTasks.add(String.format("%d", k));
                        if (meaninglessNodes.contains(prmt.h_k.get(k))) {
                            meaninglessNodes.remove(prmt.h_k.get(k));
                        }
                        if (meaninglessNodes.contains(prmt.n_k.get(k))) {
                            meaninglessNodes.remove(prmt.n_k.get(k));
                        }
                    }
                }
                logContents += String.format("A%d: ", a);
                logContents += "[" + String.join(",", _assignedTasks) + "]\n";
                aE = prmt.E_a.get(a);
                for (int e : aE) {
                    ae = new AE(a, e);
                    aeS = prmt.S_ae.get(ae);
                    aeN = prmt.N_ae.get(ae);
                    o_ae = String.format("s0_%d_%d", a, e);
                    d_ae = String.format("s%d_%d_%d", aeS.size() - 1, a, e);
                    HashMap<String, String> _route = new HashMap<>();
                    for (String i: aeN) {
                        for (String j: aeN) {
                            aeij = new AEIJ(a, e, i, j);
                            if (x_aeij.get(aeij) > 0.5) {
                                _route.put(i, j);
                            }
                        }
                    }
                    String i = o_ae;
                    String route = "";
                    ArrayList<String> _accomplishedTasks = new ArrayList<>();
                    while (!i.equals(d_ae)) {
                        if (!meaninglessNodes.contains(i)) {
                            aei = new AEI(a, e, i);
                            route += String.format("%s(%.2f)-", i, mu_aei.get(aei));
                            if (i.startsWith("n")) {
                                _accomplishedTasks.add(i.substring("n".length()));
                            }
                        }
                        i = _route.get(i);
                    }
                    aei = new AEI(a, e, i);
                    route += String.format("%s(%.2f)", i, mu_aei.get(aei));
                    logContents += String.format("\t R%d%s: %s\n", e,
                            "[" + String.join(",", _accomplishedTasks) + "]"
                            , route);
                }
            }
            bw.write(logContents);
            bw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
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
