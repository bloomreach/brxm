/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package com.bloomreach.xm.repository.security.impl;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.impl.RepositoryDecorator;
import org.hippoecm.repository.impl.SessionDecorator;
import org.onehippo.repository.InternalHippoRepository;

import com.bloomreach.xm.repository.security.DomainsManager;
import com.bloomreach.xm.repository.security.RepositorySecurityManager;
import com.bloomreach.xm.repository.security.RepositorySecurityProviders;
import com.bloomreach.xm.repository.security.RolesManager;
import com.bloomreach.xm.repository.security.RolesProvider;
import com.bloomreach.xm.repository.security.ChangePasswordManager;
import com.bloomreach.xm.repository.security.UserRolesManager;
import com.bloomreach.xm.repository.security.UserRolesProvider;

import static org.onehippo.repository.security.SecurityConstants.USERROLE_SECURITY_APPLICATION_ADMIN;
import static org.onehippo.repository.security.SecurityConstants.USERROLE_SECURITY_VIEWER;

/**
 * Implementation of the {@link RepositorySecurityManager} which is bound to a specific {@link HippoSession}
 * @see {@link HippoWorkspace#getSecurityManager()}
 */
public class RepositorySecurityManagerImpl implements RepositorySecurityManager {

    final private InternalHippoRepository internalHippoRepository;
    final private RepositorySecurityProviders securityProviders;
    final private HippoSession hippoSession;

    private ChangePasswordManagerImpl changePasswordManager;
    private RolesManagerImpl rolesManager;
    private UserRolesManagerImpl userRolesManager;
    private DomainsManagerImpl domainsManager;
    private HippoSession systemSession;

    private boolean closed;

    public RepositorySecurityManagerImpl(final HippoSession hippoSession) {
        internalHippoRepository = (InternalHippoRepository) RepositoryDecorator.unwrap(hippoSession.getRepository());
        securityProviders = internalHippoRepository.getHippoSecurityManager().getRepositorySecurityProviders();
        this.hippoSession = hippoSession;
    }

    private void createSystemSessionIfNeeded() throws RepositoryException {
        if (systemSession == null) {
            systemSession = SessionDecorator.newSessionDecorator(internalHippoRepository.createSystemSession());
        }
    }

    HippoSession getSystemSession() {
        if (systemSession == null) {
            throw new IllegalStateException("#createSystemSessionIfNeeded() should have been invoked before access " +
                    "to the system session is supported");
        }
        return systemSession;
    }

    HippoSession getHippoSession() {
        return hippoSession;
    }

    @Override
    public RolesProvider getRolesProvider() {
        return securityProviders.getRolesProvider();
    }

    @Override
    public UserRolesProvider getUserRolesProvider() {
        return securityProviders.getUserRolesProvider();
    }

    @Override
    public synchronized ChangePasswordManager getChangePasswordManager()
            throws AccessDeniedException, RepositoryException {
        checkClosed();
        if (changePasswordManager == null) {
            if (hippoSession.isSystemUser() || hippoSession.getUser().isSystemUser() || hippoSession.getUser().isExternal()) {
                throw new AccessDeniedException("Not allowed to use the ChangePasswordManager for system or external users");
            }
            createSystemSessionIfNeeded();
            changePasswordManager = new ChangePasswordManagerImpl(this);
        }
        return changePasswordManager;
    }

    @Override
    public synchronized RolesManager getRolesManager()
            throws AccessDeniedException, RepositoryException {
        checkClosed();
        if (rolesManager == null) {
            if (!hippoSession.isUserInRole(USERROLE_SECURITY_APPLICATION_ADMIN)) {
                throw new AccessDeniedException("Access denied.");
            }
            createSystemSessionIfNeeded();
            rolesManager = new RolesManagerImpl(this);
        }
        return rolesManager;
    }

    @Override
    public synchronized UserRolesManager getUserRolesManager()
            throws AccessDeniedException, RepositoryException {
        checkClosed();
        if (userRolesManager == null) {
            if (!hippoSession.isUserInRole(USERROLE_SECURITY_APPLICATION_ADMIN)) {
                throw new AccessDeniedException("Access denied.");
            }
            createSystemSessionIfNeeded();
            userRolesManager = new UserRolesManagerImpl(this);
        }
        return userRolesManager;
    }

    @Override
    public synchronized DomainsManager getDomainsManager()
            throws AccessDeniedException, RepositoryException {
        checkClosed();
        if (domainsManager == null) {
            if (!hippoSession.isUserInRole(USERROLE_SECURITY_VIEWER)) {
                throw new AccessDeniedException("Access denied.");
            }
            createSystemSessionIfNeeded();
            domainsManager = new DomainsManagerImpl(this);
        }
        return domainsManager;
    }

    /**
     * Check the underlying HippoSession is still alive and otherwise forces a "This session has been closed." RepositoryException
     * @throws RepositoryException when the underlying HippoSession is no longer live (logged out or gc'ed)
     */
    void checkClosed() throws RepositoryException {
        if (closed) {
            // force a "This session has been closed." RepositoryException
            hippoSession.getRootNode();
        }
    }

    public synchronized void close() {
        if (!closed) {
            if (hippoSession.isLive()) {
                throw new IllegalStateException("Close should/can only be called after the hippoSession is logged out!");
            }
            rolesManager = null;
            userRolesManager = null;
            changePasswordManager = null;
            domainsManager = null;
            if (systemSession != null && systemSession.isLive()) {
                systemSession.logout();
                systemSession = null;
            }
            closed = true;
        }
    }
 }
