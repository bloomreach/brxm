package org.hippoecm.repository;

import java.io.IOException;
import java.util.StringTokenizer;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import junit.framework.TestCase;

import org.apache.jackrabbit.core.security.AccessManager;
import org.hippoecm.repository.api.HippoNodeType;


public class FacetedAuthorizationTest extends TestCase {
    

    private HippoRepository server;
    private Session serverSession;
    private Session userSession;
    private Node users;
    private Node docNode;
    private static final String SYSTEMUSER_ID = "admin";
    private static final char[] SYSTEMUSER_PASSWORD = "admin".toCharArray();

    private static final String USERS_PATH = "hippo:configuration/hippo:users";

    private static final String TESTUSER_ID = "testuser";
    private static final String TESTUSER_PASS = "testpass";
    private static final String READAUTH = "readfacet";
    private static final String READAUTH_VAL = "readme";
    private static final String WRITEAUTH = "writefacet";
    private static final String WRITEAUTH_VAL = "writeme";

    private final static int NODE_COUNT = 2;
    
    public void setUp() throws RepositoryException, IOException {
        server = HippoRepositoryFactory.getHippoRepository();
        serverSession = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);

        // create user config path
        Node node = serverSession.getRootNode();
        StringTokenizer tokenizer = new StringTokenizer(USERS_PATH, "/");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            node = node.addNode(token);
        }
        
        // create test user
        users = serverSession.getRootNode().getNode(USERS_PATH);
        Node testuser = users.addNode(TESTUSER_ID, HippoNodeType.NT_USER);
        testuser.setProperty(HippoNodeType.HIPPO_PASSWORD, TESTUSER_PASS);

        // create facetAuth read
        Node authFolder = testuser.addNode(HippoNodeType.FACETAUTH_PATH, HippoNodeType.NT_FACETAUTHFOLDER);
        Node facetAuth = authFolder.addNode(READAUTH, HippoNodeType.NT_FACETAUTH);
        facetAuth.setProperty(HippoNodeType.HIPPO_FACET, READAUTH);
        facetAuth.setProperty(HippoNodeType.HIPPO_PERMISSIONS, AccessManager.READ);
        facetAuth.setProperty(HippoNodeType.HIPPO_VALUES, new String[]{READAUTH_VAL});
        
        // create facetAuth read + write
        facetAuth = authFolder.addNode(WRITEAUTH, HippoNodeType.NT_FACETAUTH);
        facetAuth.setProperty(HippoNodeType.HIPPO_FACET, WRITEAUTH);
        facetAuth.setProperty(HippoNodeType.HIPPO_PERMISSIONS, (AccessManager.READ + AccessManager.WRITE));
        facetAuth.setProperty(HippoNodeType.HIPPO_VALUES, new String[]{WRITEAUTH_VAL});

        
        if (!serverSession.getRootNode().hasNode("documents")) {
            serverSession.getRootNode().addNode("documents");
        }
        Node dnode = serverSession.getRootNode().getNode("documents");

        for (int i = 0; i < NODE_COUNT; i++) {
            node = dnode.addNode("readdoc" + i, HippoNodeType.NT_DOCUMENT);
            node.setProperty(READAUTH, READAUTH_VAL); 
            node.setProperty(WRITEAUTH, "nope");
            node = node.addNode("subnode", HippoNodeType.NT_REQUEST);
            node.setProperty("subval", "value");

            node = dnode.addNode("writedoc" + i, HippoNodeType.NT_DOCUMENT);
            node.setProperty(READAUTH, "nope"); 
            node.setProperty(WRITEAUTH, WRITEAUTH_VAL);
            node = node.addNode("subnode",  HippoNodeType.NT_REQUEST);
            node.setProperty("subval", "value");

            node = dnode.addNode("nonedoc" + i, HippoNodeType.NT_DOCUMENT);
            node.setProperty(READAUTH, "nope"); 
            node.setProperty(WRITEAUTH, "none"); 
            node = node.addNode("subnode",HippoNodeType.NT_REQUEST);
            node.setProperty("subval", "value");
        }
        


        // setup faceted navigation nodes
        if (!serverSession.getRootNode().hasNode("navigation")) {
            serverSession.getRootNode().addNode("navigation");
        }
        Node navNode = serverSession.getRootNode().getNode("navigation");
        
        // search without namespace
        node = navNode.addNode("search", HippoNodeType.NT_FACETSEARCH);
        node.setProperty(HippoNodeType.HIPPO_QUERYNAME, "search");
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, "/documents");
        node.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { READAUTH });
        

        // select without namespace
        node = navNode.addNode("select", HippoNodeType.NT_FACETSELECT);
        node.setProperty(HippoNodeType.HIPPO_MODES, new String[] { "stick" });
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, "/documents");
        //node.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { READAUTH, WRITEAUTH });
        //node.setProperty(HippoNodeType.HIPPO_VALUES, new String[] { READAUTH_VAL, WRITEAUTH_VAL });
        node.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { READAUTH });
        node.setProperty(HippoNodeType.HIPPO_VALUES, new String[] { READAUTH_VAL });
        
        serverSession.save();

        userSession = server.login(TESTUSER_ID, TESTUSER_PASS.toCharArray());
        docNode = userSession.getRootNode().getNode("documents");
    }
    
    public void tearDown() throws RepositoryException {
        if (userSession != null) {
            userSession.logout();
        }
        if (users.hasNode(TESTUSER_ID)) {
            users.getNode(TESTUSER_ID).remove();
        }
        if (serverSession != null) {
            serverSession.save();
            serverSession.logout();
        }
        if (server != null) {
            server.close();
        }
    }
    
    
    public void testReadsNotAllowed() throws RepositoryException {
        NodeIterator iter = docNode.getNodes();
        assertEquals((long) (2 * NODE_COUNT), iter.getSize());
        assertFalse(docNode.hasNode("nonedoc0"));
        assertFalse(docNode.hasNode("nonedoc0/subnode"));
        //Utilities.dump(docNode.getNode("nonedoc0/subnode"));
    }

    public void testWritesNotAllowed() throws RepositoryException {
        assertTrue(docNode.hasNode("readdoc0"));
        assertTrue(docNode.hasNode("readdoc0/subnode"));
        
        // Tests on the hippo:document node
        Node node;
        try {
            node = docNode.getNode("readdoc0");
            node.setProperty("notallowedprop", "nope");
            userSession.save();
            fail("Shouldn't be allowed to add property to node.");
        } catch( AccessDeniedException e) {
            // expected
            userSession.refresh(false);
        }
        try {
            node = docNode.getNode("readdoc0");
            node.addNode("notallowednode");
            userSession.save();
            fail("Shouldn't be allowed to add node to node.");
        } catch (AccessDeniedException e) {
            // expected
            userSession.refresh(false);
        }
        
        // Test on the subnode of hippo:document
        try {
            node = docNode.getNode("readdoc0/subnode");
            node.setProperty("notallowedprop", "nope");
            userSession.save();
            fail("Shouldn't be allowed to add property to subnode.");
        } catch( AccessDeniedException e) {
            // expected
            userSession.refresh(false);
        }
        try {
            node = docNode.getNode("readdoc0/subnode");
            node.addNode("notallowednode");
            userSession.save();
            fail("Shouldn't be allowed to add node to subnode.");
        } catch (AccessDeniedException e) {
            // expected
            userSession.refresh(false);
        }
    }


    public void testDeletesNotAllowed() throws RepositoryException {
        assertTrue(docNode.hasNode("readdoc0"));
        assertTrue(docNode.hasNode("readdoc0/subnode"));
        Node node;
        try {
            node = docNode.getNode("readdoc0");
            node.getProperty(READAUTH).remove();
            userSession.save();
            fail("Shouldn't be allowed to remove property from node.");
        } catch( AccessDeniedException e) {
            // expected
            userSession.refresh(false);
        }
        try {
            node = docNode.getNode("readdoc0");
            node.getNode("subnode").remove();
            userSession.save();
            fail("Shouldn't be allowed to remove node to node.");
        } catch (AccessDeniedException e) {
            // expected
            userSession.refresh(false);
        }
        try {
            node = docNode.getNode("readdoc0/subnode");
            node.getProperty("subval").remove();
            userSession.save();
            fail("Shouldn't be allowed to remove property from subnode.");
        } catch( AccessDeniedException e) {
            // expected
            userSession.refresh(false);
        }
    }

    public void testReadsAllowed() throws RepositoryException {
        assertTrue(docNode.hasNode("readdoc0"));
        assertTrue(docNode.getNode("readdoc0").hasProperty(READAUTH));
        assertEquals(READAUTH_VAL, docNode.getNode("readdoc0").getProperty(READAUTH).getString());
        assertTrue(docNode.hasNode("readdoc0/subnode"));
        assertTrue(docNode.getNode("readdoc0/subnode").hasProperty("subval"));
        
        assertTrue(docNode.hasNode("writedoc0"));
        assertTrue(docNode.getNode("writedoc0").hasProperty(WRITEAUTH));
        assertEquals(WRITEAUTH_VAL, docNode.getNode("writedoc0").getProperty(WRITEAUTH).getString());
        assertTrue(docNode.hasNode("writedoc0/subnode"));
        assertTrue(docNode.getNode("writedoc0/subnode").hasProperty("subval"));
    }
    
    public void testWritesAllowed() throws RepositoryException {
        assertTrue(docNode.hasNode("writedoc0"));
        assertTrue(docNode.hasNode("writedoc0/subnode"));
        Node node;
        try {
            node = docNode.getNode("writedoc0");
            node.setProperty("allowedprop", "nope");
            userSession.save();
        } catch( AccessDeniedException e) {
            fail("Should be allowed to add property to node.");
        }
        try {
            node = docNode.getNode("writedoc0");
            node.addNode("allowednode");
            userSession.save();
        } catch (AccessDeniedException e) {
            fail("Should be allowed to add node to node.");
        }
        try {
            node = docNode.getNode("writedoc0/subnode");
            node.setProperty("allowedprop", "nope");
            userSession.save();
        } catch( AccessDeniedException e) {
            fail("Should be allowed to add property to subnode.");
        }
        try {
            node = docNode.getNode("writedoc0/subnode");
            node.addNode("allowednode");
            userSession.save();
        } catch (AccessDeniedException e) {
            fail("Should be allowed to add node to subnode.");
        }
    }

    public void testDeletesAllowed() throws RepositoryException {
        assertTrue(docNode.hasNode("writedoc0"));
        assertTrue(docNode.hasNode("writedoc0/subnode"));
        Node node;
        try {
            node = docNode.getNode("writedoc0");
            node.setProperty("allowedprop", "test");
            userSession.save();
            node.getProperty("allowedprop").remove();
            userSession.save();
        } catch( AccessDeniedException e) {
            fail("Should be allowed to add and remove property from node.");
        }
        try {
            node = docNode.getNode("writedoc0");
            node.addNode("allowednode");
            userSession.save();
            node.getNode("allowednode").remove();
            userSession.save();
        } catch (AccessDeniedException e) {
            fail("Should be allowed to add and remove node from node.");
        }
        try {
            node = docNode.getNode("writedoc0/subnode");
            node.getProperty("subval").remove();
            userSession.save();
        } catch( AccessDeniedException e) {
            fail("Should be allowed to add property to subnode.");
        }
    }


    public void testFacetSearch() throws RepositoryException {
        Node navNode = userSession.getRootNode().getNode("navigation/search");
        assertTrue(navNode.hasNode("hippo:resultset/readdoc0"));
        assertTrue(navNode.hasNode("readme/hippo:resultset/readdoc0"));
        assertTrue(navNode.hasNode("hippo:resultset/writedoc0"));
        assertTrue(navNode.hasNode("nope/hippo:resultset/writedoc0"));
        NodeIterator iter = navNode.getNode("hippo:resultset").getNodes();
        assertEquals((long) (2 * NODE_COUNT), iter.getSize());   
    }

    public void testFacetSelect() throws RepositoryException {
        Node navNode = userSession.getRootNode().getNode("navigation/select");
        assertTrue(navNode.hasNode("readdoc0"));
        NodeIterator iter = navNode.getNodes();
        assertEquals((long) NODE_COUNT, iter.getSize());
    }


    public void testQueryXPath() throws RepositoryException {
        QueryManager queryManager = userSession.getWorkspace().getQueryManager();
        // XPath doesn't like the query from the root
        Query query = queryManager.createQuery("//element(*,hippo:document)", Query.XPATH);
        NodeIterator iter = query.execute().getNodes();
        assertEquals((long) (2 * NODE_COUNT), iter.getSize());
    }

    public void testQuerySQL() throws RepositoryException {
        QueryManager queryManager = userSession.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery("SELECT * FROM hippo:document", Query.SQL);
        NodeIterator iter = query.execute().getNodes();
        assertEquals((long) (2 * NODE_COUNT), iter.getSize());
    }
}
