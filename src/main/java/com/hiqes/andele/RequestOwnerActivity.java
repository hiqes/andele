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
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.View;

class RequestOwnerActivity extends RequestOwner {
    private Activity            mActivity;

    public RequestOwnerActivity(Activity activity) {
        mActivity = activity;
    }

    //@TargetApi(Build.VERSION_CODES.M)
    @TargetApi(23)
    @Override
    public int checkSelfPermission(String permission) {
        int                     ret = PackageManager.PERMISSION_GRANTED;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ret = mActivity.checkSelfPermission(permission);
        }

        return ret;
    }

    //@TargetApi(Build.VERSION_CODES.M)
    @TargetApi(23)
    @Override
    public void requestPermissions(String[] permissions, int code) {
        mActivity.requestPermissions(permissions, code);
    }

    //@TargetApi(Build.VERSION_CODES.M)
    @TargetApi(23)
    @Override
    public boolean shouldShowRequestPermissionRationale(String permission) {
        boolean                 ret = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ret = mActivity.shouldShowRequestPermissionRationale(permission);
        }

        return ret;
    }

    @Override
    PackageManager getPackageManager() {
        return mActivity.getPackageManager();
    }

    @Override
    public Context getUiContext() {
        return mActivity;
    }

    @Override
    public View getRootView() {
        return mActivity.findViewById(android.R.id.content);
    }
}
