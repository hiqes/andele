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
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.view.View;

class RequestOwnerSupportFragment extends RequestOwner {
    static final int            REQ_CODE_MASK = 0xFF;

    private Fragment            mSupportFrag;

    public RequestOwnerSupportFragment(Fragment supportFrag) {
        mSupportFrag = supportFrag;
    }

    //@TargetApi(Build.VERSION_CODES.M)
    @TargetApi(23)
    @Override
    public int checkSelfPermission(String permission) {
        int                     ret = PackageManager.PERMISSION_GRANTED;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ret = mSupportFrag.getActivity().checkSelfPermission(permission);
        }

        return ret;
    }

    //@TargetApi(Build.VERSION_CODES.M)
    @TargetApi(23)
    @Override
    public void requestPermissions(String[] permissions, int code) {
        mSupportFrag.requestPermissions(permissions, code);
    }

    //@TargetApi(Build.VERSION_CODES.M)
    @TargetApi(23)
    @Override
    public boolean shouldShowRequestPermissionRationale(String permission) {
        boolean                 ret = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ret = mSupportFrag.shouldShowRequestPermissionRationale(permission);
        }

        return ret;
    }

    @Override
    PackageManager getPackageManager() {
        return mSupportFrag.getActivity().getPackageManager();
    }

    @Override
    public int getReqeuestCodeMask() {
        return REQ_CODE_MASK;
    }

    @Override
    public Context getUiContext() {
        return mSupportFrag.getActivity();
    }

    @Override
    public View getRootView() {
        return mSupportFrag.getActivity().findViewById(android.R.id.content);
    }
}
