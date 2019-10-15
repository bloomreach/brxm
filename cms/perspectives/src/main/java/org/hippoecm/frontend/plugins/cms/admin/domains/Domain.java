/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.admin.domains;

import java.io.Serializable;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.util.JcrUtils;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_GROUPS;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_USERROLE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_USERS;

public class Domain implements Comparable<Domain>, IClusterable {

    private static final String PROP_DESCRIPTION = "hipposys:description";

    private final String path;
    private final String folder;
    private final String name;
    private String description = "";

    private transient Node node;

    private final SortedMap<String, AuthRole> authRoles = new TreeMap<>();
    private final SortedMap<String, AuthRole> namedAuthRoles = new TreeMap<>();

    public static class AuthRole implements Comparable<AuthRole>, Serializable {

        private final String name;
        private final String path;
        private final String role;
        private final String userrole;
        private final SortedSet<String> usernames = new TreeSet<>();
        private final SortedSet<String> groupnames = new TreeSet<>();

        private transient Node authRoleNode;

        public AuthRole(final Node node) {
            authRoleNode = node;
            try {
                name = NodeNameCodec.decode(node.getName());
                path = node.getPath();
                role = node.getProperty(HippoNodeType.HIPPO_ROLE).getString();
                userrole = JcrUtils.getStringProperty(node, HIPPO_USERROLE, null);
                if (node.hasProperty(HIPPO_USERS)) {
                    final Property property = node.getProperty(HIPPO_USERS);
                    for (final Value val : property.getValues()) {
                        usernames.add(val.getString());
                    }
                }
                if (node.hasProperty(HIPPO_GROUPS)) {
                    final Property property = node.getProperty(HIPPO_GROUPS);
                    for (final Value val : property.getValues()) {
                        groupnames.add(val.getString());
                    }
                }
            } catch (RepositoryException e) {
                throw new IllegalStateException("Cannot create Authrole based on node.", e);
            }
        }

        public String getName() {
            return name;
        }

        public String getPath() {
            return path;
        }

        public String getRole() {
            return role;
        }

        public String getUserrole() {
            return userrole;
        }

        public SortedSet<String> getUsernames() {
            return usernames;
        }

        public SortedSet<String> getGroupnames() {
            return groupnames;
        }


        @Override
        public boolean equals(final Object other) {
            return other instanceof AuthRole && name.equals(((AuthRole)other).name);
        }

        @Override
        public int compareTo(AuthRole o) {
            return getName().compareTo(o.getName());
        }

        public int hashCode() {
            return name.hashCode();
        }

        public void removeUser(final String user) throws RepositoryException {
            usernames.remove(user);
            if (usernames.isEmpty()) {
                final Property p = JcrUtils.getPropertyIfExists(authRoleNode, HIPPO_USERS);
                if (p != null) {
                    p.remove();
                }
            } else {
                authRoleNode.setProperty(HIPPO_USERS, usernames.toArray(new String[0]));
            }
            authRoleNode.getSession().save();
        }

        public void addUser(final String user) throws RepositoryException {
            usernames.add(user);
            authRoleNode.setProperty(HIPPO_USERS, usernames.toArray(new String[0]));
            authRoleNode.getSession().save();
        }
        public void removeGroup(final String group) throws RepositoryException {
            groupnames.remove(group);
            if (groupnames.isEmpty()) {
                final Property p = JcrUtils.getPropertyIfExists(authRoleNode, HIPPO_GROUPS);
                if (p != null) {
                    p.remove();
                }
            } else {
                authRoleNode.setProperty(HIPPO_GROUPS, groupnames.toArray(new String[0]));
            }
            authRoleNode.getSession().save();
        }

        public void addGroup(final String group) throws RepositoryException {
            groupnames.add(group);
            authRoleNode.setProperty(HIPPO_GROUPS, groupnames.toArray(new String[0]));
            authRoleNode.getSession().save();
        }

        public void setUserRole(final String userRole) throws RepositoryException {
            authRoleNode.setProperty(HIPPO_USERROLE, userRole);
            authRoleNode.getSession().save();
        }

        public void removeUserRole() throws RepositoryException {
            final Property p = JcrUtils.getPropertyIfExists(authRoleNode, HIPPO_USERROLE);
            if (p != null) {
                p.remove();
                authRoleNode.getSession().save();
            }
        }

        public void delete() throws RepositoryException {
            Session session = authRoleNode.getSession();
            authRoleNode.remove();
            session.save();
        }
    }

    public SortedMap<String, AuthRole> getAuthRoles() {
        return authRoles;
    }

    public SortedMap<String, AuthRole> getNamedAuthRoles() {
        return namedAuthRoles;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getFolder() {
        return folder;
    }

    public Domain(final Node node) throws RepositoryException {
        this.node = node;

        path = node.getPath();
        folder = node.getParent().getPath();
        name = NodeNameCodec.decode(node.getName());

        if (node.hasProperty(PROP_DESCRIPTION)) {
            description = node.getProperty(PROP_DESCRIPTION).getString();
        }

        // loop over all authroles
        final NodeIterator iter = node.getNodes();
        while (iter.hasNext()) {
            final Node child = iter.nextNode();
            if (child != null && child.getPrimaryNodeType().isNodeType(HippoNodeType.NT_AUTHROLE)) {
                final AuthRole authRole = new AuthRole(child);
                authRoles.put(authRole.getRole(), authRole);
                namedAuthRoles.put(authRole.getName(), authRole);
            }
        }
    }

    //-------------------- persistence helpers ----------//
    public AuthRole createAuthRole(String role) throws RepositoryException {
        final Node roleNode = node.addNode(role, HippoNodeType.NT_AUTHROLE);
        roleNode.setProperty(HippoNodeType.HIPPO_ROLE, role);
        node.getSession().save();
        final AuthRole authRole = new AuthRole(roleNode);
        authRoles.put(role, authRole);
        return authRole;
    }

    public AuthRole createAuthRole(final String name, final String role) throws RepositoryException {
        final Node authRoleNode = node.addNode(name, HippoNodeType.NT_AUTHROLE);
        authRoleNode.setProperty(HippoNodeType.HIPPO_ROLE, role);
        node.getSession().save();
        final AuthRole authRole = new AuthRole(authRoleNode);
        authRoles.put(role, authRole);
        namedAuthRoles.put(name, authRole);
        return authRole;
    }

    public void addGroupToRole(String role, String group) throws RepositoryException {
        if (getAuthRoles().containsKey(role)) {
            getAuthRoles().get(role).addGroup(group);
        } else {
            createAuthRole(role).addGroup(group);
        }
    }

    public void removeGroupFromRole(String role, String group) throws RepositoryException {
        if (getAuthRoles().containsKey(role)) {
            getAuthRoles().get(role).removeGroup(group);
        }
    }

    public void addUserToRole(String role, String user) throws RepositoryException {
        if (getAuthRoles().containsKey(role)) {
            getAuthRoles().get(role).addUser(user);
        } else {
            createAuthRole(role).addUser(user);
        }
    }

    public void removeUserFromRole(String role, String user) throws RepositoryException {
        if (getAuthRoles().containsKey(role)) {
            getAuthRoles().get(role).removeUser(user);
        }
    }

    public void setUserRoleToRole(String role, String userRole) throws RepositoryException {
        if (getAuthRoles().containsKey(role)) {
            getAuthRoles().get(role).setUserRole(userRole);
        } else {
            createAuthRole(role).setUserRole(userRole);
        }
    }

    public void removeUserRoleFromRole(String role) throws RepositoryException {
        if (getAuthRoles().containsKey(role)) {
            getAuthRoles().get(role).removeUserRole();
        }
    }

    public Domain.AuthRole getAuthRole(final String name) {
        return namedAuthRoles.get(name);
    }

    //--------------------- default object -------------------//

    public boolean equals(Object obj) {
        return obj instanceof Domain && path.equals(((Domain)obj).getPath());
    }

    public int hashCode() {
        return path == null ? 0 : path.hashCode();
    }

    @Override
    public int compareTo(Domain o) {
        return getPath().compareTo(o.getPath());
    }
}
