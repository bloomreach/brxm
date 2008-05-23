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

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.transaction.NotSupportedException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.PasswordHelper;
import org.hippoecm.repository.security.AAContext;
import org.hippoecm.repository.security.RepositoryAAContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A User stored in the JCR Repository
 */
public class RepositoryUser implements User {

    /** SVN id placeholder */
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    /**
     * The system/root session
     */
    private Session session;

    /**
     * The path from the root containing the users
     */
    private String usersPath;

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
     * Logger
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * The AA context
     */
    private RepositoryAAContext context;
    
    //------------------------< Interface Impl >--------------------------//
    /**
     * {@inheritDoc}
     */
    public void init(AAContext context, String userId) throws UserException {
        if (userId == null) {
            throw new IllegalArgumentException("UserId must be non-null");
        }
        this.context = (RepositoryAAContext) context;
        this.session = this.context.getRootSession();
        this.usersPath = this.context.getPath();
        this.userId = userId;
        loadUser();
        this.initialized = true;
    }

    /**
     * {@inheritDoc}
     */
    public String getUserID() throws UserException {
        if (!initialized) {
            throw new UserException("Not initialized.");
        }
        return userId;
    }

    /**
     * {@inheritDoc}
     */
    public boolean checkPassword(char[] password) throws UserException {
        if (!initialized) {
            throw new IllegalStateException("Not initialized.");
        }
        try {
            return PasswordHelper.checkHash(new String(password), getPasswordHash());
        } catch (NoSuchAlgorithmException e) {
            throw new UserException("Unknown algorithm: " + e.getMessage(), e);
        } catch (UnsupportedEncodingException e) {
            throw new UserException("Unsupported encoding: " + e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setPassword(char[] password) throws NotSupportedException {
        throw new NotSupportedException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    public boolean isActive() throws UserException {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void setActive(boolean active) throws NotSupportedException, UserException {
        throw new NotSupportedException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    public void setProperty(String key, String value) throws NotSupportedException, UserException {
        throw new NotSupportedException("Not implemented");
    }

    //------------------------< Private Helper methods >--------------------------// 
    /**
     * Find the user in the repository and set the user field
     */
    private void loadUser() throws UserException {
        log.debug("Searching for user: {}", userId);
        String path = usersPath + "/" + userId;
        try {
            user = session.getRootNode().getNode(path);
            log.debug("Found user node: {}", path);
        } catch (RepositoryException e) {
            log.debug("User not found: {}", path);
            throw new UserException("User not found: " + path);
        }
    }
    
    /**
     * Get the (optionally) hashed password of the user
     * @return the password hash
     * @throws UserException
     */
    private String getPasswordHash() throws UserException {
        try {
            return user.getProperty(HippoNodeType.HIPPO_PASSWORD).getString();
        } catch (RepositoryException e) {
            throw new UserException("User password not set.", e);
        }
    }
}
