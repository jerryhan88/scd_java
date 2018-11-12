package Other;

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
import java.nio.file.Path;
import java.util.Date;


public class Etc {
    private Date startDate;
    private double startCpuTime;
    public Path logPath = null;
    public Path solPathJSN = null;
    public Path solPathSER = null;
    public Path solPathCSV = null;
    public Path solPathTXT = null;


    public Etc() {
        startDate = new Date();
    }

    public Etc(Path _solPathJSON, Path _solPathSER, Path _solPathCSV, Path _solPathTXT) {
        startDate = new Date();
        startCpuTime = getCPU_TS();
        solPathJSN = _solPathJSON;
        solPathSER = _solPathSER;
        solPathCSV = _solPathCSV;
        solPathTXT = _solPathTXT;
    }

    public void setLogPath(Path _logPath) {
        logPath = _logPath;
    }

    private double getCPU_TS() {
        OperatingSystemMXBean operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        return operatingSystemMXBean.getProcessCpuTime() / 1000000000.;
    }

    public double getWallTime() {
        Date endDate = new Date();
        return (endDate.getTime()-startDate.getTime()) / 1000.;
    }

    public double getCpuTime() {
        return getCPU_TS() - startCpuTime;
    }
}
