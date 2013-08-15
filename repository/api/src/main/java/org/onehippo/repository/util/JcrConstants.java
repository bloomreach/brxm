/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.util;

public final class JcrConstants {

    private JcrConstants() {}

    // ------------------------------------------ uris

    public static final String URI_JCR = "http://www.jcp.org/jcr/1.0";
    public static final String URI_SV = "http://www.jcp.org/jcr/sv/1.0";

    // ------------------------------------------ node types

    public static final String MIX_VERSIONABLE = "mix:versionable";
    public static final String MIX_LOCKABLE = "mix:lockable";
    public static final String MIX_REFERENCEABLE = "mix:referenceable";

    // ------------------------------------------ properties

    public static final String JCR_PRIMARY_TYPE = "jcr:primaryType";

    public static final String JCR_LOCK_OWNER = "jcr:lockOwner";
    public static final String JCR_LOCK_IS_DEEP = "jcr:lockIsDeep";

    public static final String JCR_UUID = "jcr:uuid";

    public static final String JCR_BASE_VERSION = "jcr:baseVersion";
    public static final String JCR_IS_CHECKED_OUT = "jcr:isCheckedOut";
    public static final String JCR_PREDECESSORS = "jcr:predecessors";
    public static final String JCR_VERSION_HISTORY = "jcr:versionHistory";

}
