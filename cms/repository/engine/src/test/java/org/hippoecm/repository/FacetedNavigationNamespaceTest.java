package org.hippoecm.repository;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.NodeId;
import org.hippoecm.repository.api.HippoNodeType;

public class FacetedNavigationNamespaceTest extends FacetedNavigationAbstractTest {

    private final static int PROP_COUNT = 2;
    private final static int NODE_COUNT = 3;

    protected void commonSetup() throws RepositoryException {
        createDocuments();
        createNavigation();
        session.save();
        session.refresh(false);
    }
    
    public void createDocuments() throws RepositoryException {
        if (!session.getRootNode().hasNode("documents")) {
            session.getRootNode().addNode("documents");
        }
        Node docNode = session.getRootNode().getNode("documents");
        Node normalNode = docNode.addNode("normal");
        Node namespaceNode = docNode.addNode("namespace");
        Node bothNode = docNode.addNode("both");
        Node node;

        for (int j = 0; j < PROP_COUNT; j++) {
            for (int i = 0; i < NODE_COUNT; i++) {
                node = normalNode.addNode("docNormal" + i, HippoNodeType.NT_DOCUMENT);
                node.setProperty("facettest", "val" + j); 

                node = namespaceNode.addNode("docNamespace" + i, HippoNodeType.NT_DOCUMENT);
                node.setProperty("hippo:facettest", "val" + j);

                node = bothNode.addNode("docBoth" + i, HippoNodeType.NT_DOCUMENT);
                node.setProperty("hippo:facettest", "val" + j);
                node.setProperty("facettest", "val" + j); 
            }
        }
    }
    
    public void createNavigation() throws RepositoryException {
        if (!session.getRootNode().hasNode("navigation")) {
            session.getRootNode().addNode("navigation");
        }
        Node navNode = session.getRootNode().getNode("navigation");
        Node node;
        
        // search without namespace
        node = navNode.addNode("normalsearch", HippoNodeType.NT_FACETSEARCH);
        node.setProperty(HippoNodeType.HIPPO_QUERYNAME, "normalsearch");
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, "/documents/normal");
        node.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { "facettest" });

        // search with namespace
        node = navNode.addNode("namespacesearch", HippoNodeType.NT_FACETSEARCH);
        node.setProperty(HippoNodeType.HIPPO_QUERYNAME, "normalsearch");
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, "/documents/namespace");
        node.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { "hippo:facettest" });
        
        // search both
        node = navNode.addNode("bothsearch", HippoNodeType.NT_FACETSEARCH);
        node.setProperty(HippoNodeType.HIPPO_QUERYNAME, "bothsearch");
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, "/documents/both");
        node.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { "hippo:facettest" });
        

        // select without namespace
        node = navNode.addNode("normalselect", HippoNodeType.NT_FACETSELECT);
        node.setProperty(HippoNodeType.HIPPO_MODES, "stick");
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, "/documents/normal");
        node.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { "facettest" });
        node.setProperty(HippoNodeType.HIPPO_VALUES, new String[] { "val0" });

        // select with namespace
        node = navNode.addNode("namespaceselect", HippoNodeType.NT_FACETSELECT);
        node.setProperty(HippoNodeType.HIPPO_MODES, "stick");
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, "/documents/namespace");
        node.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { "hippo:facettest" });
        node.setProperty(HippoNodeType.HIPPO_VALUES, new String[] { "val0" });
        
        // select with both
        node = navNode.addNode("bothselect", HippoNodeType.NT_FACETSELECT);
        node.setProperty(HippoNodeType.HIPPO_MODES, "stick");
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, "/documents/both");
        node.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { "hippo:facettest" });
        node.setProperty(HippoNodeType.HIPPO_VALUES, new String[] { "val0" });
    }
    
    public void testFacetSearchWithoutNamespace() throws RepositoryException {
        commonSetup();
        Node node = session.getRootNode().getNode("navigation/normalsearch");
        for (int j = 0; j < PROP_COUNT; j++) {
            assertTrue(node.hasNode("val" + j));
            assertTrue(node.getNode("val" + j).hasProperty(HippoNodeType.HIPPO_COUNT));
            assertEquals( (double) NODE_COUNT, node.getNode("val" + j).getProperty(HippoNodeType.HIPPO_COUNT).getDouble());
        }
    }

    public void testFacetSearchWithNamespace() throws RepositoryException {
        commonSetup();
        Node node = session.getRootNode().getNode("navigation/namespacesearch");
        for (int j = 0; j < PROP_COUNT; j++) {
            assertTrue(node.hasNode("val" + j));
            assertTrue(node.getNode("val" + j).hasProperty(HippoNodeType.HIPPO_COUNT));
            assertEquals( (double) NODE_COUNT, node.getNode("val" + j).getProperty(HippoNodeType.HIPPO_COUNT).getDouble());
        }
    }
    public void testFacetSearchWithBoth() throws RepositoryException {
        commonSetup();
        Node node = session.getRootNode().getNode("navigation/bothsearch");
        for (int j = 0; j < PROP_COUNT; j++) {
            assertTrue(node.hasNode("val" + j));
            assertTrue(node.getNode("val" + j).hasProperty(HippoNodeType.HIPPO_COUNT));
            assertEquals( (double) NODE_COUNT, node.getNode("val" + j).getProperty(HippoNodeType.HIPPO_COUNT).getDouble());
        }
    }


    public void testFacetSelectWithoutNamespace() throws RepositoryException {
        commonSetup();
        Node node = session.getRootNode().getNode("navigation/normalselect");
        NodeIterator iter = node.getNodes();
        assertEquals( (long) NODE_COUNT, iter.getSize());
        for (int j = 0; j < NODE_COUNT; j++) {
            assertTrue(node.hasNode("docNormal" + j));
        }
    }

    public void testFacetSelectWithNamespace() throws RepositoryException {
        commonSetup();
        Node node = session.getRootNode().getNode("navigation/namespaceselect");
        NodeIterator iter = node.getNodes();
        assertEquals( (long) NODE_COUNT, iter.getSize());
        for (int j = 0; j < NODE_COUNT; j++) {
            assertTrue(node.hasNode("docNamespace" + j));
        }
    }

    public void testFacetSelectWithBoth() throws RepositoryException {
        commonSetup();
        Node node = session.getRootNode().getNode("navigation/bothselect");
        NodeIterator iter = node.getNodes();
        assertEquals( (long) NODE_COUNT, iter.getSize());
        for (int j = 0; j < NODE_COUNT; j++) {
            assertTrue(node.hasNode("docBoth" + j));
        }
    }
}
