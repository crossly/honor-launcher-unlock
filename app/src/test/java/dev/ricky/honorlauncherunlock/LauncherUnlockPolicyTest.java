package dev.ricky.honorlauncherunlock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LauncherUnlockPolicyTest {
    @Test
    public void systemServerTargetMatchesAndroidPackageAndProcess() {
        assertTrue(LauncherUnlockPolicy.isSystemServerTarget("android", "android"));
    }

    @Test
    public void systemServerTargetRejectsOtherProcesses() {
        assertFalse(LauncherUnlockPolicy.isSystemServerTarget("android", "com.android.systemui"));
        assertFalse(LauncherUnlockPolicy.isSystemServerTarget("com.android.permissioncontroller", "android"));
    }

    @Test
    public void permissionControllerTargetMatchesPackage() {
        assertTrue(LauncherUnlockPolicy.isPermissionControllerTarget("com.android.permissioncontroller"));
    }

    @Test
    public void permissionControllerTargetRejectsOtherPackages() {
        assertFalse(LauncherUnlockPolicy.isPermissionControllerTarget("android"));
    }

    @Test
    public void settingsTargetMatchesAndroidSettings() {
        assertTrue(LauncherUnlockPolicy.isSettingsTarget("com.android.settings"));
    }

    @Test
    public void homeSettingsActionShouldRedirect() {
        assertTrue(LauncherUnlockPolicy.shouldRedirectToLauncherPicker(
                "android.settings.HOME_SETTINGS",
                null));
    }

    @Test
    public void homeRoleRequestShouldRedirect() {
        assertTrue(LauncherUnlockPolicy.shouldRedirectToLauncherPicker(
                "android.app.role.action.REQUEST_ROLE",
                "android.app.role.HOME"));
    }

    @Test
    public void homeRoleRequestFallbackShouldRedirect() {
        assertTrue(LauncherUnlockPolicy.shouldRedirectRoleRequestToLauncherPicker(
                "android.app.role.action.REQUEST_ROLE",
                "android.app.role.HOME"));
    }

    @Test
    public void homeSettingsActionShouldNotUseRoleFallback() {
        assertFalse(LauncherUnlockPolicy.shouldRedirectRoleRequestToLauncherPicker(
                "android.settings.HOME_SETTINGS",
                null));
    }

    @Test
    public void nonHomeRoleRequestShouldNotRedirect() {
        assertFalse(LauncherUnlockPolicy.shouldRedirectToLauncherPicker(
                "android.app.role.action.REQUEST_ROLE",
                "android.app.role.BROWSER"));
    }

    @Test
    public void unrelatedSettingsActionShouldNotRedirect() {
        assertFalse(LauncherUnlockPolicy.shouldRedirectToLauncherPicker(
                "android.settings.WIFI_SETTINGS",
                null));
    }

    @Test
    public void launcherPickerPackageNameMatchesModulePackage() {
        assertEquals("dev.ricky.honorlauncherunlock",
                LauncherUnlockPolicy.modulePackageName());
    }

    @Test
    public void launcherPickerClassNameMatchesActivity() {
        assertEquals("dev.ricky.honorlauncherunlock.LauncherPickerActivity",
                LauncherUnlockPolicy.launcherPickerActivityClassName());
    }

    @Test
    public void exposesHookTargetNamesForHookEntry() {
        assertEquals("com.android.server.pm.PreferredActivityHelper",
                LauncherUnlockPolicy.preferredActivityHelperClassName());
        assertEquals("isAlreadyDefaultHomeActivity",
                LauncherUnlockPolicy.alreadyDefaultHomeMethodName());
    }
}
