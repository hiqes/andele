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

import android.app.Activity;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.view.View;


@SuppressWarnings("unused")
public class SimpleUserPrompter implements ProtectedAction.UserPromptCallback {
    private static final String TAG = SimpleUserPrompter.class.getSimpleName();

    private Activity            mActivity;
    private View                mRoot;
    private int                 mEduModalRes;
    private int                 mEduRes;
    private int                 mDeniedCriticalRes;
    private int                 mDeniedRemindRes;
    private int                 mDeniedFeedbackRes;

    public SimpleUserPrompter(Activity activity,
                              View    root,
                              int     eduModalRes,
                              int     eduRes,
                              int     deniedCriticalRes,
                              int     deniedRemindRes,
                              int     deniedFeedbackRes) {
        mActivity          = activity;
        mRoot              = root;
        mEduModalRes       = eduModalRes;
        mEduRes            = eduRes;
        mDeniedCriticalRes = (deniedCriticalRes != -1) ?
            deniedCriticalRes : R.string.andele__default_denied_critical;
        mDeniedRemindRes   = (deniedRemindRes != -1) ?
            deniedRemindRes : R.string.andele__default_denied_reminder;
        mDeniedFeedbackRes = (deniedFeedbackRes != -1) ?
            deniedFeedbackRes : R.string.andele__default_denied_feeback;
    }

    private String getPermissionText(int res, String permission) {
        String                  ret;
        String                  resString;
        String                  permLabel;

        Resources appResources = mActivity.getResources();

        resString = appResources.getString(res);

        //  Just pick up the last part of the full-qualified permission name.
        int tmp = permission.lastIndexOf('.');
        if (tmp != 0) {
            tmp++;
        }

        permLabel = permission.substring(tmp);

        ret = String.format(resString, permLabel);
        return ret;
    }

    private void doDeniedSnackbar(ProtectedAction action, int msgRes) {
        String                  feedback;

        feedback = getPermissionText(msgRes,
                                     action.mPermDetails.mPermission);

        Snackbar.make(mRoot,
                      feedback,
                      Snackbar.LENGTH_LONG)
                .setAction(R.string.andele__settings_label, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Util.startSettingsApp(mActivity);
                    }
                })
                .show();
    }

    @Override
    public void showEducateModal(ProtectedAction action, int reqCode) {
        Log.e(TAG, "showEducateModal: IMPLEMENT ME");
        //  TODO - SHOW THIS AS A MODAL DIALOG, FULL SCREEN
    }

    @Override
    public void showEducate(ProtectedAction action) {
        Log.e(TAG, "showEducate: IMPLEMENT ME");
        //  TODO - SHOW THIS AS A BOTTOM SHEET?
    }

    @Override
    public void showDeniedCritical(ProtectedAction action) {
        AlertDialog.Builder     bldr = new AlertDialog.Builder(mActivity);
        String                  message;

        message = getPermissionText(mDeniedCriticalRes,
                                    action.mPermDetails.mPermission);
        bldr.setCancelable(false)
            .setTitle(R.string.andele__critical_title)
            .setMessage(message)
            .setNegativeButton(R.string.andele__exit_text,
                    new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mActivity.finish();
                    }
                })
            .setPositiveButton(R.string.andele__settings_label,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Andele.startSettingsApp(mActivity);
                        }
                    });

        bldr.create().show();
    }

    @Override
    public void showDeniedReminder(ProtectedAction action) {
        doDeniedSnackbar(action, mDeniedRemindRes);
    }

    @Override
    public void showDeniedFeedback(ProtectedAction action) {
        doDeniedSnackbar(action, mDeniedFeedbackRes);
    }
}
