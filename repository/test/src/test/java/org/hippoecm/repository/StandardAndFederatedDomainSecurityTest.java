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
package org.hippoecm.repository;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.repository.security.SecurityManager;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.onehippo.repository.security.StandardPermissionNames.JCR_READ;
import static org.onehippo.repository.security.StandardPermissionNames.JCR_WRITE;

public class StandardAndFederatedDomainSecurityTest extends RepositoryTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();

        removeDefaultReadForTestAndDescendants();

        final String[] users = {
                "/hippo:configuration/hippo:users/intranetuser", "hipposys:user",
                "hipposys:password", "password",
                "/hippo:configuration/hippo:users/extranetuser", "hipposys:user",
                "hipposys:password", "password",
                "/hippo:configuration/hippo:users/superuser", "hipposys:user",
                "hipposys:password", "password"
        };

        final String[] defaultDomains = {
                "/hippo:configuration/hippo:domains/extranet", "hipposys:domain",
                "/hippo:configuration/hippo:domains/extranet/extranet-domain", "hipposys:domainrule",
                "/hippo:configuration/hippo:domains/extranet/extranet-domain/extranet-nodes", "hipposys:facetrule",
                "hipposys:equals", "true",
                "hipposys:facet", "jcr:path",
                "hipposys:type", "Reference",
                "hipposys:value", "/test/extranet",
                "/hippo:configuration/hippo:domains/extranet/read", "hipposys:authrole",
                "hipposys:users", "extranetuser",
                "hipposys:users", "superuser",
                "hipposys:role", "readonly"
        };

        final String[] federatedDomains = {
                "/test/intranet/intranet-domains", "hipposys:federateddomainfolder",
                "/test/intranet/intranet-domains/intranet", "hipposys:domain",
                "/test/intranet/intranet-domains/intranet/intranet-domain", "hipposys:domainrule",
                "/test/intranet/intranet-domains/intranet/intranet-domain/intranet-nodes", "hipposys:facetrule",
                "hipposys:equals", "true",
                "hipposys:facet", "jcr:path",
                "hipposys:type", "Reference",
                // Federated domains always have the root the location of the parent of the 'federateddomainfolder'
                "hipposys:value", "/",
                "/test/intranet/intranet-domains/intranet/read", "hipposys:authrole",
                "hipposys:users", "intranetuser",
                "hipposys:users", "superuser",
                "hipposys:role", "readwrite"
        };

        final String[] content = {
                "/test", "nt:unstructured",
                "/test/extranet", "nt:unstructured",
                "/test/extranet/doc", "hippo:handle",
                "/test/extranet/doc/doc", "hippo:document",
                "/test/intranet", "nt:unstructured",
                "/test/intranet/doc", "hippo:handle",
                "/test/intranet/doc/doc", "hippo:document"
        };

        build(users, session);
        build(content, session);
        build(defaultDomains, session);
        build(federatedDomains, session);

        session.save();

    }

    @After
    public void tearDown() throws Exception {

        final String[] removes = new String[]{
                "/hippo:configuration/hippo:domains/extranet",
                "/hippo:configuration/hippo:users/intranetuser",
                "/hippo:configuration/hippo:users/extranetuser",
                "/hippo:configuration/hippo:users/superuser"
        };

        for (String remove : removes) {
            if (session.nodeExists(remove)) {
                session.getNode(remove).remove();
            }
        }

        restoreDefaultReadForTestAndDescendants();

        session.save();

        super.tearDown();
    }

    @Test
    public void standard_domains_only_supported_below_hippo_configuration() throws Exception {
        session.getWorkspace().copy("/hippo:configuration/hippo:domains", "/test/standard-domains");

        Session intranetuser = null;

        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(SecurityManager.class).build()) {
            intranetuser = server.login(new SimpleCredentials("intranetuser", "password".toCharArray()));

            assertThat(interceptor.messages())
                    .as("Expected specific WARN message about skipping not-standard domain folder location")
                    .containsOnlyOnce(("Skipped all domains found in not-standard domain folder location(s): [/test/standard-domains]"));

        } finally {
            session.getNode("/test/standard-domains").remove();
            session.save();
            if (intranetuser != null) {
                intranetuser.logout();
            }
        }
    }

    @Test
    public void federated_domains_not_supported_below_jcr_root() throws Exception {
        session.move("/test/intranet/intranet-domains", "/intranet-domains");
        session.save();

        Session intranetuser = null;

        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(SecurityManager.class).build()) {
            intranetuser = server.login(new SimpleCredentials("intranetuser", "password".toCharArray()));

            assertThat(interceptor.messages())
                    .as("Expected specific WARN message about skipping not-supported federated domain folder location")
                    .containsOnlyOnce(("Skipped all domains found in not-supported federated domain folder location(s): [/intranet-domains]"));

        } finally {
            session.move("/intranet-domains", "/test/intranet/intranet-domains");
            session.save();
            if (intranetuser != null) {
                intranetuser.logout();
            }
        }
    }

    @Test
    public void federated_domains_is_allowed_below_direct_child_of_root() throws Exception {
        session.move("/test/intranet/intranet-domains", "/test/intranet-domains");
        session.save();

        Session intranetuser = null;

        server.login(new SimpleCredentials("intranetuser", "password".toCharArray()));
        try {
            intranetuser = server.login(new SimpleCredentials("intranetuser", "password".toCharArray()));

            // intranetuser now expected to have readwrite for everything below /test since its jcr:path = '/' relative
            // now to '/test'
            assertTrue(intranetuser.hasPermission("/test/extranet", JCR_READ));
            assertTrue(intranetuser.hasPermission("/test/extranet", JCR_WRITE));
            assertTrue(intranetuser.hasPermission("/test/intranet", JCR_WRITE));
            assertTrue(intranetuser.hasPermission("/test/intranet", JCR_WRITE));

        } finally {
            session.move("/test/intranet-domains", "/test/intranet/intranet-domains");
            session.save();
            if (intranetuser != null) {
                intranetuser.logout();
            }
        }
    }

    @Test
    public void federated_domains_not_supported_below_hippo_configuration() throws Exception {
        session.move("/hippo:configuration/hippo:domains", "/test/standard-domains");
        session.move("/test/intranet/intranet-domains", "/hippo:configuration/hippo:domains");
        session.save();

        Session intranetuser = null;

        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(SecurityManager.class).build()) {
            intranetuser = server.login(new SimpleCredentials("intranetuser", "password".toCharArray()));

            assertThat(interceptor.messages())
                    .as("Expected specific WARN message about skipping not-supported federated domain folder location")
                    .containsOnlyOnce(("Skipped all domains found in not-supported federated domain folder location(s): [/hippo:configuration/hippo:domains]"));

        } finally {
            session.move("/hippo:configuration/hippo:domains", "/test/intranet/intranet-domains");
            session.move("/test/standard-domains", "/hippo:configuration/hippo:domains");
            session.save();
            if (intranetuser != null) {
                intranetuser.logout();
            }
        }
    }

    @Test
    public void federated_domain_for_intranet_user() throws Exception {
        final Session extranetuser = server.login(new SimpleCredentials("extranetuser", "password".toCharArray()));
        final Session intranetuser = server.login(new SimpleCredentials("intranetuser", "password".toCharArray()));

        try {
            assertTrue(extranetuser.hasPermission("/test/extranet", JCR_READ));
            assertTrue(extranetuser.hasPermission("/test/extranet/doc", JCR_READ));
            assertTrue(extranetuser.hasPermission("/test/extranet/doc/doc", JCR_READ));
            assertFalse(extranetuser.hasPermission("/test/extranet", JCR_WRITE));
            assertFalse(extranetuser.hasPermission("/test/extranet/doc", JCR_WRITE));

            assertFalse(extranetuser.hasPermission("/test/intranet", JCR_READ));

            // intranet user has federated domain on /test/intranet
            assertTrue(intranetuser.hasPermission("/test/intranet", JCR_READ));
            assertTrue(intranetuser.hasPermission("/test/intranet/doc", JCR_READ));
            assertTrue(intranetuser.hasPermission("/test/intranet/doc/doc", JCR_READ));
            assertTrue(intranetuser.hasPermission("/test/intranet", JCR_WRITE));

            assertFalse(intranetuser.hasPermission("/test/extranet", JCR_READ));

        } finally {
            extranetuser.logout();
            intranetuser.logout();
        }
    }

    @Test
    public void federated_domain_for_intranet_user_pointing_to_deeper_descendant_results_is_allowed_and_has_implicit_ancestor_read() throws Exception {
        session.getNode("/test/intranet/intranet-domains/intranet/intranet-domain/intranet-nodes")
                .setProperty("hipposys:value", "doc/doc");
        session.save();
        final Session intranetuser = server.login(new SimpleCredentials("intranetuser", "password".toCharArray()));

        try {
            // intranet user has federated domain on /test/intranet
            assertTrue(intranetuser.hasPermission("/test/intranet", JCR_READ));
            assertTrue(intranetuser.hasPermission("/test/intranet/doc", JCR_READ));
            assertTrue(intranetuser.hasPermission("/test/intranet/doc/doc", JCR_READ));

            assertFalse(intranetuser.hasPermission("/test/intranet", JCR_WRITE));
            assertFalse(intranetuser.hasPermission("/test/intranet/doc", JCR_WRITE));

            assertTrue(intranetuser.hasPermission("/test/intranet/doc/doc", JCR_WRITE));

        } finally {
            intranetuser.logout();
        }
    }

    @Test
    public void federated_domain_facet_rule_jcr_path_is_allowed_to_start_with_slash_resulting_in_same_as_without_slash() throws Exception {
        final String[] jcrPathValuesToTest = new String[]{
                "/",
                "",
                "/doc/doc",
                "doc/doc"
        };

        for (String pathToTest : jcrPathValuesToTest) {
            session.getNode("/test/intranet/intranet-domains/intranet/intranet-domain/intranet-nodes")
                    .setProperty("hipposys:value", pathToTest);
            session.save();
            final Session intranetuser = server.login(new SimpleCredentials("intranetuser", "password".toCharArray()));

            try {
                assertTrue(intranetuser.hasPermission("/test/intranet/doc/doc", JCR_WRITE));

            } finally {
                intranetuser.logout();
            }
        }
    }

    @Test
    public void federated_domain_facet_rules_excluding_nodes_each_other() throws Exception {

        JcrUtils.copy(session,
                "/test/intranet/intranet-domains/intranet/intranet-domain/intranet-nodes",
                "/test/intranet/intranet-domains/intranet/intranet-domain/other-nodes");

        session.getNode("/test/intranet/intranet-domains/intranet/intranet-domain/other-nodes")
                .setProperty("hipposys:value", "/test/extranet/doc/doc");
        session.save();

        Session intranetuser = server.login(new SimpleCredentials("intranetuser", "password".toCharArray()));

        assertFalse("facet rule /test/extranet/doc/doc should exclude the access in intranet",
                intranetuser.hasPermission("/test/intranet", JCR_READ));

        intranetuser.logout();

        session.getNode("/test/intranet/intranet-domains/intranet/intranet-domain/other-nodes")
                .setProperty("hipposys:value", "/test/non/existing");
        session.save();

        intranetuser = server.login(new SimpleCredentials("intranetuser", "password".toCharArray()));

        assertFalse("facet rule /test/non/existing should exclude the access in intranet",
                intranetuser.hasPermission("/test/intranet", JCR_READ));

        intranetuser.logout();

        session.getNode("/test/intranet/intranet-domains/intranet/intranet-domain/other-nodes")
                .setProperty("hipposys:equals", false);
        session.save();

        intranetuser = server.login(new SimpleCredentials("intranetuser", "password".toCharArray()));

        assertTrue("facet rule /test/non/existing with 'equals = false' should NOT exclude the access in intranet",
                intranetuser.hasPermission("/test/intranet", JCR_WRITE));

        intranetuser.logout();

    }


    @Test
    public void federated_domains_referencing_non_existing_path() throws Exception {
        session.getNode("/test/intranet/intranet-domains/intranet/intranet-domain/intranet-nodes")
                .setProperty("hipposys:value", "/test/non/existing");
        session.save();

        Session intranetuser = null;

        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(SecurityManager.class).build()) {
            intranetuser = server.login(new SimpleCredentials("intranetuser", "password".toCharArray()));

            assertFalse(intranetuser.hasPermission("/test/extranet", JCR_READ));

            assertThat(interceptor.getEvents().size())
                    .as("Expected no error logs for non-existing jcr:path Reference facet rule")
                    .isEqualTo(0);

        } finally {
            if (intranetuser != null) {
                intranetuser.logout();
            }
        }
    }

    @Test
    public void federated_domain_with_non_existing_reference_which_gets_created_becomes_visible_for_logged_in_user() throws Exception {

        {
            final Session intranetuser = server.login(new SimpleCredentials("intranetuser", "password".toCharArray()));
            try {
                assertTrue(intranetuser.hasPermission("/test/intranet/doc/doc", JCR_WRITE));
            } finally {
                intranetuser.logout();
            }
        }

        session.getNode("/test/intranet/intranet-domains/intranet/intranet-domain/intranet-nodes")
                .setProperty("hipposys:value", "tobecreated");
        session.save();

        {
            final Session intranetuser = server.login(new SimpleCredentials("intranetuser", "password".toCharArray()));
            try {
                assertFalse(intranetuser.hasPermission("/test/intranet", JCR_READ));

                session.getNode("/test/intranet").addNode("tobecreated", "hippo:handle");
                session.save();

                assertTrue(intranetuser.hasPermission("/test/intranet", JCR_READ));
                assertTrue(intranetuser.hasPermission("/test/intranet/tobecreated", JCR_WRITE));

            } finally {
                if (intranetuser != null) {
                    intranetuser.logout();
                }
            }
        }
    }

    @Test
    public void federated_domain_with_reference_which_gets_updated() throws Exception {
        session.getNode("/test/intranet/intranet-domains/intranet/intranet-domain/intranet-nodes")
                .setProperty("hipposys:value", "non/existing");
        session.save();


        final Session intranetuser = server.login(new SimpleCredentials("intranetuser", "password".toCharArray()));
        try {
            assertFalse(intranetuser.hasPermission("/test/intranet", JCR_READ));

            session.getNode("/test/intranet/intranet-domains/intranet/intranet-domain/intranet-nodes")
                    .setProperty("hipposys:value", "doc/doc");
            session.save();

            assertTrue(intranetuser.hasPermission("/test/intranet", JCR_READ));
            assertTrue(intranetuser.hasPermission("/test/intranet/doc/doc", JCR_WRITE));


            session.getNode("/test/intranet/intranet-domains/intranet/intranet-domain/intranet-nodes")
                    .setProperty("hipposys:value", "non/existing");
            session.save();

            assertFalse(intranetuser.hasPermission("/test/intranet", JCR_READ));
        } finally {
            if (intranetuser != null) {
                intranetuser.logout();
            }
        }

    }

    // we bootstrapped domains '/test/intranet/intranet-domains/intranet' and '/hippo:configuration/hippo:domains/extranet'

    // If we now rename '/test/intranet/intranet-domains/intranet' to '/test/intranet/intranet-domains/extranet
    // we have two domains having the same name 'extranet'. Below /hippo:configuration/hippo:domains this
    // is prohibited by not allowing same name siblings however with federated domains, this is possible and
    // code should work correctly, for example not storing all domains in a HashSet by node name
    //
    // superuser has both the intranet and extranet domain!
    @Test
    public void duplicate_domain_names_as_a_result_of_federated_domains_dont_result_in_problems() throws Exception {

        superUserAssertions();

        session.move("/test/intranet/intranet-domains/intranet",
                "/test/intranet/intranet-domains/extranet");
        session.save();

        superUserAssertions();
    }

    private void superUserAssertions() throws RepositoryException {
        final Session superuser = server.login(new SimpleCredentials("superuser", "password".toCharArray()));
        try {

            assertTrue(superuser.hasPermission("/test/extranet", JCR_READ));
            assertTrue(superuser.hasPermission("/test/extranet/doc/doc", JCR_READ));
            assertTrue(superuser.hasPermission("/test/intranet", JCR_READ));
            assertTrue(superuser.hasPermission("/test/intranet/doc/doc", JCR_WRITE));

        } finally {
            if (superuser != null) {
                superuser.logout();
            }
        }
    }

}
