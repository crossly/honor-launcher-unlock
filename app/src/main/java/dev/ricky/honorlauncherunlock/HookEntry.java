package dev.ricky.honorlauncherunlock;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class HookEntry implements IXposedHookLoadPackage {
    private static final String TAG = "HonorLauncherUnlock";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (LauncherUnlockPolicy.isSystemServerTarget(lpparam.packageName, lpparam.processName)) {
            XposedBridge.log(TAG + ": loaded in " + lpparam.packageName
                    + " process=" + lpparam.processName);
            hookHomeAlreadyDefaultGate(lpparam.classLoader);
            return;
        }

        if (LauncherUnlockPolicy.isPermissionControllerTarget(lpparam.packageName)
                || LauncherUnlockPolicy.isSettingsTarget(lpparam.packageName)) {
            XposedBridge.log(TAG + ": loaded in " + lpparam.packageName
                    + " process=" + lpparam.processName);
            hookDefaultHomeEntryRedirects(lpparam.packageName);
        }
    }

    private static void hookHomeAlreadyDefaultGate(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                    LauncherUnlockPolicy.preferredActivityHelperClassName(),
                    classLoader,
                    LauncherUnlockPolicy.alreadyDefaultHomeMethodName(),
                    ComponentName.class,
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            ComponentName componentName = (ComponentName) param.args[0];
                            if (componentName == null) {
                                return;
                            }

                            XposedBridge.log(TAG + ": bypassing HOME AntiMal gate for "
                                    + componentName.flattenToShortString()
                                    + " user=" + param.args[1]);
                            param.setResult(true);
                        }
                    });

            XposedBridge.log(TAG + ": hooked PreferredActivityHelper.isAlreadyDefaultHomeActivity");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": failed to hook PreferredActivityHelper gate");
            XposedBridge.log(t);
        }
    }

    private static void hookDefaultHomeEntryRedirects(String packageName) {
        hookStartActivity();
        hookStartActivityWithOptions();
        hookStartActivityForResult();
        hookStartActivityForResultWithoutOptions();
        if (LauncherUnlockPolicy.isPermissionControllerTarget(packageName)) {
            hookPermissionControllerRoleFallback();
        }
        XposedBridge.log(TAG + ": hooked stable HOME entry redirects");
    }

    private static void hookStartActivity() {
        try {
            XposedHelpers.findAndHookMethod(
                    Activity.class,
                    "startActivity",
                    Intent.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            replaceHomeEntryIntent((Activity) param.thisObject, param.args, 0);
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": failed to hook Activity.startActivity");
            XposedBridge.log(t);
        }
    }

    private static void hookStartActivityWithOptions() {
        try {
            XposedHelpers.findAndHookMethod(
                    Activity.class,
                    "startActivity",
                    Intent.class,
                    Bundle.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            replaceHomeEntryIntent((Activity) param.thisObject, param.args, 0);
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": failed to hook Activity.startActivity with options");
            XposedBridge.log(t);
        }
    }

    private static void hookStartActivityForResult() {
        try {
            XposedHelpers.findAndHookMethod(
                    Activity.class,
                    "startActivityForResult",
                    Intent.class,
                    int.class,
                    Bundle.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            replaceHomeEntryIntent((Activity) param.thisObject, param.args, 0);
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": failed to hook Activity.startActivityForResult");
            XposedBridge.log(t);
        }
    }

    private static void hookStartActivityForResultWithoutOptions() {
        try {
            XposedHelpers.findAndHookMethod(
                    Activity.class,
                    "startActivityForResult",
                    Intent.class,
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            replaceHomeEntryIntent((Activity) param.thisObject, param.args, 0);
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": failed to hook Activity.startActivityForResult without options");
            XposedBridge.log(t);
        }
    }

    private static void hookPermissionControllerRoleFallback() {
        try {
            XposedHelpers.findAndHookMethod(
                    Activity.class,
                    "onCreate",
                    Bundle.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            finishHomeRoleRequestIfOpened((Activity) param.thisObject);
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": failed to hook PermissionController role fallback");
            XposedBridge.log(t);
        }
    }

    private static boolean replaceHomeEntryIntent(Activity activity, Object[] args, int intentIndex) {
        if (activity == null || args == null || args.length <= intentIndex) {
            return false;
        }

        Intent sourceIntent = (Intent) args[intentIndex];
        if (sourceIntent == null) {
            return false;
        }

        String roleName = sourceIntent.getStringExtra("android.app.role.extra.ROLE_NAME");
        if (!LauncherUnlockPolicy.shouldRedirectToLauncherPicker(
                sourceIntent.getAction(),
                roleName)) {
            return false;
        }

        args[intentIndex] = createLauncherPickerIntent();
        XposedBridge.log(TAG + ": replaced HOME entry intent action="
                + sourceIntent.getAction()
                + " role=" + roleName);
        return true;
    }

    private static boolean finishHomeRoleRequestIfOpened(Activity activity) {
        if (activity == null) {
            return false;
        }

        Intent sourceIntent = activity.getIntent();
        if (sourceIntent == null) {
            return false;
        }

        String roleName = sourceIntent.getStringExtra("android.app.role.extra.ROLE_NAME");
        if (!LauncherUnlockPolicy.shouldRedirectRoleRequestToLauncherPicker(
                sourceIntent.getAction(), roleName)) {
            return false;
        }

        activity.startActivity(createLauncherPickerIntent());
        activity.finish();
        activity.overridePendingTransition(0, 0);
        XposedBridge.log(TAG + ": finished HOME entry fallback action="
                + sourceIntent.getAction()
                + " role=" + roleName);
        return true;
    }

    private static Intent createLauncherPickerIntent() {
        Intent pickerIntent = new Intent();
        pickerIntent.setClassName(
                LauncherUnlockPolicy.modulePackageName(),
                LauncherUnlockPolicy.launcherPickerActivityClassName());
        pickerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return pickerIntent;
    }
}
