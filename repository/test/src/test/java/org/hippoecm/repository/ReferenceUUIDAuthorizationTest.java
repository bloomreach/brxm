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

import java.util.Arrays;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.codehaus.groovy.runtime.powerassert.SourceText;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeIterator;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.util.NodeIterable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.security.domain.DomainRuleExtension;
import org.onehippo.repository.security.domain.FacetRule;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReferenceUUIDAuthorizationTest extends RepositoryTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();

        // create users
        final Node users = session.getNode("/hippo:configuration/hippo:users");
        if (!users.hasNode("testSession")) {
            final Node testSession = users.addNode("testSession", "hipposys:user");
            testSession.setProperty("hipposys:password", "password");
        }

        final Node root = session.getRootNode();
        if (!root.hasNode("test")) {
            final Node test = root.addNode("test");
            final Node folder = test.addNode("folder", "hippostd:folder");
            folder.setProperty("hippostd:foldertype", new String[]{"foo", "bar"});
            folder.addNode("document", "hippo:authtestdocument");
        }

        final Node domains = session.getNode("/hippo:configuration/hippo:domains");
        if (!domains.hasNode("uuidDomain")) {
            final Node uuidDomain = domains.addNode("uuidDomain", "hipposys:domain");
            final Node domainRule = uuidDomain.addNode("read-folder", "hipposys:domainrule");
            final Node facetRule = domainRule.addNode("node-by-uuid", "hipposys:facetrule");
            facetRule.setProperty("hipposys:equals", true);
            facetRule.setProperty("hipposys:facet", "jcr:uuid");
            facetRule.setProperty("hipposys:type", "Reference");
            facetRule.setProperty("hipposys:value", "/test/folder");
            final Node testSessionIsAdmin = uuidDomain.addNode("testSession", "hipposys:authrole");
            testSessionIsAdmin.setProperty("hipposys:users", new String[]{"testSession"});
            testSessionIsAdmin.setProperty("hipposys:role", "admin");
        }

        session.save();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        removeNode("/hippo:configuration/hippo:users/testSession");
        removeNode("/hippo:configuration/hippo:domains/uuidDomain");
        super.tearDown();
    }

    @Test
    public void testSession_can_read_and_write_to_testFolder() throws Exception {

        assertTrue(session.nodeExists("/test/folder"));

        Session testSession = null;
        try {
            final Credentials testSessionCreds = new SimpleCredentials("testSession", "password".toCharArray());
            testSession = server.login(testSessionCreds);

            assertTrue(testSession.nodeExists("/test"));
            assertTrue(testSession.nodeExists("/test/folder"));

            testSession.checkPermission("/test/folder", "jcr:read");
            testSession.checkPermission("/test/folder", "jcr:write");
        } finally {
            if (testSession != null) {
                testSession.logout();
            }
        }
    }

    @Test
    public void testSession_can_find_testFolder_with_search() throws Exception {

        String xpath = "//element(*,hippostd:folder)[@hippostd:foldertype='foo']";
        final Query queryAdmin = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath");
        final QueryResult resultAdmin = queryAdmin.execute();
        assertEquals("folder", resultAdmin.getNodes().nextNode().getName());
        Session testSession = null;
        try {
            final Credentials testSessionCreds = new SimpleCredentials("testSession", "password".toCharArray());
            testSession = server.login(testSessionCreds);
            final Query queryTestSession = testSession.getWorkspace().getQueryManager().createQuery(xpath, "xpath");
            final QueryResult resultTestSession = queryTestSession.execute();
            assertEquals("folder", resultTestSession.getNodes().nextNode().getName());
        } finally {
            if (testSession != null) {
                testSession.logout();
            }
        }
    }


    @Test(expected = java.security.AccessControlException.class)
    public void testSession_cannot_read_test_document() throws Exception {
        assertTrue(session.nodeExists("/test/folder/document"));
        Session testSession = null;
        try {
            final Credentials testSessionCreds = new SimpleCredentials("testSession", "password".toCharArray());
            testSession = server.login(testSessionCreds);

            assertFalse(testSession.nodeExists("/test/folder/document"));
            testSession.checkPermission("/test/folder/document", "jcr:read");
        } finally {
            if (testSession != null) {
                testSession.logout();
            }
        }
    }

    @Test
    public void testSession_cannot_find_test_document_with_search() throws Exception {

        String xpath = "/jcr:root/test/folder//element(*,hippo:authtestdocument) order by @jcr:score";
        final Query queryAdmin = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath");
        final QueryResult resultAdmin = queryAdmin.execute();
        assertEquals("document", resultAdmin.getNodes().nextNode().getName());
        Session testSession = null;
        try {
            final Credentials testSessionCreds = new SimpleCredentials("testSession", "password".toCharArray());
            testSession = server.login(testSessionCreds);
            final Query queryTestSession = testSession.getWorkspace().getQueryManager().createQuery(xpath, "xpath");
            final QueryResult resultTestSession = queryTestSession.execute();

            // to test lucene authorization query correctness, test totalSize directly
            final NodeIterator nodes = resultTestSession.getNodes();
            assertEquals(0L, ((HippoNodeIterator)nodes).getTotalSize());


        } finally {
            if (testSession != null) {
                testSession.logout();
            }
        }
    }


}
