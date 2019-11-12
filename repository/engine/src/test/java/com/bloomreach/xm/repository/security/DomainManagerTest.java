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

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_USERROLES;
import static org.hippoecm.repository.api.HippoNodeType.NT_AUTHROLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.onehippo.repository.security.SecurityConstants.USERROLE_SECURITY_APPLICATION_ADMIN;
import static org.onehippo.repository.security.SecurityConstants.USERROLE_SECURITY_USER_ADMIN;

public class DomainManagerTest extends RepositoryTestCase {

    private final String[] defaultDomains = {
            "/hippo:configuration/hippo:domains/extranet", "hipposys:domain",
            "/hippo:configuration/hippo:domains/extranet/extranet-domain", "hipposys:domainrule",
            "/hippo:configuration/hippo:domains/extranet/extranet-domain/extranet-nodes", "hipposys:facetrule",
            "hipposys:equals", "true",
            "hipposys:facet", "jcr:path",
            "hipposys:type", "Reference",
            "hipposys:value", "/domaintest/extranet",
            "/hippo:configuration/hippo:domains/extranet/read", "hipposys:authrole",
            "hipposys:users", "testuser",
            "hipposys:groups", "dummy",
            "hipposys:role", "readonly"
    };

    private final String[] federatedDomains = {
            "/domaintest/intranet/intranet-domains", "hipposys:federateddomainfolder",
            "/domaintest/intranet/intranet-domains/intranet", "hipposys:domain",
            "/domaintest/intranet/intranet-domains/intranet/intranet-domain", "hipposys:domainrule",
            "/domaintest/intranet/intranet-domains/intranet/intranet-domain/intranet-nodes", "hipposys:facetrule",
            "hipposys:equals", "true",
            "hipposys:facet", "jcr:path",
            "hipposys:type", "Reference",
            // Federated domains always have the root the location of the parent of the 'federateddomainfolder'
            "hipposys:value", "/",
            "/domaintest/intranet/intranet-domains/intranet/read", "hipposys:authrole",
            "hipposys:users", "testuser",
            "hipposys:role", "readwrite"
    };

    private final String[] content = {
            "/domaintest", "nt:unstructured",
            "/domaintest/extranet", "nt:unstructured",
            "/domaintest/extranet/doc", "hippo:handle",
            "/domaintest/extranet/doc/doc", "hippo:document",
            "/domaintest/intranet", "nt:unstructured",
            "/domaintest/intranet/doc", "hippo:handle",
            "/domaintest/intranet/doc/doc", "hippo:document"
    };


    private final String[] testadminConfig = new String[]{
            "/hippo:configuration/hippo:users/testadmin", "hipposys:user",
            "hipposys:password", "testadmin",
            "hipposys:securityprovider", "internal",
            "hipposys:userroles", USERROLE_SECURITY_USER_ADMIN
    };

    private final String[] testuserConfig = new String[]{
            "/hippo:configuration/hippo:users/testuser", "hipposys:user",
            "hipposys:password", "testuser",
            "hipposys:securityprovider", "internal"
    };

    private final static Credentials TEST_ADMIN_CREDS = new SimpleCredentials("testadmin", "testadmin".toCharArray());
    private final static Credentials TEST_USER_CREDS = new SimpleCredentials("testuser", "testuser".toCharArray());

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        build(defaultDomains, session);
        build(content, session);
        build(federatedDomains, session);
        build(testadminConfig, session);
        build(testuserConfig, session);
        session.save();
        session.refresh(false);
    }

    @After
    @Override
    public void tearDown() throws Exception {
        session.getNode("/hippo:configuration/hippo:users/testadmin").remove();
        session.getNode("/hippo:configuration/hippo:users/testuser").remove();
        session.getNode("/hippo:configuration/hippo:domains/extranet").remove();
        session.getNode("/domaintest").remove();
        session.save();
        super.tearDown();
    }

    private DomainsManager getDomainsManager(final HippoSession user) throws RepositoryException {
        final RepositorySecurityManager securityManager = user.getWorkspace().getSecurityManager();
        return securityManager.getDomainsManager();
    }


    @Test
    public void assert_testuser_cannot_access_domain_manager() throws Exception {
        HippoSession testUser = null;
        try {
            testUser = (HippoSession) server.login(TEST_USER_CREDS);
            final Optional<HippoSession> wrapper = Optional.of(testUser);

            assertThatThrownBy(() -> getDomainsManager(wrapper.get()))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Access denied.");

        } finally {
            if (testUser != null) {
                testUser.logout();
            }
        }
    }

    @Test
    public void assert_read_xm_security_manager_role_domain_manager() throws Exception {

        HippoSession testAdmin = null;
        try {
            testAdmin = (HippoSession) server.login(TEST_ADMIN_CREDS);

            final Optional<Session> wrapper = Optional.of(testAdmin);

            final DomainsManager domainsManager = getDomainsManager(testAdmin);

            final SortedSet<DomainAuth> domainAuths = domainsManager.getDomainAuths();
            final Set<String> allDomainPathsByManager = domainAuths.stream().map(DomainAuth::getPath).collect(Collectors.toSet());

            NodeIterator allDomainNodes = session.getWorkspace().getQueryManager()
                    .createQuery("//element(*, hipposys:domain)", "xpath").execute().getNodes();

            final Set<String> allDomainPathsByJcr = new HashSet<>();

            new NodeIterable(allDomainNodes).forEach(domainNode -> {
                try {
                    allDomainPathsByJcr.add(domainNode.getPath());

                    if (!domainNode.getPath().equals("/hippo:configuration/hippo:domains/security-user-management")) {
                        assertFalse(String.format("testAdmin with userrole 'xm.security.viewer' not expected to have " +
                                "JCR read access on '%s'", domainNode.getPath()), wrapper.get().nodeExists(domainNode.getPath()));
                    }

                    DomainAuth domainAuth = domainsManager.getDomainAuth(domainNode.getPath());

                    assertNotNull(String.format("Expected domain for path '%s'", domainNode.getPath()), domainAuth);

                    final Map<String, AuthRole> authRulePathsByJcr = new TreeMap<>();
                    new NodeIterable(domainNode.getNodes()).forEach(child -> {
                        try {
                            if (child.isNodeType(NT_AUTHROLE)) {
                                authRulePathsByJcr.put(child.getName(), domainsManager.getAuthRole(child.getPath()));
                            }
                        } catch (RepositoryException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    assertEquals(authRulePathsByJcr, domainAuth.getAuthRolesMap());


                } catch (RepositoryException e) {
                    throw new RuntimeException(e);
                }
            });

            assertEquals("Expected that the domains manager would return all the domains", allDomainPathsByManager, allDomainPathsByJcr);

            final SortedSet<DomainAuth> domainAuthsForUser = domainsManager.getDomainAuthsForUser("testuser");

            assertThat(domainAuthsForUser)
                    .as("Expected 2 authroles for 'test user'")
                    .size().isEqualTo(2);

            Set<String> domainNamesForUser = domainAuthsForUser.stream().map(DomainAuth::getName).collect(Collectors.toSet());

            assertThat(domainNamesForUser)
                    .as("Both general domain 'extranet' and federated domain 'intranet' expected")
                    .containsExactly("intranet", "extranet");


            SortedSet<DomainAuth> domainAuthsForUserRole = domainsManager.getDomainAuthsForUserRole(USERROLE_SECURITY_USER_ADMIN);

            Set<String> domainNamesForUserRole = domainAuthsForUserRole.stream().map(DomainAuth::getName).collect(Collectors.toSet());

            assertThat(domainNamesForUserRole)
                    .as("Expected 'security-user-management' domain for role '%s' ", USERROLE_SECURITY_USER_ADMIN)
                    .containsExactly("security-user-management");

            SortedSet<DomainAuth> domainAuthsForGroup = domainsManager.getDomainAuthsForGroup("everybody");

            Set<String> domainNamesForGroup = domainAuthsForGroup.stream().map(DomainAuth::getName).collect(Collectors.toSet());

            assertThat(domainNamesForGroup)
                    .as("Expected 'versioning' and 'defaultread' domain for group 'everybody'", USERROLE_SECURITY_USER_ADMIN)
                    .containsExactly("versioning", "draft-document-holder-readwrite", "defaultread");

        } finally {
            if (testAdmin != null) {
                testAdmin.logout();
            }
        }
    }


    @Test
    public void assert_immutability_domain_manager_objects() throws Exception {
        HippoSession testAdmin = null;
        try {
            testAdmin = (HippoSession) server.login(TEST_ADMIN_CREDS);

            final DomainsManager domainsManager = getDomainsManager(testAdmin);


            SortedSet<String> users = domainsManager.getAuthRole("/hippo:configuration/hippo:domains/extranet/read").getUsers();

            assertThatThrownBy(() -> users.remove(users.first()))
                    .as("users should be immutable")
                    .isInstanceOf(UnsupportedOperationException.class);

            SortedSet<String> groups = domainsManager.getAuthRole("/hippo:configuration/hippo:domains/extranet/read").getGroups();

            assertThatThrownBy(() -> users.remove(groups.first()))
                    .as("users should be immutable")
                    .isInstanceOf(UnsupportedOperationException.class);

            final SortedSet<DomainAuth> testuserDomains = domainsManager.getDomainAuthsForUser("testuser");

            assertThatThrownBy(() -> testuserDomains.remove(testuserDomains.first()))
                    .as("user domains should be immutable")
                    .isInstanceOf(UnsupportedOperationException.class);

            final SortedSet<DomainAuth> everybodyDomains = domainsManager.getDomainAuthsForGroup("everybody");

            assertThatThrownBy(() -> everybodyDomains.remove(everybodyDomains.first()))
                    .as("group domains should be immutable")
                    .isInstanceOf(UnsupportedOperationException.class);


            final SortedSet<DomainAuth> securityUserManagerDomains = domainsManager.getDomainAuthsForUserRole(USERROLE_SECURITY_USER_ADMIN);

            assertThatThrownBy(() -> securityUserManagerDomains.remove(securityUserManagerDomains.first()))
                    .as("role domains should be immutable")
                    .isInstanceOf(UnsupportedOperationException.class);

            final SortedSet<DomainAuth> domainAuths = domainsManager.getDomainAuths();

            assertThatThrownBy(() -> domainAuths.remove(domainAuths.first()))
                    .as("domains should be immutable")
                    .isInstanceOf(UnsupportedOperationException.class);


        } finally {
            if (testAdmin != null) {
                testAdmin.logout();
            }
        }
    }

    @Test
    public void assert_crud_xm_security_manager_role_domain_manager_not_allowed() throws Exception {
        HippoSession testAdmin = null;
        try {
            testAdmin = (HippoSession) server.login(TEST_ADMIN_CREDS);
            final DomainsManager domainsManager = getDomainsManager(testAdmin);

            assertThatThrownBy(
                    () -> domainsManager.addAuthRole(new AuthRoleBean("readOnly", "/foo/bar", "dummy-role")))
                    .as("Role xm.security.viewer should not have enough karma to create auth roles")
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Access Denied.");


            assertThatThrownBy(
                    () -> domainsManager.updateAuthRole(new AuthRoleBean("readOnly", "/foo/bar", "dummy-role")))
                    .as("Role xm.security.viewer should not have enough karma to create auth roles")
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Access Denied.");


            assertThatThrownBy(
                    () -> domainsManager.deleteAuthRole(new AuthRoleBean("readOnly", "/foo/bar", "dummy-role")))
                    .as("Role xm.security.viewer should not have enough karma to create auth roles")
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Access Denied.");

        } finally {
            if (testAdmin != null) {
                testAdmin.logout();
            }
        }
    }

    @Test
    public void assert_delete_authrole_xm_security_application_manager_role_domain_manager() throws Exception {

        // increase karma
        session.getNode("/hippo:configuration/hippo:users/testadmin").setProperty(HIPPO_USERROLES,
                new String[]{USERROLE_SECURITY_APPLICATION_ADMIN});
        session.save();

        HippoSession testAdmin = null;
        try {
            testAdmin = (HippoSession) server.login(TEST_ADMIN_CREDS);
            final DomainsManager domainsManager = getDomainsManager(testAdmin);

            // for deleting an authrole, ONLY the 'path' needs to match (implementation choice)
            assertThat(domainsManager.deleteAuthRole(new AuthRoleBean("test", "/path/does/not/exist", "foo")))
                    .as("Non existing domain path should result in 'false'")
                    .isFalse();

            assertThat(domainsManager.deleteAuthRole(new AuthRoleBean("non-existing", "/hippo:configuration/hippo:domains/extranet", "foo")))
                    .as("Non existing auth role should result in 'false'")
                    .isFalse();

            assertThat(domainsManager.deleteAuthRole(new AuthRoleBean("read", "/hippo:configuration/hippo:domains/extranet", "foo")))
                    .as("Existing auth role name below existing domain should result in 'true' removed, even " +
                            "though the role does not match")
                    .isTrue();

            assertThat(domainsManager.deleteAuthRole(new AuthRoleBean("read", "/domaintest/intranet/intranet-domains/intranet", "foo")))
                    .as("Existing auth role name below existing domain should result in 'true' removed, even " +
                            "though the role does not match")
                    .isTrue();

            assertThat(session.nodeExists("/hippo:configuration/hippo:domains/extranet/read"))
                    .as("auth role expected to be removed, and no explicit save/persist is needed")
                    .isFalse();
            assertThat(session.nodeExists("/domaintest/intranet/intranet-domains/intranet/read"))
                    .as("auth role expected to be removed, and no explicit save/persist is needed")
                    .isFalse();

        } finally {
            if (testAdmin != null) {
                testAdmin.logout();
            }
        }

    }

    @Test
    public void assert_update_authrole_xm_security_application_manager_role_domain_manager() throws Exception {

        // increase karma
        session.getNode("/hippo:configuration/hippo:users/testadmin").setProperty(HIPPO_USERROLES,
                new String[]{USERROLE_SECURITY_APPLICATION_ADMIN});
        session.save();

        HippoSession testAdmin = null;
        try {
            testAdmin = (HippoSession) server.login(TEST_ADMIN_CREDS);
            final DomainsManager domainsManager = getDomainsManager(testAdmin);

            final AuthRole authRole = domainsManager.getAuthRole("/domaintest/intranet/intranet-domains/intranet/read");

            final SortedSet<String> before = authRole.getGroups();

            AuthRoleBean update = new AuthRoleBean(authRole);

            // via AuthRoleBean(authRole) the groups should have become mutable
            update.getGroups().add("newgroup");

            AuthRole updatedAuthRole = domainsManager.updateAuthRole(update);

            final SortedSet<String> after = authRole.getGroups();

            assertSame("An existing AuthRole instance does NOT get updated but needs to be refechted!", before, after);

            assertThat(updatedAuthRole.getGroups()).containsExactly("newgroup");

            AuthRole newInstance = domainsManager.getAuthRole("/domaintest/intranet/intranet-domains/intranet/read");

            SortedSet<String> afterNew = newInstance.getGroups();

            assertThat(afterNew).containsExactly("newgroup");

        } finally {
            if (testAdmin != null) {
                testAdmin.logout();
            }
        }
    }

    @Test
    public void assert_add_authrole_xm_security_application_manager_role_domain_manager() throws Exception {

        // increase karma
        session.getNode("/hippo:configuration/hippo:users/testadmin").setProperty(HIPPO_USERROLES,
                new String[]{USERROLE_SECURITY_APPLICATION_ADMIN});
        session.save();

        HippoSession testAdmin = null;
        try {
            testAdmin = (HippoSession) server.login(TEST_ADMIN_CREDS);
            final DomainsManager domainsManager = getDomainsManager(testAdmin);

            AuthRole authRole = domainsManager.addAuthRole(new AuthRoleBean("readnew", "/domaintest/intranet/intranet-domains/intranet", "readonly"));

            assertEquals("readnew", authRole.getName());
            assertEquals("/domaintest/intranet/intranet-domains/intranet", authRole.getDomainPath());

            // really persisted
            assertTrue(session.nodeExists("/domaintest/intranet/intranet-domains/intranet/readnew"));
        } finally {
            if (testAdmin != null) {
                testAdmin.logout();
            }
        }
    }

    @Test
    public void assert_invalid_authRole_authDomain() throws Exception {

        try {
            JcrUtils.copy(session, "/hippo:configuration/hippo:domains/extranet", "/extranet");
            JcrUtils.copy(session, "/hippo:configuration/hippo:domains/extranet/read", "/read");

            session.save();

            DomainsManager domainsManager = ((HippoSession) session).getWorkspace().getSecurityManager().getDomainsManager();

            assertThatThrownBy(() -> domainsManager.getDomainAuth("/extranet"))
                    .isInstanceOf(ItemNotFoundException.class)
                    .hasMessage("No valid hipposys:domain node found at /extranet");


            assertThatThrownBy(() -> domainsManager.getAuthRole("/read"))
                    .isInstanceOf(ItemNotFoundException.class)
                    .hasMessage("No valid hipposys:authrole node found at /read");

            // in getDomainAuth point to a hipposys:authrole
            assertThatThrownBy(() -> domainsManager.getDomainAuth("/hippo:configuration/hippo:domains/extranet/read"))
                    .isInstanceOf(ItemNotFoundException.class)
                    .hasMessage("No valid hipposys:domain node found at /hippo:configuration/hippo:domains/extranet/read");

        } finally {
            if (session.nodeExists("/extranet")) {
                session.getNode("/extranet").remove();
            }
            if (session.nodeExists("/read")) {
                session.getNode("/read").remove();
            }
            session.save();
        }

    }
}
