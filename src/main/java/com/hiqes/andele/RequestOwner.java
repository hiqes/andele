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

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.view.View;

public abstract class RequestOwner {
    static final int            DEFAULT_MASK = 0xFFFFFFFF;

    public abstract int checkSelfPermission(String permission);
    public abstract void requestPermissions(String[] permissions, int code);
    public abstract boolean shouldShowRequestPermissionRationale(String permission);
    public abstract Context getUiContext();
    public abstract View getRootView();
    abstract PackageManager getPackageManager();

    public int getReqeuestCodeMask() {
        return DEFAULT_MASK;
    }

    public final PermissionInfo getPermissionInfo(String permission) {
        PermissionInfo          info = null;
        PackageManager          pm = getPackageManager();

        //  Ask the package manager for a list of all defined permissions
        //  in the system and track this one down.
        try {
            info = pm.getPermissionInfo(permission, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            //  nothing
        }

        return info;
    }
}
