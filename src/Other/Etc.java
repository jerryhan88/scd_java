package Other;

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
import java.nio.file.Path;
import java.util.Date;


public class Etc {

    int timeLimit;
    private final Date startDate;
    private final double startCpuTime;
    private double savedLastTimestamp;
    public boolean trigerTermCondition = false;
    //
    public Path logPath = null;
    public Path lmLogPath = null;
    public Path solPathJSN = null;
    public Path solPathSER = null;
    public Path solPathCSV = null;
    public Path solPathTXT = null;

    public Etc(Path solPathJSN, Path solPathSER, Path solPathCSV, Path solPathTXT) {
        startDate = new Date();
        startCpuTime = getCPU_TS();
        this.solPathJSN = solPathJSN;
        this.solPathSER = solPathSER;
        this.solPathCSV = solPathCSV;
        this.solPathTXT = solPathTXT;
    }

    public void saveTimestamp() {
        savedLastTimestamp = getCpuTime();
    }

    public double getSavedTimestamp() {
        return savedLastTimestamp;
    }

    public void setTimeLimit(int timeLimit) {
        this.timeLimit = timeLimit;
    }

    public int getTimeLimit(){
        return timeLimit;
    }

    public double getWallTime() {
        Date endDate = new Date();
        return (endDate.getTime()-startDate.getTime()) / 1000.;
    }

    private double getCPU_TS() {
        OperatingSystemMXBean operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        return operatingSystemMXBean.getProcessCpuTime() / 1000000000.;
    }

    public double getCpuTime() {
        return getCPU_TS() - startCpuTime;
    }

    public void setLogPath(Path _logPath) {
        logPath = _logPath;
    }

    public void setLmLogPath(Path _logPath) {
        lmLogPath = _logPath;
    }
}
