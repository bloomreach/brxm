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
package org.hippoecm.repository.security.user;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.apache.jackrabbit.core.security.UserPrincipal;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.security.AAContext;
import org.hippoecm.repository.security.RepositoryAAContext;
import org.hippoecm.repository.security.RepositoryLoginHelper;
import org.hippoecm.repository.security.group.Group;
import org.hippoecm.repository.security.group.GroupNotFoundException;
import org.hippoecm.repository.security.group.RepositoryGroup;
import org.hippoecm.repository.security.role.RepositoryRole;
import org.hippoecm.repository.security.role.Role;
import org.hippoecm.repository.security.role.RoleNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryUser implements User {

    /**
     * The system/root session
     */
    private Session rootSession;

    /**
     * The path from the root containing the users
     */
    private String usersPath;

    /**
     * The path from the root containing the groups
     */
    private String groupsPath;

    /**
     * Is the class initialized
     */
    private boolean initialized = false;

    /**
     * The current user id
     */
    private String userId;

    /**
     * The node containing the current user
     */
    private Node user;

    /**
     * The  user's groups
     */
    private Set<Group> memberships = new HashSet<Group>();

    /**
     * The  user's roles
     */
    private Set<Role> roles = new HashSet<Role>();

    /**
     * The user's principals
     */
    private Set<Principal> principals = new HashSet<Principal>();

    /**
     * Logger 
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * The AA context
     */
    private RepositoryAAContext context;

    //------------------------< Interface Impl >--------------------------//
    public void init(AAContext context, String userId) throws UserNotFoundException {
        this.context = (RepositoryAAContext) context;
        this.rootSession = this.context.getRootSession();
        this.usersPath = this.context.getUsersPath();
        this.groupsPath = this.context.getGroupsPath();
        this.userId = userId;
        this.initialized = true;
        setUser();
        setPrincipals();
        setRoles();
        setMemberships();
    }

    public Set<Group> getMemberships() throws UserNotFoundException {
        if (!initialized) {
            throw new UserNotFoundException("Not initialized.");
        }
        if (user == null) {
            throw new UserNotFoundException("User not set.");
        }
        return Collections.unmodifiableSet(memberships);
    }

    public Set<Principal> getPrincipals() throws UserNotFoundException {
        if (!initialized) {
            throw new UserNotFoundException("Not initialized.");
        }
        if (user == null) {
            throw new UserNotFoundException("User not set.");
        }
        return Collections.unmodifiableSet(principals);
    }

    public Set<Role> getRoles() throws UserNotFoundException {
        if (!initialized) {
            throw new UserNotFoundException("Not initialized.");
        }
        if (user == null) {
            throw new UserNotFoundException("User not set.");
        }
        return Collections.unmodifiableSet(roles);
    }

    public String getUserID() throws UserNotFoundException {
        if (!initialized) {
            throw new UserNotFoundException("Not initialized.");
        }
        if (user == null) {
            throw new UserNotFoundException("User not set.");
        }
        return userId;
    }

    //------------------------< Public impl >--------------------------//
    public String getPasswordHash() throws UserNotFoundException {
        if (!initialized) {
            throw new UserNotFoundException("Not initialized.");
        }
        if (user == null) {
            throw new UserNotFoundException("User not set.");
        }

        try {
            return user.getProperty(HippoNodeType.HIPPO_PASSWORD).getString();
        } catch (RepositoryException e) {
            throw new UserNotFoundException("User password not set.", e);
        }
    }

    //------------------------< Private Helper methods >--------------------------//

    private void setUser() throws UserNotFoundException {
        if (!initialized) {
            throw new UserNotFoundException("Not initialized.");
        }
        if (log.isDebugEnabled()) {
            log.debug("Searching for user: " + userId);
        }
        String path = usersPath + "/" + userId;

        try {
            this.user = rootSession.getRootNode().getNode(path);
            if (log.isDebugEnabled()) {
                log.debug("Found user node: " + path);
            }
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.debug("User not found: " + path);
            }
            throw new UserNotFoundException("User not found: " + path);
        }

    }

    private void setPrincipals() throws UserNotFoundException {
        if (!initialized) {
            throw new UserNotFoundException("Not initialized.");
        }
        // set the user principal
        principals.add(new UserPrincipal(userId));
        try {
            // principals from user
            Node facetAuthPath = user.getNode(HippoNodeType.FACETAUTH_PATH);
            principals.addAll(RepositoryLoginHelper.getFacetAuths(facetAuthPath));
        } catch (PathNotFoundException e) {
            // no facet auths for user
        } catch (RepositoryException e) {
            // wrap error
            throw new UserNotFoundException("Error while getting user facet auth rules.", e);
        }
    }

    // TODO: use some kind of query
    private void setMemberships() throws UserNotFoundException {
        if (!initialized) {
            throw new UserNotFoundException("Not initialized.");
        }
        try {
            // TODO: use query to find the correct groups, for now just loop over all
            NodeIterator groupsIter = rootSession.getRootNode().getNode(groupsPath).getNodes();
            while (groupsIter.hasNext()) {
                Node group = (Node) groupsIter.next();
                // emptyp group
                if (!group.hasProperty(HippoNodeType.HIPPO_MEMBERS)) {
                    continue;
                }
                // check membership
                boolean isMember = false;
                Value[] memberValues = group.getProperty(HippoNodeType.HIPPO_MEMBERS).getValues(); 
                for (int i = 0; i < memberValues.length; i++) {
                    if (memberValues[i].getString().equals(userId)) {
                        isMember = true;
                        break;
                    }
                }
                if (!isMember) {
                    continue;
                }
                RepositoryGroup membership = new RepositoryGroup();
                try {
                    membership.init(context, group.getName());
                    memberships.add(membership);
                } catch (GroupNotFoundException e) {
                    log.warn("Unable to add group: " + group.getName(), e);
                }
            }
        } catch (PathNotFoundException e) {
            if (log.isDebugEnabled()) {
                log.debug("No memberships found for user: " + userId);
            }
        } catch (RepositoryException e) {
            log.warn("Unable to set memberships with path: " + groupsPath, e);
        }

        // add the principals to the user
        for (Group group : this.memberships) {
            try {
                this.principals.addAll(group.getPrincipals());
            } catch (GroupNotFoundException e) {
                // this shouldn't happen, we just initialized the roles
                log.error("Unable to add principals for group for user: " + userId, e);
            }
        }
    }

    private void setRoles() {
        try {
            Value[] roles = user.getProperty(HippoNodeType.HIPPO_ROLES).getValues();

            for (Value roleVal : roles) {
                String roleId;
                try {
                    roleId = roleVal.getString();
                } catch (ValueFormatException e) {
                    log.warn("Invalid role for user: " + userId, e);
                    continue;
                }

                try {
                    Role role = new RepositoryRole();
                    role.init(context, roleId);
                    this.roles.add(role);
                } catch (RoleNotFoundException e) {
                    // too bad...
                    log.warn("Role: " + roleId + "not found for user: " + userId);
                }
            }
        } catch (PathNotFoundException e) {
            if (log.isDebugEnabled()) {
                log.debug("No roles found for user: " + userId);
            }
        } catch (RepositoryException e) {
            log.error("Error while trying to get roles for user: " + userId, e);
        }

        // add the principals to the user
        for (Role role : this.roles) {
            try {
                this.principals.addAll(role.getPrincipals());
            } catch (RoleNotFoundException e) {
                // this shouldn't happen, we just initialized the roles
                log.error("Unable to add principals for role for user: " + userId, e);
            }
        }
    }

}
