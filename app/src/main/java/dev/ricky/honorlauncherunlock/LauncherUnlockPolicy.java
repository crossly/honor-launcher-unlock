package dev.ricky.honorlauncherunlock;

public final class LauncherUnlockPolicy {
    private static final String ANDROID_PACKAGE = "android";
    private static final String ANDROID_PROCESS = "android";
    private static final String PERMISSION_CONTROLLER_PACKAGE = "com.android.permissioncontroller";
    private static final String PREFERRED_ACTIVITY_HELPER_CLASS =
            "com.android.server.pm.PreferredActivityHelper";
    private static final String ALREADY_DEFAULT_HOME_METHOD = "isAlreadyDefaultHomeActivity";
    private static final String ANTI_MAL_PROTECTION_CLASS = "r2.b";
    private static final String ALLOW_LAUNCHER_METHOD = "d";
    private static final String RESET_LAUNCHER_METHOD = "c";

    private LauncherUnlockPolicy() {
    }

    public static boolean isSystemServerTarget(String packageName, String processName) {
        return ANDROID_PACKAGE.equals(packageName) && ANDROID_PROCESS.equals(processName);
    }

    public static boolean isPermissionControllerTarget(String packageName) {
        return PERMISSION_CONTROLLER_PACKAGE.equals(packageName);
    }

    public static String preferredActivityHelperClassName() {
        return PREFERRED_ACTIVITY_HELPER_CLASS;
    }

    public static String alreadyDefaultHomeMethodName() {
        return ALREADY_DEFAULT_HOME_METHOD;
    }

    public static String antiMalProtectionClassName() {
        return ANTI_MAL_PROTECTION_CLASS;
    }

    public static String allowLauncherMethodName() {
        return ALLOW_LAUNCHER_METHOD;
    }

    public static String resetLauncherMethodName() {
        return RESET_LAUNCHER_METHOD;
    }
}
