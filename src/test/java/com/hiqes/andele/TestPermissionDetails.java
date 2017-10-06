package com.hiqes.andele;

import android.Manifest;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;


public class TestPermissionDetails {
    @Test
    public void verifyGetters() throws Exception {
        PermissionDetails       details = new PermissionDetails(Manifest.permission.ACCESS_COARSE_LOCATION,
                PermissionUse.CRITICAL);

        assertEquals(Manifest.permission.ACCESS_COARSE_LOCATION,
                details.getPermission());
        assertNotEquals("com.hiqes.DUMMY_PERMISSION", details.getPermission());

        assertEquals(PermissionUse.CRITICAL, details.getPermissionUse());
        assertNotEquals(PermissionUse.ESSENTIAL, details.getPermissionUse());
        assertNotEquals(PermissionUse.FEATURE, details.getPermissionUse());
        assertNotEquals(PermissionUse.OPTIONAL, details.getPermissionUse());
    }

    @Test
    public void verifyKey() throws Exception {
        PermissionDetails       details = new PermissionDetails(Manifest.permission.ACCESS_COARSE_LOCATION,
                PermissionUse.CRITICAL);
        assertEquals(Manifest.permission.ACCESS_COARSE_LOCATION + ":" + PermissionUse.CRITICAL.name(),
                details.asKey());
    }
}

