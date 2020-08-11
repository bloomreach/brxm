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
package com.bloomreach.xm.repository.security;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.Test;
import org.onehippo.repository.security.SecurityConstants;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_USERROLES;
import static org.hippoecm.repository.api.HippoNodeType.NT_USER;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.onehippo.repository.security.SecurityConstants.USERROLE_SECURITY_APPLICATION_ADMIN;
import static org.onehippo.repository.security.SecurityConstants.USERROLE_SECURITY_VIEWER;
import static org.onehippo.repository.security.SecurityConstants.USERROLE_SECURITY_USER_ADMIN;

public class RepositorySecurityManagerTest extends RepositoryTestCase {

    private static final String TEST_USER_ID = "testuser";

    private Node testUserNode;
    private HippoSession testUserSession;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        setupTestData();
    }

    @Override
    public void tearDown() throws Exception {
        tearDownTestData();
        if (testUserSession != null && testUserSession.isLive()) {
            testUserSession.logout();
        }
        super.tearDown();
    }

    private void setupTestData() throws RepositoryException {
        tearDownTestData();
        Node users = session.getNode(SecurityConstants.CONFIG_USERS_PATH);
        testUserNode = users.addNode(TEST_USER_ID, NT_USER);
        session.save();
    }

    private void tearDownTestData() throws RepositoryException {
        Node users = session.getNode(SecurityConstants.CONFIG_USERS_PATH);
        testUserNode = JcrUtils.getNodeIfExists(users, TEST_USER_ID);
        if (testUserNode != null) {
            testUserNode.remove();
            session.save();
            testUserNode = null;
        }
    }

    private HippoSession impersonate(final String userId) throws RepositoryException {
        return (HippoSession)session.impersonate(new SimpleCredentials(userId, new char[0]));
    }

    @Test
    public void testAdministrationManagersAccess() throws RepositoryException {
        testUserSession = impersonate(TEST_USER_ID);
        RepositorySecurityManager repositorySecurityManager = testUserSession.getWorkspace().getSecurityManager();

        assertFalse(testUserSession.isUserInRole(USERROLE_SECURITY_APPLICATION_ADMIN));
        try {
            repositorySecurityManager.getRolesManager();
            fail("Should not be allowed to access RolesManager without userrole " + USERROLE_SECURITY_APPLICATION_ADMIN);
        } catch (AccessDeniedException ignore) {
        }
        try {
            repositorySecurityManager.getUserRolesManager();
            fail("Should not be allowed to access UserRolesManager without userrole " + USERROLE_SECURITY_APPLICATION_ADMIN);
        } catch (AccessDeniedException ignore) {
        }

        testUserNode.setProperty(HIPPO_USERROLES, new String[]{USERROLE_SECURITY_USER_ADMIN});
        session.save();
        testUserSession.logout();
        testUserSession = impersonate(TEST_USER_ID);
        repositorySecurityManager = testUserSession.getWorkspace().getSecurityManager();

        assertTrue(testUserSession.isUserInRole(USERROLE_SECURITY_VIEWER));
        assertTrue(testUserSession.isUserInRole(USERROLE_SECURITY_USER_ADMIN));
        assertFalse(testUserSession.isUserInRole(USERROLE_SECURITY_APPLICATION_ADMIN));
        try {
            repositorySecurityManager.getRolesManager();
            fail("Should not be allowed to access RolesManager without userrole " + USERROLE_SECURITY_APPLICATION_ADMIN);
        } catch (AccessDeniedException ignore) {
        }
        try {
            repositorySecurityManager.getUserRolesManager();
            fail("Should not be allowed to access UserRolesManager without userrole " + USERROLE_SECURITY_APPLICATION_ADMIN);
        } catch (AccessDeniedException ignore) {
        }

        // access to RoleManager and UserRoleManager should only be allowed with USERROLE_SECURITY_APPLICATION_MANAGER
        testUserNode.setProperty(HIPPO_USERROLES, new String[]{USERROLE_SECURITY_APPLICATION_ADMIN});
        session.save();
        testUserSession.logout();
        testUserSession = impersonate(TEST_USER_ID);
        repositorySecurityManager = testUserSession.getWorkspace().getSecurityManager();

        assertTrue(testUserSession.isUserInRole(USERROLE_SECURITY_APPLICATION_ADMIN));
        assertNotNull(repositorySecurityManager.getRolesManager());
        assertNotNull(repositorySecurityManager.getUserRolesManager());

        testUserSession.logout();
    }

    @Test
    public void testIllegalStateExceptionAfterLogout() throws Exception {
        testUserNode.setProperty(HIPPO_USERROLES, new String[]{USERROLE_SECURITY_APPLICATION_ADMIN});
        session.save();
        testUserSession = impersonate(TEST_USER_ID);
        RepositorySecurityManager repositorySecurityManager = testUserSession.getWorkspace().getSecurityManager();
        UserRolesManager userRolesManager = repositorySecurityManager.getUserRolesManager();
        testUserSession.logout();

        try {
            userRolesManager.deleteUserRole(USERROLE_SECURITY_APPLICATION_ADMIN);
            fail("Should not be possible to use a UserRoleManager after its HippoSession has been logged out");
        } catch (RepositoryException e) {
            if (!e.getMessage().startsWith("This session has been closed.")) {
                throw e;
            }
        }

        try {
            repositorySecurityManager.getUserRolesManager();
            fail("Should not be possible to get a UserRoleManager from a RepositorySecurityManager after its HippoSession has been logged out");
        } catch (RepositoryException e) {
            if (!e.getMessage().startsWith("This session has been closed.")) {
                throw e;
            }
        }
    }
}
