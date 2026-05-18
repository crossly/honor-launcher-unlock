package dev.ricky.honorlauncherunlock;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.UserHandle;

import java.util.ArrayList;
import java.util.List;

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
        }

        if (LauncherUnlockPolicy.isPermissionControllerTarget(lpparam.packageName)) {
            XposedBridge.log(TAG + ": loaded in " + lpparam.packageName
                    + " process=" + lpparam.processName);
            hookHomeRoleQualification(lpparam.classLoader);
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

    private static void hookHomeRoleQualification(ClassLoader classLoader) {
        for (String roleModelClassName : LauncherUnlockPolicy.roleModelClassNames()) {
            hookHomeRoleQualifiedPackages(classLoader, roleModelClassName);
            hookHomeRolePackageQualification(classLoader, roleModelClassName);
            hookLegacyHomeRolePackageQualification(classLoader, roleModelClassName);
        }
    }

    private static void hookHomeRoleQualifiedPackages(
            ClassLoader classLoader,
            String roleModelClassName) {
        try {
            XposedHelpers.findAndHookMethod(
                    roleModelClassName,
                    classLoader,
                    "getQualifyingPackagesAsUser",
                    UserHandle.class,
                    Context.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!isHomeRole(param.thisObject)) {
                                return;
                            }

                            UserHandle userHandle = (UserHandle) param.args[0];
                            Context context = (Context) param.args[1];
                            List<String> result = asMutableStringList(param.getResult());
                            appendHomePackages(result, userHandle, context);
                            param.setResult(result);
                        }
                    });

            XposedBridge.log(TAG + ": hooked " + roleModelClassName
                    + ".getQualifyingPackagesAsUser for HOME");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": failed to hook " + roleModelClassName
                    + ".getQualifyingPackagesAsUser");
            XposedBridge.log(t);
        }
    }

    private static void hookHomeRolePackageQualification(
            ClassLoader classLoader,
            String roleModelClassName) {
        try {
            XposedHelpers.findAndHookMethod(
                    roleModelClassName,
                    classLoader,
                    "isPackageQualifiedAsUser",
                    String.class,
                    UserHandle.class,
                    Context.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!isHomeRole(param.thisObject)) {
                                return;
                            }

                            String packageName = (String) param.args[0];
                            UserHandle userHandle = (UserHandle) param.args[1];
                            Context context = (Context) param.args[2];
                            if (hasHomeActivity(packageName, userHandle, context)) {
                                XposedBridge.log(TAG + ": qualifying HOME package "
                                        + packageName);
                                param.setResult(true);
                            }
                        }
                    });

            XposedBridge.log(TAG + ": hooked " + roleModelClassName
                    + ".isPackageQualifiedAsUser for HOME");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": failed to hook " + roleModelClassName
                    + ".isPackageQualifiedAsUser");
            XposedBridge.log(t);
        }
    }

    private static void hookLegacyHomeRolePackageQualification(
            ClassLoader classLoader,
            String roleModelClassName) {
        try {
            XposedHelpers.findAndHookMethod(
                    roleModelClassName,
                    classLoader,
                    "isPackageQualified",
                    String.class,
                    Context.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!isHomeRole(param.thisObject)) {
                                return;
                            }

                            String packageName = (String) param.args[0];
                            Context context = (Context) param.args[1];
                            if (hasHomeActivity(packageName, android.os.Process.myUserHandle(),
                                    context)) {
                                XposedBridge.log(TAG + ": qualifying legacy HOME package "
                                        + packageName);
                                param.setResult(true);
                            }
                        }
                    });

            XposedBridge.log(TAG + ": hooked " + roleModelClassName
                    + ".isPackageQualified for HOME");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": failed to hook " + roleModelClassName
                    + ".isPackageQualified");
            XposedBridge.log(t);
        }
    }

    private static boolean isHomeRole(Object role) {
        if (role == null) {
            return false;
        }

        try {
            Object roleName = XposedHelpers.callMethod(role, "getName");
            return LauncherUnlockPolicy.homeRoleName().equals(roleName);
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": failed to read role name");
            XposedBridge.log(t);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> asMutableStringList(Object result) {
        if (result instanceof List) {
            return new ArrayList<>((List<String>) result);
        }
        return new ArrayList<>();
    }

    private static void appendHomePackages(
            List<String> packages,
            UserHandle userHandle,
            Context context) {
        List<ResolveInfo> homeActivities = queryHomeActivities(userHandle, context);
        for (ResolveInfo resolveInfo : homeActivities) {
            ActivityInfo activityInfo = resolveInfo.activityInfo;
            if (activityInfo == null || activityInfo.packageName == null) {
                continue;
            }

            if (!packages.contains(activityInfo.packageName)) {
                packages.add(activityInfo.packageName);
                XposedBridge.log(TAG + ": added HOME candidate "
                        + activityInfo.packageName);
            }
        }
    }

    private static boolean hasHomeActivity(
            String packageName,
            UserHandle userHandle,
            Context context) {
        if (packageName == null) {
            return false;
        }

        List<ResolveInfo> homeActivities = queryHomeActivities(userHandle, context);
        for (ResolveInfo resolveInfo : homeActivities) {
            ActivityInfo activityInfo = resolveInfo.activityInfo;
            if (activityInfo != null && packageName.equals(activityInfo.packageName)) {
                return true;
            }
        }
        return false;
    }

    private static List<ResolveInfo> queryHomeActivities(UserHandle userHandle, Context context) {
        if (userHandle == null || context == null) {
            return new ArrayList<>();
        }

        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        PackageManager packageManager = context.createContextAsUser(userHandle, 0)
                .getPackageManager();
        return packageManager.queryIntentActivities(homeIntent, 0);
    }
}
