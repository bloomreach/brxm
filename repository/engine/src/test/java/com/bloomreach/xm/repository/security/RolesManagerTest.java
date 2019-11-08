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

public class RolesManagerTest extends RepositoryTestCase {

    private static final String TEST_USER_ID = "testuser";
    private static final String TEST_ROLE_NAME = "testrole";

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
    public void testRolesManager() throws RepositoryException {
        testUserNode.setProperty(HIPPO_USERROLES, new String[]{USERROLE_SECURITY_APPLICATION_ADMIN});
        session.save();
        testUserSession = impersonate(TEST_USER_ID);
        createSystemSession();
        RolesProvider rolesProvider = testUserSession.getWorkspace().getSecurityManager().getRolesProvider();
        RolesManager rolesManagerSystemUser = systemSession.getWorkspace().getSecurityManager().getRolesManager();
        RolesManager rolesManagerTestUser = testUserSession.getWorkspace().getSecurityManager().getRolesManager();

        Role testRole = rolesProvider.getRole(TEST_ROLE_NAME);
        assertNull(testRole);

        try {
            rolesManagerTestUser.addRole(null);
            fail ("Should not be possible to add a role with no arguments");
        } catch (IllegalArgumentException ignore) {
        }

        RoleBean testRoleTemplate = new RoleBean();
        assertNull(testRoleTemplate.getName());
        try {
            rolesManagerTestUser.addRole(testRoleTemplate);
            fail ("Should not be possible to add a role without a name");
        } catch (IllegalArgumentException ignore) {
        }

        testRoleTemplate.setName("");
        try {
            rolesManagerTestUser.addRole(testRoleTemplate);
            fail ("Should not be possible to add a role with an empty name");
        } catch (IllegalArgumentException ignore) {
        }

        testRoleTemplate.setName(" ");
        try {
            rolesManagerTestUser.addRole(testRoleTemplate);
            fail ("Should not be possible to add a role with a blank name");
        } catch (IllegalArgumentException ignore) {
        }

        // test basic add role
        testRoleTemplate.setName(TEST_ROLE_NAME);
        testRole = rolesManagerTestUser.addRole(testRoleTemplate);
        assertNotNull(testRole);
        assertEquals(TEST_ROLE_NAME, testRole.getName());
        assertNull(testRole.getDescription());
        assertFalse(testRole.isSystem());
        assertEquals(0, testRole.getRoles().size());

        // test basic update role
        testRoleTemplate.setDescription("foo");
        testRoleTemplate.getRoles().add("bar");
        testRoleTemplate.getPrivileges().add("baz");
        testRole = rolesManagerTestUser.updateRole(testRoleTemplate);
        assertNotSame(testRoleTemplate, testRole);
        assertNotNull(testRole);
        assertEquals("foo", testRole.getDescription());
        assertEquals(1, testRole.getRoles().size());
        assertTrue(testRole.getRoles().contains("bar"));
        assertEquals(1, testRole.getPrivileges().size());
        assertTrue(testRole.getPrivileges().contains("baz"));

        try {
            rolesManagerTestUser.addRole(testRoleTemplate);
            fail("Should not be possible to add (or update) an already existing role");
        } catch (ItemExistsException ignore) {
        }

        try (final Log4jInterceptor ignore = Log4jInterceptor.onError().deny(RolesManager.class).build()) {
            testRoleTemplate.setSystem(true);
            rolesManagerTestUser.updateRole(testRoleTemplate);
            fail("Should not be allowed to change a role to system status");
        } catch (AccessDeniedException ignore) {
        }

        // system session can set/update role system status
        testRole = rolesManagerSystemUser.updateRole(testRoleTemplate);
        assertTrue(testRole.isSystem());

        // system role may not be modified by non-system user
        testRoleTemplate = new RoleBean(testRole);
        try (final Log4jInterceptor ignore = Log4jInterceptor.onError().deny(RolesManager.class).build()) {
            rolesManagerTestUser.updateRole(testRoleTemplate);
            fail("Should not be allowed to change description of a system role");
        } catch (AccessDeniedException ignore) {
        }

        // system user however can update a system role
        testRoleTemplate.setDescription(null);
        testRoleTemplate.getRoles().clear();
        testRole = rolesManagerSystemUser.updateRole(testRoleTemplate);
        assertNull(testRole.getDescription());
        assertEquals(0, testRole.getRoles().size());

        try (final Log4jInterceptor ignore = Log4jInterceptor.onError().deny(RolesManager.class).build()) {
            testRoleTemplate.setSystem(false);
            rolesManagerTestUser.updateRole(testRoleTemplate);
            fail("Should not be allowed to change the system status of a system role");
        } catch (AccessDeniedException ignore) {
        }

        try (final Log4jInterceptor ignore = Log4jInterceptor.onError().deny(RolesManager.class).build()) {
            rolesManagerTestUser.deleteRole(testRoleTemplate.getName());
            fail("Should not be allowed to delete a system role");
        } catch (AccessDeniedException ignore) {
        }

        // system user can change the system status of a system role
        testRole = rolesManagerSystemUser.updateRole(testRoleTemplate);
        assertFalse(testRole.isSystem());

        // non-system role can be deleted by non-system user
        assertTrue(rolesManagerTestUser.deleteRole(testRoleTemplate.getName()));
        assertNull(rolesProvider.getRole(testRole.getName()));

        // only system user can add system role
        testRoleTemplate.setSystem(true);
        try (final Log4jInterceptor ignore = Log4jInterceptor.onError().deny(RolesManager.class).build()) {
            rolesManagerTestUser.addRole(testRoleTemplate);
            fail ("Should not be allowed to add a system role as a non-system user");
        } catch (AccessDeniedException ignore) {
        }
        testRole = rolesManagerSystemUser.addRole(testRoleTemplate);
        assertNotNull(testRole);
        assertTrue(testRole.isSystem());

        // system user can delete system role
        assertTrue(rolesManagerSystemUser.deleteRole(testRoleTemplate.getName()));
        assertNull(rolesProvider.getRole(testRole.getName()));

        try {
            rolesManagerSystemUser.updateRole(testRoleTemplate);
            fail("Should not be possible to update a non-existing role");
        } catch (ItemNotFoundException ignore) {
        }

        // deleting a non-existing role should be allowed but return false
        assertFalse(rolesManagerTestUser.deleteRole(testRole.getName()));

        // once its *client* session is logged out, a UserRoleManager will automatically be closed
        systemSession.logout();
        try {
            rolesManagerSystemUser.addRole(testRoleTemplate);
            fail("Should not be possible to use a UserRoleManager after its *client* session has been logged out");
        } catch (RepositoryException e) {
            if (!e.getMessage().startsWith("This session has been closed.")) {
                throw e;
            }
        }
    }
}
