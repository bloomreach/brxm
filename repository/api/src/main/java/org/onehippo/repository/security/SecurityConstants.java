/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.repository.security;

import org.hippoecm.repository.api.HippoNodeType;

public final class SecurityConstants {

    private SecurityConstants() {
    }

    public static final String CONFIGURATION_FOLDER_PATH = "/" + HippoNodeType.CONFIGURATION_PATH;
    public static final String CONFIGURATION_FOLDER_PATH_PREFIX = CONFIGURATION_FOLDER_PATH + "/";

    // Standard /hippo:configuration security folder locations

    public static final String CONFIG_DOMAINS_PATH = CONFIGURATION_FOLDER_PATH_PREFIX + HippoNodeType.DOMAINS_PATH;
    public static final String CONFIG_GROUPS_PATH = CONFIGURATION_FOLDER_PATH_PREFIX + HippoNodeType.GROUPS_PATH;
    public static final String CONFIG_ROLES_PATH = CONFIGURATION_FOLDER_PATH_PREFIX + HippoNodeType.ROLES_PATH;
    public static final String CONFIG_SECURITY_PATH = CONFIGURATION_FOLDER_PATH_PREFIX + HippoNodeType.SECURITY_PATH;
    public static final String CONFIG_USERROLES_PATH = CONFIGURATION_FOLDER_PATH_PREFIX + HippoNodeType.USERROLES_PATH;
    public static final String CONFIG_USERS_PATH = CONFIGURATION_FOLDER_PATH_PREFIX + HippoNodeType.USERS_PATH;

    // Standard system user roles

    /**
     * The user role representing a user (to be) granted jcr:all (everything) everywhere (domain everywhere)
     */
    public static final String USERROLE_REPOSITORY_ADMIN = "xm.repository.admin";

    /**
     * The user role representing a user (to be) granted access to system administrative features; implies {@link #USERROLE_REPOSITORY_ADMIN}
     */
    public static final String USERROLE_SYSTEM_ADMIN = "xm.system.admin";

    /**
     * The user role representing a user (to be) granted access to security administrative features
     */
    public static final String USERROLE_SECURITY_VIEWER = "xm.security.viewer";

    /**
     * The user role representing a user (to be) granted access to user security administrative features; implies
     * {@link #USERROLE_SECURITY_VIEWER}
     */
    public static final String USERROLE_SECURITY_USER_ADMIN = "xm.security.user-admin";

    /**
     * The user role representing a user (to be) granted access to application security administrative features; implies
     * {@link #USERROLE_SECURITY_VIEWER}
     */
    public static final String USERROLE_SECURITY_APPLICATION_ADMIN = "xm.security.application-admin";

    /**
     * The user role representing a user (to be) granted read-only view access on content
     */
    public static final String USERROLE_CONTENT_VIEWER = "xm.content.viewer";

    /**
     * The user role representing a user (to be) granted author access on content; implies {@link #USERROLE_CONTENT_VIEWER}
     */
    public static final String USERROLE_CONTENT_AUTHOR = "xm.content.author";

    /**
     * The user role representing a user (to be) granted editor access on content; implies {@link #USERROLE_CONTENT_AUTHOR}
     */
    public static final String USERROLE_CONTENT_EDITOR = "xm.content.editor";

    /**
     * The user role representing a user (to be) granted admin access on content; implies {@link #USERROLE_CONTENT_EDITOR}
     */
    public static final String USERROLE_CONTENT_ADMIN = "xm.content.admin";

    public static final String USERROLE_CONTENT_HOLDER = "xm.content.holder";

    /**
     * The user role representing a (system) user which need read access everywhere, for example the user that needs to
     * read all kind of configuration but never modify it.
     * For this, the everywhere domain grants the authrole readonly to users with this userrole.
     */
    public static final String USERROLE_REPOSITORY_READER = "xm.repository.reader";

    /**
     * The user role aggregating (implying) the standard user roles for the author user, assigned by default to the author group
     */
    public static final String USERROLE_DEFAULT_USER_AUTHOR = "xm.default-user.author";

    /**
     * The user role aggregating (implying) the standard user roles for the editor user, assigned by default to the editor group
     */
    public static final String USERROLE_DEFAULT_USER_EDITOR = "xm.default-user.editor";

    /**
     * The user role aggregating (implying) the standard user roles for the system-admin user, assigned by default to the admin group
     */
    public static final String USERROLE_DEFAULT_USER_SYSTEM_ADMIN = "xm.default-user.system-admin";

    /**
     * The user role aggregating (implying) the standard user roles for the cms-admin user, assigned by default to the cms-admin group
     */
    public static final String USERROLE_DEFAULT_USER_CMS_ADMIN = "xm.default-user.cms-admin";
}
