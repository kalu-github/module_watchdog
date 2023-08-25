package lib.kalu.monitor;

import android.util.Log;

import androidx.annotation.NonNull;

final class LogUtil {

    private static String TAG = "watch_sdk";

    public static void logE(@NonNull String s) {
        logE(s, null);
    }

    public static void logE(@NonNull String s, @NonNull Exception e) {
        try {
            if (null == s || s.length() == 0)
                throw new Exception();
            if (null == e) {
                Log.e(TAG, s);
            } else {
                Log.e(TAG, s, e);
            }
        } catch (Exception e1) {
        }
    }
}
