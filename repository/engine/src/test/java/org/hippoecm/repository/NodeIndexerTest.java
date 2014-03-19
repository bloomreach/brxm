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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.hippoecm.repository.api.HippoNodeIterator;
import org.hippoecm.repository.api.HippoNodeType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NodeIndexerTest extends RepositoryTestCase {

    // nodes that have to be cleaned up
    private static final String DOMAIN_READ_NODE = "readme";

    private static final String TEST_USER_ID = "testuser";
    private static final String TEST_USER_PASS = "password";

    static String[] content = {
            "/test", "nt:unstructured",
                "/test/doc", "hippo:handle",
                    "jcr:mixinTypes", "hippo:hardhandle",
                    "/test/doc/doc", "hippo:testdocument",
                        "jcr:mixinTypes", "mix:versionable"
    };

    private Session userSession;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        Node config = session.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH);
        Node domains = config.getNode(HippoNodeType.DOMAINS_PATH);
        Node users = config.getNode(HippoNodeType.USERS_PATH);

        // create read domain
        Node readDomain = domains.addNode(DOMAIN_READ_NODE, HippoNodeType.NT_DOMAIN);
        Node ar = readDomain.addNode("hippo:authrole", HippoNodeType.NT_AUTHROLE);
        ar.setProperty(HippoNodeType.HIPPO_ROLE, "readonly");
        ar.setProperty(HippoNodeType.HIPPO_USERS, new String[] {TEST_USER_ID});

        Node dr  = readDomain.addNode("hippo:domainrule", HippoNodeType.NT_DOMAINRULE);
        Node fr  = dr.addNode("hippo:facetrule", HippoNodeType.NT_FACETRULE);
        fr.setProperty(HippoNodeType.HIPPO_FACET, "authtest");
        fr.setProperty(HippoNodeType.HIPPOSYS_FILTER, true);
        fr.setProperty(HippoNodeType.HIPPOSYS_VALUE, "canread");
        fr.setProperty(HippoNodeType.HIPPOSYS_TYPE, "String");

        Node testUser = users.addNode(TEST_USER_ID, HippoNodeType.NT_USER);
        testUser.setProperty(HippoNodeType.HIPPO_PASSWORD, TEST_USER_PASS);

        session.save();

        // setup user session
        userSession = server.login(TEST_USER_ID, TEST_USER_PASS.toCharArray());
    }

    @After
    @Override
    public void tearDown() throws Exception {
        Node config = session.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH);
        Node domains = config.getNode(HippoNodeType.DOMAINS_PATH);
        Node users = config.getNode(HippoNodeType.USERS_PATH);

        if (domains.hasNode(DOMAIN_READ_NODE)) {
            domains.getNode(DOMAIN_READ_NODE).remove();
        }
        if (users.hasNode(TEST_USER_ID)) {
            users.getNode(TEST_USER_ID).remove();
        }

        session.save();

        super.tearDown();
    }

    @Test
    public void absentMultiValuedPropertyDoesNotMatchPropertyExistsClause() throws RepositoryException {
        build(content, session);

        session.save();

        assertTrue("test user has access to ", userSession.itemExists("/test/doc/doc"));

        final QueryManager queryManager = userSession.getWorkspace().getQueryManager();
        final Query query = queryManager.createQuery("//element(*, hippo:testdocument) order by @jcr:score", Query.XPATH);
        final HippoNodeIterator result = (HippoNodeIterator) query.execute().getNodes();
        assertEquals(1, result.getTotalSize());
    }

    @Test
    public void emptyMultiValuedPropertyMatchesPropertyExistsClause() throws RepositoryException {
        build(content, session);
        session.getNode("/test/doc/doc").setProperty("authtest", new String[]{});

        session.save();

        assertFalse("test user has access to ", userSession.itemExists("/test/doc/doc"));

        final QueryManager queryManager = userSession.getWorkspace().getQueryManager();
        final Query query = queryManager.createQuery("//element(*, hippo:testdocument) order by @jcr:score", Query.XPATH);
        final HippoNodeIterator result = (HippoNodeIterator) query.execute().getNodes();
        assertEquals(0, result.getTotalSize());
    }
}
