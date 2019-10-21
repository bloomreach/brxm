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
package com.bloomreach.xm.repository.security.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

import org.apache.jackrabbit.core.observation.SynchronousEventListener;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;

import com.bloomreach.xm.repository.security.AbstractRole;
import com.bloomreach.xm.repository.security.Role;
import com.bloomreach.xm.repository.security.UserRole;

import static org.hippoecm.repository.api.HippoNodeType.HIPPOSYS_DESCRIPTION;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_SYSTEM;

/**
 * Base provider for loading and referencing {@link Role}s or {@link UserRole}s into a {@link RolesProviderImpl}
 * or {@link UserRolesProviderImpl} in a fully thread-safe way, with asynchronous background reloading
 * triggered by a <em>synchronous</em> JCR event listener replacing the underlying model atomically.
 * @param <R> the {@link Role} or {@link UserRole} class type for specializing this base model
 */
abstract class AbstractRolesProvider<R extends AbstractRole> {

    private static String parentPath(final String path) {
        return path.substring(0, path.lastIndexOf('/'));
    }

    private static final class Model<R> {
        private final ConcurrentHashMap<String, R> roles = new ConcurrentHashMap<>();
    }

    private final class ModelEventListener implements SynchronousEventListener {

        @Override
        public void onEvent(final EventIterator events) {
            boolean reload = false;
            while (events.hasNext()) {
                Event event = events.nextEvent();
                try {
                    final String path = event.getPath();
                    switch (event.getType()) {
                        case Event.NODE_ADDED:
                            if (rolesPath.equals(parentPath(path))) {
                                // role added
                                reload = true;
                            }
                            break;
                        case Event.NODE_REMOVED:
                            if (rolesPath.equals(path)) {
                                // roles folder node removed
                                reload = true;
                            } else if (rolesPath.equals(parentPath(path))) {
                                // role removed
                                reload = true;
                            }
                            break;
                        case Event.PROPERTY_ADDED:
                        case Event.PROPERTY_CHANGED:
                        case Event.PROPERTY_REMOVED:
                            if (rolesPath.equals(parentPath(parentPath(path)))) {
                                reload = true;
                            }
                            break;
                    }
                } catch (RepositoryException e) {
                    reload = true;
                    break;
                }
            }
            if (reload) {
                synchronized (modelReference) {
                    final Model<R> model = new Model<>();
                    if (loadModel(model)) {
                        modelReference.set(model);
                    }
                    // else session no longer live, new events can be ignored
                }
            }
        }
    }

    private final Session systemSession;
    private final AtomicReference<Model<R>> modelReference = new AtomicReference<>(new Model<>());
    private final String rolesPath;
    private final String roleTypeName;
    private final String rolesPropertyName;
    private final Class<R> roleType;

    /**
     * Factory method for new role instances
     * @param node role node
     * @param name already JCR decoded role node name
     * @param description role description
     * @param system indicator if the role is a system role
     * @param roleNames all directly (not recursively) implied roles
     * @return the role instance
     * @throws RepositoryException if something went wrong
     */
    protected abstract R createRole(final Node node, final String name, final String description, final boolean system,
                                    final Set<String> roleNames)
            throws RepositoryException;

    /**
     * Creates an instance of this base provider requiring a system session (which is 'forked' through impersonation) to
     * read the JCR data, at a specific rolesPath for a specific role type (roleTypeName) and a specific rolesPropertyName
     * to read 'implied' roles.
     *
     * @param systemSession the system session required for reading the JCR data
     * @param rolesPath the parent path containing the roles
     * @param roleTypeName the role node type
     * @param rolesPropertyName the name of the multi-value string property holding 'implied' roles
     * @throws RepositoryException if something fails during the creation of this instance
     */
    protected AbstractRolesProvider(final Session systemSession, final String rolesPath, final String roleTypeName,
                                    final String rolesPropertyName, final Class<R> roleType) throws RepositoryException {
        this.systemSession = systemSession;
        this.rolesPath = rolesPath;
        this.roleTypeName = roleTypeName;
        this.rolesPropertyName = rolesPropertyName;
        this.roleType = roleType;

        this.systemSession.getWorkspace().getObservationManager().addEventListener(new ModelEventListener(),
                Event.NODE_ADDED|Event.NODE_REMOVED|Event.PROPERTY_ADDED|Event.PROPERTY_CHANGED|Event.PROPERTY_REMOVED,
                rolesPath, true, null, null, false);

        synchronized (modelReference) {
            loadModel(modelReference.get());
        }
    }

    String getRolesPath() {
        return rolesPath;
    }

    String getRolesTypeName() {
        return roleTypeName;
    }

    String getRolesPropertyName() {
        return rolesPropertyName;
    }

    Class<R> getRoleType() {
        return roleType;
    }

    /**
     * Get all the roles
     * @return set of all the roles
     */
    public Set<R> getRoles() {
        return new HashSet<>(modelReference.get().roles.values());
    }

    /**
     * Checks if a role exists
     * @param roleName role name
     * @return true if the role exists
     */
    public boolean hasRole(final String roleName) {
        return modelReference.get().roles.containsKey(roleName);
    }

    /**
     * Get the preloaded role by name
     * @param roleName role name
     * @return role instance, null if unknown
     */
    public R getRole(final String roleName) {
        return modelReference.get().roles.get(roleName);
    }

    /**
     * Resolve the set of roles representing and implied by a role name.
     * @param roleName role name
     * @return set of resolved roles instance, empty if none found
     */
    public Set<R> resolveRoles(final String roleName) {
        HashSet<R> result = new HashSet<>();
        resolveRoles(modelReference.get(), roleName, new HashSet<>(), result);
        return result;
    }

    /**
     * Resolve the names of the roles representing and implied by a role name.
     * @param roleName role name
     * @return the set of resolved role names, empty if none found
     */
    public Set<String> resolveRoleNames(final String roleName) {
        return resolveRoles(roleName).stream().map(AbstractRole::getName).collect(Collectors.toSet());
    }

    /**
     * Resolve the set of roles representing and implied by a iterable collection of role names.
     * @param roleNames iterable collection of role names
     * @return the set of resolved roles instance, empty if none found
     */
    public Set<R> resolveRoles(final Iterable<String> roleNames) {
        HashSet<R> result = new HashSet<>();
        Model<R> model = modelReference.get();
        for (String roleName : roleNames) {
            resolveRoles(model, roleName, new HashSet<>(), result);
        }
        return result;
    }

    /**
     * Resolve the set of role names representing and implied by a iterable collection of role names.
     * @param roleNames iterable collection of role names
     * @return the set of resolved role names, empty if none found
     */
    public Set<String> resolveRoleNames(final Iterable<String> roleNames) {
        return resolveRoles(roleNames).stream().map(AbstractRole::getName).collect(Collectors.toSet());
    }

    public void close() {
    }

    /**
     * Internal method to load a multi-valued string property by property name, used for specialization of loading a
     * role model, see {@link RolesProviderImpl#createRole(Node, String, boolean, Set)}
     * @param node role node
     * @param propertyName multi-valued string property
     * @return set of property string values, empty if none found
     * @throws RepositoryException if something goes wrong
     */
    protected Set<String> getValues(final Node node, final String propertyName) throws RepositoryException {
        if (node.hasProperty(propertyName)) {
            HashSet<String> values = new HashSet<>();
            for (Value value : node.getProperty(propertyName).getValues()) {
                values.add(value.getString());
            }
            return Collections.unmodifiableSet(values);
        }
        return Collections.emptySet();
    }

    /**
     * Resilient loading of a roles model, catering for possible intermittent JCR errors and then retry logic
     * with a small wait (100 ms) in between.
     * @param model model object to load
     * @return true if succeeded, false if session no longer alive
     */
    private boolean loadModel(final Model<R> model) {
        if (attemptLoadModel(model)) {
            return true;
        } else {
            while (!attemptLoadModel(model)) {
                if (systemSession.isLive()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignore) {
                    }
                } else {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Loading of a roles model (clearing it first)
     * @param model model object to load
     * @return true if succeeded (model may be empty if no roles found), false if a repository exception occurred
     */
    private boolean attemptLoadModel(final Model<R> model) {
        model.roles.clear();
        try {
            if (systemSession.nodeExists(rolesPath)) {
                Node rolesNode = systemSession.getNode(rolesPath);
                for (Node node : new NodeIterable(rolesNode.getNodes())) {
                    loadRole(model, node);
                }
            }
            return true;
        } catch (RepositoryException ignore) {
        }
        return false;
    }

    /**
     * Load a single role instance from a role node into the model
     * @param model model object to load the role into
     * @param node the role node
     * @throws RepositoryException if something went wrong
     */
    private void loadRole(final Model<R> model, final Node node) throws RepositoryException {
        if (node.isNodeType(roleTypeName)) {
            String roleName = NodeNameCodec.decode(node.getName());
            String description = JcrUtils.getStringProperty(node, HIPPOSYS_DESCRIPTION, null);
            Set<String> rolesNames = getValues(node, rolesPropertyName);
            boolean system = JcrUtils.getBooleanProperty(node, HIPPO_SYSTEM, false);
            R role = createRole(node, roleName, description, system, rolesNames);
            model.roles.putIfAbsent(roleName, role);
        }
    }

    /**
     * Recursively resolve a role and its implied roles by a role name
     * @param model the model to load the role from
     * @param roleName the role name to resolve the role(s) for
     * @param resolvedRoles in-process set of already resolved role names
     * @param result the resolved role(s)
     */
    private void resolveRoles(final Model<R> model, final String roleName, final Set<String> resolvedRoles, final HashSet<R> result) {
        if (roleName != null && !resolvedRoles.contains(roleName)) {
            final R role = model.roles.get(roleName);
            if (role != null) {
                resolvedRoles.add(roleName);
                result.add(role);
                role.getRoles().forEach(name -> resolveRoles(model, name, resolvedRoles, result));
            }
        }
    }
}

