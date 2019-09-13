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

public final class SecurityConstants {

    private SecurityConstants() {
    }

    // Standard system user roles

    /**
     * The user role reprenting a user (to be) granted jcr:all (everything) everywhere (domain everywhere)
     */
    public static final String USERROLE_ADMIN = "xm-admin";

    /**
     * The user role representing a user (to be) granted read-only access on content
     */
    public static final String USERROLE_CONTENT_READER = "xm-content-reader";

    /**
     * The user role representing a user (to be) granted author access on content; implies {@link #USERROLE_CONTENT_READER}
     */
    public static final String USERROLE_CONTENT_AUTHOR = "xm-content-author";

    /**
     * The user role representing a user (to be) granted editor access on content; implies {@link #USERROLE_CONTENT_AUTHOR}
     */
    public static final String USERROLE_CONTENT_EDITOR = "xm-content-editor";

    /**
     * The user role representing a (system) user which need read access everywhere, for example the user that needs to
     * read all kind of configuration but never modify it.
     * For this, the everywhere domain grants the authrole readonly to users with this userrole.
     */
    public static final String USERROLE_READ_EVERYWHERE = "xm-read-everywhere";
}
