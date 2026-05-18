package dev.ricky.honorlauncherunlock;

public final class LauncherUnlockPolicy {
    private static final String ANDROID_PACKAGE = "android";
    private static final String ANDROID_PROCESS = "android";
    private static final String PREFERRED_ACTIVITY_HELPER_CLASS =
            "com.android.server.pm.PreferredActivityHelper";
    private static final String ALREADY_DEFAULT_HOME_METHOD = "isAlreadyDefaultHomeActivity";

    private LauncherUnlockPolicy() {
    }

    public static boolean isSystemServerTarget(String packageName, String processName) {
        return ANDROID_PACKAGE.equals(packageName) && ANDROID_PROCESS.equals(processName);
    }

    public static String preferredActivityHelperClassName() {
        return PREFERRED_ACTIVITY_HELPER_CLASS;
    }

    public static String alreadyDefaultHomeMethodName() {
        return ALREADY_DEFAULT_HOME_METHOD;
    }
}
