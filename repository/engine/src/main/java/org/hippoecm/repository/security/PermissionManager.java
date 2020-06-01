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

import static org.onehippo.repository.security.StandardPermissionNames.JCR_ACTIONS;
import static org.onehippo.repository.security.StandardPermissionNames.JCR_ADD_CHILD_NODES;
import static org.onehippo.repository.security.StandardPermissionNames.JCR_ALL;
import static org.onehippo.repository.security.StandardPermissionNames.JCR_ALL_PRIVILEGES;
import static org.onehippo.repository.security.StandardPermissionNames.JCR_LIFECYCLE_MANAGEMENT;
import static org.onehippo.repository.security.StandardPermissionNames.JCR_LOCK_MANAGEMENT;
import static org.onehippo.repository.security.StandardPermissionNames.JCR_MODIFY_ACCESS_CONTROL;
import static org.onehippo.repository.security.StandardPermissionNames.JCR_MODIFY_PROPERTIES;
import static org.onehippo.repository.security.StandardPermissionNames.JCR_NODE_TYPE_MANAGEMENT;
import static org.onehippo.repository.security.StandardPermissionNames.JCR_READ;
import static org.onehippo.repository.security.StandardPermissionNames.JCR_READ_ACCESS_CONTROL;
import static org.onehippo.repository.security.StandardPermissionNames.JCR_REMOVE_CHILD_NODES;
import static org.onehippo.repository.security.StandardPermissionNames.JCR_REMOVE_NODE;
import static org.onehippo.repository.security.StandardPermissionNames.JCR_RETENTION_MANAGEMENT;
import static org.onehippo.repository.security.StandardPermissionNames.JCR_VERSION_MANAGEMENT;
import static org.onehippo.repository.security.StandardPermissionNames.JCR_WRITE;
import static org.onehippo.repository.security.StandardPermissionNames.JCR_WRITE_PRIVILEGES;

/**
 * Internal {@link #getInstance() singleton} and fully thread-safe PermissionManager to get the predefined jcr and
 * default custom privileges, and dynamically created privileges, and the representing permission name(s) for them.
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
 * Registered privileges and permission names are kept in memory for the life-span of this (singleton) instance!
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

    static final PermissionManager getInstance() {
        return INSTANCE;
    }

    private final Map<String, Privilege> standardPrivilegesMap;
    private final Map<String, Set<String>> permissionsMap = new ConcurrentHashMap<>();
    private final Map<String, Privilege> currentPrivilegesMap = new ConcurrentHashMap<>();

    private PermissionManager() {
        final HashMap<String, Privilege> privilegeMap = new HashMap<>();

        final Set<Privilege> jcrWritePrivileges = new HashSet<>();
        addAndColllectJcrPrivilege(JCR_MODIFY_PROPERTIES, Privilege.JCR_MODIFY_PROPERTIES, privilegeMap, jcrWritePrivileges);
        addAndColllectJcrPrivilege(JCR_ADD_CHILD_NODES, Privilege.JCR_ADD_CHILD_NODES, privilegeMap, jcrWritePrivileges);
        addAndColllectJcrPrivilege(JCR_REMOVE_NODE, Privilege.JCR_REMOVE_NODE, privilegeMap, jcrWritePrivileges);
        addAndColllectJcrPrivilege(JCR_REMOVE_CHILD_NODES, Privilege.JCR_REMOVE_CHILD_NODES, privilegeMap, jcrWritePrivileges);
        final Privilege jcrWritePrivilege = new PrivilegeImpl(JCR_WRITE, jcrWritePrivileges.toArray(new Privilege[0]));

        privilegeMap.put(JCR_WRITE, jcrWritePrivilege);
        privilegeMap.put(Privilege.JCR_WRITE, jcrWritePrivilege);
        permissionsMap.put(JCR_WRITE, JCR_WRITE_PRIVILEGES);
        permissionsMap.put(Privilege.JCR_WRITE, JCR_WRITE_PRIVILEGES);

        final Set<Privilege> jcrAllPrivileges = new HashSet<>();
        jcrAllPrivileges.add(jcrWritePrivilege);
        addAndColllectJcrPrivilege(JCR_READ, Privilege.JCR_READ, privilegeMap, jcrAllPrivileges);
        addAndColllectJcrPrivilege(JCR_READ_ACCESS_CONTROL, Privilege.JCR_READ_ACCESS_CONTROL, privilegeMap, jcrAllPrivileges);
        addAndColllectJcrPrivilege(JCR_MODIFY_ACCESS_CONTROL, Privilege.JCR_MODIFY_ACCESS_CONTROL, privilegeMap, jcrAllPrivileges);
        addAndColllectJcrPrivilege(JCR_LOCK_MANAGEMENT, Privilege.JCR_LOCK_MANAGEMENT, privilegeMap, jcrAllPrivileges);
        addAndColllectJcrPrivilege(JCR_VERSION_MANAGEMENT, Privilege.JCR_VERSION_MANAGEMENT, privilegeMap, jcrAllPrivileges);
        addAndColllectJcrPrivilege(JCR_NODE_TYPE_MANAGEMENT, Privilege.JCR_NODE_TYPE_MANAGEMENT, privilegeMap, jcrAllPrivileges);
        addAndColllectJcrPrivilege(JCR_RETENTION_MANAGEMENT, Privilege.JCR_RETENTION_MANAGEMENT, privilegeMap, jcrAllPrivileges);
        addAndColllectJcrPrivilege(JCR_LIFECYCLE_MANAGEMENT, Privilege.JCR_LIFECYCLE_MANAGEMENT, privilegeMap, jcrAllPrivileges);
        final Privilege jcrAllPrivilege = new PrivilegeImpl(JCR_ALL, jcrAllPrivileges.toArray(new Privilege[0]));

        currentPrivilegesMap.put(JCR_ALL, jcrAllPrivilege);
        currentPrivilegesMap.put(Privilege.JCR_ALL, jcrAllPrivilege);
        permissionsMap.put(JCR_ALL, JCR_ALL_PRIVILEGES);
        permissionsMap.put(Privilege.JCR_ALL, JCR_ALL_PRIVILEGES);

        // seed default product privileges
        addAndCollectPrivilege(StandardPermissionNames.HIPPO_AUTHOR, currentPrivilegesMap);
        addAndCollectPrivilege(StandardPermissionNames.HIPPO_EDITOR, currentPrivilegesMap);
        addAndCollectPrivilege(StandardPermissionNames.HIPPO_ADMIN, currentPrivilegesMap);
        addAndCollectPrivilege(StandardPermissionNames.HIPPO_REST, currentPrivilegesMap);

        privilegeMap.putAll(currentPrivilegesMap);
        standardPrivilegesMap = Collections.unmodifiableMap(privilegeMap);

        JCR_ACTIONS.forEach(action -> permissionsMap.put(action, Collections.singleton(action)));
    }

    private void addAndColllectJcrPrivilege(final String privilegeName, final String expandedName,
                                            final Map<String, Privilege> privilegeMap, final Set<Privilege> privilegeSet) {
        final Privilege privilege = new PrivilegeImpl(privilegeName);
        permissionsMap.put(privilegeName, Collections.singleton(privilegeName));
        permissionsMap.put(expandedName, Collections.singleton(privilegeName));
        privilegeMap.put(privilegeName, privilege);
        privilegeMap.put(expandedName, privilege);
        privilegeSet.add(privilege);
    }

    private void addAndCollectPrivilege(final String privilegeName, final Map<String, Privilege> privilegeMap) {
        final Privilege privilege = new PrivilegeImpl(privilegeName);
        permissionsMap.put(privilegeName, Collections.singleton(privilegeName));
        privilegeMap.put(privilegeName, privilege);
    }

    /**
     * Thread-safe retrieval of a {@link Privilege} for a specific privilege name, which if needed is created on-the-fly.
     * <p>
     * Because privileges are immutable and kept in memory for the singleton life-span of the PermissionManager, always
     * the same instance will be returned for the same privilege name, and therefore can be compared on object identity.
     * </p>
     * <p>
     * Privilege names will first be trimmed, so "foo" will create/return the same (instance of) privilege as " foo ".
     * </p>
     * <p>
     * Privilege names must be non-null, non-empty, not an empty string, and are not allowed to contain a comma (','),
     * otherwise an IllegalArgumentException is throw.
     * </p>
     * <p>
     * In addition, the standard JCR ACTION permission names are not allowed/supported and will also throw an
     * IllegalArgumentException:
     * </p>
     * <u>
     *     <li>{@link javax.jcr.Session#ACTION_READ} ("read")</li>
     *     <li>{@link javax.jcr.Session#ACTION_ADD_NODE} ("add_node")</li>
     *     <li>{@link javax.jcr.Session#ACTION_SET_PROPERTY} ("set_property")</li>
     *     <li>{@link javax.jcr.Session#ACTION_REMOVE} ("remove")</li>
     * </u>
     * @param privilegeName the name of the privilege
     * @return the privilege
     * @throws IllegalArgumentException thrown when using an invalid or unsupported privilege name
     */
    Privilege getOrCreatePrivilege(String privilegeName) throws IllegalArgumentException {
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
            if (JCR_ACTIONS.contains(trimmedName)) {
                throw new IllegalArgumentException("Standard JCR action " + trimmedName +" not allowed to create a Privilege");
            }
            privilege = currentPrivilegesMap.computeIfAbsent(privilegeName, PrivilegeImpl::new);
            permissionsMap.putIfAbsent(privilegeName, Collections.singleton(privilegeName));
        }
        return privilege;
    }

    /**
     * Thread-safe retrieval of a set of permission names and/or privilege names as referenced in the provided string of
     * actionName(s), separated by a comma (',').
     * <p>
     * For yet unknown <em>privilege</em> names, first a Privilege will be created on-the-fly through {@link #getOrCreatePrivilege(String)}.
     * </p>
     * <p>
     * As the splitted list of 'names' is first trimmed, and the standard JCR ACTION permission names are pre-registered,
     * none of the restrictions for {@link #getOrCreatePrivilege(String)} apply here, other than that the actionNames
     * parameter itself must not be null (throws IllegalArgumentException otherwise).
     * </p>
     * <p>
     *  A special case are the {@link StandardPermissionNames#JCR_WRITE} and {@link StandardPermissionNames#JCR_ALL}
     *  permission names: when requested those will be <em>replaced</em> will their set of aggregated privilege names
     *  {@link StandardPermissionNames#JCR_WRITE_PRIVILEGES} and {@link StandardPermissionNames#JCR_ALL_PRIVILEGES},
     *  <em>without</em> themselves (just because the current use-cases are more optimal that way).
     * </p>
     * @param actionNames one or more permission/privilege names, separated by comma ','
     * @return the effective set of permission names for the provided actionNames
     * @throws IllegalArgumentException in case the actionNames parameter is null
     */
    Set<String> getOrCreatePermissionNames(final String actionNames) {
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

    /**
     * Thread-safe retrieval of a set of permission names and/or privilege names as referenced in the provided set of
     * actionName(s).
     * <p>
     * For yet unknown <em>privilege</em> names, first a Privilege will be created on-the-fly through {@link #getOrCreatePrivilege(String)}.
     * </p>
     * <p>
     * Standard JCR ACTION permission names are supported (pre-registered).
     * </p>
     * <p>
     *  A special case are the {@link StandardPermissionNames#JCR_WRITE} and {@link StandardPermissionNames#JCR_WRITE}
     *  permission names: when requested those will be <em>replaced</em> will their set of aggregated privilege names
     *  {@link StandardPermissionNames#JCR_WRITE_PRIVILEGES} and {@link StandardPermissionNames#JCR_ALL_PRIVILEGES},
     *  <em>without</em> themselves (just because the current use-cases are more optimal that way).
     * </p>
     * @param actionNames a set of zero or more permission/privilege names
     * @return the effective set of permission names for the provided actionNames
     * @throws IllegalArgumentException in case the actionNames parameter is null
     */
    Set<String> getOrCreatePermissionNames(final Set<String> actionNames) {
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

    /**
     * Thread-safe retrieval of all the pre-registered and possible dynamically created {@link Privilege}s
     * @return all the pre-registered and possible dynamically created {@link Privilege}s
     */
    Privilege[] getCurrentPrivileges() {
        return currentPrivilegesMap.values().toArray(new Privilege[0]);
    }
}
