
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
            String cpuUseSystem = getCpuUseSystem(pid);
            sendBroadcast(context, cpuUseApp, cpuUseSystem);
        } catch (Exception e) {
            LogUtil.logE("CpuUtil => update => " + e.getMessage());
        }
    }

    private static void sendBroadcast(@NonNull Context context,
                                      @NonNull String cpuUseApp,
                                      @NonNull String cpuUseSystem) {
        try {
            Intent intent = new Intent();
            intent.setAction("lib.kalu.monitor.watchdog.broadcast");
            intent.putExtra("type", "cpu");
            intent.putExtra("cpuUseApp", cpuUseApp);
            intent.putExtra("cpuUseSystem", cpuUseSystem);
            WatchdogBroadcastManager.getInstance(context).sendBroadcast(intent);
        } catch (Exception e) {
            LogUtil.logE("CpuUtil => sendBroadcast => " + e.getMessage());
        }
    }

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

    private static String getCpuUseSystem(@NonNull int pid) {
        // 安卓8.0以上版本获取cpu使用率
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            java.lang.Process process = null;
            try {
                //调用shell 执行 top -n 1
                process = Runtime.getRuntime().exec("top -n 1");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                int cpuIndex = -1;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (null == line || line.length() == 0) {
                        continue;
                    }
                    int tempIndex = getCPUIndex(line);
                    if (tempIndex != -1) {
                        cpuIndex = tempIndex;
                        continue;
                    }
                    if (line.startsWith(String.valueOf(pid))) {
                        if (cpuIndex == -1) {
                            continue;
                        }
                        String[] param = line.split("\\s+");
                        if (param.length <= cpuIndex) {
                            continue;
                        }
                        String cpu = param[cpuIndex];
                        if (cpu.endsWith("%")) {
                            cpu = cpu.substring(0, cpu.lastIndexOf("%"));
                        }
                        float rate = Float.parseFloat(cpu) / Runtime.getRuntime().availableProcessors();
                        return rate + "%";
                    }
                }
            } catch (Exception e) {
            } finally {
                process.destroy();
            }
            return 0 + "%";
        } else {
            try {
                long cpuTime = 0;
                RandomAccessFile randomAccessFile = new RandomAccessFile("/proc/stat", "r");
                randomAccessFile.seek(0L);
                String procStatString = randomAccessFile.readLine();
                String procStats[] = procStatString.split(" ");
                cpuTime = Long.parseLong(procStats[2]) + Long.parseLong(procStats[3])
                        + Long.parseLong(procStats[4]) + Long.parseLong(procStats[5])
                        + Long.parseLong(procStats[6]) + Long.parseLong(procStats[7])
                        + Long.parseLong(procStats[8]);
                randomAccessFile.close();
                return cpuTime + "%";
            } catch (Exception e) {
                return "NA";
            }
        }
    }

    private static int getCPUIndex(@NonNull String data) {
        try {
            if (null == data || !data.contains("CPU"))
                throw new Exception("data error: " + data);
            String[] split = data.split("\\s+");
            int length = split.length;
            if (length == 0)
                throw new Exception("length error: " + length);
            for (int i = 0; i < length; i++) {
                String s = split[i];
                if (null == s)
                    continue;
                if (s.contains("CPU")) {
                    return i;
                }
            }
            throw new Exception("not find");
        } catch (Exception e) {
            LogUtil.logE("CpuUtil => getCPUIndex => " + e.getMessage());
            return -1;
        }
    }
}
