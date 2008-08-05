/*
 *  Copyright 2008 Hippo.
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

import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import junit.framework.TestCase;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoQuery;

public class HippoQueryTest extends TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final String SYSTEMUSER_ID = "admin";
    private static final char[] SYSTEMUSER_PASSWORD = "admin".toCharArray();

    private HippoRepository server;
    private Session session;
    private QueryManager qmgr;

    public void setUp() throws Exception {
        server = HippoRepositoryFactory.getHippoRepository();
        session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        Node root = session.getRootNode();
        Node doc, node = root.addNode("test");
        node = node.addNode("docs");
        doc = node.addNode("test1");
        doc.setProperty("p","x");
        doc = node.addNode("test2");
        doc.setProperty("p","y");
        session.save();
        qmgr = session.getWorkspace().getQueryManager();
    }

    public void tearDown() throws Exception {
        session.save();
        session.logout();
        server.close();
    }

    public void testStoreAsNodeWithType() throws RepositoryException {
        Query query;
        Node node, parent;

        query = qmgr.createQuery("//node()", Query.XPATH);
        node = query.storeAsNode("/test/query");
        parent = node.getParent();
        parent.save();
        assertTrue(node.isNodeType("nt:query"));
        assertFalse(node.isNodeType(HippoNodeType.NT_QUERY));
        node.remove();
        parent.save();

        query = qmgr.createQuery("//node()", Query.XPATH);
        assertTrue(query instanceof HippoQuery);
        node = ((HippoQuery)query).storeAsNode("/test/query", HippoNodeType.NT_QUERY);
        parent = node.getParent();
        parent.save();
        assertTrue(node.isNodeType("nt:query"));
        assertTrue(node.isNodeType(HippoNodeType.NT_QUERY));
        node.remove();
        parent.save();
    }
}
