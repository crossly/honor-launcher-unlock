package dev.ricky.honorlauncherunlock;

public final class LauncherUnlockPolicy {
    private static final String MODULE_PACKAGE = "dev.ricky.honorlauncherunlock";
    private static final String LAUNCHER_PICKER_ACTIVITY =
            "dev.ricky.honorlauncherunlock.LauncherPickerActivity";
    private static final String ANDROID_PACKAGE = "android";
    private static final String ANDROID_PROCESS = "android";
    private static final String SETTINGS_PACKAGE = "com.android.settings";
    private static final String PERMISSION_CONTROLLER_PACKAGE = "com.android.permissioncontroller";
    private static final String PREFERRED_ACTIVITY_HELPER_CLASS =
            "com.android.server.pm.PreferredActivityHelper";
    private static final String ALREADY_DEFAULT_HOME_METHOD = "isAlreadyDefaultHomeActivity";
    private static final String ACTION_HOME_SETTINGS = "android.settings.HOME_SETTINGS";
    private static final String ACTION_MANAGE_DEFAULT_APPS_SETTINGS =
            "android.settings.MANAGE_DEFAULT_APPS_SETTINGS";
    private static final String ACTION_REQUEST_ROLE = "android.app.role.action.REQUEST_ROLE";
    private static final String ROLE_HOME = "android.app.role.HOME";

    private LauncherUnlockPolicy() {
    }

    public static boolean isSystemServerTarget(String packageName, String processName) {
        return ANDROID_PACKAGE.equals(packageName) && ANDROID_PROCESS.equals(processName);
    }

    public static boolean isPermissionControllerTarget(String packageName) {
        return PERMISSION_CONTROLLER_PACKAGE.equals(packageName);
    }

    public static boolean isSettingsTarget(String packageName) {
        return SETTINGS_PACKAGE.equals(packageName);
    }

    public static boolean shouldRedirectToLauncherPicker(String action, String roleName) {
        if (ACTION_HOME_SETTINGS.equals(action)
                || ACTION_MANAGE_DEFAULT_APPS_SETTINGS.equals(action)) {
            return true;
        }

        return ACTION_REQUEST_ROLE.equals(action) && ROLE_HOME.equals(roleName);
    }

    public static boolean shouldRedirectRoleRequestToLauncherPicker(String action, String roleName) {
        return ACTION_REQUEST_ROLE.equals(action) && ROLE_HOME.equals(roleName);
    }

    public static String modulePackageName() {
        return MODULE_PACKAGE;
    }

    public static String launcherPickerActivityClassName() {
        return LAUNCHER_PICKER_ACTIVITY;
    }

    public static String preferredActivityHelperClassName() {
        return PREFERRED_ACTIVITY_HELPER_CLASS;
    }

    public static String alreadyDefaultHomeMethodName() {
        return ALREADY_DEFAULT_HOME_METHOD;
    }
}
