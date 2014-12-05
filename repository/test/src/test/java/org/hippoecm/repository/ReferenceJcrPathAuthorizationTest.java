/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.hippoecm.repository.api.HippoNodeIterator;
import org.hippoecm.repository.util.NodeIterable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReferenceJcrPathAuthorizationTest extends RepositoryTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();

        // create users
        final Node users = session.getNode("/hippo:configuration/hippo:users");
        if (!users.hasNode("bob")) {
            final Node bob = users.addNode("bob", "hipposys:user");
            bob.setProperty("hipposys:password", "password");
        }

        if (!users.hasNode("alice")) {
            final Node bob = users.addNode("alice", "hipposys:user");
            bob.setProperty("hipposys:password", "password");
        }

        final Node root = session.getRootNode();
        if (!root.hasNode("test")) {
            final Node test = root.addNode("test");
            final Node folder = test.addNode("folder", "hippostd:folder");
            folder.setProperty("hippostd:foldertype", new String[]{"foo", "bar"});
            final Node authDocument = folder.addNode("authDocument", "hippo:authtestdocument");
            authDocument.setProperty("authDocumentProp", "foo");
            final Node compound = authDocument.addNode("compound", "hippo:authtestdocument");
            compound.setProperty("compoundProp", "bar");
            final Node testDocument = folder.addNode("testDocument", "hippo:testdocument");
            testDocument.setProperty("testDocumentProp", "lux");
        }

        final Node domains = session.getNode("/hippo:configuration/hippo:domains");
        // bob can read and write to /test/folder and everything *below*
        if (!domains.hasNode("pathFacetRuleDomain")) {
            final Node pathFacetRuleDomain = domains.addNode("pathFacetRuleDomain", "hipposys:domain");
            final Node domainRule = pathFacetRuleDomain.addNode("read-all-nodes-test-folder-and-below", "hipposys:domainrule");
            final Node facetRule = domainRule.addNode("path-by-uuid", "hipposys:facetrule");
            facetRule.setProperty("hipposys:equals", true);
            facetRule.setProperty("hipposys:facet", "jcr:path");
            facetRule.setProperty("hipposys:type", "Reference");
            facetRule.setProperty("hipposys:value", "/test/folder");
            final Node bobIsAdmin = pathFacetRuleDomain.addNode("bob", "hipposys:authrole");
            bobIsAdmin.setProperty("hipposys:users", new String[]{"bob"});
            bobIsAdmin.setProperty("hipposys:role", "admin");
        }

        // alive can read and write to /test/folder and everything *below* it that is of type 'hippostd:folder' or 'hippo:testdocument'
        // thus not "hippo:authtestdocument"
        if (!domains.hasNode("pathFacetAndTypeRuleDomain")) {
            final Node pathFacetAndTypeRuleDomain = domains.addNode("pathFacetAndTypeRuleDomain", "hipposys:domain");
            {
                final Node domainRule = pathFacetAndTypeRuleDomain.addNode("read-folders-in-test-folder-and-below", "hipposys:domainrule");
                final Node facetRule = domainRule.addNode("path-by-uuid", "hipposys:facetrule");
                facetRule.setProperty("hipposys:equals", true);
                facetRule.setProperty("hipposys:facet", "jcr:path");
                facetRule.setProperty("hipposys:type", "Reference");
                facetRule.setProperty("hipposys:value", "/test/folder");

                final Node facetRule2 = domainRule.addNode("node-type-hippostd-folder", "hipposys:facetrule");
                facetRule2.setProperty("hipposys:equals", true);
                facetRule2.setProperty("hipposys:facet", "jcr:primaryType");
                facetRule2.setProperty("hipposys:type", "Name");
                facetRule2.setProperty("hipposys:value", "hippostd:folder");
            }
            {
                final Node domainRule = pathFacetAndTypeRuleDomain.addNode("read-folders-in-test-folder-and-below", "hipposys:domainrule");
                final Node facetRule = domainRule.addNode("path-by-uuid", "hipposys:facetrule");
                facetRule.setProperty("hipposys:equals", true);
                facetRule.setProperty("hipposys:facet", "jcr:path");
                facetRule.setProperty("hipposys:type", "Reference");
                facetRule.setProperty("hipposys:value", "/test/folder");

                final Node facetRule2 = domainRule.addNode("node-type-test-document", "hipposys:facetrule");
                facetRule2.setProperty("hipposys:equals", true);
                facetRule2.setProperty("hipposys:facet", "jcr:primaryType");
                facetRule2.setProperty("hipposys:type", "Name");
                facetRule2.setProperty("hipposys:value", "hippo:testdocument");
            }

            final Node aliceIsAdmin = pathFacetAndTypeRuleDomain.addNode("alice", "hipposys:authrole");
            aliceIsAdmin.setProperty("hipposys:users", new String[]{"alice"});
            aliceIsAdmin.setProperty("hipposys:role", "admin");
        }

        session.save();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        final Node users = session.getNode("/hippo:configuration/hippo:users");
        if (users.hasNode("bob")) {
            users.getNode("bob").remove();
        }
        if (users.hasNode("alice")) {
            users.getNode("alice").remove();
        }

        final Node domains = session.getNode("/hippo:configuration/hippo:domains");
        if (domains.hasNode("pathFacetRuleDomain")) {
            domains.getNode("pathFacetRuleDomain").remove();
        }
        if (domains.hasNode("pathFacetAndTypeRuleDomain")) {
            domains.getNode("pathFacetAndTypeRuleDomain").remove();
        }

        session.save();
        super.tearDown();
    }

    @Test
    public void bob_can_read_and_write_to_test_folder_and_any_node_below() throws Exception {

        assertTrue(session.nodeExists("/test/folder"));
        assertTrue(session.nodeExists("/test/folder/authDocument"));
        assertTrue(session.nodeExists("/test/folder/authDocument/compound"));
        assertTrue(session.nodeExists("/test/folder/testDocument"));

        Session bob = null;
        try {
            final Credentials bobCreds = new SimpleCredentials("bob", "password".toCharArray());
            bob = server.login(bobCreds);

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
            final Credentials bobCreds = new SimpleCredentials("bob", "password".toCharArray());
            bob = server.login(bobCreds);
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
    public void alice_can_read_and_write_to_test_folder_and_all_folders_and_hippo_document_nodes_below() throws Exception {

        assertTrue(session.nodeExists("/test/folder"));
        assertTrue(session.nodeExists("/test/folder/authDocument"));
        assertTrue(session.nodeExists("/test/folder/authDocument/compound"));
        assertTrue(session.nodeExists("/test/folder/testDocument"));

        Session alice = null;
        try {
            final Credentials aliceCreds = new SimpleCredentials("alice", "password".toCharArray());
            alice = server.login(aliceCreds);

            assertTrue(alice.nodeExists("/test"));
            assertTrue(alice.nodeExists("/test/folder"));

            assertFalse("Alice should *not* have read access on 'hippo:authtestdocument'",alice.nodeExists("/test/folder/authDocument"));
            assertFalse("Alice should *not* have read access on 'hippo:authtestdocument'", alice.nodeExists("/test/folder/authDocument/compound"));
            assertTrue("Alice *should* have read access on 'hippo:testdocument'",alice.nodeExists("/test/folder/testDocument"));

            alice.checkPermission("/test/folder", "jcr:read");
            alice.checkPermission("/test/folder", "jcr:write");

            alice.checkPermission("/test/folder/testDocument", "jcr:read");
            alice.checkPermission("/test/folder/testDocument", "jcr:write");

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
            final Credentials aliceCreds = new SimpleCredentials("alice", "password".toCharArray());
            alice = server.login(aliceCreds);
            final Query queryTestSession = alice.getWorkspace().getQueryManager().createQuery(xpath, "xpath");
            final QueryResult resultTestSession = queryTestSession.execute();
            final NodeIterator nodesAlice = resultTestSession.getNodes();
            assertEquals(2L, ((HippoNodeIterator)nodesAlice).getTotalSize());
        } finally {
            if (alice != null) {
                alice.logout();
            }
        }
    }


    @Test
    public void testSession_document_authorized_via_facet_navigation_as_well() throws Exception {
        // TODO test virtual nodes as well whether they can be read for jcr:path constraints
    }
    @Test
    public void testSession_document_searchable_via_facet_navigation_as_well() throws Exception {
        // TODO test virtual nodes as well whether they can be read for jcr:path constraints
    }


}
