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

import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static junit.framework.Assert.assertTrue;

public class ServicingNodeIndexerSimilarityTest extends RepositoryTestCase {

    @Test
    public void testSimilaritySearches() throws RepositoryException, InterruptedException {


        String testNodeName = ServicingNodeIndexerSimilarityTest.class.getSimpleName() + "-test";
        try {

            /**
             * For similarity to work, you need to meet some criteria. The criteria are:
             *
             * Only terms with at least 4 characters are considered.
             * Only terms that occur at least 2 times in the source node are considered.
             * Only terms that occur in at least 5 nodes are considered.
             *
             *
             * hence we create 5 nodes, where the first one contains 'quick quick' : thus two times quick
             */


            final Node testNode = session.getRootNode().addNode(testNodeName);
            Node sub1 = testNode.addNode("sub1");
            sub1.setProperty("sample:title", "the quick quick brown fox jumps over the lazy dog");

            Node sub2 = testNode.addNode("sub2");
            sub2.setProperty("sample:title", "the quick  brown fox jumps over the lazy dog again");

            Node sub3 = testNode.addNode("sub3");
            sub3.setProperty("sample:title", "the quick  brown fox jumps over the lazy dog again for the third time");


            Node sub4 = testNode.addNode("sub4");
            sub4.setProperty("sample:title", "the quick  brown fox jumps over the lazy dog again for the fourth time");

            Node sub5 = testNode.addNode("sub5");
            sub5.setProperty("sample:title", "the quick brown fox jumps over the lazy dog again for the fifth time");

            session.save();


            final QueryManager queryManager = session.getWorkspace().getQueryManager();

            // get documents similar to sub1 : sub1 has the term 'quick' twice : that is a 'source' criteria, see rules above
            StringBuilder statement = new StringBuilder("//*").append("[rep:similar(., '").append(sub1.getPath()).append("')] order by @jcr:score descending");
            Query query = queryManager.createQuery(statement.toString(), Query.XPATH);

            assertTrue("All documents including sub1 should be similar to sub1 ",query.execute().getNodes().getSize() == 5);

            // get documents similar to sub2 : sub2 has the term 'quick' only ONCE : 'source' criteria, see rules above, are not met. Hence, we expect no similar docs
            statement = new StringBuilder("//*").append("[rep:similar(., '").append(sub2.getPath()).append("')] order by @jcr:score descending");
            query = queryManager.createQuery(statement.toString(), Query.XPATH);

            assertTrue("No documents including sub2 should be similar to sub2 since 2 times term criteria not met ",query.execute().getNodes().getSize() == 0);

        } finally {
            if (session.getRootNode().hasNode(testNodeName)) {
                session.getRootNode().getNode(testNodeName).remove();
                session.save();
            }
        }

    }


}
