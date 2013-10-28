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

import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FacetedNavigationNamespaceTest extends RepositoryTestCase {

    private static final long PROP_COUNT = 2;
    private static final long NODE_COUNT = 3;

    private void commonStart() throws RepositoryException {
        Node test = session.getRootNode().addNode("test");
        Node docs = test.addNode("documents");
        Node nav = test.addNode("navigation");
        createDocuments(docs);
        session.save();
        session.refresh(false);
        createNavigation(nav);
        session.save();
        session.refresh(false);
    }
    
    public void createDocuments(Node docNode) throws RepositoryException {
        Node normalNode = docNode.addNode("normal", "hippo:handle");
        Node namespaceNode = docNode.addNode("namespace", "hippo:handle");
        Node bothNode = docNode.addNode("both", "hippo:handle");
        normalNode.addMixin("hippo:hardhandle");
        namespaceNode.addMixin("hippo:hardhandle");
        bothNode.addMixin("hippo:hardhandle");
        Node node;

        for (int j = 0; j < PROP_COUNT; j++) {
            for (int i = 0; i < NODE_COUNT; i++) {
                node = normalNode.addNode("docNormal" + i, "hippo:testdocument");
                node.addMixin("mix:versionable");
                node.setProperty("facettest", "val" + j);

                node = namespaceNode.addNode("docNamespace" + i, "hippo:testdocument");
                node.addMixin("mix:versionable");
                node.setProperty("hippo:facettest", "val" + j);

                node = bothNode.addNode("docBoth" + i, "hippo:testdocument");
                node.addMixin("mix:versionable");
                node.setProperty("hippo:facettest", "val" + j);
                node.setProperty("facettest", "val" + j);
            }
        }
    }

    public void createNavigation(Node navNode) throws RepositoryException {
        Node node;

        // search without namespace
        node = navNode.addNode("normalsearch", HippoNodeType.NT_FACETSEARCH);
        node.setProperty(HippoNodeType.HIPPO_QUERYNAME, "normalsearch");
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents/normal").getIdentifier());
        node.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { "facettest" });

        // search with namespace
        node = navNode.addNode("namespacesearch", HippoNodeType.NT_FACETSEARCH);
        node.setProperty(HippoNodeType.HIPPO_QUERYNAME, "normalsearch");
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents/namespace").getIdentifier());
        node.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { "hippo:facettest" });

        // search both
        node = navNode.addNode("bothsearch", HippoNodeType.NT_FACETSEARCH);
        node.setProperty(HippoNodeType.HIPPO_QUERYNAME, "bothsearch");
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents/both").getIdentifier());
        node.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { "hippo:facettest" });


        // select without namespace
        node = navNode.addNode("normalselect", HippoNodeType.NT_FACETSELECT);
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents/normal").getIdentifier());
        node.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { "facettest" });
        node.setProperty(HippoNodeType.HIPPO_VALUES, new String[] { "val0" });
        node.setProperty(HippoNodeType.HIPPO_MODES, new String[] { "stick" });

        // select with namespace
        node = navNode.addNode("namespaceselect", HippoNodeType.NT_FACETSELECT);
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents/namespace").getIdentifier());
        node.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { "hippo:facettest" });
        node.setProperty(HippoNodeType.HIPPO_VALUES, new String[] { "val0" });
        node.setProperty(HippoNodeType.HIPPO_MODES, new String[] { "stick" });

        // select with both
        node = navNode.addNode("bothselect", HippoNodeType.NT_FACETSELECT);
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents/both").getIdentifier());
        node.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { "hippo:facettest" });
        node.setProperty(HippoNodeType.HIPPO_VALUES, new String[] { "val0" });
        node.setProperty(HippoNodeType.HIPPO_MODES, new String[] { "stick" });
    }

    @Test
    public void testFacetSearchWithoutNamespace() throws RepositoryException {
        commonStart();
        Node node = session.getRootNode().getNode("test/navigation/normalsearch");
        for (int j = 0; j < PROP_COUNT; j++) {
            assertTrue(node.hasNode("val" + j));
            assertTrue(node.getNode("val" + j).hasProperty(HippoNodeType.HIPPO_COUNT));
            assertEquals(NODE_COUNT, node.getNode("val" + j).getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        }
    }

    @Test
    public void testFacetSearchWithNamespace() throws RepositoryException {
        commonStart();
        Node node = session.getRootNode().getNode("test/navigation/namespacesearch");
        for (int j = 0; j < PROP_COUNT; j++) {
            assertTrue(node.hasNode("val" + j));
            assertTrue(node.getNode("val" + j).hasProperty(HippoNodeType.HIPPO_COUNT));
            assertEquals(NODE_COUNT, node.getNode("val" + j).getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        }
    }

    @Test
    public void testFacetSearchWithBoth() throws RepositoryException {
        commonStart();
        Node node = session.getRootNode().getNode("test/navigation/bothsearch");
        for (int j = 0; j < PROP_COUNT; j++) {
            assertTrue(node.hasNode("val" + j));
            assertTrue(node.getNode("val" + j).hasProperty(HippoNodeType.HIPPO_COUNT));
            assertEquals(NODE_COUNT, node.getNode("val" + j).getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        }
    }

    @Test
    public void testFacetSelectWithoutNamespace() throws RepositoryException {
        commonStart();
        Node node = session.getRootNode().getNode("test/navigation/normalselect");
        NodeIterator iter = node.getNodes();
        assertEquals(NODE_COUNT, iter.getSize());
        for (int j = 0; j < NODE_COUNT; j++) {
            assertTrue(node.hasNode("docNormal" + j));
        }
    }

    @Test
    public void testFacetSelectWithNamespace() throws RepositoryException {
        commonStart();
        Node node = session.getRootNode().getNode("test/navigation/namespaceselect");
        NodeIterator iter = node.getNodes();
        assertEquals(NODE_COUNT, iter.getSize());
        for (int j = 0; j < NODE_COUNT; j++) {
            assertTrue(node.hasNode("docNamespace" + j));
        }
    }

    @Test
    public void testFacetSelectWithBoth() throws RepositoryException {
        commonStart();
        Node node = session.getRootNode().getNode("test/navigation/bothselect");
        NodeIterator iter = node.getNodes();
        assertEquals(NODE_COUNT, iter.getSize());
        for (int j = 0; j < NODE_COUNT; j++) {
            assertTrue(node.hasNode("docBoth" + j));
        }
    }
}
