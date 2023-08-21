
package lib.kalu.monitor;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
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

    public static void update(@NonNull Context context, @NonNull int pid) {

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
            sendBroadcast(context, cpuUse);
        } catch (Exception e) {
            LogUtil.logE("CpuUtil => update => " + e.getMessage());
        }
    }

    private static void sendBroadcast(@NonNull Context context,
                                      @NonNull String cpuUse) {
        try {
            Intent intent = new Intent();
            intent.setAction("lib.kalu.test.performance.broadcast");
            intent.putExtra("type", "cpu");
            intent.putExtra("cpuUse", cpuUse);
            WatchdogBroadcastManager.getInstance(context).sendBroadcast(intent);
        } catch (Exception e) {
        }
    }
}
