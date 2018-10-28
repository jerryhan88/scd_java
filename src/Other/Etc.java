package Other;

import java.io.File;
import java.nio.file.Path;
import java.util.Date;


public class Etc {
    public Date startDate;
    public long startTimeMs;
    public File logFile = null;
    public Path solPathJSN = null;
    public Path solPathSER = null;
    public Path solPathCSV = null;
    public Path solPathTXT = null;


    public Etc() {
        startDate = new Date();
        startTimeMs = System.currentTimeMillis();
    }

    public Etc(Path _logPath, Path _solPathJSON, Path _solPathSER, Path _solPathCSV, Path _solPathTXT) {
        startDate = new Date();
        startTimeMs = System.currentTimeMillis();
        logFile = _logPath.toFile();
        solPathJSN = _solPathJSON;
        solPathSER = _solPathSER;
        solPathCSV = _solPathCSV;
        solPathTXT = _solPathTXT;
    }

    public double getWallTime() {
        Date endDate = new Date();
        return (endDate.getTime()-startDate.getTime()) / 1000.;
    }

    public double getCpuTime() {
        long endTimeMs = System.currentTimeMillis();
        return (endTimeMs - startTimeMs) / 1000.;
    }
}
