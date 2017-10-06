/*
 * Copyright (C) 2017 HIQES LLC
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


class Log {
    static Andele.Logger        sLogger;

    static {
        //  If we are built as debug, default to the debug logger
        if (BuildConfig.DEBUG) {
            sLogger = new LoggerDebug();
        } else {
            sLogger = new LoggerDoNothing();
        }
    }

    static void setLogger(Andele.Logger newLogger) {
        if ((sLogger != null) &&
                !(sLogger instanceof LoggerDebug) &&
                !(sLogger instanceof LoggerDoNothing))
        {
            sLogger.log(android.util.Log.WARN,
                        Log.class.getName(),
                        "Logger already set, being replaced by " +
                            newLogger);
        }

        sLogger = newLogger;
    }

    static void d(String tag, String msg) {
        sLogger.log(android.util.Log.DEBUG, tag, msg);
    }

    static void v(String tag, String msg) {
        sLogger.log(android.util.Log.VERBOSE, tag, msg);
    }

    static void i(String tag, String msg) {
        sLogger.log(android.util.Log.INFO, tag, msg);
    }

    static void w(String tag, String msg) {
        sLogger.log(android.util.Log.WARN, tag, msg);
    }

    static void e(String tag, String msg) {
        sLogger.log(android.util.Log.ERROR, tag, msg);
    }
}


