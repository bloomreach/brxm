package org.hippoecm.repository;

import java.util.Calendar;
import java.net.URL;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.RepositoryException;

import junit.framework.TestCase;

import org.hippoecm.repository.jackrabbit.JarExpander;

public class ClassLoaderTest extends TestCase {

    private final static String SVN_ID = "$Id$";
    private static final String SYSTEMUSER_ID = "admin";
    private static final char[] SYSTEMUSER_PASSWORD = "admin".toCharArray();
    private static final String JAR_FILE_ENTRY = "mock/aDir/anotherFile.txt";

    private HippoRepository server;
    private Session session;
    private HippoRepositoryClassLoader loader;

    public void setUp() throws Exception {
        server = HippoRepositoryFactory.getHippoRepository();
        session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);

        loadRepository();

        loader = new HippoRepositoryClassLoader(session);

    }
   
    public void tearDown() throws Exception {
        if (loader != null) {
            loader.destroy();
        }
        session.getRootNode().getNode("hippo:plugins").remove();
        super.tearDown();
    }

    public void testClassloader() throws Exception {
        URL resource = loader.getResource(JAR_FILE_ENTRY);
        assertNotNull("Resource " + JAR_FILE_ENTRY, resource);
    }

    protected void loadRepository() throws Exception {
        Node root = session.getRootNode();
        Node node = root.addNode("hippo:plugins", "hippo:pluginfolder");
        node = node.addNode("test-plugin", "hippo:plugin");
        node = node.addNode("mock.jar", "nt:resource");

        URL url = getClass().getResource("/mock.jar");
        assertNotNull(url);

        node.setProperty("jcr:mimeType", "application/octet-stream");
        node.setProperty("jcr:data", url.openConnection().getInputStream());
        node.setProperty("jcr:lastModified", Calendar.getInstance());

        JarExpander expander = new JarExpander(node);
        expander.extract();
    }
}
