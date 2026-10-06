package com.kaixin.tvfix;

import java.lang.reflect.Method;

public class SysProp {
    public static String set(String key, String val) {
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method m = c.getDeclaredMethod("set", String.class, String.class);
            m.invoke(null, key, val);
            return key + "=" + val + " (set ok)";
        } catch (Throwable t) {
            return key + " set FAILED: " + t;
        }
    }

    public static String get(String key) {
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method m = c.getDeclaredMethod("get", String.class);
            return (String) m.invoke(null, key);
        } catch (Throwable t) {
            return "?";
        }
    }
}
