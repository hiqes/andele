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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Looper;
import android.provider.Settings;

class Util {
    private static final String         SHARED_PREFS_NAME = "andele.prefs";
    private static final String         PREFIX_EDU_DONE = "_edu_done:";
    private static final String         PREFIX_EDU_DONE_RESET = "_edu_done_reset:";

    static boolean isMainThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(SHARED_PREFS_NAME,
                                            Context.MODE_PRIVATE);
    }

    static boolean isEduDone(Context context, PermissionDetails perm) {
        boolean                 ret;
        String                  key = PREFIX_EDU_DONE + perm.asKey();

        ret = getPrefs(context).getBoolean(key, false);
        return ret;
    }

    @SuppressLint("ApplySharedPref")
    static void setEduDone(Context context, PermissionDetails perm) {
        SharedPreferences.Editor    ed = getPrefs(context).edit();
        String                      key = PREFIX_EDU_DONE + perm.asKey();

        ed.putBoolean(key, true);
        ed.commit();
    }

    static boolean isEduDoneReset(Context context, PermissionDetails perm) {
        boolean                 ret;
        String                  key = PREFIX_EDU_DONE_RESET + perm.asKey();

        ret = getPrefs(context).getBoolean(key, false);
        return ret;
    }

    @SuppressLint("ApplySharedPref")
    private static void storeEduDoneReset(Context context, PermissionDetails perm, boolean value) {
        SharedPreferences.Editor    ed = getPrefs(context).edit();
        String                      key = PREFIX_EDU_DONE_RESET + perm.asKey();

        ed.putBoolean(key, value);
        ed.commit();
    }

    static void setEduDoneReset(Context context, PermissionDetails perm) {
        storeEduDoneReset(context, perm, true);
    }

    static void clearEduDoneReset(Context context, PermissionDetails perm) {
        storeEduDoneReset(context, perm, false);
    }

    static void startSettingsApp(Context ctx) {
        String dataPkg =
                "package:" + ctx.getPackageName();
        Intent settingsIntent =
            new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        settingsIntent.addCategory(Intent.CATEGORY_DEFAULT);
        settingsIntent.setData(Uri.parse(dataPkg));
        ctx.startActivity(settingsIntent);
    }
}
