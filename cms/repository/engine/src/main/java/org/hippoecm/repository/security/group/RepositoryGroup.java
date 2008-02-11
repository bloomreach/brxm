/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.security.group;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.security.AAContext;
import org.hippoecm.repository.security.FacetAuthHelper;
import org.hippoecm.repository.security.RepositoryAAContext;
import org.hippoecm.repository.security.role.RepositoryRole;
import org.hippoecm.repository.security.role.Role;
import org.hippoecm.repository.security.role.RoleNotFoundException;
import org.hippoecm.repository.security.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryGroup implements Group {

    /**
     * The system/root session
     */
    private Session rootSession;

    /**
     * The path from the root containing the groups
     */
    private String groupPath;

    /**
     * The current group id
     */
    private String groupId;

    /**
     * The node containing the group
     */
    private Node group;

    /**
     * Is the class initialized
     */
    private boolean initialized = false;

    /**
     * The current group roles
     */
    private Set<Role> roles = new HashSet<Role>();

    /**
     * The group's principals
     */
    private Set<Principal> principals = new HashSet<Principal>();

    /**
     * The group's members
     */
    private Set<User> members = new HashSet<User>();

    /**
     * The current context
     */
    private RepositoryAAContext context;

    /**
     * Logger
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());


    //------------------------< Interface Impl >--------------------------//

    public void init(AAContext context, String groupId) throws GroupNotFoundException {
        this.context = (RepositoryAAContext) context;
        this.rootSession = this.context.getRootSession();
        this.groupPath = this.context.getGroupsPath();
        this.groupId = groupId;
        initialized = true;
        setGroup();
        setRoles();
        setPrincipals();

    }
    public String getGroupId() throws GroupNotFoundException {
        if (!initialized) {
            throw new GroupNotFoundException("Not initialized.");
        }
        return groupId;
    }

    public Set<User> getMembers() throws GroupNotFoundException {
        if (!initialized) {
            throw new GroupNotFoundException("Not initialized.");
        }
        if (group == null) {
            throw new GroupNotFoundException("Group not set.");
        }
        return Collections.unmodifiableSet(members);
    }

    public Set<Principal> getPrincipals() throws GroupNotFoundException {
        if (!initialized) {
            throw new GroupNotFoundException("Not initialized.");
        }
        if (group == null) {
            throw new GroupNotFoundException("Group not set.");
        }
        return Collections.unmodifiableSet(principals);
    }

    public Set<Role> getRoles() throws GroupNotFoundException {
        if (!initialized) {
            throw new GroupNotFoundException("Not initialized.");
        }
        if (group == null) {
            throw new GroupNotFoundException("Group not set.");
        }
        return Collections.unmodifiableSet(roles);
    }


    //------------------------< Private Helper methods >--------------------------//

    private void setGroup() throws GroupNotFoundException {
        if (!initialized) {
            throw new GroupNotFoundException("Not initialized.");
        }
        if (log.isDebugEnabled()) {
            log.debug("Searching for group: " + groupId);
        }
        String path = groupPath + "/" + groupId;

        try {
            this.group = rootSession.getRootNode().getNode(path);
            if (log.isDebugEnabled()) {
                log.debug("Found group node: " + path);
            }
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.debug("Group not found: " + path);
            }
            throw new GroupNotFoundException("Role not found: " + path);
        }

    }

    private void setRoles() {
        try {
            Value[] roles = group.getProperty(HippoNodeType.HIPPO_ROLES).getValues();

            for (Value roleVal : roles) {
                String roleId;
                try {
                    roleId = roleVal.getString();
                } catch (ValueFormatException e) {
                    log.warn("Invalid role for group: " + groupId, e);
                    continue;
                }

                try {
                    Role role = new RepositoryRole();
                    role.init(context, roleId);
                    this.roles.add(role);
                } catch (RoleNotFoundException e) {
                    // too bad...
                    log.warn("Role: " + roleId + "not found for group: " + groupId);
                }
            }
        } catch (PathNotFoundException e) {
            if (log.isDebugEnabled()) {
                log.debug("No roles found for group: " + groupId);
            }
        } catch (RepositoryException e) {
            log.error("Error while trying to get roles for group: " + groupId, e);
        }

        for (Role role : this.roles) {
            try {
                this.principals.addAll(role.getPrincipals());
            } catch (RoleNotFoundException e) {
                // this shouldn't happen, we just initialized the roles
                log.error("Role not found for user after init: " + groupId);
            }
        }

    }

    private void setPrincipals() throws GroupNotFoundException {
        try {
            Node facetAuthPath = group.getNode(HippoNodeType.FACETAUTH_PATH);
            principals.addAll(FacetAuthHelper.getFacetAuths(facetAuthPath));
        } catch (PathNotFoundException e) {
            // no facet auths for user
        } catch (RepositoryException e) {
            // wrap error
            throw new GroupNotFoundException("Error while getting group facetauth rules.", e);
        }

    }

}
