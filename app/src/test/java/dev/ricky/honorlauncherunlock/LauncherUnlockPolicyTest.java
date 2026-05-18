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
        assertTrue(LauncherUnlockPolicy.isPermissionControllerTarget(
                "com.android.permissioncontroller"));
    }

    @Test
    public void permissionControllerTargetMatchesGooglePackage() {
        assertTrue(LauncherUnlockPolicy.isPermissionControllerTarget(
                "com.google.android.permissioncontroller"));
    }

    @Test
    public void permissionControllerTargetRejectsOtherPackages() {
        assertFalse(LauncherUnlockPolicy.isPermissionControllerTarget("android"));
    }

    @Test
    public void exposesHookTargetNamesForHookEntry() {
        assertEquals("com.android.server.pm.PreferredActivityHelper",
                LauncherUnlockPolicy.preferredActivityHelperClassName());
        assertEquals("isAlreadyDefaultHomeActivity",
                LauncherUnlockPolicy.alreadyDefaultHomeMethodName());
        assertEquals("com.android.role.controller.model.Role",
                LauncherUnlockPolicy.roleModelClassNames()[0]);
        assertEquals("com.android.permissioncontroller.role.model.Role",
                LauncherUnlockPolicy.roleModelClassNames()[1]);
        assertEquals("c2.p", LauncherUnlockPolicy.roleModelClassNames()[2]);
        assertEquals("android.app.role.HOME", LauncherUnlockPolicy.homeRoleName());
    }
}
