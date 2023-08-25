
package lib.kalu.monitor;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;

final class CpuUtil {

    // Linux 默认时钟频率为 100 HZ
    private static int CLK_TCK = 100;
    // 上次的CPU使用时间
    private static long lastUsedCPUTime = 0L;
    // 上次的CPU记录时间
    private static long lastRecordCPUTime = SystemClock.uptimeMillis();
    // 存储 CPU 使用信息的文件
    // 获取 时钟频率 的命令
    private static String[] cmd = {"sh", "-c", "getconf CLK_TCK"};
    private static DecimalFormat mDecimalFormat = new DecimalFormat();

    public static void update(@NonNull Context context, @NonNull int pid, @NonNull int uid) {

        try {
            String cpuUseApp = getCpuUseApp(pid);
            String cpuUseAlive = getCpuUseAlive();
            sendBroadcast(context, cpuUseApp, cpuUseAlive);
        } catch (Exception e) {
            LogUtil.logE("CpuUtil => update => " + e.getMessage());
        }
    }

    private static void sendBroadcast(@NonNull Context context,
                                      @NonNull String cpuUseApp,
                                      @NonNull String cpuUseAlive) {
        try {
            Intent intent = new Intent();
            intent.setAction("lib.kalu.monitor.watchdog.broadcast");
            intent.putExtra("type", "cpu");
            intent.putExtra("cpuUseApp", cpuUseApp);
            intent.putExtra("cpuUseAlive", cpuUseAlive);
            WatchdogBroadcastManager.getInstance(context).sendBroadcast(intent);
        } catch (Exception e) {
            LogUtil.logE("CpuUtil => sendBroadcast => " + e.getMessage());
        }
    }

//    /**
//     * 获取当前进程的CPU使用率
//     *
//     * @return CPU的使用率
//     */
//    public static float getCpuUseApp() {
//        float totalCpuTime1 = getTotalCpuTime();
//        float processCpuTime1 = getAppCpuTime();
//        try {
//            Thread.sleep(360);
//        } catch (Exception e) {
//        }
//        float totalCpuTime2 = getTotalCpuTime();
//        float processCpuTime2 = getAppCpuTime();
//        float cpuRate = 100 * (processCpuTime2 - processCpuTime1) / (totalCpuTime2 - totalCpuTime1);
//        return cpuRate;
//    }

    private static String getCpuUseApp(@NonNull int pid) {
        try {
            File file = new File("/proc/" + pid + "/stat");
            FileInputStream inputStream = new FileInputStream(file);
            InputStreamReader reader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(reader);
            String line = bufferedReader.readLine();
            String[] res = line.split(" ");
            // 进程在用户态运行的时间
            long uTime = Long.parseLong(res[13]);
            // 进程在内核态运行的时间
            long sTime = Long.parseLong(res[14]);
            // 本次运行总时间
            long usedTime = (uTime + sTime) - lastUsedCPUTime;

            long currentTime = SystemClock.uptimeMillis();
            // 获得已过去时间 ms ==> s ==> Clock Tick
            float elapsedTime = (currentTime - lastRecordCPUTime) / 1000f * CLK_TCK;

            lastUsedCPUTime = uTime + sTime;
            lastRecordCPUTime = currentTime;

            float usage = usedTime / elapsedTime * 100;
            float result = usage / Runtime.getRuntime().availableProcessors();

            mDecimalFormat.setGroupingUsed(false);
            mDecimalFormat.setMaximumFractionDigits(2);
            mDecimalFormat.setMinimumFractionDigits(0);
            String use = mDecimalFormat.format((double) result);
            String cpuUse = use + "%";
            return cpuUse;
        } catch (Exception e) {
            return "NA";
        }
    }

    private static String getCpuUseAlive() {
        try {
            long[] cpuTime1 = getTotalCpuTime();
            long totalCpuTime1 = cpuTime1[0];
            float totalUsedCpuTime1 = cpuTime1[0] - cpuTime1[1];
            Thread.sleep(100);
            long[] cpuTime2 = getTotalCpuTime();
            long totalCpuTime2 = cpuTime2[0];
            float totalUsedCpuTime2 = cpuTime2[0] - cpuTime2[1];
            float result = 100 * (totalUsedCpuTime2 - totalUsedCpuTime1) / (totalCpuTime2 - totalCpuTime1);
            mDecimalFormat.setGroupingUsed(false);
            mDecimalFormat.setMaximumFractionDigits(2);
            mDecimalFormat.setMinimumFractionDigits(0);
            String use = mDecimalFormat.format((double) (100 - result));
            String cpuUse = use + "%";
            return cpuUse;
        } catch (Exception e) {
            return "NA";
        }
    }

    /**
     * 获取系统总CPU使用时间
     *
     * @return 系统CPU总的使用时间
     */
    private static long[] getTotalCpuTime() {
        try {
            FileInputStream fileInputStream = new FileInputStream("/proc/stat");
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader reader = new BufferedReader(inputStreamReader, 1000);
            String load = reader.readLine();
            reader.close();
            inputStreamReader.close();
            fileInputStream.close();
            String[] cpuInfos = load.split(" ");
            long usertime = Long.parseLong(cpuInfos[2]);
            long nicetime = Long.parseLong(cpuInfos[3]);
            long systemtime = Long.parseLong(cpuInfos[4]);
            long idletime = Long.parseLong(cpuInfos[5]);
            long iowaittime = Long.parseLong(cpuInfos[6]);
            long irqtime = Long.parseLong(cpuInfos[7]);
            long softirqtime = Long.parseLong(cpuInfos[8]);
            long totalCpu = usertime
                    + nicetime + systemtime
                    + idletime + iowaittime
                    + irqtime + softirqtime;
            return new long[]{totalCpu, idletime};
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取当前进程的CPU使用时间
     *
     * @return 当前进程的CPU使用时间
     */
    private static long getAppCpuTime() {
        // 获取应用占用的CPU时间
        String[] cpuInfos = null;
        try {
            int pid = android.os.Process.myPid();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream("/proc/" + pid + "/stat")), 1000);
            String load = reader.readLine();
            reader.close();
            cpuInfos = load.split(" ");
        } catch (Exception ex) {
        }
        long appCpuTime = Long.parseLong(cpuInfos[13])
                + Long.parseLong(cpuInfos[14]) + Long.parseLong(cpuInfos[15])
                + Long.parseLong(cpuInfos[16]);
        return appCpuTime;
    }

//    static Status sStatus = new Status();
//
//    static class Status {
//        public long usertime;
//        public long nicetime;
//        public long systemtime;
//        public long idletime;
//        public long iowaittime;
//        public long irqtime;
//        public long softirqtime;
//
//        public long getTotalTime() {
//            return (usertime + nicetime + systemtime + idletime + iowaittime
//                    + irqtime + softirqtime);
//        }
//    }

//    private static String getCpuUseTotal(@NonNull int pid) {
//        // 安卓8.0以上版本获取cpu使用率
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            java.lang.Process process = null;
//            try {
//                //调用shell 执行 top -n 1
//                process = Runtime.getRuntime().exec("top -n 1");
//                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//                String line;
//                int cpuIndex = -1;
//                while ((line = reader.readLine()) != null) {
//                    line = line.trim();
//                    if (null == line || line.length() == 0) {
//                        continue;
//                    }
//                    int tempIndex = getCPUIndex(line);
//                    if (tempIndex != -1) {
//                        cpuIndex = tempIndex;
//                        continue;
//                    }
//                    if (line.startsWith(String.valueOf(pid))) {
//                        if (cpuIndex == -1) {
//                            continue;
//                        }
//                        String[] param = line.split("\\s+");
//                        if (param.length <= cpuIndex) {
//                            continue;
//                        }
//                        String cpu = param[cpuIndex];
//                        if (cpu.endsWith("%")) {
//                            cpu = cpu.substring(0, cpu.lastIndexOf("%"));
//                        }
//                        float rate = Float.parseFloat(cpu) / Runtime.getRuntime().availableProcessors();
//                        return rate + "%";
//                    }
//                }
//            } catch (Exception e) {
//            } finally {
//                process.destroy();
//            }
//            return 0 + "%";
//        } else {
//            try {
//                long cpuTime = 0;
//                RandomAccessFile randomAccessFile = new RandomAccessFile("/proc/stat", "r");
//                randomAccessFile.seek(0L);
//                String procStatString = randomAccessFile.readLine();
//                String procStats[] = procStatString.split(" ");
//                cpuTime = Long.parseLong(procStats[2]) + Long.parseLong(procStats[3])
//                        + Long.parseLong(procStats[4]) + Long.parseLong(procStats[5])
//                        + Long.parseLong(procStats[6]) + Long.parseLong(procStats[7])
//                        + Long.parseLong(procStats[8]);
//                randomAccessFile.close();
//                return cpuTime + "%";
//            } catch (Exception e) {
//                return "NA";
//            }
//        }
//    }

//    private static int getCPUIndex(@NonNull String data) {
//        try {
//            if (null == data || !data.toLowerCase().contains("cpu"))
//                throw new Exception("data error: " + data);
//            String[] split = data.split("\\s+");
//            int length = split.length;
//            if (length == 0)
//                throw new Exception("length error: " + length);
//            for (int i = 0; i < length; i++) {
//                String s = split[i];
//                if (null == s)
//                    continue;
//                if (s.toLowerCase().contains("cpu")) {
//                    return i;
//                }
//            }
//            throw new Exception("not find");
//        } catch (Exception e) {
//            LogUtil.logE("CpuUtil => getCPUIndex => " + e.getMessage());
//            return -1;
//        }
//    }
}
