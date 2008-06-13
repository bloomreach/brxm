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
import javax.jcr.ValueFormatException;
import javax.transaction.NotSupportedException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.security.AAContext;
import org.hippoecm.repository.security.RepositoryAAContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A group stored in the JCR Repository
 */
public class RepositoryGroup implements Group {

    /** SVN id placeholder */
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    /**
     * The system/root session
     */
    private Session session;

    /**
     * The path from the root containing the groups
     */
    private String groupsPath;

    /**
     * The current group id
     */
    private String groupId;

    /**
     * Is the class initialized
     */
    private boolean initialized = false;

    /**
     * The group's members
     */
    private Set<String> members = new HashSet<String>();

    /**
     * The current context
     */
    private RepositoryAAContext context;

    /**
     * The wildcard string that matches everything or every user
     */
    private static final String WILDCARD = "*";

    /**
     * Logger
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());


    //------------------------< Interface Impl >--------------------------//
    /**
     * {@inheritDoc}
     */
    public void init(AAContext context, String groupId) throws GroupException {
        this.context = (RepositoryAAContext) context;
        this.session = this.context.getRootSession();
        this.groupsPath = this.context.getPath();
        this.groupId = groupId;
        setMembers();
        initialized = true;
    }

    /**
     * {@inheritDoc}
     */
    public String getGroupId() throws GroupException {
        if (!initialized) {
            throw new IllegalStateException("Not initialized.");
        }
        return groupId;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> listMemebers() throws GroupException {
        if (!initialized) {
            throw new IllegalStateException("Not initialized.");
        }
        return Collections.unmodifiableSet(members);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMemeber(String userId) throws GroupException {
        if (!initialized) {
            throw new IllegalStateException("Not initialized.");
        }
        Set<String> memebers = listMemebers();
        for (String member : memebers) {
            if (WILDCARD.equals(member)) {
                return true;
            }
            if (member.equals(userId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean addMember(String userId) throws NotSupportedException, GroupException {
        throw new NotSupportedException("Not implemented yet.");
    }

    /**
     * {@inheritDoc}
     */
    public boolean deleteMember(String userid) throws NotSupportedException, GroupException {
        throw new NotSupportedException("Not implemented yet.");
    }

    //------------------------< Private Helper methods >--------------------------//
    /**
     * Get the members of the group from the repository and add them to the group
     */
    private void setMembers() throws GroupException {
        log.debug("Searching for group: {}", groupId);
        String path = groupsPath + "/" + groupId;
        try {
            Node groupNode = session.getRootNode().getNode(path);
            log.debug("Found group node: {}", path);
            Value[] memberValues = groupNode.getProperty(HippoNodeType.HIPPO_MEMBERS).getValues();
            for (Value member : memberValues) {
                try {
                    members.add(member.getString());
                    log.debug("Added memeber: {} for group: {}", member.getString(), groupId);
                } catch (ValueFormatException e) {
                    log.warn("Invalid member userId for group node: " + path, e);
                }
            }
        } catch (RepositoryException e) {
            log.warn("Group node not found: {}", path);
        }
    }
}
