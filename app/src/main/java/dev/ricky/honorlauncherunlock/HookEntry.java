package dev.ricky.honorlauncherunlock;

import android.content.ComponentName;
import android.content.Context;
import android.os.UserHandle;

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

        if (LauncherUnlockPolicy.isPermissionControllerTarget(lpparam.packageName)) {
            XposedBridge.log(TAG + ": loaded in " + lpparam.packageName
                    + " process=" + lpparam.processName);
            hookPermissionControllerAntiMal(lpparam.classLoader);
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

    private static void hookPermissionControllerAntiMal(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                    LauncherUnlockPolicy.antiMalProtectionClassName(),
                    classLoader,
                    LauncherUnlockPolicy.allowLauncherMethodName(),
                    String.class,
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            String packageName = (String) param.args[0];
                            XposedBridge.log(TAG + ": allowing launcher in settings for "
                                    + packageName + " user=" + param.args[1]);
                            param.setResult(true);
                        }
                    });
            XposedBridge.log(TAG + ": hooked AntiMalProtectionUtils.d");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": failed to hook AntiMalProtectionUtils.d");
            XposedBridge.log(t);
        }

        try {
            XposedHelpers.findAndHookMethod(
                    LauncherUnlockPolicy.antiMalProtectionClassName(),
                    classLoader,
                    LauncherUnlockPolicy.resetLauncherMethodName(),
                    Context.class,
                    UserHandle.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            XposedBridge.log(TAG + ": blocked PermissionController launcher reset");
                            param.setResult(null);
                        }
                    });
            XposedBridge.log(TAG + ": hooked AntiMalProtectionUtils.c");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": failed to hook AntiMalProtectionUtils.c");
            XposedBridge.log(t);
        }
    }
}
