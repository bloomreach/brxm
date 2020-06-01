/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertTrue;

public class OrderByNodeNameTest extends RepositoryTestCase {

    private Node testPath;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        testPath = session.getRootNode().addNode("test");
        for (String name : new String[] { "aa", "aab", "ba", "aa", "aaa", "_aaa", "jcr:aa", "jcr:bb", "jcr:ba", "hippo:ba", "hippo:aa", "hippo:ab" }) {
            testPath.addNode(name);
        }
        session.save();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }
    
    @Test
    public void testSortingNamesXpathDefault() throws RepositoryException {
        String xpath = "/jcr:root/test//* order by @jcr:name";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        String prev = null;
        for (NodeIterator nodeit = queryResult.getNodes(); nodeit.hasNext();) {
            String nodename = nodeit.nextNode().getName();
            if (prev != null) {
                assertTrue(prev.compareTo(nodename) <= 0);
            }
            prev = nodename;
        }
    }

    @Test
    public void testSortingNamesXpathAscending() throws RepositoryException {
        String xpath = "/jcr:root/test//* order by @jcr:name ascending";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        String prev = null;
        for (NodeIterator nodeit = queryResult.getNodes(); nodeit.hasNext();) {
            String nodename = nodeit.nextNode().getName();
            if (prev != null) {
                assertTrue(prev.compareTo(nodename) <= 0);
            }
            prev = nodename;
        }
    }

    @Test
    public void testSortingNamesXpathDescending() throws RepositoryException {
        String xpath = "/jcr:root/test//* order by @jcr:name descending";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        String prev = null;
        for (NodeIterator nodeit = queryResult.getNodes(); nodeit.hasNext();) {
            String nodename = nodeit.nextNode().getName();
            if (prev != null) {
                assertTrue(prev.compareTo(nodename) >= 0);
            }
            prev = nodename;
        }
    }

    @Test
    public void testSortingNamesSqlDefault() throws RepositoryException {
        String sql = "SELECT * FROM nt:base WHERE jcr:path LIKE '/test/%' ORDER BY jcr:name";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(sql, "sql").execute();
        String prev = null;
        for (NodeIterator nodeit = queryResult.getNodes(); nodeit.hasNext();) {
            String nodename = nodeit.nextNode().getName();
            if (prev != null) {
                assertTrue(prev.compareTo(nodename) <= 0);
            }
            prev = nodename;
        }
    }

    @Test
    public void testSortingNamesSqlAsc() throws RepositoryException {
        String sql = "SELECT * FROM nt:base WHERE jcr:path LIKE '/test/%' ORDER BY jcr:name ASC";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(sql, "sql").execute();
        String prev = null;
        for (NodeIterator nodeit = queryResult.getNodes(); nodeit.hasNext();) {
            String nodename = nodeit.nextNode().getName();
            if (prev != null) {
                assertTrue(prev.compareTo(nodename) <= 0);
            }
            prev = nodename;
        }
    }

    @Test
    public void testSortingNamesSqlDesc() throws RepositoryException {
        String sql = "SELECT * FROM nt:base WHERE jcr:path LIKE '/test/%' ORDER BY jcr:name DESC";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(sql, "sql").execute();
        String prev = null;
        for (NodeIterator nodeit = queryResult.getNodes(); nodeit.hasNext();) {
            String nodename = nodeit.nextNode().getName();
            if (prev != null) {
                assertTrue(prev.compareTo(nodename) >= 0);
            }
            prev = nodename;
        }
    }
}
