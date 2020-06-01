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
package org.hippoecm.hst.security.impl;

import java.security.Principal;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.security.Role;
import org.hippoecm.hst.security.TransientRole;
import org.hippoecm.hst.security.TransientUser;
import org.hippoecm.hst.security.User;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onehippo.repository.security.SecurityConstants;
import org.onehippo.repository.security.SessionUser;
import org.onehippo.repository.testutils.RepositoryTestCase;

import com.bloomreach.xm.repository.security.UserRoleBean;
import com.bloomreach.xm.repository.security.UserRolesManager;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PASSWORD;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_USERROLES;
import static org.hippoecm.repository.api.HippoNodeType.NT_USER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.onehippo.repository.security.SecurityConstants.USERROLE_DEFAULT_USER_SYSTEM_ADMIN;

public class TestRepositoryAuthenticationProvider extends RepositoryTestCase {

    private RepositoryAuthenticationProvider authenticationProvider;
    private Node testUserNode;

    @BeforeClass
    public static void setUpClass() throws Exception {
        //Enable legacy project structure mode (without extensions)
        System.setProperty("use.hcm.sites", "false");
        RepositoryTestCase.setUpClass();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        System.clearProperty("use.hcm.sites");
        RepositoryTestCase.tearDownClass();
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        Repository systemRepo = server.getRepository();
        Credentials systemCreds = new SimpleCredentials("admin", "admin".toCharArray());
        Repository userRepo = server.getRepository();
        
        authenticationProvider = new RepositoryAuthenticationProvider(systemRepo, systemCreds, userRepo);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        cleanupCustomUserRolesAndUser();
        super.tearDown();
    }

    private void setupCustomUserRolesAndUser() throws Exception {
        UserRolesManager userRolesManager = ((HippoSession)session).getWorkspace().getSecurityManager().getUserRolesManager();
        UserRoleBean userRoleBean;

        userRoleBean = new UserRoleBean("manager", null);
        userRolesManager.addUserRole(userRoleBean);

        userRoleBean.setName("SITEA_developer");
        userRolesManager.addUserRole(userRoleBean);

        userRoleBean.setName("SITEB_engineer");
        userRolesManager.addUserRole(userRoleBean);

        testUserNode = session.getNode(SecurityConstants.CONFIG_USERS_PATH).addNode("test", NT_USER);
        testUserNode.setProperty(HIPPO_PASSWORD, "test");
        testUserNode.setProperty(HIPPO_USERROLES, new String[]{"manager", "SITEA_developer", "SITEB_engineer"});
        session.save();
    }

    private void cleanupCustomUserRolesAndUser() throws Exception {
        UserRolesManager userRolesManager = ((HippoSession)session).getWorkspace().getSecurityManager().getUserRolesManager();

        userRolesManager.deleteUserRole("manager");
        userRolesManager.deleteUserRole("SITEA_developer");
        userRolesManager.deleteUserRole("SITEB_engineer");

        testUserNode = JcrUtils.getNodeIfExists(SecurityConstants.CONFIG_USERS_PATH + "/test", session);
        if (testUserNode != null) {
            testUserNode.remove();
            session.save();
        }
    }

    private Set<Role> getRolesByUser(final String username, final String password) throws SecurityException {
        TransientUser user = authenticationProvider.authenticate(username, password.toCharArray());
        assertEquals(username, user.getName());
        assertNotNull(user.getUserObject());
        assertTrue(user.getUserObject() instanceof SessionUser);
        return authenticationProvider.getRolesByUser(user);
    }

    @Test
    public void testRequiredUserRole() {
        authenticationProvider.setExcludedUserRolePrefixes(null);
        authenticationProvider.setIncludedUserRolePrefix(null);
        authenticationProvider.setDefaultRoleName(null);

        authenticationProvider.setRequiredUserRole(USERROLE_DEFAULT_USER_SYSTEM_ADMIN);
        try {
            getRolesByUser("admin", "admin");
        } catch (SecurityException e) {
            fail("admin user should have the required userrole "+ USERROLE_DEFAULT_USER_SYSTEM_ADMIN);
        }

        authenticationProvider.setRequiredUserRole("doesn't exist");
        try {
            getRolesByUser("admin", "admin");
            fail("admin user should be allowed to authenticate (should not have userrole 'doesn't exist')");
        } catch (SecurityException ignore) {
        }
    }

    @Test
    public void testGetRolesByUser() {
        authenticationProvider.setExcludedUserRolePrefixes(null);
        authenticationProvider.setIncludedUserRolePrefix(null);
        authenticationProvider.setDefaultRoleName(null);

        Set<Role> rolesByUser = getRolesByUser("admin", "admin");
        assertFalse(rolesByUser.isEmpty());
        assertEquals(rolesByUser, authenticationProvider.getRolesByUser((User) () -> "admin"));
        assertEquals(rolesByUser, authenticationProvider.getRolesByUsername("admin"));
    }

    @Test
    public void testDefaultSetup() {
        authenticationProvider.setExcludedUserRolePrefixes("xm.");
        authenticationProvider.setIncludedUserRolePrefix("xm.");
        authenticationProvider.setDefaultRoleName(null);

        Set<Role> roleSet = getRolesByUser("admin", "admin");
        assertTrue(roleSet.isEmpty());

        authenticationProvider.setExcludedUserRolePrefixes(null);
        roleSet = getRolesByUser("admin", "admin");
        assertFalse(roleSet.isEmpty());

        authenticationProvider.setRolePrefix("ROLE_");
        roleSet = getRolesByUser("admin", "admin");
        assertTrue(roleSet.stream().allMatch(r->r.getName().startsWith("ROLE_")));

        int roles = roleSet.size();

        authenticationProvider.setDefaultRoleName("everybody");
        roleSet = getRolesByUser("admin", "admin");
        assertEquals(roleSet.size(), roles + 1);
        assertTrue(roleSet.contains(new TransientRole("everybody")));
    }

    private Set<String> roleNames(final Set<Role> roles) {
        return roles.stream().map(Principal::getName).collect(Collectors.toSet());
    }

    private boolean equals(final Set<String> roles, final String... roleNames) {
        return roles.size() == roleNames.length && roles.containsAll(Arrays.asList(roleNames));
    }

    @Test
    public void testCustomRoles() throws Exception {
        setupCustomUserRolesAndUser();

        SessionUser testSessionUser = (SessionUser)authenticationProvider.authenticate("test", "test".toCharArray()).getUserObject();
        authenticationProvider.setExcludedUserRolePrefixes("xm.");
        authenticationProvider.setIncludedUserRolePrefix(null);
        authenticationProvider.setStripIncludedUserRolePrefix(false);
        authenticationProvider.setDefaultRoleName(null);

        Set<String> roleNames;
        roleNames = roleNames(authenticationProvider.getRolesByUser(testSessionUser));
        assertTrue(equals(roleNames, "manager", "SITEA_developer", "SITEB_engineer" ));

        authenticationProvider.setExcludedUserRolePrefixes("xm.,SITEB_");
        roleNames = roleNames(authenticationProvider.getRolesByUser(testSessionUser));
        assertTrue(equals(roleNames, "manager", "SITEA_developer" ));

        authenticationProvider.setIncludedUserRolePrefix("SITEA_");
        roleNames = roleNames(authenticationProvider.getRolesByUser(testSessionUser));
        assertTrue(equals(roleNames, "SITEA_developer" ));

        authenticationProvider.setStripIncludedUserRolePrefix(true);
        roleNames = roleNames(authenticationProvider.getRolesByUser(testSessionUser));
        assertTrue(equals(roleNames, "developer" ));

        authenticationProvider.setExcludedUserRolePrefixes(null);
        roleNames = roleNames(authenticationProvider.getRolesByUser(testSessionUser));
        assertTrue(equals(roleNames, "developer" ));
    }
}
