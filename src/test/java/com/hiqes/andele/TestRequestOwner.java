package com.hiqes.andele;


import android.app.Activity;
import android.app.Application;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.support.v7.app.AppCompatActivity;
import android.widget.FrameLayout;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

import static org.mockito.Mockito.*;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TestRequestOwner {
    private static final String         TEST_PACKAGE = "com.hiqes.andele.test.ui";
    private static final String         DUMMY_PERMISSION = "com.hiqes.andele.test.DUMMY_PERMISSION";
    private static final String         DUMMY_PERM_GROUP = "com.hiqes.andele.test.PERM_GROUP_TEST";
    private static final String         TEST_ACTIVITY = "TestActivity";
    private static final String         TEST_OTHER_ACTIVITY = "TestOtherActivity";
    private static final String         TEST_FRAGMENT_TAG = "TestFragment";
    private static final int            TEST_FRAGMENT_ID = 0x5a5a;
    private static final String         TEST_OTHER_FRAGMENT_TAG = "TestOtherFragment";
    private static final int            TEST_OTHER_FRAGMENT_ID = 0xdbdb;

    @Mock
    private Activity            mActivity;

    @Mock
    private Activity            mOtherActivity;

    @Mock
    private AppCompatActivity   mAppCompatActivity;

    @Mock
    private AppCompatActivity   mOtherAppCompatActivity;

    @Mock
    private Fragment            mFragment;

    @Mock
    private Fragment            mOtherFragment;

    @Mock
    private Application         mApplication;

    @Mock
    private PackageManager      mPackageMgr;

    @Mock
    private FrameLayout         mRootView;

    @Mock
    private PermissionInfo      mPermInfo;

    @Mock
    private ComponentName       mTestCompName;

    @Mock
    private ComponentName       mOtherCompName;

    @Before
    public void setup() throws Exception {
        when(mTestCompName.getPackageName()).thenReturn(TEST_PACKAGE);
        when(mTestCompName.getClassName()).thenReturn(TEST_ACTIVITY);
        when(mActivity.getComponentName()).thenReturn(mTestCompName);

        mPermInfo.flags = PermissionInfo.FLAG_INSTALLED;
        mPermInfo.group = DUMMY_PERM_GROUP;
        mPermInfo.protectionLevel = PermissionInfo.PROTECTION_DANGEROUS;

        when(mPackageMgr.getPermissionInfo(DUMMY_PERMISSION,
                PackageManager.GET_META_DATA))
                .thenReturn(mPermInfo);

        when(mActivity.getPackageManager()).thenReturn(mPackageMgr);
        when(mActivity.getApplication()).thenReturn(mApplication);
        when(mActivity.findViewById(android.R.id.content))
                .thenReturn(mRootView);

        when(mOtherCompName.getPackageName()).thenReturn(TEST_PACKAGE);
        when(mOtherCompName.getClassName()).thenReturn(TEST_OTHER_ACTIVITY);
        when(mOtherActivity.getComponentName()).thenReturn(mOtherCompName);

        when(mAppCompatActivity.getComponentName()).thenReturn(mTestCompName);
        when(mAppCompatActivity.getPackageManager()).thenReturn(mPackageMgr);
        when(mAppCompatActivity.getApplication()).thenReturn(mApplication);
        when(mAppCompatActivity.findViewById(android.R.id.content))
                .thenReturn(mRootView);

        when(mOtherAppCompatActivity.getComponentName()).thenReturn(mOtherCompName);

        when(mFragment.getActivity()).thenReturn(mActivity);
        when(mOtherFragment.getActivity()).thenReturn(mOtherActivity);
    }

    //////////////////////////////////////////////////////////////////////////
    //  RequestOwnerActivity tests
    //////////////////////////////////////////////////////////////////////////
    @Test
    public void testRequestOwnerActivityGetters() {
        RequestOwnerActivity testClass = new RequestOwnerActivity(mActivity);
        assertEquals(mPackageMgr, testClass.getPackageManager());
        assertEquals(mApplication, testClass.getApplication());
        assertEquals(mActivity, testClass.getUiContext());
        assertEquals(mRootView, testClass.getRootView());
    }

    @Test
    public void testRequestOwnerActivityOwnerApis() {
        RequestOwnerActivity testClass = new RequestOwnerActivity(mActivity);
        RequestOwnerActivity sameOwner = new RequestOwnerActivity(mActivity);
        assertTrue(testClass.isSameOwner(sameOwner));

        RequestOwnerActivity otherRequestOwner = new RequestOwnerActivity(mOtherActivity);
        assertFalse(testClass.isSameOwner(otherRequestOwner));

        assertTrue(testClass.isParentActivity(mActivity));
        assertFalse(testClass.isParentActivity(mOtherActivity));
    }

    @Test
    public void testRequestOwnerActivityPermissionInfo() {
        RequestOwnerActivity testClass = new RequestOwnerActivity(mActivity);
        PermissionInfo testInfo = testClass.getPermissionInfo(DUMMY_PERMISSION);
        assertEquals(PermissionInfo.FLAG_INSTALLED, testInfo.flags);
        assertEquals(DUMMY_PERM_GROUP, testInfo.group);
        assertEquals(PermissionInfo.PROTECTION_DANGEROUS, testInfo.protectionLevel);
    }

    //////////////////////////////////////////////////////////////////////////
    //  RequestOwnerAppCompatActivity tests
    //////////////////////////////////////////////////////////////////////////
    @Test
    public void testRequestOwnerAppCompatActivityGetters() {
        RequestOwnerActivity testClass = new RequestOwnerActivity(mAppCompatActivity);
        assertEquals(mPackageMgr, testClass.getPackageManager());
        assertEquals(mApplication, testClass.getApplication());
        assertEquals(mAppCompatActivity, testClass.getUiContext());
        assertEquals(mRootView, testClass.getRootView());
    }

    @Test
    public void testRequestOwnerAppCompatActivityOwnerApis() {
        RequestOwnerActivity testClass = new RequestOwnerActivity(mAppCompatActivity);
        RequestOwnerActivity sameOwner = new RequestOwnerActivity(mAppCompatActivity);
        assertTrue(testClass.isSameOwner(sameOwner));

        RequestOwnerActivity otherRequestOwner = new RequestOwnerActivity(mOtherAppCompatActivity);
        assertFalse(testClass.isSameOwner(otherRequestOwner));

        assertTrue(testClass.isParentActivity(mAppCompatActivity));
        assertFalse(testClass.isParentActivity(mOtherAppCompatActivity));
    }

    @Test
    public void testRequestOwnerAppCompatActivityPermissionInfo() throws Exception {
        RequestOwnerActivity testClass = new RequestOwnerActivity(mAppCompatActivity);
        PermissionInfo testInfo = testClass.getPermissionInfo(DUMMY_PERMISSION);
        assertEquals(PermissionInfo.FLAG_INSTALLED, testInfo.flags);
        assertEquals(DUMMY_PERM_GROUP, testInfo.group);
        assertEquals(PermissionInfo.PROTECTION_DANGEROUS, testInfo.protectionLevel);
    }


    //////////////////////////////////////////////////////////////////////////
    //  RequestOwnerFragment tests
    //////////////////////////////////////////////////////////////////////////
    @Test
    public void testRequestOwnerFragmentGetters() {
        RequestOwnerFragment testClass = new RequestOwnerFragment(mFragment);
        assertEquals(mPackageMgr, testClass.getPackageManager());
        assertEquals(mApplication, testClass.getApplication());
        assertEquals(mActivity, testClass.getUiContext());
        assertEquals(mRootView, testClass.getRootView());
    }

    @Test
    public void testRequestOwnerFragmentOwnerApis() {

        //  TODO: HERE NEED TO RUN TESTS WITH DIFFERENT FRAGMENT TAGS AND IDS
        RequestOwnerFragment testClass = new RequestOwnerFragment(mFragment);
        RequestOwnerFragment sameOwner = new RequestOwnerFragment(mFragment);
        assertTrue(testClass.isSameOwner(sameOwner));

        RequestOwnerFragment otherRequestOwner = new RequestOwnerFragment(mOtherFragment);
        assertFalse(testClass.isSameOwner(otherRequestOwner));

        assertTrue(testClass.isParentActivity(mActivity));
        assertFalse(testClass.isParentActivity(mOtherActivity));
    }

    @Test
    public void testRequestOwnerFragmentPermissionInfo() throws Exception {
        RequestOwnerFragment testClass = new RequestOwnerFragment(mFragment);
        PermissionInfo testInfo = testClass.getPermissionInfo(DUMMY_PERMISSION);
        assertEquals(PermissionInfo.FLAG_INSTALLED, testInfo.flags);
        assertEquals(DUMMY_PERM_GROUP, testInfo.group);
        assertEquals(PermissionInfo.PROTECTION_DANGEROUS, testInfo.protectionLevel);
    }


    //////////////////////////////////////////////////////////////////////////
    //  Cannot verify RequestOwnerSupportFragment because the support
    //  Fragment's getActivity() method is final!
    //////////////////////////////////////////////////////////////////////////
}
