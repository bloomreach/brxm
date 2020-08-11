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

import java.io.IOException;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FacetedNavigationPerfTestCase extends FacetedNavigationAbstractTest {


    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }
    
    @Test
    public void testPerformance() throws RepositoryException, IOException {
        int[] numberOfNodesInTests = new int[] { 50000 };
        for (int i = 0; i < numberOfNodesInTests.length; i++) {
            commonStart(numberOfNodesInTests[i]);
            for (int j = 0; j < 10; j++) {
                Node doc = getRandomDocNode();
                doc.setProperty("other", "z");
                session.save();
                session.refresh(false);

                Node node = getSearchNode();
                long count, tBefore, tAfter;
                tBefore = System.currentTimeMillis();
                node = node.getNode("x1");
                node = node.getNode("y2");
                node = node.getNode("z2");
                node = node.getNode(HippoNodeType.HIPPO_RESULTSET);
                count = node.getProperty(HippoNodeType.HIPPO_COUNT).getLong();
                tAfter = System.currentTimeMillis();
                System.out.println("FacetedNavigationPerfTest" + numberOfNodesInTests[i] + " " + Long.toString(tAfter - tBefore) + "ms");
            }
        }
        commonEnd();
    }

    @Test
    public void testFullFacetedNavigationTraversal() throws RepositoryException, IOException {
        long tBefore, tAfter;
        commonStart(500);
        Node node = getSearchNode();

        tBefore = System.currentTimeMillis();
        facetedNavigationNodeTraversal(node, 1, node.getDepth() + 10);
        tAfter = System.currentTimeMillis();

        System.out.println("FullFacetedNavigationTraversal" + 500 + " "+ Long.toString(tAfter - tBefore) + "ms");
        commonEnd();
    }

    private void facetedNavigationNodeTraversal(Node node, int indent, int depth) throws RepositoryException {
        String s = "                                   ";
        Iterator nodeIterator = node.getNodes();
        while(nodeIterator.hasNext()){
            Node childNode = (Node)nodeIterator.next();
            if (childNode.isNodeType(HippoNodeType.NT_FACETSUBSEARCH)) {
                if (this.getVerbose()) {
                        System.out.println(s.substring(0, Math.min(indent,s.length())) + childNode.getName() + " ("+childNode.getProperty("hippo:count").getString() +")");
                }
                if(childNode.getDepth() <= depth ) {
                    facetedNavigationNodeTraversal(childNode, indent + 6, depth);
                }
            }
        }
    }
}
