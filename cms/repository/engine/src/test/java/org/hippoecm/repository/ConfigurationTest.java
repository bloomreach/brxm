package org.hippoecm.repository;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.RepositoryException;

import junit.framework.TestCase;

public class ConfigurationTest extends TestCase {

    private final static String SVN_ID = "$Id$";
    private static final String SYSTEMUSER_ID = "admin";
    private static final char[] SYSTEMUSER_PASSWORD = "admin".toCharArray();

    private HippoRepository server;
    private Session session;

    /**
     * Handle atomikos setup and create transaction test node
     */
    public void setUp() throws Exception {
        server = HippoRepositoryFactory.getHippoRepository();
        session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
    }
   
    public void tearDown() throws Exception {
        Node root = session.getRootNode();
        try {
            root.getNode("configtest").remove();
            root.getNode("hippo:configuration/hippo:initialize/testnode").remove();
        } catch (RepositoryException e) {
            // ignore
        }
        session.save();
        session.logout();
        server.close();
    }

    public synchronized void testConfiguration() throws Exception {
        Node root = session.getRootNode();
        Node node = root.addNode("hippo:configuration/hippo:initialize/testnode", "hippo:initializeitem");
        node.setProperty("hippo:content", "configtest.xml");
        node.setProperty("hippo:contentroot", "/configtest");
        session.save();

        // observation manager calls listeners asynchronously  
        wait(1000);

        node = root.getNode("configtest");
        assertNotNull(node.getNode("testnode"));
    }
}
