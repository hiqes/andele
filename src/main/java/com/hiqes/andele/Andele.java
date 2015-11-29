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
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Andele, the Android Permissions delegate helper API.
 * <p>
 * Use this to manage {@link com.hiqes.andele.ProtectedAction} instances to
 * assist with requesting permissions and interacting with the user.
 */
public class Andele {
    static final String                 TAG = Andele.class.getSimpleName();

    static final int                           MSG_DO_ACTION = 1;
    static final int                           MSG_SHOW_EDUCATE = 10;
    static final int                           MSG_SHOW_EDUCATE_NEXT = 11;
    static final int                           MSG_EDUCATE_DONE = 12;
    static final int                           MSG_SHOW_EDUCATE_REMINDER = 13;
    static final int                           MSG_SHOW_DENIED_CRITICAL = 14;
    static final int                           MSG_SHOW_DENIED_FEEDBACK = 15;
    static final int                           MSG_DENIED = 16;
    static final int                           MSG_GO_TO_SETTINGS = 50;

    private static HashMap<Integer, Request>   sActiveReqs = new HashMap<>();
    private static Random                      sRand = new Random(System.nanoTime());
    private static PermResultHandler           sHandler = new PermResultHandler(Looper.getMainLooper());
    private static ProtectedAction             sCurEducate;
    private static List<ProtectedAction>       sEducateQueue = new ArrayList<>();

    //  TODO: REVISE THIS TO CONFINE CODES WITHIN A "RANGE" SO WE DON'T CONFLICT WITH APP
    private static int getNextCode(int mask) {
        int                     ret;

        synchronized(Andele.class) {
            //  Only get a random number 1x.  Just add to it through the
            //  loop so we don't unnecessarily tax the system.
            ret = sRand.nextInt() & mask;
            while (true) {
                if (!sActiveReqs.containsKey(ret)) {
                    break;
                }

                ret = (ret + 1) & mask;
            }
        }

        return ret;
    }

    private static int queueRequests(RequestOwner owner, ProtectedAction[] actions) {
        Handler                 handler = Util.isMainThread() ? sHandler : new PermResultHandler();
        Request                 req = new Request(owner, actions, handler);

        int reqCode = getNextCode(owner.getReqeuestCodeMask());

        synchronized(Andele.class) {
            sActiveReqs.put(reqCode, req);
        }

        return reqCode;
    }

    private static int queueRequest(RequestOwner owner, ProtectedAction action) {
        ProtectedAction[]       actions = new ProtectedAction[1];
        actions[0] = action;
        return queueRequests(owner, actions);
    }

    private static void doRequest(int reqCode) {
        Request                 req;

        synchronized (Andele.class) {
            req = sActiveReqs.get(reqCode);
        }

        if (req != null) {
            ProtectedAction[]   actions = req.getActions();
            String[]            perms = new String[actions.length];

            for (int i = 0; i < actions.length; i++) {
                perms[i] = actions[i].mPermDetails.mPermission;
            }

            req.getOwner().requestPermissions(perms, reqCode);
        }
    }

    private static void checkAndExecute(RequestOwner owner, ProtectedAction[] actions) {
        ArrayList<ProtectedAction>  reqActions = null;
        int                         status;
        int                         firstEduIndex = -1;

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
            int reqCode = queueRequests(owner, needyActions);

            //  If firstEduIndex is not -1, somebody needs an explanation.
            if (firstEduIndex != -1) {
                Log.d(TAG, "Show edu for req " + reqCode);
                showEducateUi(reqCode, firstEduIndex);
            } else {
                doRequest(reqCode);
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
     * <li>Educate in context: presented before permission request, if not already granted.
     * This will only be shown the first time the permission is used/requested.</li>
     * <li>Denied critical: presented to inform the user that a CRITICAL permission
     * has been denied and they must go into Settings and adjust permissions in
     * order to use the app.  This is typically shown as a modal and when dismissed
     * the app exits.</li>
     * <li>Denied remind: presented when permission has been denied but the user
     * does not want to be bothered again and the ProtectedAction is
     * ESSENTIAL.  Gives the user the option of going to Settings to adjust.
     * This is typically presented using a Snackbar.</li>
     * <li>Denial feedback: presented when permission has been denied, optionally
     * give the user another chance to enable it by going to Settings.  This
     * is typically presented as a Snackbar.</li>
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
    public static void checkAndExecute(android.support.v4.app.Fragment fragment, ProtectedAction action) {
        checkAndExecute(new RequestOwnerSupportFragment(fragment), action);
    }

    private static ProtectedAction.ActionCallback mEmptyActionCallback = new ProtectedAction.ActionCallback() {
        @Override
        public void doAction(ProtectedAction action) {
            //  Do nothing.  This is used for check-only tests so the code
            //  is common.
        }
    };

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
        //  the callbacks but kept the reset, the ProtectedAction listeners
        //  will get hit as expected but no action callback.
        checkAndExecute(owner, actions);
    }

    /**
     * Check to see if the application has been granted the provided
     * permissions.  If the permissions have not been granted to the app, this
     * will request them from the system.  These permissions should only be
     * {@link PermissionUse#CRITICAL CRITICAL} or {@link PermissionUse#ESSENTIAL ESSENTIAL}
     * permissions.  Others will be
     * silently ignored and should be used with {@code checkAndExecute}.  The
     * action callback for the for the provided action will not be executed,
     * but any attached listener will be called to notify the app that the
     * permissions are either granted or denied.  The provided ProtectedActions
     * may have an "educate" layout associated with it to inform the user that
     * a permission is needed to function.  This only applies for ESSENTIAL
     * permissions.  Additionally, if a permission is denied, the user will be
     * presented a full screen dialog explaining the need for the permission
     * in order for the app to function.  This applies to CRITICAL permissions.
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
    public static void checkAndRequestMandatoryPermissions(Activity activity, ProtectedAction[] actions) {
        checkAndRequestMandatoryPermissions(new RequestOwnerActivity(activity), actions);
    }

    /**
     * Check to see if the application has been granted the provided
     * permissions.  See {@link #checkAndRequestMandatoryPermissions(Activity, ProtectedAction[])}
     * for details about the behavior.
     * <p>
     * @param activity   The owning Activity making the request
     * @param actions    An array of ProtectedActions describing the permissions
     *                   which are mandatory (i.e. {@code} CRITICAL) for the app.
     */
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
    public static void checkAndRequestMandatoryPermissions(android.support.v4.app.Fragment fragment, ProtectedAction[] actions) {
        checkAndRequestMandatoryPermissions(new RequestOwnerSupportFragment(fragment), actions);
    }

    //  TODO: NEED TO DETECT DESTROY OF ACTIVITIES, REMOVE REQUEST(S) TO AVOID MEM LEAKS

    public static void markEducateModalDone(Context context, int reqCode, ProtectedAction action) {
        Request                 req;
        int                     actionIndex = -1;
        ProtectedAction[]       actions;

        //  Sanity check
        synchronized (Andele.class) {
            req = sActiveReqs.get(reqCode);
            if (req == null) {
                Log.w(TAG, "markEducateModalDone called for unknown req: " + reqCode);
                return;
            }
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
        Util.setEduDone(context, action.mPermDetails);
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
     * @Override
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
    public static boolean onRequestPermissionsResult(int reqCode, String[] permissions, int[] grantResults) {
        boolean handled = false;
        Request req;

        //  Lookup the code, remove the node if there, complain if it doesn't exist
        synchronized (Andele.class) {
            req = sActiveReqs.remove(reqCode);
        }

        if (req == null) {
            Log.w(TAG, "Active request not found for code " + reqCode);
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
                            showDeniedCritical(req, j);
                            continue;
                        } else if (curUsage == PermissionUse.ESSENTIAL) {
                            //  If this is an ESSENTIAL permission then remind
                            //  the user of the problems with denial.
                            showDeniedReminder(req, j);
                            continue;
                        } else if (req.getOwner().shouldShowRequestPermissionRationale(curPerm)) {
                            //  This permission covers a secondary type of feature
                            //  so as long as the user is open to feedback go
                            //  ahead and provide it.
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

        return handled;
    }

    /**
     * Helper API to start the Settings app directly into the details page
     * for the app. This allows the user to quickly change the permissions
     * granted/denied for the app.
     * <p>
     * @param activity   The Activity in the running state making the request.
     */
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
    public static void startSettingsApp(android.support.v4.app.Fragment fragment) {
        startSettingsApp((Context)fragment.getActivity());
    }

    private static void startSettingsApp(Context uiContext) {
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
        public PermResultHandler() {
            super();
        }

        public PermResultHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            ProtectedAction     action;
            Request             req;
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

                    synchronized (Andele.class) {
                        req = sActiveReqs.get(msg.arg1);
                    }

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
                    //  done to be done.
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

                    //  If we get here, there were no more permissions to show
                    //  education info about, so do the request.
                    if (!skipAsk) {
                        doRequest(msg.arg1);
                    }

                    break;

                case MSG_SHOW_DENIED_CRITICAL:
                    req = (Request)msg.obj;
                    action = req.getActions()[msg.arg1];
                    action.mPromptCb.showDeniedCritical(action);
                    break;

                case MSG_SHOW_EDUCATE_REMINDER:
                    req = (Request)msg.obj;
                    action = req.getActions()[msg.arg1];
                    action.mPromptCb.showDeniedReminder(action);

                    //  After the user has been shown UI, notify app
                    notifyDenied(action);
                    break;

                case MSG_SHOW_DENIED_FEEDBACK:
                    req = (Request)msg.obj;
                    action = req.getActions()[msg.arg1];
                    action.mPromptCb.showDeniedFeedback(action);

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

                    //  TODO: NEED TO PURGE ALL EXISTING REQUESTS AND MESSAGES AS WE ARE LEAVING THE APP FOR SETTINGS?
                    break;

                default:
                    Log.e(TAG, "Unknown message received: " + msg.what);
                    break;
            }
        }
    }
}
