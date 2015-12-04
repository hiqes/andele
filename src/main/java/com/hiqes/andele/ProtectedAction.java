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
import android.content.Context;

/**
 * A ProtectedAction object contains the necessary information for Andele
 * to properly handle requesting permissions and displaying UI information
 * to the user, if necessary.  Apps will create one or more instances of
 * this class, each having an ActionCallback which contains the code the
 * app needs to execute once the permission has been granted.
 * <p>
 * Andele will display UI prompts to the user depending on the permission
 * use specified when the ProtectedAction is built.  See
 * {@link com.hiqes.andele.Andele#checkAndExecute(Activity, ProtectedAction) Andele.checkAndExecute}
 * for more details.
 */
public class ProtectedAction {
    static final String                 TAG = ProtectedAction.class.getSimpleName();

    final PermissionDetails        mPermDetails;
    final Listener                 mListener;
    final UserPromptCallback       mPromptCb;
    final ActionCallback           mActionCb;

    @Override
    public int hashCode() {
        int                     ret = 719;

        //  make a simple hash of the permissions string and usage
        ret = 12 * ret + mPermDetails.mPermission.hashCode();
        ret = 12 * ret + mPermDetails.mUsage.ordinal();
        return ret;
    }

    @Override
    public boolean equals(Object obj) {
        boolean                 ret = false;

        if (this == obj) {
            ret = true;
        } else if (obj instanceof ProtectedAction) {
            ProtectedAction     otherAction = (ProtectedAction)obj;

            //  In order for these to be equal, the perm details must match.
            if (mPermDetails.mPermission.equals(otherAction.mPermDetails.mPermission) &&
                (mPermDetails.mUsage == otherAction.mPermDetails.mUsage)) {
                ret = true;
            }
        }

        return ret;
    }

    /**
     * Listener interface used to inform the app when a permission has been
     * granted or rejected so it can enable or disable functionality.  The app
     * must not present a prompt UI to the user as Andele will have already
     * done so appropriately via the UserPromptCallback.  The Listener is
     * always called back on the main thread of the app.
     */
    public interface Listener {
        void onPermissionGranted(PermissionDetails permission);
        void onPermissionDenied(PermissionDetails permission);
    }

    /**
     * Callback interface implemented by the app to execute the operation(s)
     * protected by the permission managed by this delegate.
     */
    public interface ActionCallback {
        /**
         * Andele will call this callback after the listener has been notified
         * of the permission being granted.  The callback will be executed in
         * the thread context in which the {@code checkAndExecute} method was
         * called.
         *
         * @param action   The protected action being executed
         */
        void doAction(ProtectedAction action);
    }

    /**
     * Callback interface implemented by the app to interact with the user
     * at specific times when requesting permissions or if permissions are
     * denied. The callbacks are always called on the main (UI) thread of
     * the app.
     */
    public interface UserPromptCallback {
        /**
         * Called when an action is being checked which is
         * an ESSENTIAL type, which should show an "Educate Upfront" type
         * modal UI to inform the user of the feature and permission.
         * Andele tracks whether or not the educate modal has been presented
         * to the user based on the app calling back to Andele once education
         * modal has been shown, ensuring it will only be shown the first time the
         * permission is requested.
         * <p>
         * @param action   The action which requires an education UI.
         * @param reqCode  The internal Andele request code corresponding to
         *                 the modal.  This is to be provided back to Andele
         *                 when calling {@link com.hiqes.andele.Andele#markEducateModalDone(Context,int,ProtectedAction) markEducateModealDone()}
         */
        void showEducateModal(ProtectedAction action, int reqCode);

        /**
         * Called when an action is being checked which is an OPTIONAL type,
         * which should show an "Educate in Context" type UI.  This can be
         * a bottom sheet or dialog which informs the user that a feature
         * will be used which requires a permission.  If the educate is shown
         * once but later denied, the educate will be shown again the next
         * time it is requested.  The first time showEducate() is called, the
         * permission will not be requested or the action taken.  The app must
         * call checkAndExecute() again to cause the permission to be requested
         * and/or the action to be taken.
         * <p>
         * @param action   The action which requires an education prompt
         */
        void showEducate(ProtectedAction action);

        /**
         * Called when an action with a CRITICAL permission has been denied,
         * so the app should inform the user then exit.
         * <p>
         * @param action   The action with a permission which has been denied
         */
        void showDeniedCritical(ProtectedAction action);

        /**
         * Called when a permission has been denied which is of type
         * ESSENTIAL, so the app may be crippled.  The UI to be presented
         * is not modal or educational, but a timed UI reminding the user the
         * app will be crippled and giving the user the ability to go
         * to the Settings app to change permissions.  Because the permission
         * is ESSENTIAL, this UI reminder will be shown every time it is
         * requested and denied.  See
         * {@link com.hiqes.andele.Andele#startSettingsApp(Activity) startSettinsApp(Activity)}
         * for more detailed.
         * <p>
         * @param action   The action which requires a permission which has
         *                 been denied.
         */
        void showDeniedReminder(ProtectedAction action);

        /**
         * Called when a permission has been denied and is of type FEATURE
         * or OPTIONAL.  This is an informational UI type, so it should not
         * be modal or education, but a timed UI informing the user of the
         * denial.  This UI will only be shown when the permission has been
         * denied and the user has asked to not be prompted again.  The UI
         * should provide the user the ability to go to the Settings app to
         * change the app's permissions.  See
         * {@link com.hiqes.andele.Andele#startSettingsApp(Activity) startSettinsApp(Activity)}
         * for more detailed.
         * <p>
         * @param action   The action which requires a permission which has
         *                 been denied.
         */
        void showDeniedFeedback(ProtectedAction action);
    }

    private ProtectedAction(PermissionDetails  details,
                            ActionCallback     actionCb,
                            UserPromptCallback promptCb,
                            Listener           listener) {
        mPermDetails       = details;
        mActionCb          = actionCb;
        mPromptCb          = promptCb;
        mListener          = listener;
    }

    /**
     * Use this to construct a new ProtectedAction instance for handling
     * an operation which is protected by a specific permission.
     */
    public static class Builder {
        private String              mPerm;
        private PermissionUse       mUsage;
        private ActionCallback      mActionCb;
        private UserPromptCallback  mPromptCb;
        private Listener            mListener;

        /**
         * Construct a new ProtectedAction object with the properties
         * setup in this object.  The following properties are not required
         * before calling build():
         * <p>
         * Listener<br>
         * Educate resource (only for CRITICAL and FEATURE, otherwise required)<br>
         * @return A new ProtectedAction object.
         */
        public ProtectedAction build() {
            //  Listener is optional, everything else is required
            if (mListener == null) {
                mListener = new EmptyListener();
            }

            if (mPerm == null) {
                throw new IllegalStateException("Permission must be set");
            }

            if (mUsage == null) {
                throw new IllegalStateException("Permission usage must be set");
            }

            if (mActionCb == null) {
                throw new IllegalStateException("Action callback must be set");
            }

            if (mPromptCb == null) {
                throw new IllegalStateException("User prompt callback must be set");
            }

            PermissionDetails permDetails =
                new PermissionDetails(mPerm, mUsage);
            return new ProtectedAction(permDetails,
                                       mActionCb,
                                       mPromptCb,
                                       mListener);
        }

        /**
         * Specify the Android permission used to protect the action
         * which is performed by the ProtectedAction object.  For example,
         * {@link android.Manifest.permission#CAMERA android.Manifest.permission.CAMERA} or
         * <code>"com.hiqes.sample.permission.MY_PERMISSION"</code>.
         * <p>
         * @param perm   The Android permission which is used to protect the
         *               action performed by the ProtectedAction.
         * @return The Builder object.
         */
        public Builder withPermission(String perm) {
            mPerm = perm;
            return this;
        }

        /**
         * Specify the permission usage for this action.  Andele will use
         * this information to present an appropriate UI to the user, if
         * needed.
         * <p>
         * @param usage   The PermissionUse describing the role of the
         *                ProtectedAction within the app.
         * @return The Builder object.
         */
        public Builder withUsage(PermissionUse usage) {
            mUsage = usage;
            return this;
        }

        /**
         * Set a listener to be called back when the permission used
         * to protected this action is denied or granted.
         * <p>
         * @param listener   The Listener object to be notified of changes.
         * @return The Builder object
         */
        public Builder listener(Listener listener) {
            if (listener == null) {
                throw new IllegalArgumentException("Listener cannot be null");
            }

            if (this.mListener != null) {
                throw new IllegalStateException("Listener is already set");
            }

            mListener = listener;
            return this;
        }

        /**
         * Set the ActionCallback to be executed when permissions have been
         * granted and ProtectedAction.execute() is called.  The callback
         * is executed on the same thread in which the execute() method
         * is called.  However, there are concerns.  See the documentation
         * on the {@link com.hiqes.andele.Andele#checkAndExecute} methods for more details.
         * <p>
         * @param actionCb   The callback object which will be called when
         *                   the protected action is to be performed.
         * @return The Builder object
         */
        public Builder actionCallback(ActionCallback actionCb) {
            if (actionCb == null) {
                throw new IllegalArgumentException("Action callback cannot be null");
            }

            if (this.mActionCb != null) {
                throw new IllegalStateException("Action callback is already set");
            }

            mActionCb = actionCb;
            return this;
        }

        public Builder userPromptCallback(UserPromptCallback promptCb) {
            if (promptCb == null) {
                throw new IllegalArgumentException("Prompt callback cannot be null");
            }

            if (this.mPromptCb != null) {
                throw new IllegalStateException("Prompt callback is already set");
            }

            mPromptCb = promptCb;
            return this;
        }

        private class EmptyListener implements Listener {

            @Override
            public void onPermissionGranted(PermissionDetails permission) {
                //  Do nothing
            }

            @Override
            public void onPermissionDenied(PermissionDetails permission) {
                //  Do nothing
            }
        }
    }

    public boolean hasUserEdu() {
        boolean                 ret = false;

        //  Only ESSENTIAL and OPTIONAL include user education
        if ((mPermDetails.mUsage == PermissionUse.ESSENTIAL) ||
            (mPermDetails.mUsage == PermissionUse.OPTIONAL)) {
            ret = true;
        }

        return ret;
    }

    public PermissionDetails getPermissionDetails() {
        return mPermDetails;
    }
}
