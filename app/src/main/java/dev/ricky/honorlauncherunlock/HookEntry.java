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
            hookDefaultHomeEntryRedirect(lpparam.classLoader);
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

    private static void hookDefaultHomeEntryRedirect(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                    Activity.class,
                    "onCreate",
                    Bundle.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            redirectHomeEntryIfNeeded((Activity) param.thisObject);
                        }
                    });
            XposedBridge.log(TAG + ": hooked Activity.onCreate for HOME entry redirects");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": failed to hook Activity.onCreate redirect");
            XposedBridge.log(t);
        }
    }

    private static boolean redirectHomeEntryIfNeeded(Activity activity) {
        if (activity == null) {
            return false;
        }

        Intent sourceIntent = activity.getIntent();
        if (sourceIntent == null) {
            return false;
        }

        String roleName = sourceIntent.getStringExtra("android.app.role.extra.ROLE_NAME");
        if (!LauncherUnlockPolicy.shouldRedirectToLauncherPicker(
                sourceIntent.getAction(),
                roleName)) {
            return false;
        }

        Intent pickerIntent = new Intent();
        pickerIntent.setClassName(
                "dev.ricky.honorlauncherunlock",
                "dev.ricky.honorlauncherunlock.LauncherPickerActivity");
        pickerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(pickerIntent);
        activity.finish();
        activity.overridePendingTransition(0, 0);
        XposedBridge.log(TAG + ": redirected HOME entry action="
                + sourceIntent.getAction()
                + " role=" + roleName);
        return true;
    }
}
