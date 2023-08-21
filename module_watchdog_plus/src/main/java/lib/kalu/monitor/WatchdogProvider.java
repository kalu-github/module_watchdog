package lib.kalu.monitor;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Keep
public class WatchdogProvider extends ContentProvider {

    private ExecutorService mExecutorService = Executors.newSingleThreadExecutor();

    private void startThread(@NonNull Context context, @NonNull String packageName) {
        try {
            int myPid = ProcessUtil.getMyPid();
            int myUid = ProcessUtil.getMyUid();
            mExecutorService.submit(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        SystemClock.sleep(100);
                        NetUtil.update(context);
                        CpuUtil.update(context, myPid);
                        RamUtil.update(context, myUid);
                        LogUtil.logE("EmmageeProvider => startThread => run => next");
                    }
                }
            });
        } catch (Exception e) {
            LogUtil.logE("EmmageeProvider => startThread => " + e.getMessage());
        }
    }

    private void closeThread() {
        try {
            mExecutorService.shutdownNow();
        } catch (Exception e) {
            LogUtil.logE("EmmageeProvider => closeThread => " + e.getMessage());
        }
    }

    private void stopWatch() {
        closeThread();
    }

    private void startWatch() {
        try {
            int pid = android.os.Process.myPid();
            Context context = getContext().getApplicationContext();
            String packageName = context.getPackageName();
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo processInfo : runningAppProcesses) {
                if (null == processInfo)
                    continue;
                if (processInfo.pid != pid)
                    continue;
//                String processName = processInfo.processName;
                startThread(context, packageName);
                break;
            }
            throw new Exception("not find");
        } catch (Exception e) {
            Log.e("EmmageeProvider", "startWatch => " + e.getMessage());
        }
    }

    private void registerCallbacks() {
        try {
            Context context = getContext().getApplicationContext();
            if (!(context instanceof Application))
                throw new Exception("context error: not instanceof Application");
            ((Application) context).registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {

                private void removeRegist(@NonNull Activity activity) {
                    try {
                        ViewGroup decorGroup = (ViewGroup) activity.getWindow().getDecorView();
                        if (null == decorGroup)
                            throw new Exception("decorGroup error: " + decorGroup);
                        BroadcastReceiver broadcastReceiver = (BroadcastReceiver) decorGroup.getTag(R.id.common_floating_root);
                        if (null == broadcastReceiver)
                            throw new Exception("broadcastReceiver error: null");
                        WatchdogBroadcastManager.getInstance(activity).unregisterReceiver(broadcastReceiver);
                    } catch (Exception e) {
                        Log.e("EmmageeProvider", "registerCallbacks => removeRegist => " + e.getMessage());
                    }
                }

                private void addRegist(@NonNull Activity activity) {
                    try {
                        // 1
                        ViewGroup decorGroup = (ViewGroup) activity.getWindow().getDecorView();
                        if (null == decorGroup)
                            throw new Exception("decorGroup error: " + decorGroup);
                        // 2
                        ViewGroup viewGroup = decorGroup.findViewById(R.id.common_floating_root);
                        if (null == viewGroup) {
                            View inflate = LayoutInflater.from(activity).inflate(R.layout.common_floating, null);
                            ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                            inflate.setLayoutParams(layoutParams);
                            decorGroup.addView(inflate);
                        }
                        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                String action = intent.getAction();
                                if ("lib.kalu.test.performance.broadcast".equals(action)) {

                                    String type = intent.getStringExtra("type");
                                    if ("memory".equals(type)) {
                                        // 总共内存
                                        String memoryTotal = intent.getStringExtra("memoryTotal");
                                        // 剩余内存
                                        String memoryAvail = intent.getStringExtra("memoryAvail");
                                        // 使用内存
                                        ArrayList<String> memoryUse = intent.getStringArrayListExtra("memoryUse");
                                        StringBuilder stringBuilder = new StringBuilder();
                                        stringBuilder.append("总计内存：");
                                        stringBuilder.append(memoryTotal);
                                        stringBuilder.append("\n");
                                        stringBuilder.append("剩余内存：");
                                        stringBuilder.append(memoryAvail);
                                        if (null == memoryUse || memoryUse.size() == 0) {
                                            stringBuilder.append("\n");
                                            stringBuilder.append("使用内存：");
                                            stringBuilder.append("NA");
                                        } else {
                                            for (String s : memoryUse) {
                                                stringBuilder.append("\n");
                                                stringBuilder.append("使用内存：");
                                                stringBuilder.append(s);
                                            }
                                        }
                                        String s = stringBuilder.toString();
                                        ViewGroup floatGroup = decorGroup.findViewById(R.id.common_floating_root);
                                        TextView textView = floatGroup.findViewById(R.id.common_floating_memory);
                                        textView.setText(s);
                                    } else if ("cpu".equals(type)) {
                                        // 总共内存
                                        String cpuUse = intent.getStringExtra("cpuUse");
                                        StringBuilder stringBuilder = new StringBuilder();
                                        stringBuilder.append("剩余CPU：");
                                        stringBuilder.append("NA");
                                        stringBuilder.append("\n");
                                        stringBuilder.append("使用CPU：");
                                        stringBuilder.append(cpuUse);
                                        String s = stringBuilder.toString();
                                        ViewGroup floatGroup = decorGroup.findViewById(R.id.common_floating_root);
                                        TextView textView = floatGroup.findViewById(R.id.common_floating_cpu);
                                        textView.setText(s);
                                    } else if ("net".equals(type)) {
                                        // 当前网速
                                        String netSpeed = intent.getStringExtra("netSpeed");
                                        // 接收流量
                                        String netReceive = intent.getStringExtra("netReceive");
                                        // 发送流量
                                        String netSent = intent.getStringExtra("netSent");
                                        StringBuilder stringBuilder = new StringBuilder();
                                        stringBuilder.append("接收流量：");
                                        stringBuilder.append(netReceive);
                                        stringBuilder.append("\n");
                                        stringBuilder.append("发送流量：");
                                        stringBuilder.append(netSent);
                                        stringBuilder.append("\n");
                                        stringBuilder.append("当前网速：");
                                        stringBuilder.append(netSpeed);
                                        String s = stringBuilder.toString();
                                        ViewGroup floatGroup = decorGroup.findViewById(R.id.common_floating_root);
                                        TextView textView = floatGroup.findViewById(R.id.common_floating_net);
                                        textView.setText(s);
                                    }
                                }
                            }
                        };
                        IntentFilter intentFilter = new IntentFilter("lib.kalu.test.performance.broadcast");
                        WatchdogBroadcastManager.getInstance(activity).registerReceiver(broadcastReceiver, intentFilter);
                        decorGroup.setTag(R.id.common_floating_root, broadcastReceiver);
                    } catch (Exception e) {
                        Log.e("EmmageeProvider", "registerCallbacks => addRegist => " + e.getMessage());
                    }
                }

                @Override
                public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
                    Log.e("EmmageeProvider", "registerCallbacks => onActivityCreated => activity = " + activity);
                    addRegist(activity);
                }

                @Override
                public void onActivityDestroyed(@NonNull Activity activity) {
                    Log.e("EmmageeProvider", "registerCallbacks => onActivityDestroyed => activity = " + activity);
                    removeRegist(activity);
                }

                @Override
                public void onActivityStarted(@NonNull Activity activity) {
                }

                @Override
                public void onActivityResumed(@NonNull Activity activity) {
                }

                @Override
                public void onActivityPaused(@NonNull Activity activity) {
                }

                @Override
                public void onActivityStopped(@NonNull Activity activity) {
                }

                @Override
                public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {
                }
            });
        } catch (Exception e) {
        }
    }


    @Override
    public boolean onCreate() {
        startWatch();
        registerCallbacks();
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] strings, @Nullable String s, @Nullable String[] strings1, @Nullable String s1) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String s, @Nullable String[] strings) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) {
        return 0;
    }

//    public boolean isServiceExisted(String className) {
//
//        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
//
//        List serviceList = am.getRunningServices(Integer.MAX_VALUE);
//
//        int myUid = android.os.Process.myUid();
//
//        for (ActivityManager.RunningServiceInfo runningServiceInfo : serviceefccKJimYkList) {
//
//            if (runningServiceInfo.uid == myUid && runningServiceInfo.service.getClassName().equals(className)) {
//
//                return true;
//
//            }
//
//        }
//
//        return false;
//
//    }
}
