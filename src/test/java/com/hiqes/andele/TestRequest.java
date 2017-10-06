package com.hiqes.andele;

import android.app.Activity;
import android.app.Application;
import android.app.Fragment;
import android.content.ComponentName;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

import static org.mockito.Mockito.*;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
public class TestRequest implements ProtectedAction.UserPromptCallback {
    private static final String         TEST_PACKAGE = "com.hiqes.andele.test.ui";
    private static final String         DUMMY_PERMISSION = "com.hiqes.andele.test.DUMMY_PERMISSION";
    private static final String         DUMMY_PERMISSION2 = "com.hiqes.andele.test.DUMMY_PERMISSION2";
    private static final String         DUMMY_PERMISSION3 = "com.hiqes.andele.test.DUMMY_PERMISSION3";
    private static final String         TEST_ACTIVITY = "TestActivity";

    @Mock
    private Application         mApp;

    @Mock
    private ComponentName       mTestCompName;

    @Mock
    private Activity            mActivity;

    @Mock
    private Activity            mSameActivity;

    @Mock
    private AppCompatActivity   mAppCompatActivity;

    @Mock
    private AppCompatActivity   mSameAppCompatActivity;

    @Mock
    private Fragment            mFragment;

    @Mock
    private Fragment            mSameFragment;

    //////////////////////////////////////////////////////////////////////////
    //  Cannot verify RequestOwnerSupportFragment because the support
    //  Fragment's getActivity() method is final!
    //////////////////////////////////////////////////////////////////////////

    private Application.ActivityLifecycleCallbacks  mCb;

    @Mock
    private Handler             mHandler;

    @Before
    public void setup() {
        when(mTestCompName.getPackageName()).thenReturn(TEST_PACKAGE);
        when(mTestCompName.getClassName()).thenReturn(TEST_ACTIVITY);

        when(mActivity.getComponentName()).thenReturn(mTestCompName);
        when(mActivity.getApplication()).thenReturn(mApp);

        when(mSameActivity.getComponentName()).thenReturn(mTestCompName);
        when(mSameActivity.getApplication()).thenReturn(mApp);

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                mCb = (Application.ActivityLifecycleCallbacks)invocation.getArguments()[0];
                return null;
            }
        }).when(mApp)
                .registerActivityLifecycleCallbacks(any(Application.ActivityLifecycleCallbacks.class));

        when(mAppCompatActivity.getComponentName()).thenReturn(mTestCompName);
        when(mAppCompatActivity.getApplication()).thenReturn(mApp);

        when(mSameAppCompatActivity.getComponentName()).thenReturn(mTestCompName);
        when(mSameAppCompatActivity.getApplication()).thenReturn(mApp);

        when(mFragment.getActivity()).thenReturn(mActivity);
        when(mSameFragment.getActivity()).thenReturn(mSameActivity);
    }


    private void testRequestSingleActionGetters(RequestOwner owner) {
        ProtectedAction.Builder bldr = new ProtectedAction.Builder();
        ProtectedAction[]       actions = new ProtectedAction[1];

        actions[0] =
                bldr.withPermission(DUMMY_PERMISSION)
                        .withUsage(PermissionUse.CRITICAL)
                        .actionCallback(new ProtectedAction.ActionCallback() {
                            @Override
                            public void doAction(ProtectedAction action) {
                                //  DO NOTHING
                            }
                        })
                        .userPromptCallback(this)
                        .build();

        Request newReq = new Request(owner, actions, mHandler);

        RequestOwner verifyOwner = newReq.getOwner();
        assertEquals(owner, verifyOwner);

        int verifyActionCount = newReq.getActionCount();
        assertEquals(1, verifyActionCount);

        ProtectedAction verifyAction = newReq.getAction();
        assertEquals(actions[0], verifyAction);

        ProtectedAction verifyActions[] = newReq.getActions();
        assertEquals(1, verifyActions.length);
        assertEquals(actions[0], verifyActions[0]);
    }

    private void testRequestMultiActionGetters(RequestOwner owner) {
        ProtectedAction.Builder bldr = new ProtectedAction.Builder();
        ProtectedAction.Builder bldr2 = new ProtectedAction.Builder();
        ProtectedAction.Builder bldr3 = new ProtectedAction.Builder();
        ProtectedAction[]       actions = new ProtectedAction[3];

        actions[0] =
                bldr.withPermission(DUMMY_PERMISSION)
                        .withUsage(PermissionUse.CRITICAL)
                        .actionCallback(new ProtectedAction.ActionCallback() {
                            @Override
                            public void doAction(ProtectedAction action) {
                                //  DO NOTHING
                            }
                        })
                        .userPromptCallback(this)
                        .build();

        actions[1] =
                bldr2.withPermission(DUMMY_PERMISSION2)
                        .withUsage(PermissionUse.ESSENTIAL)
                        .actionCallback(new ProtectedAction.ActionCallback() {
                            @Override
                            public void doAction(ProtectedAction action) {
                                //  DO NOTHING
                            }
                        })
                        .userPromptCallback(this)
                        .build();

        actions[2] =
                bldr3.withPermission(DUMMY_PERMISSION3)
                        .withUsage(PermissionUse.FEATURE)
                        .actionCallback(new ProtectedAction.ActionCallback() {
                            @Override
                            public void doAction(ProtectedAction action) {
                                //  DO NOTHING
                            }
                        })
                        .userPromptCallback(this)
                        .build();

        Request newReq = new Request(owner, actions, mHandler);

        RequestOwner verifyOwner = newReq.getOwner();
        assertEquals(owner, verifyOwner);

        int verifyActionCount = newReq.getActionCount();
        assertEquals(3, verifyActionCount);

        ProtectedAction verifyActions[] = newReq.getActions();
        assertEquals(3, verifyActions.length);
        for (int i = 0; i < verifyActionCount; i++) {
            assertEquals(actions[i], verifyActions[i]);
        }
    }


    @Test
    public void testRequestSingleActionGetters_RequestOwnerActivity() {
        RequestOwnerActivity  roAct = new RequestOwnerActivity(mActivity);
        testRequestSingleActionGetters(roAct);
    }

    @Test
    public void testRequestSingleActionGetters_RequestOwnerAppCompatActivity() {
        RequestOwnerAppCompatActivity   roAct = new RequestOwnerAppCompatActivity(mAppCompatActivity);
        testRequestSingleActionGetters(roAct);
    }

    @Test
    public void testRequestSingleActionGetters_RequestOwnerFragment() {
        RequestOwnerFragment    roFrag = new RequestOwnerFragment(mFragment);
        testRequestSingleActionGetters(roFrag);
    }

    @Test
    public void testRequestMultiActionGetters_RequestOwnerActivity() {
        RequestOwnerActivity  roAct = new RequestOwnerActivity(mActivity);
        testRequestMultiActionGetters(roAct);
    }

    @Test
    public void testRequestMultiActionGetters_RequestOwnerAppCompatActivity() {
        RequestOwnerAppCompatActivity   roAct = new RequestOwnerAppCompatActivity(mAppCompatActivity);
        testRequestMultiActionGetters(roAct);
    }

    @Test
    public void testRequestMultiActionGetters_RequestOwnerFragment() {
        RequestOwnerFragment    roFrag = new RequestOwnerFragment(mFragment);
        testRequestMultiActionGetters(roFrag);
    }


    public void testRequestSame(RequestOwner owner1, RequestOwner owner2) {
        ProtectedAction.Builder bldr1 = new ProtectedAction.Builder();
        ProtectedAction[]       actions1 = new ProtectedAction[1];
        ProtectedAction.Builder bldr2 = new ProtectedAction.Builder();
        ProtectedAction[]       actions2 = new ProtectedAction[1];

        actions1[0] =
                bldr1.withPermission(DUMMY_PERMISSION)
                        .withUsage(PermissionUse.CRITICAL)
                        .actionCallback(new ProtectedAction.ActionCallback() {
                            @Override
                            public void doAction(ProtectedAction action) {
                                //  DO NOTHING
                            }
                        })
                        .userPromptCallback(this)
                        .build();

        actions2[0] =
                bldr2.withPermission(DUMMY_PERMISSION)
                        .withUsage(PermissionUse.CRITICAL)
                        .actionCallback(new ProtectedAction.ActionCallback() {
                            @Override
                            public void doAction(ProtectedAction action) {
                                //  DO NOTHING
                            }
                        })
                        .userPromptCallback(this)
                        .build();

        Request newReq1 = new Request(owner1, actions1, mHandler);
        Request newReq2 = new Request(owner2, actions2, mHandler);

        assertTrue(newReq1.isSameRequest(newReq2));
        assertTrue(newReq2.isSameRequest(newReq2));
    }

    @Test
    public void testRequestSame_RequestOwnerActivity() {
        RequestOwnerActivity  roAct1 = new RequestOwnerActivity(mActivity);
        RequestOwnerActivity  roAct2 = new RequestOwnerActivity(mSameActivity);

        testRequestSame(roAct1, roAct2);
    }

    @Test
    public void testRequestSame_RequestOwnerAppCompatActivity() {
        RequestOwnerAppCompatActivity   roAct1 = new RequestOwnerAppCompatActivity(mAppCompatActivity);
        RequestOwnerAppCompatActivity   roAct2 = new RequestOwnerAppCompatActivity(mSameAppCompatActivity);

        testRequestSame(roAct1, roAct2);
    }

    @Test
    public void testRequestSame_RequestOwnerFragment() {
        RequestOwnerFragment    roFrag1 = new RequestOwnerFragment(mFragment);
        RequestOwnerFragment    roFrag2 = new RequestOwnerFragment(mSameFragment);

        testRequestSame(roFrag1, roFrag2);
    }

    //////////////////////////////////////////////////////////////////////////
    //  UI callbacks, do nothing
    //////////////////////////////////////////////////////////////////////////
    @Override
    public void showEducateModal(ProtectedAction action, int reqCode) {
        //  DO NOTHING
    }

    @Override
    public void showEducate(ProtectedAction action) {
        //  DO NOTHING
    }

    @Override
    public void showDeniedCritical(ProtectedAction action) {
        //  DO NOTHING
    }

    @Override
    public void showDeniedReminder(ProtectedAction action) {
        //  DO NOTHING
    }

    @Override
    public void showDeniedFeedback(ProtectedAction action) {
        //  DO NOTHING
    }
}
