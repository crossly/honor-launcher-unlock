package dev.ricky.honorlauncherunlock;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.UserHandle;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
            hookHonorAntiMalPolicy(lpparam.classLoader);
        } else if (lpparam.packageName != null
                && lpparam.packageName.contains("permissioncontroller")) {
            XposedBridge.log(TAG + ": ignoring non-target PermissionController package "
                    + lpparam.packageName + " process=" + lpparam.processName);
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
        Set<Class<?>> roleClasses = findRoleModelClasses(classLoader);
        if (roleClasses.isEmpty()) {
            XposedBridge.log(TAG + ": no Role model class found in PermissionController");
            return;
        }

        for (Class<?> roleClass : roleClasses) {
            hookHomeRoleQualifiedPackages(roleClass);
            hookHomeRolePackageQualification(roleClass);
            hookLegacyHomeRolePackageQualification(roleClass);
        }
    }

    private static void hookHonorAntiMalPolicy(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                    "com.hihonor.android.securitydiagnose.HwSecurityDiagnoseManager",
                    classLoader,
                    "getAntimalProtectionPolicy",
                    int.class,
                    Bundle.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            Bundle bundle = (Bundle) param.args[1];
                            String packageName = bundle != null ? bundle.getString("pkg") : null;
                            if (hasHomeActivity(packageName, getPermissionControllerContext())) {
                                XposedBridge.log(TAG + ": allowing Honor HOME AntiMal policy for "
                                        + packageName + " type=" + param.args[0]
                                        + " user=" + bundle.getInt("userid", -1));
                                param.setResult(0);
                            }
                        }
                    });

            XposedBridge.log(TAG + ": hooked HwSecurityDiagnoseManager"
                    + ".getAntimalProtectionPolicy(int, Bundle) for HOME");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": unavailable HwSecurityDiagnoseManager"
                    + ".getAntimalProtectionPolicy(int, Bundle): " + t);
            hookHonorAntiMalUiFallback(classLoader);
        }
    }

    private static void hookHonorAntiMalUiFallback(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                    "r2.b",
                    classLoader,
                    "d",
                    String.class,
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            String packageName = (String) param.args[0];
                            if (hasHomeActivity(packageName, getPermissionControllerContext())) {
                                XposedBridge.log(TAG + ": bypassing Honor HOME AntiMal UI filter for "
                                        + packageName + " user=" + param.args[1]);
                                param.setResult(true);
                            }
                        }
                    });

            XposedBridge.log(TAG + ": hooked fallback r2.b.d(String, int) HOME AntiMal UI filter");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": unavailable fallback r2.b.d(String, int): " + t);
        }
    }

    private static Set<Class<?>> findRoleModelClasses(ClassLoader classLoader) {
        Set<Class<?>> roleClasses = new LinkedHashSet<>();
        for (String roleModelClassName : LauncherUnlockPolicy.roleModelClassNames()) {
            Class<?> roleClass = XposedHelpers.findClassIfExists(roleModelClassName, classLoader);
            if (roleClass != null) {
                roleClasses.add(roleClass);
            }
        }

        roleClasses.addAll(findLoadedRoleModelClasses(classLoader));
        return roleClasses;
    }

    private static List<Class<?>> findLoadedRoleModelClasses(ClassLoader classLoader) {
        List<Class<?>> roleClasses = new ArrayList<>();
        try {
            Object pathList = XposedHelpers.getObjectField(classLoader, "pathList");
            Object[] dexElements = (Object[]) XposedHelpers.getObjectField(pathList, "dexElements");
            for (Object dexElement : dexElements) {
                Object dexFile = XposedHelpers.getObjectField(dexElement, "dexFile");
                if (dexFile == null) {
                    continue;
                }

                java.util.Enumeration<String> entries = ((dalvik.system.DexFile) dexFile).entries();
                while (entries.hasMoreElements()) {
                    String className = entries.nextElement();
                    if (!couldBeScannedClass(className)) {
                        continue;
                    }

                    Class<?> roleClass = XposedHelpers.findClassIfExists(className, classLoader);
                    if (roleClass != null && hasRoleModelMethods(roleClass)) {
                        roleClasses.add(roleClass);
                        XposedBridge.log(TAG + ": discovered Role model class " + className);
                    }
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": failed to scan PermissionController role classes: " + t);
        }
        return roleClasses;
    }

    private static boolean couldBeScannedClass(String className) {
        return className != null
                && className.indexOf('$') < 0
                && !className.startsWith("android.")
                && !className.startsWith("androidx.")
                && !className.startsWith("java.")
                && !className.startsWith("javax.")
                && !className.startsWith("kotlin.")
                && !className.startsWith("kotlinx.");
    }

    private static boolean hasRoleModelMethods(Class<?> roleClass) {
        return XposedHelpers.findMethodExactIfExists(
                roleClass,
                "getQualifyingPackagesAsUser",
                UserHandle.class,
                Context.class) != null
                || XposedHelpers.findMethodExactIfExists(
                        roleClass,
                        "i",
                        UserHandle.class,
                        Context.class) != null
                || XposedHelpers.findMethodExactIfExists(
                        roleClass,
                        "isPackageQualifiedAsUser",
                        String.class,
                        UserHandle.class,
                        Context.class) != null;
    }

    private static void hookHomeRoleQualifiedPackages(Class<?> roleClass) {
        try {
            XposedHelpers.findAndHookMethod(
                    roleClass,
                    "getQualifyingPackagesAsUser",
                    UserHandle.class,
                    Context.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!isHomeRole(param.thisObject)) {
                                return;
                            }

                            Context context = (Context) param.args[1];
                            List<String> result = asMutableStringList(param.getResult());
                            appendHomePackages(result, context);
                            param.setResult(result);
                        }
                    });

            XposedBridge.log(TAG + ": hooked " + roleClass.getName()
                    + ".getQualifyingPackagesAsUser for HOME");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": unavailable " + roleClass.getName()
                    + ".getQualifyingPackagesAsUser: " + t);
        }

        try {
            XposedHelpers.findAndHookMethod(
                    roleClass,
                    "i",
                    UserHandle.class,
                    Context.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!isHomeRole(param.thisObject)) {
                                return;
                            }

                            Context context = (Context) param.args[1];
                            List<String> result = asMutableStringList(param.getResult());
                            appendHomePackages(result, context);
                            param.setResult(result);
                        }
                    });

            XposedBridge.log(TAG + ": hooked " + roleClass.getName()
                    + ".i(UserHandle, Context) for HOME");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": unavailable " + roleClass.getName()
                    + ".i(UserHandle, Context): " + t);
        }
    }

    private static void hookHomeRolePackageQualification(Class<?> roleClass) {
        try {
            XposedHelpers.findAndHookMethod(
                    roleClass,
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
                            Context context = (Context) param.args[2];
                            if (hasHomeActivity(packageName, context)) {
                                XposedBridge.log(TAG + ": qualifying HOME package "
                                        + packageName);
                                param.setResult(true);
                            }
                        }
                    });

            XposedBridge.log(TAG + ": hooked " + roleClass.getName()
                    + ".isPackageQualifiedAsUser for HOME");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": unavailable " + roleClass.getName()
                    + ".isPackageQualifiedAsUser: " + t);
        }

        try {
            XposedHelpers.findAndHookMethod(
                    roleClass,
                    "y",
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
                            Context context = (Context) param.args[2];
                            if (hasHomeActivity(packageName, context)) {
                                XposedBridge.log(TAG + ": qualifying obfuscated HOME package "
                                        + packageName);
                                param.setResult(true);
                            }
                        }
                    });

            XposedBridge.log(TAG + ": hooked " + roleClass.getName()
                    + ".y(String, UserHandle, Context) for HOME");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": unavailable " + roleClass.getName()
                    + ".y(String, UserHandle, Context): " + t);
        }
    }

    private static void hookLegacyHomeRolePackageQualification(Class<?> roleClass) {
        try {
            XposedHelpers.findAndHookMethod(
                    roleClass,
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
                            if (hasHomeActivity(packageName, context)) {
                                XposedBridge.log(TAG + ": qualifying legacy HOME package "
                                        + packageName);
                                param.setResult(true);
                            }
                        }
                    });

            XposedBridge.log(TAG + ": hooked " + roleClass.getName()
                    + ".isPackageQualified for HOME");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": unavailable " + roleClass.getName()
                    + ".isPackageQualified: " + t);
        }
    }

    private static boolean isHomeRole(Object role) {
        if (role == null) {
            return false;
        }

        try {
            Object roleName = XposedHelpers.callMethod(role, "getName");
            return LauncherUnlockPolicy.homeRoleName().equals(roleName);
        } catch (Throwable ignored) {
            try {
                Object roleName = XposedHelpers.callMethod(role, "h");
                return LauncherUnlockPolicy.homeRoleName().equals(roleName);
            } catch (Throwable t) {
                XposedBridge.log(TAG + ": failed to read role name");
                XposedBridge.log(t);
                return false;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> asMutableStringList(Object result) {
        if (result instanceof List) {
            return new ArrayList<>((List<String>) result);
        }
        return new ArrayList<>();
    }

    private static void appendHomePackages(List<String> packages, Context context) {
        List<ResolveInfo> homeActivities = queryHomeActivities(context);
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

    private static boolean hasHomeActivity(String packageName, Context context) {
        if (packageName == null) {
            return false;
        }

        List<ResolveInfo> homeActivities = queryHomeActivities(context);
        for (ResolveInfo resolveInfo : homeActivities) {
            ActivityInfo activityInfo = resolveInfo.activityInfo;
            if (activityInfo != null && packageName.equals(activityInfo.packageName)) {
                return true;
            }
        }
        return false;
    }

    private static List<ResolveInfo> queryHomeActivities(Context context) {
        if (context == null) {
            return new ArrayList<>();
        }

        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        PackageManager packageManager = context.getPackageManager();
        return packageManager.queryIntentActivities(homeIntent, 0);
    }

    private static Context getPermissionControllerContext() {
        try {
            return (Context) XposedHelpers.callStaticMethod(
                    XposedHelpers.findClass("android.app.ActivityThread", null),
                    "currentApplication");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": failed to get PermissionController context: " + t);
            return null;
        }
    }
}
