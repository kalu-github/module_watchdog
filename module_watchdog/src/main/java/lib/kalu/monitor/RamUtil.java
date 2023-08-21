package lib.kalu.monitor;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Debug;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

final class RamUtil {

    public static void update(@NonNull Context context, @NonNull int appUid) {
        try {
            // 总共内存
            String memoryTotal = getMemoryTotal();
            // 剩余内存
            String memoryAvail = getMemoryAvail(context);
            // 使用内存
            ArrayList<String> memoryUse = getMemoryUse(context, appUid);
            sendBroadcast(context, memoryTotal, memoryAvail, memoryUse);
        } catch (Exception e) {
            LogUtil.logE("MemoryUtil => update => " + e.getMessage());
        }
    }

    private static void sendBroadcast(@NonNull Context context,
                                      @NonNull String memoryTotal,
                                      @NonNull String memoryAvail,
                                      @NonNull ArrayList<String> memoryUse) {
        try {
            Intent intent = new Intent();
            intent.setAction("lib.kalu.monitor.watchdog.broadcast");
            intent.putExtra("type", "memory");
            intent.putExtra("memoryTotal", memoryTotal);
            intent.putExtra("memoryAvail", memoryAvail);
            intent.putStringArrayListExtra("memoryUse", memoryUse);
            WatchdogBroadcastManager.getInstance(context).sendBroadcast(intent);
        } catch (Exception e) {
        }
    }

    private static ArrayList<String> getMemoryUse(@NonNull Context context, @NonNull int appUid) {
        try {
            ArrayList<String> arrayList = null;
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo runningAppProcessInfo : runningAppProcesses) {
                int uid = runningAppProcessInfo.uid;
                if (uid != appUid)
                    continue;
                if (null == arrayList) {
                    arrayList = new ArrayList<>();
                }
                String processName = runningAppProcessInfo.processName;
                int pid = runningAppProcessInfo.pid;
                int[] pids = {pid};
                Debug.MemoryInfo[] processMemoryInfo = activityManager.getProcessMemoryInfo(pids);
//                processMemoryInfo[0].getTotalSharedDirty();
                int totalPss = processMemoryInfo[0].getTotalPss() / 1024;
                arrayList.add(totalPss + "MB | " + processName);
                LogUtil.logE("MemoryUtil => getMemoryUse => pid = " + pid + ", uid = " + uid + ", processName = " + processName + ", totalPss = " + totalPss);
            }
            if (null == arrayList)
                throw new Exception("arrayList error: null");
            return arrayList;
        } catch (Exception e) {
            LogUtil.logE("MemoryUtil => getMemoryUse => " + e.getMessage());
            return null;
        }
    }

    public static String getMemoryTotal() {
        try {
            String path = "/proc/meminfo";
            FileReader fileReader = new FileReader(path);
            BufferedReader br = new BufferedReader(fileReader, 4096);
            String ramMemorySize = br.readLine().split("\\s+")[1];
            br.close();
            if (null == ramMemorySize)
                throw new Exception();
            int totalRam = (int) Math.ceil((Float.valueOf(Float.parseFloat(ramMemorySize) / (1024)).doubleValue()));
            return totalRam + "MB";
        } catch (Exception e) {
            LogUtil.logE("MemoryUtil => getMemoryTotal => " + e.getMessage());
            return "NA";
        }
    }

    private static String getMemoryAvail(@NonNull Context context) {
        try {
            final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);
            long avaliMem = memoryInfo.availMem / (1024 * 1024);
//            Log.i(tag,"系统剩余内存:"+(info.availMem >> 10)+"k");
//            Log.i(tag,"系统是否处于低内存执行："+info.lowMemory);
//            Log.i(tag,"当系统剩余内存低于"+info.threshold+"时就看成低内存执行");
            return avaliMem + "MB";
        } catch (Exception e) {
            LogUtil.logE("MemoryUtil => getMemoryAvail => " + e.getMessage());
            return "NA";
        }
    }
}
