/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.security.role;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.security.ManagerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The role implementation storing the roles in the JCR Repository
 */
public class RepositoryRole implements Role {

    /** SVN id placeholder */

    /**
     * The system/root session
     */
    private Session session;

    /**
     * The role node
     */
    Node roleNode;

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
     * The jcr permissions of the current role
     */
    private int permissions;

    /**
     * Logger
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    //------------------------< Interface Impl >--------------------------//
    /**
     * {@inheritDoc}
     */
    public void init(ManagerContext context, String roleId) throws RoleException {
        this.session = context.getSession();
        this.rolePath = context.getPath();
        this.roleId = roleId;
        loadRole();
        initialized = true;
    }

    /**
     * {@inheritDoc}
     */
    public String getRoleId() throws RoleException {
        if (!initialized) {
            throw new IllegalStateException("Not initialized.");
        }
        return roleId;
    }

    /**
     * {@inheritDoc}
     */
    public int getJCRPermissions() throws RoleException {
        if (!initialized) {
            throw new IllegalStateException("Not initialized.");
        }
        return permissions;
    }

    //  ------------------------< Private Helper methods >--------------------------//
    /**
     * Load the role from the repository and fetch the permissions for the role
     * @throws RoleException
     */
    private void loadRole() throws RoleException {
        permissions = 0;
        log.debug("Searching for role: {}", roleId);
        String path = rolePath + "/" + roleId;
        try {
            roleNode = session.getRootNode().getNode(path);
            log.debug("Found role node: {}", path);

            try {
                if (roleNode.getProperty(HippoNodeType.HIPPO_JCRREAD).getBoolean()) {
                    log.trace("Adding jcr read permissions for role: {}", roleId);
                    permissions += READ;
                }
            } catch (PathNotFoundException e) {
                // ignore, role doesn't has the permission
            }

            try {
                if (roleNode.getProperty(HippoNodeType.HIPPO_JCRWRITE).getBoolean()) {
                    log.trace("Adding jcr write permissions for role: {}", roleId);
                    permissions += WRITE;
                }
            } catch (PathNotFoundException e) {
                // ignore, role doesn't has the permission
            }

            try {
                if (roleNode.getProperty(HippoNodeType.HIPPO_JCRREMOVE).getBoolean()) {
                    log.trace("Adding jcr remove permissions for role: {}", roleId);
                    permissions += REMOVE;
                }
            } catch (PathNotFoundException e) {
                // ignore, role doesn't has the permission
            }
        } catch (RepositoryException e) {
            log.debug("Role not found: {}", path);
            throw new RoleException("Role not found: {}" + path);
        }
    }
}
