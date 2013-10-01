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
package org.onehippo.repository.search;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class ServicingNodeIndexerTest extends RepositoryTestCase {


    @Test
    public void testExcludeFromNodeScope() throws RepositoryException {

        String testUserNodeName = ServicingNodeIndexerTest.class.getSimpleName() + "-user";
        final Node users = session.getNode("/" + HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.USERS_PATH);
        try {
            final Node user = users.addNode(testUserNodeName, HippoNodeType.NT_USER);
            user.setProperty("hipposys:password", "password");
            session.save();
            final QueryManager queryManager = session.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery("//*[jcr:contains(. ,'password')]", Query.XPATH);

            assertFalse(query.execute().getNodes().hasNext());

            query = queryManager.createQuery("//*[@hipposys:password = 'password']", Query.XPATH);
            assertTrue(query.execute().getNodes().hasNext());
        } finally {
            if (users.hasNode(testUserNodeName)) {
                users.getNode(testUserNodeName).remove();
                session.save();
            }
        }
    }


    @Test
    public void testNotSpecialProperty() throws RepositoryException {
        String testNodeName = ServicingNodeIndexerTest.class.getSimpleName() + "-test";
        try {
            final Node testNode = session.getRootNode().addNode(testNodeName);
            Node sub1 = testNode.addNode("sub1");
            sub1.setProperty("sample:normal", "the quick brown fox jumps over the lazy dog");
            Node sub2 = testNode.addNode("sub2");
            sub2.setProperty("sample:normal", "aaa bbbb");
            Node sub3 = testNode.addNode("sub3");
            sub3.setProperty("sample:normal", "zzz cccc");
            session.save();

            final QueryManager queryManager = session.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery("//*[jcr:contains(., 'brown fox jumps')]", Query.XPATH);
            assertTrue(query.execute().getNodes().hasNext());

            query = queryManager.createQuery("//*[jcr:contains(@sample:normal, 'brown fox jumps')]", Query.XPATH);
            assertTrue(query.execute().getNodes().hasNext());

            query = queryManager.createQuery("//*[@sample:normal = 'quick brown']", Query.XPATH);
            assertFalse(query.execute().getNodes().hasNext());

            query = queryManager.createQuery("//*[@sample:normal = 'the quick brown fox jumps over the lazy dog']", Query.XPATH);
            assertTrue(query.execute().getNodes().hasNext());

            query = queryManager.createQuery("//*[@sample:normal NOT ISNULL] order by @sample:normal descending", Query.XPATH);
            final QueryResult descendingResult = query.execute();

            query = queryManager.createQuery("//*[@sample:normal NOT ISNULL] order by @sample:normal ascending", Query.XPATH);
            final QueryResult ascendingResult = query.execute();

            assertTrue(descendingResult.getNodes().getSize() == 3);
            assertTrue(ascendingResult.getNodes().getSize() == 3);


            // sub2 starts with aaa
            assertTrue(ascendingResult.getNodes().nextNode().getName().equals("sub2"));

            // sub3 starts with zzz
            assertTrue(descendingResult.getNodes().nextNode().getName().equals("sub3"));

        } finally {
            if (session.getRootNode().hasNode(testNodeName)) {
                session.getRootNode().getNode(testNodeName).remove();
                session.save();
            }
        }

    }

    @Test
    public void testExcludeSingleIndexTermCanQueryContains() throws RepositoryException {
        String testNodeName = ServicingNodeIndexerTest.class.getSimpleName() + "-test";
        try {
            final Node testNode = session.getRootNode().addNode(testNodeName);
            testNode.setProperty("sample:nosingleindexterm", "foo bar");
            session.save();

            final QueryManager queryManager = session.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery("//*[jcr:contains(., 'foo')]", Query.XPATH);
            assertTrue(query.execute().getNodes().hasNext());

            query = queryManager.createQuery("//*[jcr:contains(@sample:nosingleindexterm, 'foo')]", Query.XPATH);
            assertTrue(query.execute().getNodes().hasNext());

        } finally {
            if (session.getRootNode().hasNode(testNodeName)) {
                session.getRootNode().getNode(testNodeName).remove();
                session.save();
            }
        }
    }

    @Test
    public void testExcludeSingleIndexTermCannotQueryPropEquals() throws RepositoryException {
        String testNodeName = ServicingNodeIndexerTest.class.getSimpleName() + "-test";
        try {
            final Node testNode = session.getRootNode().addNode(testNodeName);
            testNode.setProperty("sample:nosingleindexterm", "foo bar");
            session.save();

            final QueryManager queryManager = session.getWorkspace().getQueryManager();

            Query query = queryManager.createQuery("//*[@sample:nosingleindexterm = 'foo bar']", Query.XPATH);
            assertFalse(query.execute().getNodes().hasNext());
        } finally {
            if (session.getRootNode().hasNode(testNodeName)) {
                session.getRootNode().getNode(testNodeName).remove();
                session.save();
            }
        }
    }

    @Test
    public void testExcludeSingleIndexTermCannotOrderOnProp() throws RepositoryException {
        String testNodeName = ServicingNodeIndexerTest.class.getSimpleName() + "-test";
        try {
            final Node testNode = session.getRootNode().addNode(testNodeName);
            Node sub1 = testNode.addNode("sub1");
            sub1.setProperty("sample:nosingleindexterm", "the quick brown fox jumps over the lazy dog");
            sub1.setProperty("sample:normal", "the quick brown fox jumps over the lazy dog");
            Node sub2 = testNode.addNode("sub2");
            sub2.setProperty("sample:nosingleindexterm", "aaa bbbb");
            sub2.setProperty("sample:normal", "aaa bbbb");
            Node sub3 = testNode.addNode("sub3");
            sub3.setProperty("sample:nosingleindexterm", "zzz cccc");
            sub3.setProperty("sample:normal", "zzz cccc");
            session.save();

            final QueryManager queryManager = session.getWorkspace().getQueryManager();

            Query query = queryManager.createQuery("//*[@sample:normal NOT ISNULL] order by @sample:normal descending", Query.XPATH);
            final QueryResult descendingResultCanOrderOn = query.execute();


            query = queryManager.createQuery("//*[@sample:nosingleindexterm NOT ISNULL] order by @sample:nosingleindexterm descending", Query.XPATH);
            final QueryResult descendingResultCannotOrderOn = query.execute();

            query = queryManager.createQuery("//*[@sample:normal NOT ISNULL] order by @sample:normal ascending", Query.XPATH);
            final QueryResult ascendingResultCanOrderOn = query.execute();

            query = queryManager.createQuery("//*[@sample:nosingleindexterm NOT ISNULL] order by @sample:nosingleindexterm ascending", Query.XPATH);
            final QueryResult ascendingResultCannotOrderOn = query.execute();

            // on @sample:normal we *CAN* order. On '@sample:nosingleindexterm' the ordering cannot take place as there is term
            // indexed to do the sorting on

            // correct ordering
            assertFalse(descendingResultCanOrderOn.getNodes().nextNode().getPath().equals(ascendingResultCanOrderOn.getNodes().nextNode().getPath()));

            // no ordering
            assertTrue(descendingResultCannotOrderOn.getNodes().nextNode().getPath().equals(ascendingResultCannotOrderOn.getNodes().nextNode().getPath()));

        } finally {
            if (session.getRootNode().hasNode(testNodeName)) {
                session.getRootNode().getNode(testNodeName).remove();
                session.save();
            }
        }
    }

}
