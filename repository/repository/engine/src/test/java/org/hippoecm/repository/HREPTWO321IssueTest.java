package org.hippoecm.repository;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.FacetedNavigationAbstractTest.Document;
import org.hippoecm.repository.api.HippoNodeType;

public class HREPTWO321IssueTest extends FacetedNavigationAbstractTest {

    private static final String SYSTEMUSER_ID = "admin";
    private static final char[] SYSTEMUSER_PASSWORD = "admin".toCharArray();


    public void testCounts() throws RepositoryException, IOException {
        numDocs = 500;
        commonStart();
        check("/navigation/xyz/x1", 1, 0, 0);
        commonEnd();
    }
    
    protected void check(String facetPath, int x, int y, int z)
    throws RepositoryException {
    int realCount = -1;
    Node node = session.getRootNode();
    if(facetPath.startsWith("/"))
        facetPath = facetPath.substring(1); // skip the initial slash
    String[] pathElements = facetPath.split("/");
    
    try {
        for(int i=0; i<pathElements.length; i++) {
            node = node.getNode(pathElements[i]);
        }
        Node nodeResultSet = node.getNode(HippoNodeType.HIPPO_RESULTSET);
        NodeIterator iter = nodeResultSet.getNodes();
        realCount = 0;
        while(iter.hasNext()) {
            iter.next();
            ++realCount;
        }
        if(node.hasProperty(HippoNodeType.HIPPO_COUNT)) {
            long obtainedCount = (int) node.getProperty(HippoNodeType.HIPPO_COUNT).getLong();
            if(realCount != obtainedCount){
                assertEquals("Issue is NOT resolved" , 1, 1);
            } else {
                assertEquals("Issue is resolved is fails!!" , 1, 2);
            }
            
        }
    } catch(PathNotFoundException ex) {
        System.err.println("PathNotFoundException: "+ex.getMessage());
        ex.printStackTrace(System.err);
        realCount = 0;
      }
    }
}
