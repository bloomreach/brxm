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
        final Node users = session.getNode("/" + HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.USERS_PATH);
        final Node user = users.addNode("user", HippoNodeType.NT_USER);
        user.setProperty("hipposys:password", "password");
        session.save();

        final QueryManager queryManager = session.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery("//*[jcr:contains(. ,'password')]", Query.XPATH);
        assertFalse(query.execute().getNodes().hasNext());

        query = queryManager.createQuery("//*[@hipposys:password = 'password']", Query.XPATH);
        assertTrue(query.execute().getNodes().hasNext());
    }

    @Test
    public void testExcludeSingleIndexTerm() throws RepositoryException {
        final Node testNode = session.getRootNode().addNode("test");
        testNode.setProperty("sample:nosingleindexterm", "foo bar");
        final Node subnode = testNode.addNode("subnode");
        subnode.setProperty("sample:nosingleindexterm", "bar foo");
        session.save();

        final QueryManager queryManager = session.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery("//*[jcr:contains(., 'foo')]", Query.XPATH);
        assertTrue(query.execute().getNodes().hasNext());

        query = queryManager.createQuery("//*[jcr:contains(@sample:nosingleindexterm, 'foo')]", Query.XPATH);
        assertTrue(query.execute().getNodes().hasNext());

        query = queryManager.createQuery("//*[@sample:nosingleindexterm = 'foo bar']", Query.XPATH);
        assertTrue(query.execute().getNodes().hasNext());

        query = queryManager.createQuery("//*[@sample:nosingleindexterm NOT ISNULL] order by @sample:nosingleindexterm desc", Query.XPATH);
        final QueryResult descendingResult = query.execute();

        query = queryManager.createQuery("//*[@sample:nosingleindexterm NOT ISNULL] order by @sample:nosingleindexterm asc", Query.XPATH);
        final QueryResult ascendingResult = query.execute();

        // assert that sorting has no effect
        assertTrue(descendingResult.getNodes().nextNode().getPath().equals(ascendingResult.getNodes().nextNode().getPath()));
    }

}
