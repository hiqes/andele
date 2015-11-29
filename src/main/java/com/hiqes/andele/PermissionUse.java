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

/**
 * Specifies how a specific permission is used by the application.
 */
public enum PermissionUse {
    /**
     * The permission is necessary for the core functionality of the app
     * and is obvious from the app's description.
     */
    CRITICAL,

    /**
     * The permission is necessary for the core functionality of the app
     * but is not obvious to the user from the app's description.
     */
    ESSENTIAL,

    /**
     * The permission is needed for a feature of the app which is not
     * part of the core functionality.  It is obvious to the user that
     * this feature will require some functionality protected by a
     * permission.
     */
    FEATURE,

    /**
     * The permission is needed for a feature of the app which is not
     * part of the core functionality.  However, it is not obvious that
     * this feature requires some functionality which is protected by
     * a permission.
     */
    OPTIONAL,
}
