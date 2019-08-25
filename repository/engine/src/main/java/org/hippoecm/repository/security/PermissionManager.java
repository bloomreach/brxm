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
package org.hippoecm.repository.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.security.Privilege;

import org.apache.commons.lang3.StringUtils;
import org.onehippo.repository.security.StandardPermissionNames;

/**
 * Internal {@link #getInstance() singleton} PermissionManager to get the predefined jcr and default custom privileges,
 * and dynamically created privileges, and the representing permission name(s) for them.
 * <p>
 * The jcr privileges are pre-registered for both their qualified and expanded JCR name, and for the aggregate privileges
 * {@link StandardPermissionNames#JCR_WRITE} and {@link StandardPermissionNames#JCR_ALL} all the permission names for
 * their aggregated privileges are stored. See also: ({@link StandardPermissionNames#JCR_WRITE_PRIVILEGES} and
 * {@link StandardPermissionNames#JCR_ALL_PRIVILEGES}).
 * </p>
 * <p>
 * In addition to the jcr privileges, the standard jcr permission actions are also pre-registered as permission names,
 * which are used by the {@link HippoAccessManager} to check and map to regular jcr privileges (see also JSR-283, 16.6.2).
 * Note: {@link #getOrCreatePrivilege(String)} for these permissions is not allowed and will throw an IllegalStateException!
 * </p>
 * <p>
 * Privileges can be retrieved by name, and possibly created/stored on the fly through {@link #getOrCreatePrivilege(String)}.
 * </p>
 * <p>
 * An expanded set of all the names of permissions can be retrieved for either an actionsNames string with one or more
 * comma delimited action names through {@link #getOrCreatePermissionNames(String)}, or for an actionsNames string set
 * through {@link #getOrCreatePermissionNames(Set)}, which both also will create/store new privileges for such
 * permissions on the fly if needed.
 * </p>
 * <p>Permission and privilege names may not contain the comma (',') character and are always first trimmed on input.
 * </p>
 * <p>
 * The currently registered privileges can be retrieved through {@link #getCurrentPrivileges()}
 * </p>
 * <p>
 * Registered privileges and permission names are kept/cached <em>forever</em>!
 * </p>
 */
class PermissionManager {

    private static final class PrivilegeImpl implements Privilege {

        private final String name;
        private final Privilege[] declaredAggregatePrivileges;
        private final Privilege[] aggregatePrivileges;

        private PrivilegeImpl(final String name) {
            this(name, (Privilege[])null);
        }

        private PrivilegeImpl(final String name, final Privilege... privileges) {
            this.name = name;
            if (privileges == null || privileges.length == 0) {
                this.declaredAggregatePrivileges = aggregatePrivileges = new Privilege[0];
            } else {
                this.declaredAggregatePrivileges = privileges.clone();
                LinkedHashSet<Privilege> allPrivileges = new LinkedHashSet<>();
                for (Privilege p : privileges) {
                    if (allPrivileges.add(p) && p.isAggregate()) {
                        for (Privilege i : p.getAggregatePrivileges()) {
                            allPrivileges.add(i);
                        }
                    }
                }
                this.aggregatePrivileges = allPrivileges.toArray(new Privilege[0]);
            }
        }

        public String getName() {
            return name;
        }

        public boolean isAbstract() {
            return false;
        }

        public boolean isAggregate() {
            return declaredAggregatePrivileges.length > 0;
        }

        public Privilege[] getDeclaredAggregatePrivileges() {
            return declaredAggregatePrivileges.clone();
        }

        public Privilege[] getAggregatePrivileges() {
            return aggregatePrivileges.clone();
        }

        public String toString() {
            return getName();
        }
    }

    private static final PermissionManager INSTANCE = new PermissionManager();

    public static final PermissionManager getInstance() {
        return INSTANCE;
    }

    private final Map<String, Privilege> standardPrivilegesMap;
    private final Map<String, Set<String>> permissionsMap = new ConcurrentHashMap<>();
    private final Map<String, Privilege> currentPrivilegesMap = new ConcurrentHashMap<>();

    private PermissionManager() {
        HashMap<String, Privilege> map = new HashMap<>();
        Set<Privilege> privileges = new HashSet<>();

        privileges.add(addJcrPrivilege(StandardPermissionNames.JCR_MODIFY_PROPERTIES, Privilege.JCR_MODIFY_PROPERTIES, map));
        privileges.add(addJcrPrivilege(StandardPermissionNames.JCR_ADD_CHILD_NODES, Privilege.JCR_ADD_CHILD_NODES, map));
        privileges.add(addJcrPrivilege(StandardPermissionNames.JCR_REMOVE_NODE, Privilege.JCR_REMOVE_NODE, map));
        privileges.add(addJcrPrivilege(StandardPermissionNames.JCR_REMOVE_CHILD_NODES, Privilege.JCR_REMOVE_CHILD_NODES, map));

        final Privilege jcrWritePrivilege = new PrivilegeImpl(StandardPermissionNames.JCR_WRITE, privileges.toArray(new Privilege[0]));
        map.put(StandardPermissionNames.JCR_WRITE, jcrWritePrivilege);
        map.put(Privilege.JCR_WRITE, jcrWritePrivilege);
        permissionsMap.put(StandardPermissionNames.JCR_WRITE, StandardPermissionNames.JCR_WRITE_PRIVILEGES);
        permissionsMap.put(Privilege.JCR_WRITE, StandardPermissionNames.JCR_WRITE_PRIVILEGES);

        privileges.clear();
        privileges.add(jcrWritePrivilege);
        privileges.add(addJcrPrivilege(StandardPermissionNames.JCR_READ, Privilege.JCR_READ, map));
        privileges.add(addJcrPrivilege(StandardPermissionNames.JCR_READ_ACCESS_CONTROL, Privilege.JCR_READ_ACCESS_CONTROL, map));
        privileges.add(addJcrPrivilege(StandardPermissionNames.JCR_MODIFY_ACCESS_CONTROL, Privilege.JCR_MODIFY_ACCESS_CONTROL, map));
        privileges.add(addJcrPrivilege(StandardPermissionNames.JCR_LOCK_MANAGEMENT, Privilege.JCR_LOCK_MANAGEMENT, map));
        privileges.add(addJcrPrivilege(StandardPermissionNames.JCR_VERSION_MANAGEMENT, Privilege.JCR_VERSION_MANAGEMENT, map));
        privileges.add(addJcrPrivilege(StandardPermissionNames.JCR_NODE_TYPE_MANAGEMENT, Privilege.JCR_NODE_TYPE_MANAGEMENT, map));
        privileges.add(addJcrPrivilege(StandardPermissionNames.JCR_RETENTION_MANAGEMENT, Privilege.JCR_RETENTION_MANAGEMENT, map));
        privileges.add(addJcrPrivilege(StandardPermissionNames.JCR_LIFECYCLE_MANAGEMENT, Privilege.JCR_LIFECYCLE_MANAGEMENT, map));

        final Privilege jcrAllPrivilege = new PrivilegeImpl(StandardPermissionNames.JCR_ALL, privileges.toArray(new Privilege[0]));
        currentPrivilegesMap.put(StandardPermissionNames.JCR_ALL, jcrAllPrivilege);
        currentPrivilegesMap.put(Privilege.JCR_ALL, jcrAllPrivilege);
        permissionsMap.put(StandardPermissionNames.JCR_ALL, StandardPermissionNames.JCR_ALL_PRIVILEGES);
        permissionsMap.put(Privilege.JCR_ALL, StandardPermissionNames.JCR_ALL_PRIVILEGES);

        // seed default product privileges
        addPrivilege(StandardPermissionNames.HIPPO_AUTHOR, currentPrivilegesMap);
        addPrivilege(StandardPermissionNames.HIPPO_EDITOR, currentPrivilegesMap);
        addPrivilege(StandardPermissionNames.HIPPO_ADMIN, currentPrivilegesMap);
        addPrivilege(StandardPermissionNames.HIPPO_REST, currentPrivilegesMap);

        map.putAll(currentPrivilegesMap);
        standardPrivilegesMap = Collections.unmodifiableMap(map);

        StandardPermissionNames.JCR_ACTIONS.forEach(a -> permissionsMap.put(a, Collections.singleton(a)));
    }

    private Privilege addJcrPrivilege(final String privilegeName, final String expandedName, final Map<String, Privilege> privilegeMap) {
        final Privilege privilege = addPrivilege(privilegeName, privilegeMap);
        privilegeMap.put(expandedName, privilege);
        permissionsMap.put(expandedName, Collections.singleton(privilegeName));
        return privilege;
    }

    private Privilege addPrivilege(final String privilegeName, final Map<String, Privilege> privilegeMap) {
        final Privilege privilege = new PrivilegeImpl(privilegeName);
        privilegeMap.put(privilegeName, privilege);
        permissionsMap.put(privilegeName, Collections.singleton(privilegeName));
        return privilege;
    }

    public Privilege getOrCreatePrivilege(String privilegeName) {
        if (StringUtils.isBlank(privilegeName)) {
            throw new IllegalArgumentException("privilegeName cannot be null, empty or blank");
        }
        final String trimmedName = privilegeName.trim();
        Privilege privilege = standardPrivilegesMap.get(trimmedName);
        if (privilege != null) {
            return privilege;
        }
        privilege = currentPrivilegesMap.get(trimmedName);
        if (privilege == null) {
            if (privilegeName.contains(",")) {
                throw new IllegalArgumentException("privilegeName cannot contain ',' character");
            }
            if (StandardPermissionNames.JCR_ACTIONS.contains(trimmedName)) {
                throw new IllegalArgumentException("Standard JCR action " + trimmedName +" not allowed to create a Privilege");
            }
            privilege = currentPrivilegesMap.computeIfAbsent(privilegeName, PrivilegeImpl::new);
            permissionsMap.putIfAbsent(privilegeName, Collections.singleton(privilegeName));
        }
        return privilege;
    }

    public Set<String> getOrCreatePermissionNames(final String actionNames) {
        if (actionNames == null) {
            throw new IllegalArgumentException("actionNames cannot be null");
        }
        Set<String> permissionNames = permissionsMap.get(actionNames);
        if (permissionNames == null) {
            permissionNames = getOrCreatePermissionNames(new HashSet<>(Arrays.asList(actionNames.trim().split("\\s*,\\s*"))));
            permissionsMap.putIfAbsent(actionNames, Collections.unmodifiableSet(permissionNames));
        }
        // return mutable set
        return new HashSet<>(permissionNames);
    }

    public Set<String> getOrCreatePermissionNames(final Set<String> actionNames) {
        if (actionNames == null) {
            throw new IllegalArgumentException("actionNames cannot be null");
        }
        Privilege privilege;
        HashSet<String> permissionNames = new HashSet<>();
        for (String actionName : actionNames) {
            // ignore null values
            if (actionName != null) {
                actionName = actionName.trim();
                // ignore empty names or containing ',' delimiter
                if (actionName.length() > 0 && actionName.indexOf(',') < 0) {
                    if (permissionsMap.containsKey(actionName)) {
                        permissionNames.addAll(permissionsMap.get(actionName));
                    } else {
                        privilege = standardPrivilegesMap.get(actionName);
                        if (privilege == null) {
                            privilege = getOrCreatePrivilege(actionName);
                        }
                        permissionNames.add(privilege.getName());
                        if (privilege.isAggregate()) {
                            for (Privilege p : privilege.getAggregatePrivileges()) {
                                permissionNames.add(p.getName());
                            }
                        }
                    }
                }
            }
        }
        return permissionNames;
    }

    public Privilege[] getCurrentPrivileges() {
        return currentPrivilegesMap.values().toArray(new Privilege[0]);
    }
}
