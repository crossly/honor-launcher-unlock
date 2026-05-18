package dev.ricky.honorlauncherunlock;

public final class LauncherUnlockPolicy {
    private static final String ANDROID_PACKAGE = "android";
    private static final String ANDROID_PROCESS = "android";
    private static final String PERMISSION_CONTROLLER_PACKAGE = "com.android.permissioncontroller";
    private static final String PREFERRED_ACTIVITY_HELPER_CLASS =
            "com.android.server.pm.PreferredActivityHelper";
    private static final String ALREADY_DEFAULT_HOME_METHOD = "isAlreadyDefaultHomeActivity";
    private static final String[] ROLE_MODEL_CLASSES = {
            "com.android.role.controller.model.Role",
            "com.android.permissioncontroller.role.model.Role"
    };
    private static final String HOME_ROLE = "android.app.role.HOME";

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

    public static String[] roleModelClassNames() {
        return ROLE_MODEL_CLASSES.clone();
    }

    public static String homeRoleName() {
        return HOME_ROLE;
    }
}
