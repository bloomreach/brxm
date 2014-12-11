/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MissingReferenceAuthorizationTest extends RepositoryTestCase {

    private Node testDomain;

    @Before
    public void setUp() throws Exception {
        super.setUp();

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

        session.save();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        removeNode("/hippo:configuration/hippo:users/testUser");
        removeNode("/hippo:configuration/hippo:domains/testDomain");
        super.tearDown();
    }


    @Test(expected = AccessControlException.class)
    public void one_true_missing_reference_in_facet_rule_domain_rule() throws Exception {

        // domain with single facet rule that has equals = true & non existing reference
        // should result in no read access below /test/folder

        final Node domainRule = testDomain.addNode("domainrule", "hipposys:domainrule");
        createFacetRule(domainRule, true, "jcr:path", "Reference", "/test/folder/non/existing");
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
    public void one_false_missing_reference_in_facet_rule_domain_rule() throws Exception {

        // domain with single facet rule that has equals = false & non existing reference
        // should result in no read access below /test/folder

        final Node domainRule = testDomain.addNode("domainRule", "hipposys:domainrule");
        createFacetRule(domainRule, false, "jcr:path", "Reference", "/test/folder/non/existing");
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
        createFacetRule(domainRule, true, "jcr:path", "Reference", "/test/folder");
        createFacetRule(domainRule, true, "jcr:path", "Reference", "/test/folder/non/existing");
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
        createFacetRule(domainRule, true, "jcr:path", "Reference", "/test/folder");
        createFacetRule(domainRule, false, "jcr:path", "Reference", "/test/folder/non/existing");
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
        createFacetRule(domainRule, true, "jcr:path", "Reference", "/test/folder/non/existing/one");
        createFacetRule(domainRule, true, "jcr:path", "Reference", "/test/folder/non/existing/two");
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
    public void only_false_missing_references_in_facet_rules_domain_rule() throws Exception {

        // domain with two AND-ed facet rules : both have equals = true and one contains
        // non existing reference. Result should be no read access below /test/folder

        final Node domainRule = testDomain.addNode("domainRule", "hipposys:domainrule");
        createFacetRule(domainRule, false, "jcr:path", "Reference", "/test/folder/non/existing/one");
        createFacetRule(domainRule, false, "jcr:path", "Reference", "/test/folder/non/existing/two");
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
        createFacetRule(domainRule, true, "jcr:path", "Reference", "/test/folder");
        // INSTEAD OF jcr:path now jcr:uuid !!!!!!!!!!!!!!!!!!!!!!!!!!!!
        createFacetRule(domainRule, false, "jcr:uuid", "Reference", "/test/folder/non/existing");
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

    private Session loginTestUser() throws RepositoryException {
        return server.login(new SimpleCredentials("testUser", "password".toCharArray()));
    }

    private void createFacetRule(final Node domainRule, boolean equals, String facet, String type, String value) throws RepositoryException {
        final Node facetRule = domainRule.addNode("hippo:facetrule", "hipposys:facetrule");
        facetRule.setProperty("hipposys:equals", equals);
        facetRule.setProperty("hipposys:facet", facet);
        facetRule.setProperty("hipposys:type", type);
        facetRule.setProperty("hipposys:value", value);
    }

}
