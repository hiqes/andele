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
import android.view.View;

import java.lang.ref.WeakReference;

class RequestOwnerActivity extends RequestOwner {
    private static final String         TAG = RequestOwnerActivity.class.getSimpleName();

    private WeakReference<Activity>     mActivityRef;
    private ComponentName               mCompName;

    RequestOwnerActivity(Activity activity) {
        mActivityRef = new WeakReference<>(activity);
        mCompName = activity.getComponentName();
    }

    private Activity getActivity() {
        return mActivityRef.get();
    }

    //@TargetApi(Build.VERSION_CODES.M)
    @TargetApi(23)
    @Override
    public int checkSelfPermission(String permission) {
        int                     ret = PackageManager.PERMISSION_GRANTED;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Activity            act = getActivity();

            if (act == null) {
                //  The Activity has been garbage collected, should NOT
                //  happen but fail gracefully with a log
                Log.e(TAG, "checkSelfPermission: Activity no long valid, assume DENIED");
                ret = PackageManager.PERMISSION_DENIED;
            } else {
                ret = act.checkSelfPermission(permission);
            }
        }

        return ret;
    }

    //@TargetApi(Build.VERSION_CODES.M)
    @TargetApi(23)
    @Override
    public void requestPermissions(String[] permissions, int code) {
        Activity                act = getActivity();

        if (act == null) {
            throw new IllegalStateException("Activity no longer valid");
        }

        act.requestPermissions(permissions, code);
    }

    //@TargetApi(Build.VERSION_CODES.M)
    @TargetApi(23)
    @Override
    public boolean shouldShowRequestPermissionRationale(String permission) {
        boolean                 ret = false;
        Activity                act = getActivity();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (act == null) {
                Log.e(TAG, "shouldShowRequestPermissionRationale: Activity no longer valid, assume FALSE");
            } else {
                ret = act.shouldShowRequestPermissionRationale(permission);
            }
        }

        return ret;
    }

    @Override
    public boolean isSameOwner(RequestOwner otherOwner) {
        boolean                 ret = false;
        RequestOwnerActivity    otherOwnerAct = null;

        try {
            otherOwnerAct = (RequestOwnerActivity)otherOwner;
        } catch (ClassCastException e) {
            //  just pass through
        }

        if (otherOwnerAct != null) {
            if (mCompName.equals(otherOwnerAct.mCompName)) {
                ret = true;
            }
        }

        return ret;
    }

    @Override
    public boolean isParentActivity(Object obj) {
        boolean                 ret = false;

        try {
            Activity            otherAct = (Activity)obj;
            Activity            act = mActivityRef.get();

            if ((act != null) && (act == otherAct)) {
                ret = true;
            }
        } catch (ClassCastException e) {
            //  Ignore
        }

        return ret;
    }

    @Override
    PackageManager getPackageManager() {
        Activity                act = getActivity();

        //  Do not check for null, if this fails we have a state problem and
        //  the NPE we'll get will be helpful to track it down.
        return act.getPackageManager();
    }

    @Override
    Application getApplication() {
        return getActivity().getApplication();
    }

    @Override
    public Context getUiContext() {
        return getActivity();
    }

    @Override
    public View getRootView() {
        return getActivity().findViewById(android.R.id.content);
    }
}
