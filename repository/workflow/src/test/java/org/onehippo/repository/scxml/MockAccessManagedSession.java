/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.repository.scxml;

import java.security.AccessControlException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.mock.MockSession;

/**
 * Extended MockSession supporting access management permissions checks.
 * <p>
 *     Default all permission checks return true, but through {@link #setPermissions(String, String, boolean)}
 *     specific permissions can be granted (allowed or denied) on a specific path.
 * </p>
 */
public class MockAccessManagedSession extends MockSession {

    private Map<String, Set<String>> allowedPermissions = new HashMap<>();
    private Map<String, Set<String>> deniedPermissions = new HashMap<>();

    public MockAccessManagedSession(final MockNode root) {
        super(root);
    }

    public void setPermissions(String path, String permissions, boolean allowed) {

        Set<String> permSet = null;
        if (permissions != null) {
            permSet = new HashSet<>(Arrays.asList(permissions.split(",")));
            if (permSet.isEmpty()) {
                permSet = null;
            }
        }
        Map<String, Set<String>> target = allowed ? allowedPermissions : deniedPermissions;
        if (permSet == null) {
            target.remove(path);
        }
        else {
            target.put(path, permSet);
        }
        if (allowed) {
            deniedPermissions.remove(path);
        }
        else {
            allowedPermissions.remove(path);
        }
    }

    @Override
    public boolean hasPermission(final String absPath, final String actions) {
        if (actions != null) {
            Set<String> actionSet = new HashSet<>(Arrays.asList(actions.split(",")));
            if (!actionSet.isEmpty()) {
                Set<String> permissions = deniedPermissions.get(absPath);
                if (permissions != null) {
                    for (String action : actionSet) {
                        if (permissions.contains(action)) {
                            return false;
                        }
                    }
                }
                permissions = allowedPermissions.get(absPath);
                return (permissions == null || permissions.containsAll(actionSet));
            }
        }
        return false;
    }

    @Override
    public void checkPermission(final String absPath, final String actions) {
        if (!hasPermission(absPath, actions)) {
            throw new AccessControlException("Privileges '" + actions + "' denied for " + absPath);
        }
    }
}
