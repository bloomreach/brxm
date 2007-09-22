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

import java.io.IOException;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hippoecm.repository.api.HippoNodeType;

import org.hippoecm.testutils.history.HistoryWriter;

public class FacetedNavigationPerfTest extends FacetedNavigationAbstractTest {
    private static HistoryWriter historyWriter;

    public static Test suite() {
        TestSuite suite = new TestSuite(FacetedNavigationPerfTest.class);
        historyWriter = new HistoryWriter(suite);
        return historyWriter;
    }

    public FacetedNavigationPerfTest() throws RepositoryException {
        super();
    }

    public void testPerformance() throws RepositoryException, IOException {
        int[] numberOfNodesInTests = new int[] { 500 };
        for (int i = 0; i < numberOfNodesInTests.length; i++) {
            numDocs = numberOfNodesInTests[i];
            Node node = commonStart();
            long count, tBefore, tAfter;
            tBefore = System.currentTimeMillis();
            count = node.getNode("x1").getNode("y2").getNode("z2").getNode(HippoNodeType.HIPPO_RESULTSET)
                    .getProperty(HippoNodeType.HIPPO_COUNT).getLong();
            tAfter = System.currentTimeMillis();
            historyWriter.write("FacetedNavigationPerfTest" + numDocs, Long.toString(tAfter - tBefore), "ms");
        }
        commonEnd();
    }
    
    public void testFullFacetedNavigationTraversal() throws RepositoryException, IOException {
        numDocs = 0;
        long tBefore, tAfter;
        
        Node node = commonStart();
        
        tBefore = System.currentTimeMillis();
        facetedNavigationNodeTraversal(node,1 , node.getDepth() + 10);
        tAfter = System.currentTimeMillis();

        historyWriter.write("FullFacetedNavigationTraversal" + numDocs, Long.toString(tAfter - tBefore), "ms");
        commonEnd();
    }

    private void facetedNavigationNodeTraversal(Node node, int indent, int depth) throws RepositoryException {
        String s = "                                   ";
        Iterator nodeIterator = node.getNodes();
        while(nodeIterator.hasNext()){
            Node childNode = (Node)nodeIterator.next();
            if( childNode.hasProperty("hippo:count") && !childNode.getName().equals(HippoNodeType.HIPPO_RESULTSET)) {
                if(this.getVerbose()){
                    System.out.println(s.substring(0, Math.min(indent,s.length())) + childNode.getName() + " ("+childNode.getProperty("hippo:count").getString() +")");
                }
            }
            if(childNode.getDepth() <= depth && !childNode.getName().equals(HippoNodeType.HIPPO_RESULTSET)){
                facetedNavigationNodeTraversal(childNode, indent + 6, depth);
            } 
        }
    }
    
}
