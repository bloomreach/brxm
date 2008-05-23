/*
 * Copyright 2008 Hippo
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.transaction.NotSupportedException;

import org.hippoecm.repository.security.AAContext;
import org.hippoecm.repository.security.RepositoryAAContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The GroupManager that stores the groups in the JCR Repository
 */
public class RepositoryGroupManager implements GroupManager {

    /** SVN id placeholder */
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    
    /**
     * The current context
     */
    private RepositoryAAContext context;

    /**
     * The jcr system/root session
     */
    private Session session;

    /**
     * The path from the root containing the groups
     */
    private String groupsPath;

    /**
     * Is the class initialized
     */
    private boolean initialized = false;


    /**
     * The current groups
     */
    private Set<Group> groups = new HashSet<Group>();
    
    /**
     * Logger
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());


    //------------------------< Interface Impl >--------------------------//
    /**
     * {@inheritDoc}
     */
    public void init(AAContext aacontext) throws GroupException {
        context = (RepositoryAAContext) aacontext;
        session = context.getRootSession();
        groupsPath = context.getPath();
        loadGroups();
        initialized = true;
    }

    /**
     * {@inheritDoc}
     */
    public Set<Group> listGroups() throws GroupException {
        if (!initialized) {
            throw new IllegalStateException("Not initialized.");
        }
        return Collections.unmodifiableSet(groups);
    }

    /**
     * {@inheritDoc}
     */
    public Set<Group> listMemeberships(String userId) throws GroupException {
        if (!initialized) {
            throw new IllegalStateException("Not initialized.");
        }
        Set<Group> memberships = new HashSet<Group>();
        for (Group group : groups) {
            if (group.isMemeber(userId)) {
                memberships.add(group);
            }
        }
        return Collections.unmodifiableSet(memberships);
    }

    /**
     * {@inheritDoc}
     */
    public boolean addGroup(Group group) throws NotSupportedException, GroupException {
        throw new NotSupportedException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    public boolean deleteGroup(Group group) throws NotSupportedException, GroupException {
        throw new NotSupportedException("Not implemented");
    }

    
    //------------------------< Private Helper methods >--------------------------//
    /**
     * Load all groups from the repository and add them to the groups set
     */
    private void loadGroups() {
        log.debug("Searching for groups node: {}", groupsPath);
        try {
            Node groupsNode = session.getRootNode().getNode(groupsPath);
            log.debug("Found groups node: {}", groupsPath);
            NodeIterator groupIter = groupsNode.getNodes();
            while (groupIter.hasNext()) {
                Group group = new RepositoryGroup();
                group.init(context, groupIter.nextNode().getName());
                groups.add(group);
            }
        } catch (RepositoryException e) {
            log.warn("Exception while parsing groups from path: {}", groupsPath);
        }
    }
}
