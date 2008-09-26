package org.hippoecm.repository;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import junit.framework.TestCase;

import org.hippoecm.repository.api.HippoNodeType;

public class IssueHREPTWO1493Test extends TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final String SYSTEMUSER_ID = "admin";
    private static final char[] SYSTEMUSER_PASSWORD = "admin".toCharArray();

    protected HippoRepository server;
    protected Session session;

    public void setUp() throws Exception {
        server = HippoRepositoryFactory.getHippoRepository();
        session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        if(session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
        }
        session.save();
    }

    public void tearDown() throws Exception {
        session.refresh(false);
        if(session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
        }
        if(session != null) {
            session.logout();
        }
        if (server != null) {
            server.close();
        }
    }

    public void testIssuetest() throws RepositoryException {
        // the test below is the real test
    }
    
    public void xxxIssue() throws RepositoryException {
        session.getRootNode().getNode("hippo:configuration").addMixin("mix:referenceable");
        
        Node node = session.getRootNode().addNode("mirror", HippoNodeType.NT_FACETSELECT);
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("hippo:configuration").getUUID());
        node.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { });
        node.setProperty(HippoNodeType.HIPPO_VALUES, new String[] { });
        node.setProperty(HippoNodeType.HIPPO_MODES, new String[] { });
        session.save();
        
        
        traverse(session.getRootNode().getNode("mirror"));
        
        Session secondSession  = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        Node changeNode = (Node)secondSession.getItem("/hippo:configuration/hippo:documents/embedded");
        changeNode.setProperty("jcr:statement", "changedval");
        secondSession.save();
      
        Session thirdSession = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        Property prop = (Property)thirdSession.getItem("/hippo:configuration/hippo:documents/embedded/jcr:statement");
        Property sameProp = (Property)secondSession.getItem("/hippo:configuration/hippo:documents/embedded/jcr:statement");
 
        String value = prop.getString();
        String sameValue = sameProp.getString();
        
        assertFalse(value.equals(sameValue));
       
        secondSession.logout();
        thirdSession.logout();
    }

    
    protected void traverse(Node node) throws RepositoryException {
      
        for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
            Node child = iter.nextNode();
            if (!child.getPath().equals("/jcr:system"))
                traverse(child);
        }
    }
}
