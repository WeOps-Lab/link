package com.weops.utils;

public class Platform {
    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("windows");
    }

    public static boolean isMac() {
        return System.getProperty("os.name").toLowerCase().startsWith("mac os x");
    }
}
