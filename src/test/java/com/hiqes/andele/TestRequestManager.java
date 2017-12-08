package com.hiqes.andele;

import android.app.Activity;
import android.app.Application;
import android.app.Fragment;
import android.content.ComponentName;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

import static org.mockito.Mockito.*;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
public class TestRequestManager implements ProtectedAction.UserPromptCallback {
    private static final String         TEST_PACKAGE = "com.hiqes.andele.test.ui";
    private static final String         DUMMY_PERMISSION = "com.hiqes.andele.test.DUMMY_PERMISSION";
    private static final String         TEST_ACTIVITY = "TestActivity";

    private RequestManager              mManager = new RequestManager();

    @Mock
    private ComponentName               mTestCompName;

    @Mock
    private Activity                    mActivity;

    @Mock
    private AppCompatActivity           mAppCompatActivity;

    @Mock
    private Fragment                    mFragment;

    @Mock
    private Application                 mApplication;

    private Application.ActivityLifecycleCallbacks  mCb;

    @Mock
    private Handler                     mHandler;

    @BeforeClass
    public static void preClassSetup() {
        Andele.setLogger(new Andele.Logger() {
            @Override
            public void log(int priority, String tag, String msg) {
                System.out.printf("[%d] [%s] %s\n", priority, tag, msg);
            }
        });
    }

    @Before
    public void setup() {
        when(mTestCompName.getPackageName()).thenReturn(TEST_PACKAGE);
        when(mTestCompName.getClassName()).thenReturn(TEST_ACTIVITY);

        when(mActivity.getComponentName()).thenReturn(mTestCompName);
        when(mActivity.getApplication()).thenReturn(mApplication);

        when(mAppCompatActivity.getComponentName()).thenReturn(mTestCompName);
        when(mAppCompatActivity.getApplication()).thenReturn(mApplication);

        when(mFragment.getActivity()).thenReturn(mActivity);

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                mCb = (Application.ActivityLifecycleCallbacks)invocation.getArguments()[0];
                return null;
            }
        }).when(mApplication)
                .registerActivityLifecycleCallbacks(any(Application.ActivityLifecycleCallbacks.class));
    }

    private void testRequestManager_manageRequest(RequestOwner owner) {
        ProtectedAction.Builder paBldr = new ProtectedAction.Builder();
        ProtectedAction actions[] = new ProtectedAction[1];
        actions[0] =
                paBldr.withPermission(DUMMY_PERMISSION)
                        .withUsage(PermissionUse.CRITICAL)
                        .actionCallback(new ProtectedAction.ActionCallback() {
                            @Override
                            public void doAction(ProtectedAction action) {
                                throw new RuntimeException("doAction called");
                            }
                        })
                        .userPromptCallback(this)
                        .build();

        int reqCode = mManager.queueRequest(owner, actions, mHandler);
        assertNotEquals(-1, reqCode);

        //  Retrieve the request, verify it is the same
        Request verifyReq = mManager.getRequest(reqCode);
        assertNotNull(verifyReq);
        assertEquals(owner, verifyReq.getOwner());
        assertEquals(actions[0], verifyReq.getAction());

        //  Remove the request, verify it is the same as what we got before
        Request rmReq = mManager.removeRequest(reqCode);
        assertEquals(verifyReq, rmReq);

        //  Retrieve the request again, should not get anything
        Request againReq = mManager.getRequest(reqCode);
        assertNull(againReq);
    }

    @Test
    public void testRequestManager_manageRequest_Activity() {
        //  Create a protected action and queue it, verify that works
        RequestOwnerActivity reqOwnerAct = new RequestOwnerActivity(mActivity);
        testRequestManager_manageRequest(reqOwnerAct);
    }

    @Test
    public void testRequestManager_manageRequest_AppCompatActivity() {
        //  Create a protected action and queue it, verify that works
        RequestOwnerAppCompatActivity reqOwnerAct = new RequestOwnerAppCompatActivity(mAppCompatActivity);
        testRequestManager_manageRequest(reqOwnerAct);
    }

    @Test
    public void testRequestManager_manageRequest_Fragment() {
        //  Create a protected action and queue it, verify that works
        RequestOwnerFragment reqOwnerFrag = new RequestOwnerFragment(mFragment);
        testRequestManager_manageRequest(reqOwnerFrag);
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
