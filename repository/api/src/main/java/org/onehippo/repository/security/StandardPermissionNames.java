/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.Session;

public final class StandardPermissionNames {

    /* JCR standard privilege names, see javax.jcr.security.Privilege */

    public static final String JCR_READ = "jcr:read";
    public static final String JCR_MODIFY_PROPERTIES = "jcr:modifyProperties";
    public static final String JCR_ADD_CHILD_NODES = "jcr:addChildNodes";
    public static final String JCR_REMOVE_NODE = "jcr:removeNode";
    public static final String JCR_REMOVE_CHILD_NODES = "jcr:removeChildNodes";
    public static final String JCR_WRITE = "jcr:write";
    public static final String JCR_READ_ACCESS_CONTROL = "jcr:readAccessControl";
    public static final String JCR_MODIFY_ACCESS_CONTROL = "jcr:modifyAccessControl";
    public static final String JCR_LOCK_MANAGEMENT = "jcr:lockManagement";
    public static final String JCR_VERSION_MANAGEMENT = "jcr:versionManagement";
    public static final String JCR_NODE_TYPE_MANAGEMENT = "jcr:nodeTypeManagement";
    public static final String JCR_RETENTION_MANAGEMENT = "jcr:retentionManagement";
    public static final String JCR_LIFECYCLE_MANAGEMENT = "jcr:lifecycleManagement";
    public static final String JCR_ALL = "jcr:all";

    /**
     * jcr:write aggregated privilege names
     */
    public static final Set<String> JCR_WRITE_PRIVILEGES = Collections.unmodifiableSet(new TreeSet<>(Arrays.asList(
            JCR_MODIFY_PROPERTIES,
            JCR_ADD_CHILD_NODES,
            JCR_REMOVE_NODE,
            JCR_REMOVE_CHILD_NODES
    )));

    /**
     * jcr:all aggregated privilege names, without jcr:write
     */
    public static final Set<String> JCR_ALL_PRIVILEGES = Collections.unmodifiableSet(new TreeSet<>(Arrays.asList(
                    JCR_READ,
                    JCR_MODIFY_PROPERTIES,
                    JCR_ADD_CHILD_NODES,
                    JCR_REMOVE_NODE,
                    JCR_REMOVE_CHILD_NODES,
                    JCR_READ_ACCESS_CONTROL,
                    JCR_MODIFY_ACCESS_CONTROL,
                    JCR_LOCK_MANAGEMENT,
                    JCR_VERSION_MANAGEMENT,
                    JCR_NODE_TYPE_MANAGEMENT,
                    JCR_RETENTION_MANAGEMENT,
                    JCR_LIFECYCLE_MANAGEMENT
            )));

    /**
     * all default {@link Session#hasPermission(String, String) JCR Session} permission action names
     */
    public static final Set<String> JCR_ACTIONS = Collections.unmodifiableSet(new TreeSet<>(Arrays.asList(
            Session.ACTION_ADD_NODE,
            Session.ACTION_READ,
            Session.ACTION_REMOVE,
            Session.ACTION_SET_PROPERTY
    )));

    /* default custom privilege names */
    public static final String HIPPO_AUTHOR = "hippo:author";
    public static final String HIPPO_EDITOR = "hippo:editor";
    public static final String HIPPO_ADMIN = "hippo:admin";
    public static final String HIPPO_REST = "hippo:rest";

    private StandardPermissionNames() {}
}
