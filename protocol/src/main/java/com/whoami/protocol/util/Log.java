package com.whoami.protocol.util;

/**
 * Tiny logging facade. Silent by default so the test output stays clean.
 * Enabled in real runtimes (MainServer / Launcher) or via -Dwhoami.verbose=true.
 */
public final class Log {

    private static volatile boolean enabled = Boolean.getBoolean("whoami.verbose");

    private Log() {
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void info(String message) {
        if (enabled) {
            System.out.println(message);
        }
    }

    public static void error(String message) {
        if (enabled) {
            System.err.println(message);
        }
    }

    public static void error(String message, Throwable cause) {
        if (enabled) {
            System.err.println(message + ": " + cause.getMessage());
        }
    }
}
