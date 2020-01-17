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
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;

import java.util.ArrayList;


/**
 * Andele, the Android Permissions delegate helper API.
 * <p>
 * Use this to manage {@link com.hiqes.andele.ProtectedAction} instances to
 * assist with requesting permissions and interacting with the user.
 */
@SuppressWarnings("WeakerAccess")
public class Andele {
    static private final String                TAG = Andele.class.getSimpleName();

    static private final int                   MSG_DO_ACTION = 1;
    static private final int                   MSG_SHOW_EDUCATE = 10;
    static private final int                   MSG_SHOW_EDUCATE_REMINDER = 13;
    static private final int                   MSG_SHOW_DENIED_CRITICAL = 14;
    static private final int                   MSG_SHOW_DENIED_FEEDBACK = 15;
    static private final int                   MSG_DENIED = 16;
    static private final int                   MSG_GO_TO_SETTINGS = 50;

    private static final RequestManager        sReqMgr = new RequestManager();
    private static PermResultHandler           sHandler;

    private static void lazyInit(Context context) {
        if (sHandler == null) {
             sHandler = new PermResultHandler(context.getMainLooper());
        }
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    private static Handler getReqHandler() {
        Handler handler = Util.isMainThread() ? sHandler : new PermResultHandler();
        return handler;
    }

    private static void doRequest(int reqCode) {
        Request                 req;

        req = sReqMgr.getRequest(reqCode);

        if (req != null) {
            ProtectedAction[]   actions = req.getActions();
            String[]            perms = new String[actions.length];

            for (int i = 0; i < actions.length; i++) {
                perms[i] = actions[i].mPermDetails.mPermission;
            }

            req.getOwner().requestPermissions(perms, reqCode);
        }
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    private static void checkAndExecute(RequestOwner owner, ProtectedAction[] actions) {
        ArrayList<ProtectedAction>  reqActions = null;
        int                         status;
        int                         firstEduIndex = -1;

        //  Do a lazy init to make sure our main thread handler is setup
        lazyInit(owner.getApplication());

        //  Walk through the actions, check the permissions.  If the app
        //  already has them then call back the app.  Otherwise, we'll need
        //  to interface with the user on this.
        for (int i = 0; i < actions.length; i++) {
            ProtectedAction     curAction = actions[i];

            status = owner.checkSelfPermission(curAction.mPermDetails.mPermission);
            if (status == PackageManager.PERMISSION_GRANTED) {
                //  Boom!  We got it already!  Call back the action immediately
                //  as we are already in the same context.
                curAction.mListener.onPermissionGranted(curAction.mPermDetails);
                curAction.mActionCb.doAction(curAction);
            } else {
                //  We don't have this one, so we need to ask for it or
                //  possibly educate the user.
                if (reqActions == null) {
                    reqActions = new ArrayList<>();
                }

                reqActions.add(curAction);

                if (curAction.hasUserEdu() && (firstEduIndex == -1)) {
                    firstEduIndex = reqActions.indexOf(curAction);
                    Log.d(TAG,
                          "Action with perm '" +
                              curAction.mPermDetails.mPermission +
                              "' has edu, index: " +
                              firstEduIndex);
                }
            }
        }

        //  If reqActions is not empty, we need to ask for the permission(s)
        //  and possibly educate te user.
        if ((reqActions != null) && (reqActions.size() > 0)) {
            ProtectedAction[]   needyActions = new ProtectedAction[reqActions.size()];
            needyActions = reqActions.toArray(needyActions);

            //  First things first, queue the request with the needy actions
            //  which contains just the subset of stuff that needs edu/req.
            int reqCode = sReqMgr.queueRequest(owner, needyActions, getReqHandler());
            if (reqCode >= 0) {
                //  If firstEduIndex is not -1, somebody needs an explanation.
                if (firstEduIndex != -1) {
                    Log.d(TAG, "Show edu for req " + reqCode);
                    showEducateUi(reqCode, firstEduIndex);
                } else {
                    doRequest(reqCode);
                }
            } else {
                Log.d(TAG, "checkAndExecute: req already queued and being processed");
            }
        }
    }

    private static void checkAndExecute(RequestOwner owner, ProtectedAction action) {
        ProtectedAction[]       actions = new ProtectedAction[1];

        actions[0] = action;
        checkAndExecute(owner, actions);
    }

    /**
     * Check to see if the application has been granted the permission
     * described by the {@link com.hiqes.andele.ProtectedAction ProtectedAction}
     * before trying to execute the action.  The action will only be taken
     * if the permission has already been granted or the user grants it.
     * If the user has not already granted the app this permission, it will be
     * requested.  This call may educate the user or prompt them with details
     * in order to provide a consistent UX.  Note that the educate UX pattern
     * used here is intended to be used for "educate in context" as the action
     * will be executed once permission has been granted.  This means that for
     * Critical, non-obvious actions ({@link com.hiqes.andele.PermissionUse#ESSENTIAL ESSENTIAL}
     * the educate UX may be presented in context if you fail to first use
     * #checkMandatoryPermissions.
     * <p>
     * UX types:
     * <ul>
     * <li>Educate in context: presented before permission request, if not already granted.
     * This will only be shown the first time the permission is used/requested.</li>
     * <li>Denied critical: presented to inform the user that a CRITICAL permission
     * has been denied and they must go into Settings and adjust permissions in
     * order to use the app.  This is typically shown as a modal and when dismissed
     * the app exits.</li>
     * <li>Denied remind: presented when permission has been denied for a ProtectedAction
     * which is ESSENTIAL, thus crippling the application's functionality.  Gives the user the
     * option of going to Settings to adjust.  This is typically presented using a Snackbar.</li>
     * <li>Denial feedback: presented when permission has been denied, optionally
     * give the user another chance to enable it by going to Settings.  This
     * is typically presented as a Snackbar.</li>
     * </ul>
     *
     * The ProtectedAction.Listener (if present) is called back on grants
     * as soon as possible, in case the UX needs to change before the ProtectedAction.ActionCallback
     * is executed.  When permissions are denied, the ProtectedAction.Listener
     * is called back after any denial UX is presented and dismissed.
     *
     * @param activity   The owning Activity from which the action is being made
     * @param action     The ProtectedAction describing the permission needed,
     *                   the ActionCallback to execute when granted, etc.
     */
    @SuppressWarnings({"WeakerAccess", "unused"})
    public static void checkAndExecute(Activity activity, ProtectedAction action) {
        checkAndExecute(new RequestOwnerActivity(activity), action);
    }

    /**
     * Check to see if the application has been granted the permission
     * described by the {@link com.hiqes.andele.ProtectedAction ProtectedAction}
     * before trying to execute the action.  The action will only be taken
     * if the permission has already been granted or the user grants it.
     * If the user has not already granted the app this permission, it will be
     * requested.  See {@link #checkAndExecute(Activity, ProtectedAction)} for
     * more details.
     * <p>
     * @param activity   The owning Activity (compatibility library) from
     *                   which the action is being made
     * @param action     The ProtectedAction describing the permission needed,
     *                   the ActionCallback to execute when granted, etc.
     */
    @SuppressWarnings({"WeakerAccess", "unused"})
    public static void checkAndExecute(AppCompatActivity activity, ProtectedAction action) {
        checkAndExecute(new RequestOwnerAppCompatActivity(activity), action);
    }

    /**
     * Check to see if the application has been granted the permission
     * described by the {@link com.hiqes.andele.ProtectedAction ProtectedAction}
     * before trying to execute the action.  The action will only be taken
     * if the permission has already been granted or the user grants it.
     * If the user has not already granted the app this permission, it will be
     * requested.  See {@link #checkAndExecute(Activity, ProtectedAction)} for
     * more details.
     * <p>
     * @param fragment         The owning Fragment from which the action is
     *                         being made
     * @param action           The ProtectedAction describing the permission needed,
     *                         the ActionCallback to execute when granted, etc.
     */
    @SuppressWarnings({"WeakerAccess", "unused"})
    public static void checkAndExecute(Fragment fragment, ProtectedAction action) {
        checkAndExecute(new RequestOwnerFragment(fragment), action);
    }

    /**
     * Check to see if the application has been granted the permission
     * described by the {@link com.hiqes.andele.ProtectedAction ProtectedAction}
     * before trying to execute the action.  The action will only be taken
     * if the permission has already been granted or the user grants it.
     * If the user has not already granted the app this permission, it will be
     * requested.  See {@link #checkAndExecute(Activity, ProtectedAction)} for
     * more details.
     * <p>
     * @param fragment         The owning Fragment (support library) from
     *                         which the action is being made
     * @param action           The ProtectedAction describing the permission needed,
     *                         the ActionCallback to execute when granted, etc.
     */
    @SuppressWarnings({"WeakerAccess", "unused"})
    public static void checkAndExecute(androidx.fragment.app.Fragment fragment, ProtectedAction action) {
        checkAndExecute(new RequestOwnerAndroidXFragment(fragment), action);
    }

    private static final ProtectedAction.ActionCallback mEmptyActionCallback = new ProtectedAction.ActionCallback() {
        @Override
        public void doAction(ProtectedAction action) {
            //  Do nothing.  This is used for check-only tests so the code
            //  is common.
        }
    };

    @SuppressWarnings("ForLoopReplaceableByForEach")
    private static void checkAndRequestMandatoryPermissions(RequestOwner owner, ProtectedAction[] actions) {
        ArrayList<ProtectedAction>  revisedActions = new ArrayList<>();

        //  It's important that this API only be used for CRITICAL and
        //  ESSENTIAL type permission use.  This is a way of asking for
        //  these perms up front in the app.
        for (int i = 0; i < actions.length; i++) {
            ProtectedAction     curAction = actions[i];

            if ((curAction.mPermDetails.mUsage != PermissionUse.CRITICAL) &&
                (curAction.mPermDetails.mUsage != PermissionUse.ESSENTIAL)) {
                Log.w(TAG,
                      "checkAndRequestMandatoryPermissions: Toss perm \'" +
                          curAction.mPermDetails.mPermission +
                          "', marked " +
                          curAction.mPermDetails.mUsage.name());

                continue;
            }

            ProtectedAction.Builder bldr = new ProtectedAction.Builder();
            bldr.actionCallback(mEmptyActionCallback)
                .userPromptCallback(curAction.mPromptCb)
                .listener(curAction.mListener)
                .withPermission(curAction.mPermDetails.mPermission)
                .withUsage(curAction.mPermDetails.mUsage);
            revisedActions.add(bldr.build());
        }

        //  Now create a new request that has all of the permissions together.
        actions = new ProtectedAction[revisedActions.size()];
        actions = revisedActions.toArray(actions);

        //  Call through to the "normal" checkAndExecute.  Since we took over
        //  the callbacks but kept the rest, the ProtectedAction listeners
        //  will get hit as expected but no action callback.
        checkAndExecute(owner, actions);
    }

    /**
     * Check to see if the application has been granted the provided
     * permissions.  If the permissions have not been granted to the app, this
     * will request them from the system.  These permissions should only be
     * {@link PermissionUse#CRITICAL CRITICAL} or {@link PermissionUse#ESSENTIAL ESSENTIAL}
     * permissions.  Others will be silently ignored and should be used with
     * {@code checkAndExecute}.  The action callback for the provided
     * action will not be executed, but any attached listener will be called
     * to notify the app that the permissions are either granted or denied.
     *
     * Both {@code PermissionUse.CRITICAL} and {@code PermissionUse.ESSENTIAL}
     * may educate the user of their purpose.  This is accomplished via the
     * {@link ProtectedAction.UserPromptCallback} associated with the
     * {@code ProtectedAction} for a specific permission.  In both cases,
     * the specific educate UI callback will be triggered via this call, if needed.
     * In this case of {@code CRITICAL} permissions, the
     * {@code UserPromptCallback.showDeniedCritical} callback will be executed
     * if the permission is denied.  It is up to the app to properly exit if
     * a {@code CRITICAL} permission is denied.
     * The listener callback for denial will not be triggered until after any
     * UX components are shown.
     * <p>
     * This method can trigger asynchronous operations.  Apps utilizing this call
     * should not proceed with normal operations until the listener callback(s)
     * are triggered for each of the provided ProtectedAction objects.
     *
     * @param activity   The owning Activity making the request
     * @param actions    An array of ProtectedActions describing the permissions
     *                   which are mandatory (i.e. {@code} CRITICAL) for the app.
     */
    @SuppressWarnings({"WeakerAccess", "unused"})
    public static void checkAndRequestMandatoryPermissions(Activity activity, ProtectedAction[] actions) {
        checkAndRequestMandatoryPermissions(new RequestOwnerActivity(activity), actions);
    }

    /**
     * Check to see if the application has been granted the provided
     * permissions.  See {@link #checkAndRequestMandatoryPermissions(Activity, ProtectedAction[])}
     * for details about the behavior.
     * <p>
     * @param activity   The owning Activity (compatibility library) making the request
     * @param actions    An array of ProtectedActions describing the permissions
     *                   which are mandatory (i.e. {@code} CRITICAL) for the app.
     */
    @SuppressWarnings("unused")
    public static void checkAndRequestMandatoryPermissions(AppCompatActivity activity, ProtectedAction[] actions) {
        checkAndRequestMandatoryPermissions(new RequestOwnerAppCompatActivity(activity), actions);
    }

    /**
     * Check to see if the application has been granted the provided
     * permissions.  See {@link #checkAndRequestMandatoryPermissions(Activity, ProtectedAction[])}
     * for details about the behavior.
     * <p>
     * @param fragment   The owning Fragment making the request
     * @param actions    An array of ProtectedActions describing the permissions
     *                   which are mandatory (i.e. {@code} CRITICAL) for the app.
     */
    @SuppressWarnings("unused")
    public static void checkAndRequestMandatoryPermissions(Fragment fragment, ProtectedAction[] actions) {
        checkAndRequestMandatoryPermissions(new RequestOwnerFragment(fragment), actions);
    }

    /**
     * Check to see if the application has been granted the provided
     * permissions.  See {@link #checkAndRequestMandatoryPermissions(Activity, ProtectedAction[])}
     * for details about the behavior.
     * <p>
     * @param fragment   The owning support Fragment making the request
     * @param actions    An array of ProtectedActions describing the permissions
     *                   which are mandatory (i.e. {@code} CRITICAL) for the app.
     */
    @SuppressWarnings("unused")
    public static void checkAndRequestMandatoryPermissions(androidx.fragment.app.Fragment fragment, ProtectedAction[] actions) {
        checkAndRequestMandatoryPermissions(new RequestOwnerAndroidXFragment(fragment), actions);
    }

    /**
     * This method should be called once the educate UI modal has been displayed
     * to the user for the specific ProtectedAction and request code passed
     * to the app's showEducateModal() method.  Using this mechanism, the app
     * can inform Andele that educate has been done for an action protected
     * by an ESSENTIAL usage permission so it will not be requested again.
     * <p>
     * @param reqCode   The request code previously provided to the app's
     *                  showEducateModal() method
     * @param action    The ProtectedAction previously provided to the app's
     *                  showEducateModal() method
     */
    @SuppressWarnings({"WeakerAccess", "unused"})
    public static void markEducateModalDone(int reqCode, ProtectedAction action) {
        Request                 req;
        int                     actionIndex = -1;
        ProtectedAction[]       actions;

        //  Sanity check
        req = sReqMgr.getRequest(reqCode);
        if (req == null) {
            Log.w(TAG, "markEducateModalDone: unknown req " + reqCode);
            return;
        }

        if (action == null) {
            throw new IllegalArgumentException("No action provided");
        }

        if (action.mPermDetails.mUsage != PermissionUse.ESSENTIAL) {
            throw new IllegalArgumentException("Invalid action provided: " +
                                               action.mPermDetails.mPermission +
                                               ", " +
                                               action.mPermDetails.mUsage);
        }

        actions = req.getActions();
        for (int i = 0; i < actions.length; i++) {
            if (actions[i] == action) {
                actionIndex = i;
                break;
            }
        }

        if (actionIndex == -1) {
            throw new IllegalArgumentException("Provided action not part of active request");
        }

        //  Mark the action's permission has been done then re-call showEducateUi
        //  so we'll move on to the next (if any.)
        Util.setEduDone(req.getOwner().getUiContext(), action.mPermDetails);
        showEducateUi(reqCode, actionIndex);
    }

    /**
     * After calling {@link #checkAndExecute}, Andele may have requested the required
     * permissions for the ProtectedAction.  This is an asynchronous operation
     * and will ultimately provide a response via the owner (Activity, Fragment,
     * etc.)  The app must properly call this method from the owner's
     * callback first to determine if this is a permission request that Andele
     * is handling.  If the app is using Andele for all permissions handling
     * (and it should!) then simply call through to this method and do nothing
     * else.
     * <p>
     * Note that for Activities where permissions are being requested (this
     * includes AppCompatActivity derivatives), care must be taken when
     * Fragments are also in use or if you decided to mix and handle some
     * permissions work yourself.  The best practice is to call through to
     * {@code onRequestPermissionsResult()} and if it returns {@code false}
     * call through to the superclass:
     * <pre>
     * {@code
     * {@literal @>}Override
     * public static void onRequestPermissionsResult(int reqCode, String[] permissions, int[] grantResults) {
     *     if (!Andele.onRequestPermissionsResult(reqCode, permissions, grantResults)) {
     *         super.onRequestPermissionsResult(reqCode, permissions, grantResults);
     *     }
     * }
     * }
     * </pre>
     * @param reqCode   The request code for the permissions request
     * @param permissions   The array of permissions which were requested
     * @param grantResults  The results of the permissions request
     * @return true if this method handled the results, otherwise false
     */
    @SuppressWarnings("unused")
    public static boolean onRequestPermissionsResult(int reqCode, String[] permissions, int[] grantResults) {
        boolean                 handled = false;
        Request                 req;
        boolean                 removeReq = true;

        //  Get the request from the manager.  We do not remove it from the
        //  manager until it is completely processed (success or fail.)
        //  If we cannot find it, complain about it.
        req = sReqMgr.getRequest(reqCode);
        if (req == null) {
            Log.w(TAG, "onRequestPermissionsResult: request not found for code " + reqCode);
        } else {
            ProtectedAction[] reqActions = req.getActions();
            int actionCount = req.getActionCount();

            for (int i = 0; i < permissions.length; i++) {
                String curPerm = permissions[i];

                for (int j = 0; j < actionCount; j++) {
                    ProtectedAction curAction = reqActions[j];

                    if (!TextUtils.equals(curAction.mPermDetails.mPermission,
                            curPerm)) {
                        continue;
                    }

                    Message msg;

                    //  Found a match with the request, fire up
                    //  the right message to deal with it.
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        //  Call back the action handler, let them know
                        //  the grant was done.
                        curAction.mListener.onPermissionGranted(curAction.mPermDetails);

                        //  Now allow the action to take place.  Use the
                        //  request's handler for this since the original
                        //  execute request could have come on a different
                        //  thread.
                        msg = new Message();
                        msg.what = MSG_DO_ACTION;
                        msg.obj = curAction;
                        req.getHandler().sendMessage(msg);
                    } else {
                        //  The permission request was denied.  Now figure out
                        //  what to show the user, if anything.
                        PermissionUse curUsage = curAction.mPermDetails.mUsage;
                        if (curUsage == PermissionUse.CRITICAL) {
                            //  If the permission is CRITICAL we need to inform
                            //  the user this is a big problem.
                            removeReq = false;
                            showDeniedCritical(req, j);
                            continue;
                        } else if (curUsage == PermissionUse.ESSENTIAL) {
                            //  If this is an ESSENTIAL permission then remind
                            //  the user of the problems with denial.
                            removeReq = false;
                            showDeniedReminder(req, j);
                            continue;
                        } else if (req.getOwner().shouldShowRequestPermissionRationale(curPerm)) {
                            //  This permission covers a secondary type of feature
                            //  so as long as the user is open to feedback go
                            //  ahead and provide it.
                            removeReq = false;
                            showDeniedFeedback(req, j);
                            continue;
                        }

                        //  If we get here there was either no UI to show for
                        //  this action or the app disabled UX helper.
                        notifyDenied(curAction);
                    }
                }
            }

            handled = true;
        }

        if (removeReq) {
            sReqMgr.removeRequest(reqCode);
        }

        return handled;
    }

    /**
     * Helper API to start the Settings app directly into the details page
     * for the app. This allows the user to quickly change the permissions
     * granted/denied for the app.
     * <p>
     * @param activity   The Activity in the running state making the request.
     */
    @SuppressWarnings({"WeakerAccess", "unused"})
    public static void startSettingsApp(Activity activity) {
        startSettingsApp((Context)activity);
    }

    /**
     * Helper API to start the Settings app directly into the details page
     * for the app. This allows the user to quickly change the permissions
     * granted/denied for the app.
     * <p>
     * @param activity   The Activity in the running state making the request.
     */
    @SuppressWarnings({"WeakerAccess", "unused"})
    public static void startSettingsApp(AppCompatActivity activity) {
        startSettingsApp((Context)activity);
    }

    /**
     * Helper API to start the Settings app directly into the details page
     * for the app. This allows the user to quickly change the permissions
     * granted/denied for the app.
     * <p>
     * @param fragment   The Fragment in the running state making the request.
     */
    @SuppressWarnings({"WeakerAccess", "unused"})
    public static void startSettingsApp(Fragment fragment) {
        startSettingsApp((Context)fragment.getActivity());
    }

    /**
     * Helper API to start the Settings app directly into the details page
     * for the app. This allows the user to quickly change the permissions
     * granted/denied for the app.
     * <p>
     * @param fragment   The Fragment in the running state making the request.
     */
    @SuppressWarnings({"WeakerAccess", "unused"})
    public static void startSettingsApp(androidx.fragment.app.Fragment fragment) {
        startSettingsApp((Context)fragment.getActivity());
    }


    /**
     * Interface for internal logging calls.  An external logger can be used
     * by Andele by calling {@link Andele#setLogger(Logger)}.
     */
    public interface Logger {
        /**
         * Make a log entry with the provided information.
         *
         * @param priority  One of the priority values from {@link android.util.Log}.
         * @param tag       A module specific tag for the log entry
         * @param msg       The message to be logged
         */
        void log(int priority, String tag, String msg);
    }


    /**
     * Use this to set an internal logger to be used by Andele.  By default
     * Andele will not have logging enabled until it is built for debug.
     *
     * @param newLogger   A class implementing the {@code Logger} interface.
     */
    public static void setLogger(Logger newLogger) {
        Log.setLogger(newLogger);
    }


    private static void startSettingsApp(Context uiContext) {
        //  Do lazyInit to make sure the handler is ready to go
        lazyInit(uiContext);

        Message msg = sHandler.obtainMessage(MSG_GO_TO_SETTINGS);
        msg.obj = uiContext;
        msg.sendToTarget();
    }

    private static void notifyDenied(ProtectedAction action) {
        Message                 msg = sHandler.obtainMessage(MSG_DENIED);

        msg.obj = action;
        msg.sendToTarget();
    }

    private static void showEducateUi(int reqCode, int actionIndex) {
        Message                 msg = sHandler.obtainMessage(MSG_SHOW_EDUCATE);

        msg.arg1 = reqCode;
        msg.arg2 = actionIndex;
        msg.sendToTarget();
    }

    private static void showDeniedCritical(Request req, int actionIndex) {
        Message                 msg = sHandler.obtainMessage(MSG_SHOW_DENIED_CRITICAL);

        msg.obj = req;
        msg.arg1 = actionIndex;
        msg.sendToTarget();
    }

    private static void showDeniedReminder(Request req, int actionIndex) {
        Message                 msg = sHandler.obtainMessage(MSG_SHOW_EDUCATE_REMINDER);

        msg.obj = req;
        msg.arg1 = actionIndex;
        msg.sendToTarget();
    }

    private static void showDeniedFeedback(Request req, int actionIndex) {
        Message                 msg = sHandler.obtainMessage(MSG_SHOW_DENIED_FEEDBACK);

        msg.obj = req;
        msg.arg1 = actionIndex;
        msg.sendToTarget();
    }

    private static class PermResultHandler extends Handler {
        PermResultHandler() {
            super();
        }

        PermResultHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            ProtectedAction     action;
            Request             req = null;
            boolean             removeReq = false;
            boolean             curEduDone;
            Context             context;

            switch(msg.what) {
                case MSG_DO_ACTION:
                    //  It's go time!  Let the originator fire things up
                    action = (ProtectedAction)msg.obj;
                    action.mActionCb.doAction(action);
                    break;

                case MSG_SHOW_EDUCATE:
                    boolean skipAsk = false;

                    req = sReqMgr.getRequest(msg.arg1);
                    if (req == null) {
                        //  The request in the message is no longer in
                        //  the active queue.  Complain about it.
                        Log.w(TAG, "Req id " + msg.arg1 + " does not exist");
                        break;
                    }

                    context = req.getOwner().getUiContext();
                    action = req.getActions()[msg.arg2];
                    curEduDone = Util.isEduDone(context,
                                                action.mPermDetails);

                    //  Little tricky logic here, if edu has been done but
                    //  this is a FEATURE type usage then we need to reset
                    //  the fact that edu has been done (and ask again!)
                    if (curEduDone &&
                        (action.mPermDetails.mUsage == PermissionUse.FEATURE)) {
                        //  If this permission's edu status hasn't been reset
                        //  then it needs to be.
                        if (!Util.isEduDoneReset(context, action.mPermDetails)) {
                            curEduDone = false;
                            Util.setEduDoneReset(context, action.mPermDetails);
                        } else {
                            //  We reset it last time through, no need to show
                            //  the user again, but clear the reset state.
                            Util.clearEduDoneReset(context, action.mPermDetails);
                        }
                    }

                    //  If the education hasn't been done, request it to be
                    //  done now.  If it has been done, try to find the next
                    //  one to be done.
                    if (!curEduDone) {
                        //  The type of education depends on whether the
                        //  permission usage is ESSENTIAL or FEATURE.  When
                        //  it is ESSENTIAL, we need a modal type UI.  Otherwise
                        //  just an impromptu will work.
                        if (action.mPermDetails.mUsage == PermissionUse.ESSENTIAL) {
                            action.mPromptCb.showEducateModal(action, msg.arg1);
                        } else {
                            action.mPromptCb.showEducate(action);
                            curEduDone = true;
                            skipAsk = true;
                        }

                        //  Save off the show edu state for this permissin
                        if (curEduDone) {
                            Util.setEduDone(context, action.mPermDetails);
                        }
                    }

                    //  Find the next index in the request which needs
                    //  some user prompting (if any).
                    for (int i = msg.arg2 + 1;
                         i < req.getActions().length;
                         i++) {
                        action = req.getActions()[i];
                        if (action.hasUserEdu()) {
                            showEducateUi(msg.arg1, i);
                            break;
                        }
                    }

                    if (!skipAsk) {
                        //  If we get here, there were no more permissions to show
                        //  education info about, so do the request.
                        doRequest(msg.arg1);
                    } else {
                        //  As we are skipping to ask the user for the permission
                        //  because the user is not interested when educated,
                        //  we must remove the request from the active requests queue
                        sReqMgr.removeRequest(msg.arg1);
                    }

                    break;

                case MSG_SHOW_DENIED_CRITICAL:
                    req = (Request)msg.obj;
                    action = req.getActions()[msg.arg1];
                    action.mPromptCb.showDeniedCritical(action);
                    removeReq = true;
                    break;

                case MSG_SHOW_EDUCATE_REMINDER:
                    req = (Request)msg.obj;
                    action = req.getActions()[msg.arg1];
                    action.mPromptCb.showDeniedReminder(action);
                    removeReq = true;

                    //  After the user has been shown UI, notify app
                    notifyDenied(action);
                    break;

                case MSG_SHOW_DENIED_FEEDBACK:
                    req = (Request)msg.obj;
                    action = req.getActions()[msg.arg1];
                    action.mPromptCb.showDeniedFeedback(action);
                    removeReq = true;

                    //  After the user has been shown UI, notify app
                    notifyDenied(action);
                    break;

                case MSG_DENIED:
                    if (msg.obj != null) {
                        action = (ProtectedAction)msg.obj;
                        action.mListener.onPermissionDenied(action.mPermDetails);
                    }

                    break;

                case MSG_GO_TO_SETTINGS:
                    //  Now create an Intent for Settings with the app's FQPN
                    //  so the user will be taken directly to it.
                    context = (Context)msg.obj;
                    Util.startSettingsApp(context);
                    break;

                default:
                    Log.e(TAG, "Unknown message received: " + msg.what);
                    break;
            }

            if (removeReq) {
                sReqMgr.removeRequest(req);
            }
        }
    }
}
