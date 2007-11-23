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

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.ISO9075Helper;

public class FacetedNavigationIssueTest extends FacetedNavigationAbstractTest {



    public void testFacetIssues() throws RepositoryException {
        Node nodeFromFacet;
        Node node = session.getRootNode().getNode("navigation");
        node = node.addNode("navxy",HippoNodeType.NT_FACETSEARCH);
        node.setProperty(HippoNodeType.HIPPO_QUERYNAME,"navxy");
        node.setProperty(HippoNodeType.HIPPO_DOCBASE,"/documents");
        node.setProperty(HippoNodeType.HIPPO_FACETS,new String[] { "x", "y" });
        session.save();
        
        if (!session.getRootNode().hasNode("documents")) {
            session.getRootNode().addNode("documents");
        }
        node = session.getRootNode().getNode("documents");
        String simple = "testnode";
        String encodeMe = "2..,!@#$%^&*()_-[]{}|\\:;'\".,/?testnode";
        //String encodeMe = "yadida"; 
        Node docNode = node.addNode(simple,HippoNodeType.NT_DOCUMENT);
        docNode.setProperty("x", "success");
        docNode.setProperty("y", encodeMe);
        session.save();
        
        // this fails because org.hippoecm.repository.servicing.ItemDecorator.hashCode(ItemDecorator.java:164)
        // gives a NPE
        String path = "navigation/navxy/success";
        Node facetNode = session.getRootNode().getNode(path);
        assertNotNull(facetNode);
        try {
            facetNode.hashCode(); // <= NPE
            assertEquals(ISO9075Helper.encodeLocalName("success"), facetNode.getName());
            fail("!!!!!!!!!!!!!! ISSUE Resolved !!!!!!!!");
        } catch (NullPointerException e) {
            //e.printStackTrace();
            System.out.println("!!!!!!!!!!!!!! ISSUE HREPTWO-269 STILL OPEN !!!!!!!!");
        }
        
        nodeFromFacet = (Node) facetNode.getNode(HippoNodeType.HIPPO_RESULTSET).getNodes().nextNode();
        assertEquals(docNode, nodeFromFacet);
        
        // this fails, but should work
        try {
            path = "navigation/navxy/success/"+ISO9075Helper.encodeLocalName(encodeMe);
            facetNode = session.getRootNode().getNode(path);
            System.out.println(facetNode.getName());
            //assertEquals(simpleNode, facetNode);
            assertEquals(ISO9075Helper.encodeLocalName(encodeMe), facetNode.getName());
            nodeFromFacet = (Node) facetNode.getNode(HippoNodeType.HIPPO_RESULTSET).getNodes().nextNode();
            assertEquals(docNode, nodeFromFacet);
            fail("!!!!!!!!!!!!!! ISSUE Resolved !!!!!!!!");
        } catch (PathNotFoundException e) {
            //e.printStackTrace();
            System.out.println("!!!!!!!!!!!!!! ISSUE HREPTWO-270 STILL OPEN !!!!!!!!");
        }

        session.refresh(false);
    }

}
