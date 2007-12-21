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
package org.hippoecm.repository.security.role;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.security.AAContext;
import org.hippoecm.repository.security.RepositoryAAContext;
import org.hippoecm.repository.security.RepositoryLoginHelper;
import org.hippoecm.repository.security.principals.AdminPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryRole implements Role {

    /**
     * The system/root session
     */
    private Session rootSession;

    /**
     * The role node
     */
    Node role;

    /**
     * The path from the root containing the users
     */
    private String rolePath;

    /**
     * Is the class initialized
     */
    private boolean initialized = false;

    /**
     * The current role id
     */
    private String roleId;

    /**
     * The role's principals
     */
    private Set<Principal> principals = new HashSet<Principal>();
    
    /**
     * The admin role
     */
    private static final String ADMIN_ROLE_NAME = "admin";

    /**
     * Logger 
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    //------------------------< Interface Impl >--------------------------//

    public void init(AAContext context, String roleId) throws RoleNotFoundException {
        this.rootSession = ((RepositoryAAContext) context).getRootSession();
        this.rolePath = ((RepositoryAAContext) context).getRolesPath();
        this.roleId = roleId;
        initialized = true;
        setRole();
        setPrincipals();
    }

    public Set<Principal> getPrincipals() throws RoleNotFoundException {
        if (!initialized) {
            throw new RoleNotFoundException("Not initialized.");
        }
        if (role == null) {
            throw new RoleNotFoundException("Role not set.");
        }
        return Collections.unmodifiableSet(principals);
    }
    
    public String getRoleId() throws RoleNotFoundException {
        if (!initialized) {
            throw new RoleNotFoundException("Not initialized.");
        }
        return roleId;
    }

    //  ------------------------< Private Helper methods >--------------------------//

    private void setRole() throws RoleNotFoundException {
        if (!initialized) {
            throw new RoleNotFoundException("Not initialized.");
        }
        if (log.isDebugEnabled()) {
            log.debug("Searching for role: " + roleId);
        }
        String path = rolePath + "/" + roleId;

        try {
            this.role = rootSession.getRootNode().getNode(path);
            if (log.isDebugEnabled()) {
                log.debug("Found role node: " + path);
            }
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.debug("Role not found: " + path);
            }
            throw new RoleNotFoundException("Role not found: " + path);
        }
    }

    private void setPrincipals() throws RoleNotFoundException {
        // Special roles
        if (ADMIN_ROLE_NAME.equals(roleId)) {
            if (log.isDebugEnabled()) {
                log.debug("Adding admin principal.");
            }
            principals.add(new AdminPrincipal());
        }
        
        try {
            Node facetAuthPath = role.getNode(HippoNodeType.FACETAUTH_PATH);
            principals.addAll(RepositoryLoginHelper.getFacetAuths(facetAuthPath));
        } catch (PathNotFoundException e) {
            // no facet auths for role
        } catch (RepositoryException e) {
            // wrap error
            throw new RoleNotFoundException("Error while getting role facets.", e);
        }

    }

}
