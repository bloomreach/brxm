/*
 *  Copyright 2014-2019 Hippo B.V. (http://www.onehippo.com)
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

import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.assertj.core.api.Assertions;
import org.hippoecm.repository.security.HippoAccessManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MissingReferenceAuthorizationTest extends RepositoryTestCase {

    private Node testDomain;

    private Node domainTestNode;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        removeDefaultReadForTestAndDescendants();

        // create content
        final Node root = session.getRootNode();
        final Node test = root.addNode("test");
        final Node folder = test.addNode("folder", "hippostd:folder");
        folder.setProperty("hippostd:foldertype", new String[]{"foo", "bar"});
        final Node authDocument = folder.addNode("authDocument", "hippo:authtestdocument");
        authDocument.setProperty("authDocumentProp", "foo");
        final Node compound = authDocument.addNode("compound", "hippo:authtestdocument");
        compound.setProperty("compoundProp", "bar");
        final Node testDocument = folder.addNode("testDocument", "hippo:testdocument");
        testDocument.setProperty("testDocumentProp", "lux");

        // create test user
        final Node users = session.getNode("/hippo:configuration/hippo:users");
        final Node user = users.addNode("testUser", "hipposys:user");
        user.setProperty("hipposys:password", "password");

        // create test domain
        final Node domains = session.getNode("/hippo:configuration/hippo:domains");
        testDomain = domains.addNode("testDomain", "hipposys:domain");

        // test user is admin in test domain
        final Node authRole = testDomain.addNode("authRole", "hipposys:authrole");
        authRole.setProperty("hipposys:users", new String[]{ "testUser" });
        authRole.setProperty("hipposys:role", "admin");

        // give read access to only /test and no descendants
        domainTestNode = addDefaultReadForTestNode();

        session.save();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        removeNode("/hippo:configuration/hippo:users/testUser");
        removeNode("/hippo:configuration/hippo:domains/testDomain");
        restoreDefaultReadForTestAndDescendants();
        domainTestNode.remove();
        session.save();
        super.tearDown();
    }

    @Test(expected = AccessControlException.class)
    public void missing_reference_in_facet_rule_domain_rule_does_not_give_implicit_read_access_to_ancestors() throws Exception {

        // domain with single facet rule that has equals = true & non existing reference
        // should result in no read access below /test/folder

        final Node domainRule = testDomain.addNode("domainrule", "hipposys:domainrule");
        createFacetRule(domainRule, "facetRule", true, "jcr:path", "Reference", "/test/folder/non/existing");
        session.save();

        Session testSession = null;
        try {
            testSession = loginTestUser();
            assertTrue(testSession.nodeExists("/test"));
            assertFalse(testSession.nodeExists("/test/folder"));
            testSession.checkPermission("/test/folder", "jcr:read");
        } finally {
            if (testSession != null) {
                testSession.logout();
            }
        }
    }

    @Ignore("See REPO-2212 : It won't be allowed to have 'false' as a single facet " +
            "rule in a domain for a path reference facet rule : In such cases, the entire domain will be dropped")
    @Test(expected = AccessControlException.class)
    public void one_false_missing_reference_in_facet_rule_domain_rule() throws Exception {

        // domain with single facet rule that has equals = false & non existing reference
        // should result in no read access below /test/folder

        final Node domainRule = testDomain.addNode("domainRule", "hipposys:domainrule");
        createFacetRule(domainRule, "facetRule", false, "jcr:path", "Reference", "/test/folder/non/existing");
        session.save();

        Session testSession = null;
        try {
            testSession = loginTestUser();
            assertTrue(testSession.nodeExists("/test"));
            assertFalse(testSession.nodeExists("/test/folder"));
            testSession.checkPermission("/test/folder", "jcr:read");
        } finally {
            if (testSession != null) {
                testSession.logout();
            }
        }
    }

    @Test(expected = AccessControlException.class)
    public void combined_true_missing_reference_in_facet_rules_domain_rule() throws Exception {

        // domain with two AND-ed facet rules : both have equals = true and one contains
        // non existing reference. Result should be no read access below /test/folder

        final Node domainRule = testDomain.addNode("domainRule", "hipposys:domainrule");
        createFacetRule(domainRule, "facetRule1", true, "jcr:path", "Reference", "/test/folder");
        createFacetRule(domainRule, "facetRule2", true, "jcr:path", "Reference", "/test/folder/non/existing");
        session.save();

        Session testSession = null;
        try {
            testSession = loginTestUser();
            assertTrue(testSession.nodeExists("/test"));
            assertFalse(testSession.nodeExists("/test/folder"));
            testSession.checkPermission("/test/folder", "jcr:read");
        } finally {
            if (testSession != null) {
                testSession.logout();
            }
        }
    }

    @Test
    public void combined_false_missing_reference_in_facet_rules_domain_rule() throws Exception {

        // domain with two AND-ed facet rules : one rule matches everything below /test/folder
        // and other one has a non-existing reference but has equals = false. Result should be that everything below /test/folder
        // is readable

        final Node domainRule = testDomain.addNode("domainRule", "hipposys:domainrule");
        createFacetRule(domainRule, "facetRule1", true, "jcr:path", "Reference", "/test/folder");
        createFacetRule(domainRule, "facetRule2", false, "jcr:path", "Reference", "/test/folder/non/existing");
        session.save();

        Session testSession = null;
        try {
            testSession = loginTestUser();

            assertTrue(testSession.nodeExists("/test"));
            assertTrue(testSession.nodeExists("/test/folder"));
            assertTrue(testSession.nodeExists("/test/folder/authDocument"));
            assertTrue(testSession.nodeExists("/test/folder/authDocument/compound"));
            assertTrue(testSession.nodeExists("/test/folder/testDocument"));

            testSession.checkPermission("/test/folder", "jcr:read");
            testSession.checkPermission("/test/folder", "jcr:write");

            testSession.checkPermission("/test/folder/authDocument", "jcr:read");
            testSession.checkPermission("/test/folder/authDocument", "jcr:write");

            testSession.checkPermission("/test/folder/authDocument/compound", "jcr:read");
            testSession.checkPermission("/test/folder/authDocument/compound", "jcr:write");

            testSession.checkPermission("/test/folder/testDocument", "jcr:read");
            testSession.checkPermission("/test/folder/testDocument", "jcr:write");

        } finally {
            if (testSession != null) {
                testSession.logout();
            }
        }
    }

    @Test(expected = AccessControlException.class)
    public void only_true_missing_references_in_facet_rules_domain_rule() throws Exception {

        // domain with two AND-ed facet rules : both have equals = true and one contains
        // non existing reference. Result should be no read access below /test/folder

        final Node domainRule = testDomain.addNode("domainRule", "hipposys:domainrule");
        createFacetRule(domainRule, "facetRule1", true, "jcr:path", "Reference", "/test/folder");
        createFacetRule(domainRule, "facetRule2", true, "jcr:path", "Reference", "/test/folder/non/existing/two");
        session.save();

        Session testSession = null;
        try {
            testSession = loginTestUser();
            assertTrue(testSession.nodeExists("/test"));
            assertFalse(testSession.nodeExists("/test/folder"));
            testSession.checkPermission("/test/folder", "jcr:read");
        } finally {
            if (testSession != null) {
                testSession.logout();
            }
        }
    }


    @Ignore("See REPO-2212 : It won't be allowed to have only equals =  'false' facet " +
            "rules in a domain for a path references : In such cases, the entire domain will be dropped")
    @Test(expected = AccessControlException.class)
    public void only_false_missing_references_in_facet_rules_domain_rule() throws Exception {

        // domain with two AND-ed facet rules : both have equals = true and one contains
        // non existing reference. Result should be no read access below /test/folder

        final Node domainRule = testDomain.addNode("domainRule", "hipposys:domainrule");
        createFacetRule(domainRule, "facetRule1", false, "jcr:path", "Reference", "/test/folder/non/existing/one");
        createFacetRule(domainRule, "facetRule2", false, "jcr:path", "Reference", "/test/folder/non/existing/two");
        session.save();

        Session testSession = null;
        try {
            testSession = loginTestUser();
            assertTrue(testSession.nodeExists("/test"));
            assertFalse(testSession.nodeExists("/test/folder"));
            testSession.checkPermission("/test/folder", "jcr:read");
        } finally {
            if (testSession != null) {
                testSession.logout();
            }
        }
    }

    @Test
    public void combined_false_missing_reference_UUID_in_facet_rules_domain_rule() throws Exception {

        // domain with two AND-ed facet rules : one rule matches everything below /test/folder
        // and other one has a non-existing reference but has equals = false and with jcr:uuid. Result should be that everything below /test/folder
        // is readable

        final Node domainRule = testDomain.addNode("domainRule", "hipposys:domainrule");
        createFacetRule(domainRule, "facetRule1", true, "jcr:path", "Reference", "/test/folder");
        // INSTEAD OF jcr:path now jcr:uuid !!!!!!!!!!!!!!!!!!!!!!!!!!!!
        createFacetRule(domainRule, "facetRule2", false, "jcr:uuid", "Reference", "/test/folder/non/existing");
        session.save();

        Session testSession = null;
        try {
            testSession = loginTestUser();

            assertTrue(testSession.nodeExists("/test"));
            assertTrue(testSession.nodeExists("/test/folder"));
            assertTrue(testSession.nodeExists("/test/folder/authDocument"));
            assertTrue(testSession.nodeExists("/test/folder/authDocument/compound"));
            assertTrue(testSession.nodeExists("/test/folder/testDocument"));

            testSession.checkPermission("/test/folder", "jcr:read");
            testSession.checkPermission("/test/folder", "jcr:write");

            testSession.checkPermission("/test/folder/authDocument", "jcr:read");
            testSession.checkPermission("/test/folder/authDocument", "jcr:write");

            testSession.checkPermission("/test/folder/authDocument/compound", "jcr:read");
            testSession.checkPermission("/test/folder/authDocument/compound", "jcr:write");

            testSession.checkPermission("/test/folder/testDocument", "jcr:read");
            testSession.checkPermission("/test/folder/testDocument", "jcr:write");

        } finally {
            if (testSession != null) {
                testSession.logout();
            }
        }
    }

    @Test
    public void missing_reference_which_gets_added_later_on_results_in_read_access_for_user_session() throws Exception {


        final Node domainRule = testDomain.addNode("domainRule", "hipposys:domainrule");
        createFacetRule(domainRule, "facetRule1", true, "jcr:path", "Reference", "/test/folder/tobecreated");
        session.save();

        final String[] xpaths = {
                "/jcr:root/test/folder/tobecreated",
                "//element(folder)/tobecreated",
                "//element(tobecreated)",
        };


        Session testSession = null;
        try {
            testSession = loginTestUser();

            final Session modifier1 = session;
            final Session modifier2 = testSession;

            for (Session modifier : new Session[] {modifier1, modifier2}) {

                if (modifier.getUserID().equals("testUser")) {
                    // make first sure the 'testUser' gets access to '/test/folder' because otherwise it can't add
                    // the folder 'tobecreated'
                    final Node domainRule1 = testDomain.addNode("domainRule1", "hipposys:domainrule");
                    createFacetRule(domainRule1, "facetRule1", true, "jcr:uuid", "Reference", "/test/folder");
                    session.save();

                    // reset the testsession
                    testSession.logout();
                    testSession = loginTestUser();
                    modifier = testSession;
                }

                final QueryManager queryManager = testSession.getWorkspace().getQueryManager();

                assertTrue(testSession.nodeExists("/test"));

                assertFalse(testSession.nodeExists("/test/folder/tobecreated"));

                for (String xpath : xpaths) {
                    final QueryResult result = queryManager.createQuery(xpath, "xpath").execute();
                    assertEquals(String.format("Xpath '%s' did not return expected result size", xpath),
                            0L, result.getNodes().getSize());
                }

                // add 'tobecreated'
                modifier.getNode("/test/folder").addNode("tobecreated", "hippostd:folder");

                // new node not yet searchable
                for (String xpath : xpaths) {
                    final QueryResult result = queryManager.createQuery(xpath, "xpath").execute();
                    assertEquals(0L, result.getNodes().getSize());
                }
                modifier.save();

                // after creation and save, the node should be directly visible for the 'testSession'
                assertTrue(testSession.nodeExists("/test/folder/tobecreated"));

                for (String xpath : xpaths) {
                    final QueryResult result = queryManager.createQuery(xpath, "xpath").execute();
                    assertEquals(String.format("Xpath '%s' did not return expected result size", xpath),
                            1L, result.getNodes().getSize());
                }

                modifier.getNode("/test/folder/tobecreated").remove();
                modifier.save();

                assertFalse(testSession.nodeExists("/test/folder/tobecreated"));

                // of course no hits, just invoke so in case unexpected exceptions happen this test fails
                for (String xpath : xpaths) {
                    final QueryResult result = queryManager.createQuery(xpath, "xpath").execute();

                    assertEquals(String.format("Xpath '%s' did not return expected result size", xpath),
                            0L, result.getNodes().getSize());
                }

                // recreation should make the node available again after the other session saves it
                modifier.getNode("/test/folder").addNode("tobecreated", "hippostd:folder");
                modifier.save();

                // after creation and save, the node should be directly visible for the 'testSession'
                assertTrue(testSession.nodeExists("/test/folder/tobecreated"));

                // the authorization query should have been updated with the new UUID for the new 'tobecreated' node!
                for (String xpath : xpaths) {
                    final QueryResult result = queryManager.createQuery(xpath, "xpath").execute();

                    assertEquals(String.format("Xpath '%s' did not return expected result size", xpath),
                            1L, result.getNodes().getSize());
                }

                // now a subtle one: We remove the node and add it again in two transactions without in between accessing
                // 'testSession' : that should also work
                modifier.getNode("/test/folder/tobecreated").remove();
                modifier.save();

                modifier.getNode("/test/folder").addNode("tobecreated", "hippostd:folder");
                modifier.save();
                assertTrue(testSession.nodeExists("/test/folder/tobecreated"));

                for (String xpath : xpaths) {
                    final QueryResult result = queryManager.createQuery(xpath, "xpath").execute();
                    assertEquals(String.format("Xpath '%s' did not return expected result size", xpath),
                            1L, result.getNodes().getSize());
                }


                // another subtle one: We remove the node and add it again in a single transaction. Then the 'testSession'
                // should still be able to access the node
                modifier.getNode("/test/folder/tobecreated").remove();
                modifier.getNode("/test/folder").addNode("tobecreated", "hippostd:folder");
                modifier.save();

                assertTrue(testSession.nodeExists("/test/folder/tobecreated"));

                for (String xpath : xpaths) {
                    final QueryResult result = queryManager.createQuery(xpath, "xpath").execute();
                    assertEquals(String.format("Xpath '%s' did not return expected result size", xpath),
                            1L, result.getNodes().getSize());
                }

                modifier.getNode("/test/folder/tobecreated").remove();
                modifier.save();
            }

        }  finally {
            if (testSession != null) {
                testSession.logout();
            }
        }
    }

    @Test
    public void concurrent_remove_and_add_of_referenced_domain_node_with_sync_for_explicit_expectations() throws Exception {

        final Node domainRule = testDomain.addNode("domainRule", "hipposys:domainrule");
        createFacetRule(domainRule, "facetRule1", true, "jcr:path", "Reference", "/test/folder");

        session.save();

        final long durationMillis = TimeUnit.SECONDS.toMillis(5);
        final long start = System.currentTimeMillis();

        final Exception[] e = new Exception[1];

        final AtomicBoolean folderShouldExist = new AtomicBoolean(true);

        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(HippoAccessManager.class).build()){

            final List<Thread> workers = new ArrayList<>();
            final Thread modifiedThread = new Thread(() -> {
                try {
                    while (System.currentTimeMillis() < start + durationMillis) {
                        synchronized (e) {
                            if (e[0] != null) {
                                return;
                            }
                        }
                        synchronized (folderShouldExist) {
                            final Node node = session.getNode("/test/folder");
                            node.remove();
                            session.save();
                            folderShouldExist.getAndSet(false);
                        }

                        synchronized (folderShouldExist) {
                            final Node folder = session.getNode("/test").addNode("folder", "hippostd:folder");
                            if (new Random().nextInt(5) == 0 ) {
                                // sometimes add some extra child nodes to create randomness
                                folder.addNode("sub1");
                                folder.addNode("sub2");
                                folder.addNode("sub3");
                                folder.addNode("sub4");
                                folder.addNode("sub5");
                            }
                            session.save();
                            folderShouldExist.getAndSet(true);
                        }
                    }
                } catch (RepositoryException e1) {
                    synchronized (e) {
                        e[0] = e1;
                    }
                }
            });

            final int numberOfReaders = 100;
            final Session[] testUsers = new Session[numberOfReaders];

            for (int i = 0; i < numberOfReaders; i++) {
                // TODO this line below should be disabled, then testUser = loginTestUser(); will be created
                // TODO below concurrent with the above session that makes modifications. However, for some reason
                // TODO only during login a modification can be missed.
                testUsers[i] = loginTestUser();
            }

            for (int i = 0; i < numberOfReaders ; i++) {
                final int counter = i;
                final Thread readWorker = new Thread(() -> {
                    Session testUser = null;
                    try {
                        if (testUsers[counter] != null) {
                            testUser = testUsers[counter];
                        } else {
                            testUser = loginTestUser();
                        }

                        while (System.currentTimeMillis() < start + durationMillis) {
                            synchronized (e) {
                                if (e[0] != null) {
                                    return;
                                }
                            }
                            // below should trigger a reinitialization in case /test/folder was (re)added and should never
                            // in some exception or warning
                            synchronized (folderShouldExist) {
                                if (folderShouldExist.get()) {
                                    if (!testUser.nodeExists("/test/folder")) {
                                        e[0] = new IllegalStateException("/test/folder should exist");
                                    }
                                } else {
                                    if (testUser.nodeExists("/test/folder")) {
                                        e[0] = new IllegalStateException("/test/folder should not exist");
                                    }
                                }
                            }
                        }
                    } catch (Exception e1) {
                        synchronized (e) {
                            e[0] = e1;
                        }
                    } finally {
                        testUser.logout();
                    }
                });
                workers.add(readWorker);
            }

            workers.add(modifiedThread);

            for (Thread worker : workers) {
                worker.start();
            }

            for (Thread worker : workers) {
                worker.join();
            }

            if (e[0] != null) {
                throw e[0];
            }

            Assertions.assertThat(interceptor.getEvents().size())
                    .as("There should had been no warnings or errors but was: %s",
                            interceptor.messages().collect(Collectors.joining(",")))
                    .isEqualTo(0);
        }

    }


    /**
     * we do not synchronize now the writer thread and reader thread so we cannot guarantee whether the /test/folder
     * should exist or not. All we can now do is test that no exceptions happen and no warnings or errors are logged
     */
    @Test
    public void concurrent_remove_and_add_of_referenced_domain_node_without_sync_so_no_explicit_expectations() throws Exception {

        final Node domainRule = testDomain.addNode("domainRule", "hipposys:domainrule");
        createFacetRule(domainRule, "facetRule1", true, "jcr:path", "Reference", "/test/folder");

        session.save();

        final long durationMillis = TimeUnit.SECONDS.toMillis(5);
        final long start = System.currentTimeMillis();

        final Exception[] e = new Exception[1];

        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(HippoAccessManager.class).build()){

            final List<Thread> workers = new ArrayList<>();
            final Thread modifiedThread = new Thread(() -> {
                try {
                    while (System.currentTimeMillis() < start + durationMillis) {
                        synchronized (e) {
                            if (e[0] != null) {
                                return;
                            }
                        }
                        final Node node = session.getNode("/test/folder");
                        node.remove();
                        session.save();

                        final Node folder = session.getNode("/test").addNode("folder", "hippostd:folder");
                        if (new Random().nextInt(5) == 0 ) {
                            // sometimes add some extra child nodes to create randomness
                            folder.addNode("sub1");
                            folder.addNode("sub2");
                            folder.addNode("sub3");
                            folder.addNode("sub4");
                            folder.addNode("sub5");
                        }
                        session.save();

                    }
                } catch (RepositoryException e1) {
                    synchronized (e) {
                        e[0] = e1;
                    }
                }
            });

            final int numberOfReaders = 100;
            final Session[] testUsers = new Session[numberOfReaders];

            for (int i = 0; i < numberOfReaders; i++) {
                // TODO this line below should be disabled, then testUser = loginTestUser(); will be created
                // TODO below concurrent with the above session that makes modifications. However, for some reason
                // TODO only during login a modification can be missed.
                testUsers[i] = loginTestUser();
            }

            for (int i = 0; i < numberOfReaders ; i++) {
                final int counter = i;
                final Thread readWorker = new Thread(() -> {
                    Session testUser = null;
                    try {
                        if (testUsers[counter] != null) {
                            testUser = testUsers[counter];
                        } else {
                            testUser = loginTestUser();
                        }

                        while (System.currentTimeMillis() < start + durationMillis) {
                            synchronized (e) {
                                if (e[0] != null) {
                                    return;
                                }
                            }
                            // below should trigger a reinitialization in case /test/folder was (re)added and should never
                            // in some exception or warning
                            testUser.nodeExists("/test/folder");
                        }
                    } catch (Exception e1) {
                        synchronized (e) {
                            e[0] = e1;
                        }
                    } finally {
                        testUser.logout();
                    }
                });
                workers.add(readWorker);
            }

            workers.add(modifiedThread);

            for (Thread worker : workers) {
                worker.start();
            }

            for (Thread worker : workers) {
                worker.join();
            }

            if (e[0] != null) {
                throw e[0];
            }

            Assertions.assertThat(interceptor.getEvents().size())
                    .as("There should had been no warnings or errors but was: %s",
                            interceptor.messages().collect(Collectors.joining(",")))
                    .isEqualTo(0);
        }

    }

    private Session loginTestUser() throws RepositoryException {
        return server.login(new SimpleCredentials("testUser", "password".toCharArray()));
    }

    private void createFacetRule(final Node domainRule, final String ruleName, boolean equals, String facet, String type, String value) throws RepositoryException {
        final Node facetRule = domainRule.addNode(ruleName, "hipposys:facetrule");
        facetRule.setProperty("hipposys:equals", equals);
        facetRule.setProperty("hipposys:facet", facet);
        facetRule.setProperty("hipposys:type", type);
        facetRule.setProperty("hipposys:value", value);
    }


}
