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
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.impl.RepositoryDecorator;
import org.hippoecm.repository.impl.SessionDecorator;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.Test;
import org.onehippo.repository.InternalHippoRepository;
import org.onehippo.repository.security.SecurityConstants;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_USERROLES;
import static org.hippoecm.repository.api.HippoNodeType.NT_USER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.onehippo.repository.security.SecurityConstants.USERROLE_SECURITY_APPLICATION_ADMIN;

public class UserRolesManagerTest extends RepositoryTestCase {

    private static final String TEST_USER_ID = "testuser";
    private static final String TEST_USER_ROLE_NAME = "testuserrole";

    private Node testUserNode;
    private HippoSession testUserSession;
    private HippoSession systemSession;

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
        if (systemSession != null && systemSession.isLive()) {
            systemSession.logout();
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
            testUserNode = null;
        }
        Node userroles = session.getNode(SecurityConstants.CONFIG_USERROLES_PATH);
        Node testUserRoleNode = JcrUtils.getNodeIfExists(userroles, TEST_USER_ID);
        if (testUserRoleNode != null) {
            testUserRoleNode.remove();
        }
        session.save();
    }

    private HippoSession impersonate(final String userId) throws RepositoryException {
        return (HippoSession)session.impersonate(new SimpleCredentials(userId, new char[0]));
    }

    private void createSystemSession() throws RepositoryException {
        Session tmpSystemSession = ((InternalHippoRepository)RepositoryDecorator.unwrap(session.getRepository())).createSystemSession();
        try {
            systemSession = SessionDecorator.newSessionDecorator(tmpSystemSession.impersonate(new SimpleCredentials("system", new char[0])));
        } finally {
            tmpSystemSession.logout();
        }
    }

    @Test
    public void testUserRolesManager() throws RepositoryException {
        testUserNode.setProperty(HIPPO_USERROLES, new String[]{USERROLE_SECURITY_APPLICATION_ADMIN});
        session.save();
        testUserSession = impersonate(TEST_USER_ID);
        createSystemSession();
        UserRolesProvider userRolesProvider = testUserSession.getWorkspace().getSecurityManager().getUserRolesProvider();
        UserRolesManager userRolesManagerSystemUser = systemSession.getWorkspace().getSecurityManager().getUserRolesManager();
        UserRolesManager userRolesManagerTestUser = testUserSession.getWorkspace().getSecurityManager().getUserRolesManager();

        UserRole testUserRole = userRolesProvider.getRole(TEST_USER_ROLE_NAME);
        assertNull(testUserRole);

        try {
            userRolesManagerTestUser.addUserRole(null);
            fail ("Should not be possible to add a userrole with no arguments");
        } catch (IllegalArgumentException ignore) {
        }

        UserRoleBean testUserRoleTemplate = new UserRoleBean();
        assertNull(testUserRoleTemplate.getName());
        try {
            userRolesManagerTestUser.addUserRole(testUserRoleTemplate);
            fail ("Should not be possible to add a userrole without a name");
        } catch (IllegalArgumentException ignore) {
        }

        testUserRoleTemplate.setName("");
        try {
            userRolesManagerTestUser.addUserRole(testUserRoleTemplate);
            fail ("Should not be possible to add a userrole with an empty name");
        } catch (IllegalArgumentException ignore) {
        }

        testUserRoleTemplate.setName(" ");
        try {
            userRolesManagerTestUser.addUserRole(testUserRoleTemplate);
            fail ("Should not be possible to add a userrole with a blank name");
        } catch (IllegalArgumentException ignore) {
        }

        testUserRoleTemplate.setName(TEST_USER_ROLE_NAME);
        testUserRoleTemplate.setSystem(true);
        try (final Log4jInterceptor ignore = Log4jInterceptor.onError().deny(UserRolesManager.class).build()) {
            userRolesManagerTestUser.addUserRole(testUserRoleTemplate);
            fail ("Should not be allowed to add a system userrole as a non-system user");
        } catch (AccessDeniedException ignore) {
        }
        // system user however can create a system userrole
        assertNotNull(userRolesManagerSystemUser.addUserRole(testUserRoleTemplate));

        userRolesManagerSystemUser.deleteUserRole(testUserRoleTemplate.getName());
        testUserRoleTemplate.setSystem(false);

        // test basic add userrole
        testUserRole = userRolesManagerTestUser.addUserRole(testUserRoleTemplate);
        assertNotNull(testUserRole);
        assertEquals(TEST_USER_ROLE_NAME, testUserRole.getName());
        assertNull(testUserRole.getDescription());
        assertFalse(testUserRole.isSystem());
        assertEquals(0, testUserRole.getRoles().size());

        // test basic update userrole
        testUserRoleTemplate.setDescription("foo");
        testUserRoleTemplate.getRoles().add("bar");
        testUserRole = userRolesManagerTestUser.updateUserRole(testUserRoleTemplate);
        assertNotSame(testUserRoleTemplate, testUserRole);
        assertNotNull(testUserRole);
        assertEquals("foo", testUserRole.getDescription());
        assertEquals(1, testUserRole.getRoles().size());
        assertTrue(testUserRole.getRoles().contains("bar"));

        try {
            userRolesManagerTestUser.addUserRole(testUserRoleTemplate);
            fail("Should not be possible to add (or update) an already existing userrole");
        } catch (ItemExistsException ignore) {
        }

        testUserRoleTemplate.setSystem(true);
        try (final Log4jInterceptor ignore = Log4jInterceptor.onError().deny(UserRolesManager.class).build()) {
            userRolesManagerTestUser.updateUserRole(testUserRoleTemplate);
            fail("Should not be allowed to change a userrole to system status");
        } catch (AccessDeniedException ignore) {
        }

        // system session can set/update userrole system status
        testUserRole = userRolesManagerSystemUser.updateUserRole(testUserRoleTemplate);
        assertTrue(testUserRole.isSystem());

        // system userrole may not be modified by non-system user
        testUserRoleTemplate = new UserRoleBean(testUserRole);
        try (final Log4jInterceptor ignore = Log4jInterceptor.onError().deny(UserRolesManager.class).build()) {
            userRolesManagerTestUser.updateUserRole(testUserRoleTemplate);
            fail("Should not be allowed to change a system userrole");
        } catch (AccessDeniedException ignore) {
        }

        // system user however can update a system userrole
        testUserRoleTemplate.setDescription(null);
        testUserRoleTemplate.getRoles().clear();
        testUserRole = userRolesManagerSystemUser.updateUserRole(testUserRoleTemplate);
        assertNull(testUserRole.getDescription());
        assertEquals(0, testUserRole.getRoles().size());

        try (final Log4jInterceptor ignore = Log4jInterceptor.onError().deny(UserRolesManager.class).build()) {
            testUserRoleTemplate.setSystem(false);
            userRolesManagerTestUser.updateUserRole(testUserRoleTemplate);
            fail("Should not be allowed to change the system status of a system userrole");
        } catch (AccessDeniedException ignore) {
        }

        try (final Log4jInterceptor ignore = Log4jInterceptor.onError().deny(UserRolesManager.class).build()) {
            userRolesManagerTestUser.deleteUserRole(testUserRoleTemplate.getName());
            fail("Should not be allowed to delete a system userrole");
        } catch (AccessDeniedException ignore) {
        }

        // system user can change the system status of a system userrole
        testUserRole = userRolesManagerSystemUser.updateUserRole(testUserRoleTemplate);
        assertFalse(testUserRole.isSystem());

        // non-system userrole can be deleted by non-system user
        assertTrue(userRolesManagerTestUser.deleteUserRole(testUserRoleTemplate.getName()));
        assertNull(userRolesProvider.getRole(testUserRole.getName()));

        testUserRoleTemplate.setSystem(true);
        // only system user can add system role
        try (final Log4jInterceptor ignore = Log4jInterceptor.onError().deny(UserRolesManager.class).build()) {
            userRolesManagerTestUser.addUserRole(testUserRoleTemplate);
            fail ("Should not be allowed to add a system userrole as a non-system user");
        } catch (AccessDeniedException ignore) {
        }
        testUserRole = userRolesManagerSystemUser.addUserRole(testUserRoleTemplate);
        assertNotNull(testUserRole);
        assertTrue(testUserRole.isSystem());

        // system user can delete system userrole
        assertTrue(userRolesManagerSystemUser.deleteUserRole(testUserRoleTemplate.getName()));
        assertNull(userRolesProvider.getRole(testUserRole.getName()));

        try {
            userRolesManagerSystemUser.updateUserRole(testUserRoleTemplate);
            fail("Should not be possible to update a non-existing userrole");
        } catch (ItemNotFoundException ignore) {
        }

        // deleting a non-existing userrole should be allowed but return false
        assertFalse(userRolesManagerTestUser.deleteUserRole(testUserRole.getName()));

        // once its *client* session is logged out, a UserRoleManager will automatically be closed
        systemSession.logout();
        try {
            userRolesManagerSystemUser.addUserRole(testUserRoleTemplate);
            fail("Should not be possible to use a UserRoleManager after its *client* session has been logged out");
        } catch (RepositoryException e) {
            if (!e.getMessage().startsWith("This session has been closed.")) {
                throw e;
            }
        }
    }
}
