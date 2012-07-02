/*
 *  Copyright 2010 Hippo.
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
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import org.hippoecm.repository.api.HippoNodeIterator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class TotalSizeTest extends TestCase {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp(true);
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testTotalSize() throws RepositoryException {
        Session authorSession = server.login("author", "author".toCharArray());
        assertEquals("author", authorSession.getUserID());

        String queryStatement = "SELECT * FROM hipposys:domain ORDER BY jcr:score";
        String queryLanguage = Query.SQL;

        Query adminQuery = session.getWorkspace().getQueryManager().createQuery(queryStatement, queryLanguage);
        QueryResult adminResult = adminQuery.execute();
        NodeIterator adminIterator = adminResult.getNodes();
        assertTrue(adminIterator.getSize() > 0L);
        assertTrue(adminIterator instanceof HippoNodeIterator);
        long adminTotal = ((HippoNodeIterator)adminIterator).getTotalSize();
        int adminCount = 0;
        while(adminIterator.hasNext()) {
            Node node = adminIterator.nextNode();
            if(node != null)
                ++adminCount;
        }
        long adminSize = adminIterator.getSize();
        assertEquals(adminSize, adminTotal);
        assertEquals(adminCount, adminSize);

        Query authorQuery = authorSession.getWorkspace().getQueryManager().createQuery(queryStatement, queryLanguage);
        QueryResult authorResult = authorQuery.execute();
        NodeIterator authorIterator = authorResult.getNodes();
        assertTrue(authorIterator instanceof HippoNodeIterator);
        long authorTotal = ((HippoNodeIterator)authorIterator).getTotalSize();
        int authorCount = 0;
        while(authorIterator.hasNext()) {
            Node node = authorIterator.nextNode();
            if(node != null)
                ++authorCount;
        }
        assertEquals(0L, authorIterator.getSize());
        assertEquals(0L, authorCount);
        assertEquals(0L, authorTotal);
    }
}
