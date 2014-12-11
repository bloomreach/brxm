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
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.hippoecm.repository.api.HippoNodeIterator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

public class ReferenceJcrPathAuthorizationTest extends RepositoryTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();

        // create users
        createUser("bob");
        createUser("downScopedBob");
        createUser("alice");

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

        // set up authorization rules
        final Node domains = session.getNode("/hippo:configuration/hippo:domains");
        // bob can read and write to /test/folder and everything *below*
        final Node pathFacetRuleDomain = domains.addNode("pathFacetRuleDomain", "hipposys:domain");
        Node domainRule = pathFacetRuleDomain.addNode("read-all-nodes-test-folder-and-below", "hipposys:domainrule");
        Node facetRule = domainRule.addNode("path-by-uuid", "hipposys:facetrule");
        facetRule.setProperty("hipposys:equals", true);
        facetRule.setProperty("hipposys:facet", "jcr:path");
        facetRule.setProperty("hipposys:type", "Reference");
        facetRule.setProperty("hipposys:value", "/test/folder");
        createAdminAuthRole(pathFacetRuleDomain, "bob");

        // downScopedBob can read and write to /test/folder and everything below *except* node /test/folder/authDocument
        final Node doublePathFacetRuleDomain = domains.addNode("doublePathFacetRuleDomain", "hipposys:domain");
        domainRule = doublePathFacetRuleDomain.addNode("read-all-nodes-test-folder-and-below-but-not-authDocument", "hipposys:domainrule");
        facetRule = domainRule.addNode("read-test-folder", "hipposys:facetrule");
        facetRule.setProperty("hipposys:equals", true);
        facetRule.setProperty("hipposys:facet", "jcr:path");
        facetRule.setProperty("hipposys:type", "Reference");
        facetRule.setProperty("hipposys:value", "/test/folder");

        facetRule = domainRule.addNode("cant-read-authDocument-below-test-folder", "hipposys:facetrule");
        facetRule.setProperty("hipposys:equals", false);
        facetRule.setProperty("hipposys:facet", "jcr:path");
        facetRule.setProperty("hipposys:type", "Reference");
        facetRule.setProperty("hipposys:value", "/test/folder/authDocument");

        createAdminAuthRole(doublePathFacetRuleDomain, "downScopedBob");

        // alice can read and write to /test/folder and everything *below* it that is of type 'hippostd:folder' or 'hippo:testdocument'
        // thus not "hippo:authtestdocument"
        final Node pathFacetAndTypeRuleDomain = domains.addNode("pathFacetAndTypeRuleDomain", "hipposys:domain");
        domainRule = pathFacetAndTypeRuleDomain.addNode("read-folders-in-test-folder-and-below", "hipposys:domainrule");
        facetRule = domainRule.addNode("path-by-uuid", "hipposys:facetrule");
        facetRule.setProperty("hipposys:equals", true);
        facetRule.setProperty("hipposys:facet", "jcr:path");
        facetRule.setProperty("hipposys:type", "Reference");
        facetRule.setProperty("hipposys:value", "/test/folder");

        facetRule = domainRule.addNode("node-type-hippostd-folder", "hipposys:facetrule");
        facetRule.setProperty("hipposys:equals", true);
        facetRule.setProperty("hipposys:facet", "jcr:primaryType");
        facetRule.setProperty("hipposys:type", "Name");
        facetRule.setProperty("hipposys:value", "hippostd:folder");

        domainRule = pathFacetAndTypeRuleDomain.addNode("read-folders-in-test-folder-and-below", "hipposys:domainrule");
        facetRule = domainRule.addNode("path-by-uuid", "hipposys:facetrule");
        facetRule.setProperty("hipposys:equals", true);
        facetRule.setProperty("hipposys:facet", "jcr:path");
        facetRule.setProperty("hipposys:type", "Reference");
        facetRule.setProperty("hipposys:value", "/test/folder");

        facetRule = domainRule.addNode("node-type-test-document", "hipposys:facetrule");
        facetRule.setProperty("hipposys:equals", true);
        facetRule.setProperty("hipposys:facet", "jcr:primaryType");
        facetRule.setProperty("hipposys:type", "Name");
        facetRule.setProperty("hipposys:value", "hippo:testdocument");

        createAdminAuthRole(pathFacetAndTypeRuleDomain, "alice");

        session.save();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        removeNode("/hippo:configuration/hippo:users/bob");
        removeNode("/hippo:configuration/hippo:users/downScopedBob");
        removeNode("/hippo:configuration/hippo:users/alice");
        removeNode("/hippo:configuration/hippo:domains/pathFacetRuleDomain");
        removeNode("/hippo:configuration/hippo:domains/doublePathFacetRuleDomain");
        removeNode("/hippo:configuration/hippo:domains/pathFacetAndTypeRuleDomain");
        super.tearDown();
    }

    @Test
    public void bob_can_read_and_write_to_test_folder_and_any_node_below() throws Exception {

        assumePreconditions();

        Session bob = null;
        try {
            bob = loginUser("bob");

            assertTrue(bob.nodeExists("/test"));
            assertTrue(bob.nodeExists("/test/folder"));
            assertTrue(bob.nodeExists("/test/folder/authDocument"));
            assertTrue(bob.nodeExists("/test/folder/authDocument/compound"));
            assertTrue(bob.nodeExists("/test/folder/testDocument"));

            bob.checkPermission("/test/folder", "jcr:read");
            bob.checkPermission("/test/folder", "jcr:write");

            bob.checkPermission("/test/folder/authDocument", "jcr:read");
            bob.checkPermission("/test/folder/authDocument", "jcr:write");

            bob.checkPermission("/test/folder/authDocument/compound", "jcr:read");
            bob.checkPermission("/test/folder/authDocument/compound", "jcr:write");

            bob.checkPermission("/test/folder/testDocument", "jcr:read");
            bob.checkPermission("/test/folder/testDocument", "jcr:write");

        } finally {
            if (bob != null) {
                bob.logout();
            }
        }
    }

    @Test
    public void bob_finds_test_folder_and_any_node_below() throws Exception {

        String xpath = "/jcr:root/test//*[@jcr:primaryType='hippostd:folder' or @jcr:primaryType='hippo:authtestdocument' " +
                "or @jcr:primaryType='hippo:testdocument'] order by @jcr:score";

        final Query queryAdmin = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath");
        final QueryResult resultAdmin = queryAdmin.execute();

        // expected results are: "/test/folder",  "/test/folder/authDocument", "/test/folder/authDocument/compound", and "/test/folder/testDocument";
        final NodeIterator nodesAdmin = resultAdmin.getNodes();
        assertEquals(4L, ((HippoNodeIterator)nodesAdmin).getTotalSize());

        Session bob = null;
        try {
            bob = loginUser("bob");
            final Query queryTestSession = bob.getWorkspace().getQueryManager().createQuery(xpath, "xpath");
            final QueryResult resultTestSession = queryTestSession.execute();
            final NodeIterator nodesBob = resultTestSession.getNodes();
            assertEquals(4L, ((HippoNodeIterator)nodesBob).getTotalSize());
        } finally {
            if (bob != null) {
                bob.logout();
            }
        }
    }

    @Test(expected = AccessControlException.class)
    public void downScopedBob_can_read_and_write_to_test_folder_and_below_except_below_authDocument() throws Exception {

        assumePreconditions();

        Session downScopedBob = null;
        try {
            downScopedBob = loginUser("downScopedBob");

            assertTrue(downScopedBob.nodeExists("/test"));
            assertTrue(downScopedBob.nodeExists("/test/folder"));
            assertTrue(downScopedBob.nodeExists("/test/folder/testDocument"));

            // authDocument and nodes below are downscoped
            assertFalse(downScopedBob.nodeExists("/test/folder/authDocument"));
            assertFalse(downScopedBob.nodeExists("/test/folder/authDocument/compound"));

            try {
                downScopedBob.checkPermission("/test/folder", "jcr:read");
                downScopedBob.checkPermission("/test/folder", "jcr:write");

                downScopedBob.checkPermission("/test/folder/testDocument", "jcr:read");
                downScopedBob.checkPermission("/test/folder/testDocument", "jcr:write");
            } catch (AccessControlException e) {
                fail("Permissions check failed: " + e);
            }

            downScopedBob.checkPermission("/test/folder/authDocument", "jcr:read");

        } finally {
            if (downScopedBob != null) {
                downScopedBob.logout();
            }
        }
    }

    @Test
    public void downScopedBob_finds_test_folder_and_below_except_below_authDocument() throws Exception {

        String xpath = "/jcr:root/test//*[@jcr:primaryType='hippostd:folder' or @jcr:primaryType='hippo:authtestdocument' " +
                "or @jcr:primaryType='hippo:testdocument'] order by @jcr:score";

        final Query queryAdmin = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath");
        final QueryResult resultAdmin = queryAdmin.execute();

        // expected results are: "/test/folder",  "/test/folder/authDocument", "/test/folder/authDocument/compound", and "/test/folder/testDocument";
        final NodeIterator nodesAdmin = resultAdmin.getNodes();
        assertEquals(4L, ((HippoNodeIterator)nodesAdmin).getTotalSize());

        Session downScopedBob = null;
        try {
            downScopedBob = loginUser("downScopedBob");
            final Query queryTestSession = downScopedBob.getWorkspace().getQueryManager().createQuery(xpath, "xpath");
            final QueryResult resultTestSession = queryTestSession.execute();
            final NodeIterator nodesBob = resultTestSession.getNodes();
            assertEquals(2L, ((HippoNodeIterator)nodesBob).getTotalSize());
        } finally {
            if (downScopedBob != null) {
                downScopedBob.logout();
            }
        }
    }

    @Test(expected = AccessControlException.class)
    public void alice_can_read_and_write_to_test_folder_and_all_folders_and_hippo_document_nodes_below() throws Exception {

        assumePreconditions();

        Session alice = null;
        try {
            alice = loginUser("alice");

            assertTrue(alice.nodeExists("/test"));
            assertTrue(alice.nodeExists("/test/folder"));

            assertFalse("Alice should *not* have read access on 'hippo:authtestdocument'",alice.nodeExists("/test/folder/authDocument"));
            assertFalse("Alice should *not* have read access on 'hippo:authtestdocument'", alice.nodeExists("/test/folder/authDocument/compound"));
            assertTrue("Alice *should* have read access on 'hippo:testdocument'",alice.nodeExists("/test/folder/testDocument"));

            try {
                alice.checkPermission("/test/folder", "jcr:read");
                alice.checkPermission("/test/folder", "jcr:write");

                alice.checkPermission("/test/folder/testDocument", "jcr:read");
                alice.checkPermission("/test/folder/testDocument", "jcr:write");
            } catch (AccessControlException e) {
                fail("Permissions check failed: " + e);
            }

            alice.checkPermission("/test/folder/authDocument", "jcr:read");

        } finally {
            if (alice != null) {
                alice.logout();
            }
        }
    }

    @Test
    public void alice_finds_test_folder_and_nodes_of_some_types_below() throws Exception {

        String xpath = "/jcr:root/test//*[@jcr:primaryType='hippostd:folder' or @jcr:primaryType='hippo:authtestdocument' " +
                "or @jcr:primaryType='hippo:testdocument'] order by @jcr:score";

        final Query queryAdmin = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath");
        final QueryResult resultAdmin = queryAdmin.execute();

        // expected results are: "/test/folder",  "/test/folder/authDocument", "/test/folder/authDocument/compound", and "/test/folder/testDocument";
        final NodeIterator nodesAdmin = resultAdmin.getNodes();
        assertEquals(4L, ((HippoNodeIterator)nodesAdmin).getTotalSize());

        // alice can not read "/test/folder/authDocument/compound" and "/test/folder/authDocument" because she is not allowed
        // to read hippo:authtestdocument nodes
        Session alice = null;
        try {
            alice = loginUser("alice");
            final Query queryTestSession = alice.getWorkspace().getQueryManager().createQuery(xpath, "xpath");
            final QueryResult resultTestSession = queryTestSession.execute();
            final NodeIterator nodesAlice = resultTestSession.getNodes();
            assertEquals(2L, ((HippoNodeIterator) nodesAlice).getTotalSize());
        } finally {
            if (alice != null) {
                alice.logout();
            }
        }
    }


    private void createAdminAuthRole(final Node pathFacetRuleDomain, final String user) throws RepositoryException {
        final Node bobIsAdmin = pathFacetRuleDomain.addNode(user, "hipposys:authrole");
        bobIsAdmin.setProperty("hipposys:users", new String[]{ user });
        bobIsAdmin.setProperty("hipposys:role", "admin");
    }

    private Node createUser(String name) throws RepositoryException {
        final Node users = session.getNode("/hippo:configuration/hippo:users");
        if (!users.hasNode(name)) {
            final Node user = users.addNode(name, "hipposys:user");
            user.setProperty("hipposys:password", "password");
        }
        return users;
    }

    private Session loginUser(String user) throws RepositoryException {
        return server.login(new SimpleCredentials(user, "password".toCharArray()));
    }

    private void assumePreconditions() throws RepositoryException {
        assumeTrue(session.nodeExists("/test/folder"));
        assumeTrue(session.nodeExists("/test/folder/authDocument"));
        assumeTrue(session.nodeExists("/test/folder/authDocument/compound"));
        assumeTrue(session.nodeExists("/test/folder/testDocument"));
    }

}
