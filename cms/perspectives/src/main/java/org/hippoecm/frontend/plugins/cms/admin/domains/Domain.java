/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;

public class Domain implements Comparable<Domain>, IClusterable {

    private static final String PROP_DESCRIPTION = "hipposys:description";
    private static final String DOMAINS_BASE_LOCATION = "/jcr:root/hippo:configuration/hippo:domains";

    private final String path;
    private final String name;
    private String description = "";

    private transient Node node;

    private final SortedMap<String, AuthRole> authRoles = new TreeMap<>();

    public static class AuthRole implements Serializable {

        private final String role;
        private final SortedSet<String> usernames = new TreeSet<>();
        private final SortedSet<String> groupnames = new TreeSet<>();

        private transient Node authRoleNode;

        public AuthRole(final Node node) {
            assert node != null;

            authRoleNode = node;
            try {
                role = node.getProperty(HippoNodeType.HIPPO_ROLE).getString();
                if (node.hasProperty(HippoNodeType.HIPPO_USERS)) {
                    final Property property = node.getProperty(HippoNodeType.HIPPO_USERS);
                    for (final Value val : property.getValues()) {
                        usernames.add(val.getString());
                    }
                }
                if (node.hasProperty(HippoNodeType.HIPPO_GROUPS)) {
                    final Property property = node.getProperty(HippoNodeType.HIPPO_GROUPS);
                    for (final Value val : property.getValues()) {
                        groupnames.add(val.getString());
                    }
                }
            } catch (RepositoryException e) {
                throw new IllegalStateException("Cannot create Authrole based on node.", e);
            }
        }

        public String getRole() {
            return role;
        }

        public SortedSet<String> getUsernames() {
            return usernames;
        }

        public SortedSet<String> getGroupnames() {
            return groupnames;
        }

        public void removeGroup(String group) throws RepositoryException {
            groupnames.remove(group);
            authRoleNode.setProperty(HippoNodeType.HIPPO_GROUPS, groupnames.toArray(new String[groupnames.size()]));
            authRoleNode.getSession().save();
        }

        public void addGroup(String group) throws RepositoryException {
            groupnames.add(group);
            authRoleNode.setProperty(HippoNodeType.HIPPO_GROUPS, groupnames.toArray(new String[groupnames.size()]));
            authRoleNode.getSession().save();
        }
    }

    public SortedMap<String, AuthRole> getAuthRoles() {
        return authRoles;
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

    public Domain(final Node node) throws RepositoryException {
        this.node = node;

        path = node.getPath().substring(1);
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

    public static Domain forName(String domainName) {
        final Session session = UserSession.get().getJcrSession();
        try {
            final String pathToDomain = DOMAINS_BASE_LOCATION + "/" + domainName;
            if (!session.nodeExists(pathToDomain)) {
                throw new IllegalArgumentException(String.format("Domain with name %s does not exist.", domainName));
            }
            final Node node = session.getNode(pathToDomain);
            return new Domain(node);
        } catch (RepositoryException e) {
            final String msg = String.format("Repository error occurred when trying to obtain domain with name %s",
                    domainName);
            throw new IllegalStateException(msg, e);
        }
    }

    //--------------------- default object -------------------//

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        final Domain other = (Domain) obj;
        return other.getPath().equals(getPath());
    }

    public int hashCode() {
        return path == null ? 0 : path.hashCode();
    }

    @Override
    public int compareTo(Domain o) {
        return getName().compareTo(o.getName());
    }
}
