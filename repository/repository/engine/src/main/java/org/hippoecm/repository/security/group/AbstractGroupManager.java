/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.repository.security.group;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.transaction.NotSupportedException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.security.ManagerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractGroupManager implements GroupManager {

    @SuppressWarnings("unused")
    final static String SVN_ID = "$Id$";

    /**
     * The system/root session
     */
    protected Session session;

    /**
     * The path from the root containing the groups
     */
    protected String groupsPath;

    /**
     * Is the class initialized
     */
    protected boolean initialized = false;

    /**
     * The id of the provider that this manager instance belongs to
     */
    protected String providerId;

    /**
     * Logger
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    /**
     * {@inheritDoc}
     */
    public void init(ManagerContext context) throws RepositoryException {
        this.session = context.getSession();
        this.groupsPath = context.getPath();
        this.providerId = context.getProviderId();
        initManager(context);
    }
    
    /**
     * {@inheritDoc}
     */
    public final Node getGroupNode(String groupId) throws RepositoryException {
        if (!isInitialized()) {
            throw new IllegalStateException("Not initialized.");
        }
        log.trace("Looking for group: {} in path: {}", groupId, groupsPath);
        return session.getRootNode().getNode(groupsPath + "/" + groupId);
    }

    /**
     * {@inheritDoc}
     */
    public final Node createGroupNode(String groupId, String nodeType) throws RepositoryException {
        if (!isInitialized()) {
            throw new IllegalStateException("Not initialized.");
        }
        log.debug("Creating node for group: {} in path: {}", groupId, groupsPath);
        Node group = session.getRootNode().getNode(groupsPath).addNode(groupId, nodeType);
        group.setProperty(HippoNodeType.HIPPO_MEMBERS, new Value[] {});
        return group;
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean isInitialized() {
        return initialized;
    }


    /**
     * {@inheritDoc}
     */
    public void syncGroup(String groupId) {
        // default do nothing
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean addGroup(String groupId) throws NotSupportedException, RepositoryException {
        throw new NotSupportedException("Add group not supported.");
    }

    /**
     * {@inheritDoc}
     */
    public boolean deleteGroup(String groupId) throws NotSupportedException, RepositoryException {
        throw new NotSupportedException("Delete group not supported.");
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> listGroups() throws RepositoryException {
        return Collections.unmodifiableSet(new HashSet<String>(0));
    }

}
