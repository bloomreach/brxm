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
package org.hippoecm.repository.security.user;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.transaction.NotSupportedException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.security.ManagerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UserManager provider that stores the users inside the JCR repository
 */
public abstract class AbstractUserManager implements UserManager {

    /** SVN id placeholder */
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    /**
     * The system/root session
     */
    protected Session session;

    /**
     * The path from the root containing the users
     */
    protected String usersPath;

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

    //------------------------< Interface Impl >--------------------------//
    /**
     * Don't override. This is the general init stuff. Use managerInit for implementation
     * specific stuff. 
     */
    public final void init(ManagerContext context) throws RepositoryException {
        this.session = context.getSession();
        this.usersPath = context.getPath();
        this.providerId = context.getProviderId();
        initManager(context);
    }

    /**
     * {@inheritDoc}
     */
    public boolean authenticate(SimpleCredentials creds) throws RepositoryException {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public final boolean hasUserNode(String userId) throws RepositoryException {
        if (!isInitialized()) {
            throw new IllegalStateException("Not initialized.");
        }
        log.trace("Looking for user: {} in path: {}", userId, usersPath);
        return session.getRootNode().hasNode(usersPath + "/" + userId);
    }
    
    /**
     * {@inheritDoc}
     */
    public final Node getUserNode(String userId) throws RepositoryException {
        if (!isInitialized()) {
            throw new IllegalStateException("Not initialized.");
        }
        log.trace("Looking for user: {} in path: {}", userId, usersPath);
        return session.getRootNode().getNode(usersPath + "/" + userId);
    }

    /**
     * {@inheritDoc}
     */
    public final Node createUserNode(String userId, String nodeType) throws RepositoryException {
        if (!isInitialized()) {
            throw new IllegalStateException("Not initialized.");
        }
        log.debug("Creating node for user: {} in path: {}", userId, usersPath);
        return session.getRootNode().getNode(usersPath).addNode(userId, nodeType);
    }

    public final void updateLastLogin(String userId) {
        if (!isInitialized()) {
            throw new IllegalStateException("Not initialized.");
        }
        try {
            Node user = getUserNode(userId);
            user.setProperty(HippoNodeType.HIPPO_LASTLOGIN, Calendar.getInstance());
            user.save();
        } catch (RepositoryException e) {
            log.info("Unable to set lastlogin for user: " + userId);
        }
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
    public void syncUser(String userId) {
        // default do nothing
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> listUsers() throws RepositoryException {
        return new HashSet<String>(0);
    }

    /**
     * {@inheritDoc}
     */
    public boolean addUser(String userId, char[] password) throws NotSupportedException, RepositoryException {
        throw new NotSupportedException("addUser not supported");
    }

    /**
     * {@inheritDoc}
     */
    public boolean deleteUser(String userId) throws NotSupportedException, RepositoryException {
        throw new NotSupportedException("deleteUser not supported");
    }

    /**
     * {@inheritDoc}
     */
    public boolean isActive(String userId) throws RepositoryException {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void setActive(String userId, boolean active) throws NotSupportedException, RepositoryException {
        throw new NotSupportedException("setActive not supported");
    }

    /**
     * {@inheritDoc}
     */
    public void setProperty(String userId, String key, String value) throws NotSupportedException, RepositoryException {
        throw new NotSupportedException("setProperty not supported");
    }

}
