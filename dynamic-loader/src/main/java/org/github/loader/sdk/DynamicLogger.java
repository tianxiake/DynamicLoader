package org.github.loader.sdk;

import android.util.Log;

/**
 * @author LiuQiang on 2016.09.21
 */

public class DynamicLogger {
    /**
     * 小包 SDK 的日志开关
     */
    private static boolean LOG_ENABLE = true;

    public static void logEnable() {
        LOG_ENABLE = true;
    }

    public static boolean isLogEnable(){
        return LOG_ENABLE;
    }

    private DynamicLogger() {
    }

    public static void verbose(String tag, String msg) {
        if (LOG_ENABLE) {
            Log.v(tag, msg);
        }
    }

    public static void debug(String tag, String msg) {
        if (LOG_ENABLE) {
            Log.v(tag, msg);
        }
    }

    public static void info(String tag, String msg) {
        if (LOG_ENABLE) {
            Log.i(tag, msg);
        }
    }

    public static void warn(String tag, String msg) {
        if (LOG_ENABLE) {
            Log.w(tag, msg);
        }
    }

    public static void warn(String tag, String msg, Throwable throwable) {
        if (LOG_ENABLE) {
            Log.v(tag, msg, throwable);
        }
    }

    public static void error(String tag, String msg) {
        if (LOG_ENABLE) {
            Log.e(tag, msg);
        }
    }

    public static void error(String tag, String msg, Throwable throwable) {
        if (LOG_ENABLE) {
            Log.e(tag, msg, throwable);
        }
    }
}
