
package lib.kalu.monitor;

import android.content.Context;
import android.content.Intent;
import android.net.TrafficStats;

import androidx.annotation.NonNull;

final class NetUtil {

    private static String UNIT_DOT = ".";
    private static String UNIT_KB = "Kb/s";
    private static String UNIT_MB = "Mb/s";
    private static long lastTotalRxBytes = 0;
    private static long lastTimeStamp = 0;

    public static void update(@NonNull Context context) {
        try {
            String netSpeed = getNetSpeed();
            String netReceive = getNetReceive();
            String netSent = getNetSent();
            sendBroadcast(context, netSpeed, netReceive, netSent);
        } catch (Exception e) {
            LogUtil.logE("NetUtil => update => " + e.getMessage());
        }
    }

    private static void sendBroadcast(@NonNull Context context,
                                      @NonNull String netSpeed,
                                      @NonNull String netReceive,
                                      @NonNull String netSent) {
        try {
            Intent intent = new Intent();
            intent.setAction("lib.kalu.test.performance.broadcast");
            intent.putExtra("type", "net");
            intent.putExtra("netSpeed", netSpeed);
            intent.putExtra("netReceive", netReceive);
            intent.putExtra("netSent", netSent);
            WatchdogBroadcastManager.getInstance(context).sendBroadcast(intent);
        } catch (Exception e) {
            LogUtil.logE("NetUtil => sendBroadcast => " + e.getMessage());
        }
    }

    private static String getNetReceive() {
        try {
            int myUid = ProcessUtil.getMyUid();
            long received = TrafficStats.getUidRxBytes(myUid) / 1024;// 累计接收数据 KB
            return received + "KB";
        } catch (Exception e) {
            LogUtil.logE("NetUtil => getNetReceive => " + e.getMessage());
            return "NA";
        }
    }

    private static String getNetSent() {
        try {
            int myUid = ProcessUtil.getMyUid();
            long sent = TrafficStats.getUidTxBytes(myUid) / 1024;// 累计发送字节 KB
            return sent + "KB";
        } catch (Exception e) {
            LogUtil.logE("NetUtil => getNetSent => " + e.getMessage());
            return "NA";
        }
    }

    private static String getNetSpeed() {
        try {
            int myUid = ProcessUtil.getMyUid();
            long total = getBytesTotal(myUid);
            long time = System.currentTimeMillis();
            long speed = ((total - lastTotalRxBytes) * 1000 / (time - lastTimeStamp));//毫秒转换
            lastTimeStamp = time;
            lastTotalRxBytes = total;
            if (speed > 1000) {
                long a = speed / 1000;
                long b = speed % 1000;
                if (b <= 0) {
                    return a + UNIT_MB;
                } else if (b >= 100) {
                    return a + UNIT_DOT + 99 + UNIT_MB;
                } else {
                    return a + UNIT_DOT + b + UNIT_MB;
                }
            } else {
                return speed + UNIT_KB;
            }
        } catch (Exception e) {
            LogUtil.logE("NetUtil => getNetSpeed => " + e.getMessage());
            return "NA";
        }
    }

    private static long getBytesTotal(int uid) {
        return TrafficStats.getUidRxBytes(uid) == TrafficStats.UNSUPPORTED ? 0 : (TrafficStats.getTotalRxBytes() / 1024);//转为KB
    }
}
