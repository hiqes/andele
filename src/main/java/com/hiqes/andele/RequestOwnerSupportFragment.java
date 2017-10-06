/*
 * Copyright (C) 2015 HIQES LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hiqes.andele;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.view.View;

import java.lang.ref.WeakReference;

class RequestOwnerSupportFragment extends RequestOwner {
    private static final String         TAG = RequestOwnerSupportFragment.class.getSimpleName();
    private static final int            REQ_CODE_MASK = 0x7F;

    private WeakReference<Fragment> mSupportFragRef;
    private ComponentName           mActivityCompName;
    private int                     mId;
    private String                  mTag;

    private Fragment getFragment() {
        return mSupportFragRef.get();
    }


    RequestOwnerSupportFragment(Fragment supportFrag) {
        mSupportFragRef = new WeakReference<>(supportFrag);
        mActivityCompName = supportFrag.getActivity().getComponentName();
        mId = supportFrag.getId();
        mTag = supportFrag.getTag();
    }

    //@TargetApi(Build.VERSION_CODES.M)
    @TargetApi(23)
    @Override
    public int checkSelfPermission(String permission) {
        int                     ret = PackageManager.PERMISSION_GRANTED;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Fragment frag = getFragment();

            if (frag == null) {
                //  The Fragment has been garbage collected, should NOT
                //  happen but fail gracefully with a log
                Log.e(TAG, "checkSelfPermission: Fragment no long valid, assume DENIED");
                ret = PackageManager.PERMISSION_DENIED;
            } else {
                ret = frag.getActivity().checkSelfPermission(permission);
            }
        }

        return ret;
    }

    //@TargetApi(Build.VERSION_CODES.M)
    @TargetApi(23)
    @Override
    public void requestPermissions(String[] permissions, int code) {
        Fragment                frag = getFragment();

        if (frag == null) {
            throw new IllegalStateException("Fragment no longer valid");
        }

        frag.requestPermissions(permissions, code);
    }

    //@TargetApi(Build.VERSION_CODES.M)
    @TargetApi(23)
    @Override
    public boolean shouldShowRequestPermissionRationale(String permission) {
        boolean                 ret = false;
        Fragment                frag = getFragment();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (frag == null) {
                Log.e(TAG, "shouldShowRequestPermissionRationale: Fragment no longer valid, assume FALSE");
            } else {
                ret = frag.shouldShowRequestPermissionRationale(permission);
            }
        }

        return ret;
    }

    @Override
    public boolean isSameOwner(RequestOwner otherOwner) {
        boolean                         ret = false;
        RequestOwnerSupportFragment     otherOwnerFrag;

        try {
            otherOwnerFrag = (RequestOwnerSupportFragment)otherOwner;

            //  Verify the parent activity is the same component
            if (mActivityCompName.equals(otherOwnerFrag.mActivityCompName)) {
                //  Tag may or may not be set.  If it is, make sure they
                //  match.  Otherwise, check the IDs.
                if ((mTag != null) &&
                        (otherOwnerFrag.mTag != null) &&
                        mTag.equals(otherOwnerFrag.mTag)) {
                    ret = true;
                } else if (mId == otherOwnerFrag.mId) {
                    ret = true;
                }
            }
        } catch (ClassCastException e) {
            //  Ignore
        }

        return ret;
    }

    @Override
    public boolean isParentActivity(Object obj) {
        boolean                 ret = false;
        Activity                otherAct;
        Fragment                frag = getFragment();

        try {
            otherAct = (Activity)obj;
            if (frag != null) {
                Activity        parentAct = frag.getActivity();

                if ((parentAct != null) && (parentAct == otherAct)) {
                    ret = true;
                }
            }
        } catch (ClassCastException e) {
            //  Ignore
        }
        return ret;
    }

    @Override
    PackageManager getPackageManager() {
        Fragment                frag = getFragment();

        //  Do not check for null, if this fails we have a state problem and
        //  the NPE we'll get will be helpful to track it down.
        return frag.getActivity().getPackageManager();
    }

    @Override
    Application getApplication() {
        return getFragment().getActivity().getApplication();
    }

    @Override
    public int getReqeuestCodeMask() {
        return REQ_CODE_MASK;
    }

    @Override
    public Context getUiContext() {
        return getFragment().getActivity();
    }

    @Override
    public View getRootView() {
        return getFragment().getActivity().findViewById(android.R.id.content);
    }
}
