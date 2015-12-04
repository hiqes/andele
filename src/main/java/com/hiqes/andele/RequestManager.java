package com.hiqes.andele;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class RequestManager {
    private static final String         TAG = RequestManager.class.getSimpleName();

    private HashMap<Integer, Request>   mActiveReqs;
    private HashMap<Integer, Request>   mOrphanReqs;
    private Random                      mRand;
    private OrphanTracker               mOrphanTracker;

    public RequestManager() {
        mActiveReqs = new HashMap<>();
        mOrphanReqs = new HashMap<>();
        mRand = new Random(System.nanoTime());
    }


    //  TODO: REVISE THIS TO CONFINE CODES WITHIN A "RANGE" SO WE DON'T CONFLICT WITH APP
    private int getNextCode(int mask) {
        int                     ret;

        synchronized(this) {
            //  Only get a random number 1x.  Just add to it through the
            //  loop so we don't unnecessarily tax the system.
            ret = mRand.nextInt() & mask;
            while (true) {
                if (!mActiveReqs.containsKey(ret) &&
                    !mOrphanReqs.containsKey(ret)) {
                    break;
                }

                ret = (ret + 1) & mask;
            }
        }

        return ret;
    }


    int queueRequest(RequestOwner owner, ProtectedAction[] actions, Handler handler) {
        Request                 req = new Request(owner, actions, handler);
        int                     reqCode = getNextCode(owner.getReqeuestCodeMask());;

        //  Make sure we're setup to track orphans by registering as an
        //  Activity lifecycle callback receiver.
        if (mOrphanTracker == null) {
            mOrphanTracker = new OrphanTracker();
            owner.getApplication().registerActivityLifecycleCallbacks(mOrphanTracker);
        }

        //  Before queuing a new request, see if this request is actually
        //  out there already.  It *should* be in the orphan queue if it is.
        synchronized (this) {
            Set<Map.Entry<Integer, Request>>    allOrphans;

            allOrphans = mOrphanReqs.entrySet();
            for (Map.Entry<Integer, Request> curEntry : allOrphans) {
                Request         curReq = curEntry.getValue();

                if (curReq.isSameRequest(req)) {
                    int         curKey = curEntry.getKey();

                    //  This orphan is being "restored" via this new
                    //  request.  This can happen because of config change
                    //  (e.g. screen rotation) where the Activity owning
                    //  the request was torn down and re-created while an
                    //  active request was happening.  So stick this new
                    //  request on the active map and purge the old one
                    //  from the orphan map.
                    Log.d(TAG, "queueRequest: restoring orphan " + curKey);
                    mOrphanReqs.remove(curKey);
                    mActiveReqs.put(curKey, req);
                    reqCode = -1;
                    break;
                }
            }
        }

        if (reqCode >= 0) {
            synchronized (this) {
                mActiveReqs.put(reqCode, req);
            }
        }

        return reqCode;
    }


    Request getRequest(int code) {
        Request                 ret;

        synchronized (this) {
            //  Try active requests first.
            ret = mActiveReqs.get(code);

            //  If we don't get a match, see if it's a stale request
            //  sitting in the orphan map.
            if (ret == null) {
                ret = mOrphanReqs.remove(code);
                if (ret != null) {
                    Log.i(TAG, "getRequest: cleanup orphan req " + code);
                    ret = null;
                }
            }
        }

        if (ret == null) {
            Log.w(TAG, "getRequest: req " + code + " not found");
        }

        return ret;
    }


    Request removeRequest(int code) {
        Request                 ret;

        synchronized (this) {
            //  Try active requests first
            ret = mActiveReqs.remove(code);

            //  If we don't get a match, try orphans
            if (ret == null) {
                ret = mOrphanReqs.remove(code);
                if (ret != null) {
                    Log.i(TAG, "removeRequest: cleanup orphan req " + code);
                    ret = null;
                }
            }
        }

        if (ret == null) {
            Log.w(TAG, "removeRequest: req " + code + " not found");
        }

        return ret;
    }

    private class OrphanTracker implements Application.ActivityLifecycleCallbacks {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            //  Don't care
        }

        @Override
        public void onActivityStarted(Activity activity) {
            //  Don't care
        }

        @Override
        public void onActivityResumed(Activity activity) {
            //  Don't care
        }

        @Override
        public void onActivityPaused(Activity activity) {
            //  Don't care
        }

        @Override
        public void onActivityStopped(Activity activity) {
            //  Don't care
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            //  Don't care
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            //  Here's where we care.  We need to track down any active
            //  requests for this Activity so it can be released
            synchronized (RequestManager.this) {
                Set<Map.Entry<Integer, Request>> allActiveReqs = mActiveReqs.entrySet();

                for (Map.Entry<Integer, Request> curEntry : allActiveReqs) {
                    Request curReq = curEntry.getValue();

                    if (curReq.getOwner().isParentActivity(activity)) {
                        //  This Activity is going away so this request is going
                        //  to become an orphan.  It may be reclaimed later when
                        //  the Activity is restored and the request re-submitted.
                        Log.d(TAG,
                                "onActivityDestroyed: tracking orphan req " +
                                        curEntry.getKey());
                        mActiveReqs.remove(curEntry.getKey());
                        mOrphanReqs.put(curEntry.getKey(), curEntry.getValue());
                    }
                }
            }
        }
    }
}
