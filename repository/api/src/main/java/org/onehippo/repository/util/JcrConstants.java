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

    public static final String NT_BASE = "nt:base";
    public static final String NT_UNSTRUCTURED = "nt:unstructured";
    public static final String NT_FOLDER = "nt:folder";
    public static final String NT_FILE = "nt:file";
    public static final String NT_RESOURCE = "nt:resource";
    public static final String NT_VERSION = "nt:version";
    public static final String NT_VERSION_HISTORY = "nt:versionHistory";
    public static final String NT_FROZEN_NODE = "nt:frozenNode";


    // ------------------------------------------ properties

    public static final String JCR_PRIMARY_TYPE = "jcr:primaryType";
    public static final String JCR_MIXIN_TYPES = "jcr:mixinTypes";

    public static final String JCR_LOCK_OWNER = "jcr:lockOwner";
    public static final String JCR_LOCK_IS_DEEP = "jcr:lockIsDeep";

    public static final String JCR_UUID = "jcr:uuid";

    public static final String JCR_ROOT_VERSION = "jcr:rootVersion";
    public static final String JCR_BASE_VERSION = "jcr:baseVersion";
    public static final String JCR_IS_CHECKED_OUT = "jcr:isCheckedOut";
    public static final String JCR_PREDECESSORS = "jcr:predecessors";
    public static final String JCR_VERSION_HISTORY = "jcr:versionHistory";
    public static final String JCR_FROZEN_PRIMARY_TYPE = "jcr:frozenPrimaryType";
    public static final String JCR_FROZEN_MIXIN_TYPES = "jcr:frozenMixinTypes";
    public static final String JCR_FROZEN_NODE = "jcr:frozenNode";
    public static final String JCR_FROZEN_UUID = "jcr:frozenUuid";

    public static final String JCR_ENCODING = "jcr:encoding";
    public static final String JCR_MIME_TYPE = "jcr:mimeType";
    public static final String JCR_DATA = "jcr:data";
    public static final String JCR_LAST_MODIFIED = "jcr:lastModified";
    public static final String JCR_CREATED = "jcr:created";

    public static final String JCR_CONTENT = "jcr:content";

    // ------------------------------------------ permissions

    public static final String JCR_READ = "jcr:read";
    public static final String JCR_WRITE = "jcr:write";

}
