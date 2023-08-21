package lib.kalu.monitor;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

final class ProcessUtil {

     static int getMyUid() {
        try {
            return android.os.Process.myUid();
        } catch (Exception e) {
            LogUtil.logE("ProcessUtil => getMyUid => " + e.getMessage());
            return -1;
        }
    }

     static int getMyPid() {
        try {
            return android.os.Process.myPid();
        } catch (Exception e) {
            LogUtil.logE("ProcessUtil => getMyPid => " + e.getMessage());
            return -1;
        }
    }

     int getPackageUid(@NonNull Context context, @NonNull String packageName) {
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_ACTIVITIES);
            return applicationInfo.uid;
        } catch (Exception e) {
            LogUtil.logE("ProcessUtil => getPackageUid => " + e.getMessage());
            return -1;
        }
    }

     int getPackagePid(@NonNull Context context, @NonNull String packageName) {
        try {
            if (Build.VERSION.SDK_INT < 22) {
                ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                List<ActivityManager.RunningAppProcessInfo> runningAppProcessInfos = activityManager.getRunningAppProcesses();
                for (ActivityManager.RunningAppProcessInfo runningAppProcessInfo : runningAppProcessInfos) {
                    if (null == runningAppProcessInfo)
                        continue;
                    String processName = runningAppProcessInfo.processName;
                    if (null == processName || processName.length() == 0)
                        continue;
                    if (processName.equals(packageName)) {
                        return runningAppProcessInfo.pid;
                    }
                }
            } else {
                Process p = Runtime.getRuntime().exec("top -m 100 -n 1");
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    if (line.contains(packageName)) {
                        line = line.trim();
                        String[] splitLine = line.split("\\s+");
                        if (packageName.equals(splitLine[splitLine.length - 1])) {
                            return Integer.parseInt(splitLine[0]);
                        }
                    }
                }
            }
            throw new Exception("not find");
        } catch (Exception e) {
            LogUtil.logE("ProcessUtil => getPackagePid => " + e.getMessage());
            return -1;
        }
    }
}
