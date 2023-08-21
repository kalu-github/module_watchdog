package lib.kalu.monitor;

import java.io.IOException;

final class RuntimeUtil {

    public static Runtime getRuntime() {
        try {
            return Runtime.getRuntime();
        } catch (Exception e) {
            LogUtil.logE("RuntimeUtil => getRuntime => " + e.getMessage());
            return null;
        }
    }

    public static void clearLogcat() {
        try {
            getRuntime().exec("logcat -c");
        } catch (IOException e) {
            LogUtil.logE("RuntimeUtil => clearLogcat => " + e.getMessage());
        }
    }

    public static Process getProcess() {
        try {
            return getRuntime().exec("su");
        } catch (IOException e) {
            LogUtil.logE("RuntimeUtil => getProcess => " + e.getMessage());
            return null;
        }
    }
}
