/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

import org.hippoecm.repository.api.HippoQuery;

public class HippoQueryTest extends TestCase {

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

    public void testMangle() throws RepositoryException {
        Query query;
        Node node;

        query = qmgr.createQuery("foo//bar$x/qux", Query.XPATH);
        node = query.storeAsNode("/test/query");
        assertEquals("foo//barMAGICxCIGAM/qux",node.getProperty("jcr:statement").getString());
        node.remove();

        query = qmgr.createQuery("foo//bar[@x='$']/qux", Query.XPATH);
        node = query.storeAsNode("/test/query");
        assertEquals("foo//bar[@x='$']/qux",node.getProperty("jcr:statement").getString());
        node.remove();
    }

    public void testSimple() throws RepositoryException {
        Query query = qmgr.createQuery("test//$which[p='x']", Query.XPATH);
        query.storeAsNode("/test/query");
        Node queryNode = session.getRootNode().getNode("test/query");
        query = qmgr.getQuery(queryNode);
        HippoQuery advancedQuery = (HippoQuery) query;

        assertEquals(1, advancedQuery.getArgumentCount());
        String[] queryArguments = advancedQuery.getArguments();
        assertNotNull(queryArguments);
        assertEquals(1, queryArguments.length);
        assertEquals("which", queryArguments[0]);
        Map<String,String> arguments = new TreeMap<String,String>();
        arguments.put("which","test1");
        QueryResult result = advancedQuery.execute(arguments);
        int count = 0;
        for(NodeIterator iter = result.getNodes(); iter.hasNext(); ) {
            ++count;
            Node node = iter.nextNode();
            assertEquals("/test/docs/test1", node.getPath());
        }
        assertEquals(1, count);
    }
}
