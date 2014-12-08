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

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.hippoecm.repository.api.HippoNodeIterator;
import org.hippoecm.repository.query.lucene.HippoQueryResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FacetValueWildcardAuthorizationTest extends RepositoryTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();

        // create users
        final Node users = session.getNode("/hippo:configuration/hippo:users");
        if (!users.hasNode("readEveryWhereSession")) {
            final Node testSession = users.addNode("readEveryWhereSession", "hipposys:user");
            testSession.setProperty("hipposys:password", "password");
        }
        if (!users.hasNode("readNoWhereSession")) {
            final Node testSession = users.addNode("readNoWhereSession", "hipposys:user");
            testSession.setProperty("hipposys:password", "password");
        }

        if (!users.hasNode("folderTypeAnySession")) {
            final Node testSession = users.addNode("folderTypeAnySession", "hipposys:user");
            testSession.setProperty("hipposys:password", "password");
        }

        if (!users.hasNode("folderTypeNotAnySession")) {
            final Node testSession = users.addNode("folderTypeNotAnySession", "hipposys:user");
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
        if (!domains.hasNode("allDomain")) {
            final Node allDomain = domains.addNode("allDomain", "hipposys:domain");
            final Node domainRule = allDomain.addNode("all-nodes", "hipposys:domainrule");
            final Node facetRule = domainRule.addNode("match-all-types", "hipposys:facetrule");
            facetRule.setProperty("hipposys:equals", true);
            facetRule.setProperty("hipposys:facet", "jcr:primaryType");
            facetRule.setProperty("hipposys:type", "Name");
            facetRule.setProperty("hipposys:value", "*");
            final Node testSessionIsAdmin = allDomain.addNode("readEveryWhereSession", "hipposys:authrole");
            testSessionIsAdmin.setProperty("hipposys:users", new String[]{"readEveryWhereSession"});
            testSessionIsAdmin.setProperty("hipposys:role", "admin");
        }

        if (!domains.hasNode("nonDomain")) {
            final Node nonDomain = domains.addNode("nonDomain", "hipposys:domain");
            final Node domainRule = nonDomain.addNode("no-nodes", "hipposys:domainrule");
            final Node facetRule = domainRule.addNode("match-no-types", "hipposys:facetrule");
            facetRule.setProperty("hipposys:equals", false);
            facetRule.setProperty("hipposys:facet", "jcr:primaryType");
            facetRule.setProperty("hipposys:type", "Name");
            facetRule.setProperty("hipposys:value", "*");
            final Node testSessionIsAdmin = nonDomain.addNode("readNoWhereSession", "hipposys:authrole");
            testSessionIsAdmin.setProperty("hipposys:users", new String[]{"readNoWhereSession"});
            testSessionIsAdmin.setProperty("hipposys:role", "admin");
        }

        if (!domains.hasNode("folderTypeAnyDomain")) {
            final Node folderTypeAnyDomain = domains.addNode("folderTypeAnyDomain", "hipposys:domain");
            final Node domainRule = folderTypeAnyDomain.addNode("anyType", "hipposys:domainrule");
            final Node facetRule = domainRule.addNode("match-any-types", "hipposys:facetrule");
            facetRule.setProperty("hipposys:equals", true);
            facetRule.setProperty("hipposys:facet", "hippostd:foldertype");
            facetRule.setProperty("hipposys:type", "String");
            facetRule.setProperty("hipposys:value", "*");
            final Node testSessionIsAdmin = folderTypeAnyDomain.addNode("folderTypeAnySession", "hipposys:authrole");
            testSessionIsAdmin.setProperty("hipposys:users", new String[]{"folderTypeAnySession"});
            testSessionIsAdmin.setProperty("hipposys:role", "admin");
        }

        if (!domains.hasNode("folderTypeNotAnyDomain")) {
            final Node folderTypeNotAnyDomain = domains.addNode("folderTypeNotAnyDomain", "hipposys:domain");
            final Node domainRule = folderTypeNotAnyDomain.addNode("anyType", "hipposys:domainrule");
            final Node facetRule = domainRule.addNode("match-non-types", "hipposys:facetrule");
            facetRule.setProperty("hipposys:equals", false);
            facetRule.setProperty("hipposys:facet", "hippostd:foldertype");
            facetRule.setProperty("hipposys:type", "String");
            facetRule.setProperty("hipposys:value", "*");
            final Node testSessionIsAdmin = folderTypeNotAnyDomain.addNode("folderTypeNotAnyDomain", "hipposys:authrole");
            testSessionIsAdmin.setProperty("hipposys:users", new String[]{"folderTypeNotAnySession"});
            testSessionIsAdmin.setProperty("hipposys:role", "admin");
        }

        session.save();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        removeNode("/hippo:configuration/hippo:users/readNoWhereSession");
        removeNode("/hippo:configuration/hippo:users/readEveryWhereSession");
        removeNode("/hippo:configuration/hippo:users/folderTypeAnySession");
        removeNode("/hippo:configuration/hippo:users/folderTypeNotAnySession");
        removeNode("/hippo:configuration/hippo:domains/nonDomain");
        removeNode("/hippo:configuration/hippo:domains/allDomain");
        removeNode("/hippo:configuration/hippo:domains/folderTypeAnyDomain");
        removeNode("/hippo:configuration/hippo:domains/folderTypeNotAnyDomain");
        super.tearDown();
    }

    @Test
    public void readEveryWhereSession_can_read_testFolder() throws Exception {

        assertTrue(session.nodeExists("/test/folder"));

        Session readEveryWhereSession = null;
        try {
            final Credentials readEveryWhereSessionCreds = new SimpleCredentials("readEveryWhereSession", "password".toCharArray());
            readEveryWhereSession = server.login(readEveryWhereSessionCreds);

            assertTrue(readEveryWhereSession.nodeExists("/test"));
            assertTrue(readEveryWhereSession.nodeExists("/test/folder"));
            assertTrue(readEveryWhereSession.nodeExists("/test/folder/document"));
        } finally {
            if (readEveryWhereSession != null) {
                readEveryWhereSession.logout();
            }
        }
    }

    @Test
    public void readNoWhereSession_cannot_read_testFolder() throws Exception {

        assertTrue(session.nodeExists("/test/folder"));

        Session readNoWhereSession = null;
        try {
            final Credentials readNoWhereSessionCreds = new SimpleCredentials("readNoWhereSession", "password".toCharArray());
            readNoWhereSession = server.login(readNoWhereSessionCreds);
            assertFalse(readNoWhereSession.nodeExists("/test/folder"));

        } finally {
            if (readNoWhereSession != null) {
                readNoWhereSession.logout();
            }
        }
    }

    @Test
    public void folderTypeAnySession_can_read_testFolder() throws Exception {

        assertTrue(session.nodeExists("/test/folder"));

        Session folderTypeAnySession = null;
        try {
            final Credentials folderTypeAnySessionCreds = new SimpleCredentials("folderTypeAnySession", "password".toCharArray());
            folderTypeAnySession = server.login(folderTypeAnySessionCreds);
            assertTrue(folderTypeAnySession.nodeExists("/test/folder"));

        } finally {
            if (folderTypeAnySession != null) {
                folderTypeAnySession.logout();
            }
        }
    }

    @Test
    public void folderTypeNotAnySession_cannot_read_testFolder() throws Exception {

        assertTrue(session.nodeExists("/test/folder"));

        Session folderTypeNotAnySession = null;
        try {
            final Credentials folderTypeNotAnySessionCreds = new SimpleCredentials("folderTypeNotAnySession", "password".toCharArray());
            folderTypeNotAnySession = server.login(folderTypeNotAnySessionCreds);
            assertFalse(folderTypeNotAnySession.nodeExists("/test/folder"));

        } finally {
            if (folderTypeNotAnySession != null) {
                folderTypeNotAnySession.logout();
            }
        }
    }

    @Test
    public void readEveryWhereSession_finds_folder_hits_below_test() throws Exception {
        Session readEveryWhereSession = null;
        try {
            final Credentials readEveryWhereSessionCreds = new SimpleCredentials("readEveryWhereSession", "password".toCharArray());
            readEveryWhereSession = server.login(readEveryWhereSessionCreds);
            String xpath = "//element(*,hippostd:folder)[@hippostd:foldertype='foo'] order by @jcr:score";
            final Query query = readEveryWhereSession.getWorkspace().getQueryManager().createQuery(xpath, "xpath");
            final QueryResult result = query.execute();

            final NodeIterator nodes = result.getNodes();
            assertEquals(1L, ((HippoNodeIterator) nodes).getTotalSize());

        } finally {
            if (readEveryWhereSession != null) {
                readEveryWhereSession.logout();
            }
        }
    }

    @Test
    public void readNoWhereSession_finds_no_folder_hits_below_test() throws Exception {
        Session readNoWhereSession = null;
        try {
            final Credentials readNoWhereSessionCreds = new SimpleCredentials("readNoWhereSession", "password".toCharArray());
            readNoWhereSession = server.login(readNoWhereSessionCreds);
            String xpath = "//element(*,hippostd:folder)[@hippostd:foldertype='foo'] order by @jcr:score";
            final Query query = readNoWhereSession.getWorkspace().getQueryManager().createQuery(xpath, "xpath");
            final QueryResult result = query.execute();

            final NodeIterator nodes = result.getNodes();
            assertEquals(0L, ((HippoNodeIterator)nodes).getTotalSize());

        } finally {
            if (readNoWhereSession != null) {
                readNoWhereSession.logout();
            }
        }
    }


    @Test
    public void folderTypeAnySession_finds_folder_hits_below_test() throws Exception {
        Session folderTypeAnySession = null;
        try {
            final Credentials folderTypeAnySessionCreds = new SimpleCredentials("folderTypeAnySession", "password".toCharArray());
            folderTypeAnySession = server.login(folderTypeAnySessionCreds);
            String xpath = "//element(*,hippostd:folder)[@hippostd:foldertype='foo'] order by @jcr:score";
            final Query query = folderTypeAnySession.getWorkspace().getQueryManager().createQuery(xpath, "xpath");
            final QueryResult result = query.execute();

            final NodeIterator nodes = result.getNodes();
            assertEquals(1L, ((HippoNodeIterator)nodes).getTotalSize());

        } finally {
            if (folderTypeAnySession != null) {
                folderTypeAnySession.logout();
            }
        }
    }

    @Test
    public void folderTypeNotAnySession_finds_no_folder_hits_below_test() throws Exception {
        Session folderTypeNotAnySession = null;
        try {
            final Credentials folderTypeNotAnySessionCreds = new SimpleCredentials("folderTypeNotAnySession", "password".toCharArray());
            folderTypeNotAnySession = server.login(folderTypeNotAnySessionCreds);
            String xpath = "//element(*,hippostd:folder)[@hippostd:foldertype='foo'] order by @jcr:score";
            final Query query = folderTypeNotAnySession.getWorkspace().getQueryManager().createQuery(xpath, "xpath");
            final QueryResult result = query.execute();

            final NodeIterator nodes = result.getNodes();
            assertEquals(0L, ((HippoNodeIterator)nodes).getTotalSize());

        } finally {
            if (folderTypeNotAnySession != null) {
                folderTypeNotAnySession.logout();
            }
        }
    }


}
