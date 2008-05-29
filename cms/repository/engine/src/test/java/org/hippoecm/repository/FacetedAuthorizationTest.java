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

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.hippoecm.repository.api.HippoNodeType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class FacetedAuthorizationTest extends TestCase {
    
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    
    Node hipDocDomain;
    Node readDomain;
    Node writeDomain;
    Node testUser;

    Node testData;
    Node testNav;
    
    Session userSession; 
    
    // nodes that have to be cleaned up
    private static final String DOMAIN_DOC_NODE = "hippodocument";
    private static final String DOMAIN_READ_NODE = "readme";
    private static final String DOMAIN_WRITE_NODE= "writeme";
    private static final String TEST_DATA_NODE = "testdata";
    private static final String TEST_NAVIGATION_NODE = "navigation";
    
    private static final String TEST_USER_ID = "testuser";
    private static final String TEST_USER_PASS = "password";
    
    
    public void cleanup() throws RepositoryException  {
        Node config = session.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH);
        Node domains = config.getNode(HippoNodeType.DOMAINS_PATH);
        Node users = config.getNode(HippoNodeType.USERS_PATH);

        if (domains.hasNode(DOMAIN_DOC_NODE)) {
            domains.getNode(DOMAIN_DOC_NODE).remove();
        }
        if (domains.hasNode(DOMAIN_READ_NODE)) {
            domains.getNode(DOMAIN_READ_NODE).remove();
        }
        if (domains.hasNode(DOMAIN_WRITE_NODE)) {
            domains.getNode(DOMAIN_WRITE_NODE).remove();
        }
        if (users.hasNode(TEST_USER_ID)) {
            users.getNode(TEST_USER_ID).remove();
        }
        if (session.getRootNode().hasNode(TEST_DATA_NODE)) {
            session.getRootNode().getNode(TEST_DATA_NODE).remove();
        }
        if (session.getRootNode().hasNode(TEST_NAVIGATION_NODE)) {
            session.getRootNode().getNode(TEST_NAVIGATION_NODE).remove();
        }
        session.save();
    }
    
    @Before
    public void setUp() throws Exception {
        super.setUp();
        
        cleanup();
        Node config = session.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH);
        Node domains = config.getNode(HippoNodeType.DOMAINS_PATH);
        Node users = config.getNode(HippoNodeType.USERS_PATH);
        
        
        // create test user
        testUser = users.addNode(TEST_USER_ID, HippoNodeType.NT_USER);
        testUser.setProperty(HippoNodeType.HIPPO_PASSWORD, TEST_USER_PASS);

        // create hippodoc domain
        hipDocDomain = domains.addNode(DOMAIN_DOC_NODE, HippoNodeType.NT_DOMAIN);
        Node ar = hipDocDomain.addNode("hippo:authrole", HippoNodeType.NT_AUTHROLE);
        Node dr  = hipDocDomain.addNode("hippo:domainrule", HippoNodeType.NT_DOMAINRULE);
        Node fr  = dr.addNode("hippo:facetrule", HippoNodeType.NT_FACETRULE);
        ar.setProperty(HippoNodeType.HIPPO_ROLE, "jcrread");
        ar.setProperty(HippoNodeType.HIPPO_USERS, new String[] {TEST_USER_ID});
        fr.setProperty(HippoNodeType.HIPPO_FACET, "nodetype");
        fr.setProperty(HippoNodeType.HIPPO_VALUE, "hippo:testdocument");
        fr.setProperty(HippoNodeType.HIPPO_TYPE, "Name");

        // create read domain
        readDomain = domains.addNode(DOMAIN_READ_NODE, HippoNodeType.NT_DOMAIN);
        ar = readDomain.addNode("hippo:authrole", HippoNodeType.NT_AUTHROLE);
        dr  = readDomain.addNode("hippo:domainrule", HippoNodeType.NT_DOMAINRULE);
        fr  = dr.addNode("hippo:facetrule", HippoNodeType.NT_FACETRULE);
        ar.setProperty(HippoNodeType.HIPPO_ROLE, "jcrread");
        ar.setProperty(HippoNodeType.HIPPO_USERS, new String[] {TEST_USER_ID});
        fr.setProperty(HippoNodeType.HIPPO_FACET, HippoNodeType.HIPPO_TYPE);
        fr.setProperty(HippoNodeType.HIPPO_VALUE, "canread");
        fr.setProperty(HippoNodeType.HIPPO_TYPE, "String");

        // create write domain
        writeDomain = domains.addNode(DOMAIN_WRITE_NODE, HippoNodeType.NT_DOMAIN);
        ar = writeDomain.addNode("hippo:authrole", HippoNodeType.NT_AUTHROLE);
        dr  = writeDomain.addNode("hippo:domainrule", HippoNodeType.NT_DOMAINRULE);
        fr  = dr.addNode("hippo:facetrule", HippoNodeType.NT_FACETRULE);
        ar.setProperty(HippoNodeType.HIPPO_ROLE, "jcrall");
        ar.setProperty(HippoNodeType.HIPPO_USERS, new String[] {TEST_USER_ID});
        fr.setProperty(HippoNodeType.HIPPO_FACET, HippoNodeType.HIPPO_TYPE);
        fr.setProperty(HippoNodeType.HIPPO_VALUE, "canwrite");
        fr.setProperty(HippoNodeType.HIPPO_TYPE, "String");

        // create test data
        testData = session.getRootNode().addNode(TEST_DATA_NODE);
        testData.addMixin("mix:referenceable");
        
        testData.addNode("readdoc0",  "hippo:ntunstructured").setProperty(HippoNodeType.HIPPO_TYPE, "canread");
        testData.addNode("writedoc0", "hippo:ntunstructured").setProperty(HippoNodeType.HIPPO_TYPE, "canwrite");
        testData.addNode("nothing0",  "hippo:ntunstructured").setProperty(HippoNodeType.HIPPO_TYPE, "nothing");
        testData.getNode("readdoc0").addMixin("hippo:harddocument");
        testData.getNode("writedoc0").addMixin("hippo:harddocument");
        testData.getNode("nothing0").addMixin("hippo:harddocument");
        
        testData.getNode("readdoc0").addNode("subread",  "hippo:ntunstructured").setProperty(HippoNodeType.HIPPO_TYPE, "canread");
        testData.getNode("readdoc0").addNode("subwrite",  "hippo:ntunstructured").setProperty(HippoNodeType.HIPPO_TYPE, "canwrite");
        testData.getNode("readdoc0").addNode("subnothing",  "hippo:ntunstructured").setProperty(HippoNodeType.HIPPO_TYPE, "nothing");
        testData.getNode("readdoc0/subread").addMixin("hippo:harddocument");
        testData.getNode("readdoc0/subwrite").addMixin("hippo:harddocument");
        testData.getNode("readdoc0/subnothing").addMixin("hippo:harddocument");
        
        testData.getNode("writedoc0").addNode("subread",  "hippo:ntunstructured").setProperty(HippoNodeType.HIPPO_TYPE, "canread");
        testData.getNode("writedoc0").addNode("subwrite",  "hippo:ntunstructured").setProperty(HippoNodeType.HIPPO_TYPE, "canwrite");
        testData.getNode("writedoc0").addNode("subnothing",  "hippo:ntunstructured").setProperty(HippoNodeType.HIPPO_TYPE, "nothing");
        testData.getNode("writedoc0/subread").addMixin("hippo:harddocument");
        testData.getNode("writedoc0/subwrite").addMixin("hippo:harddocument");
        testData.getNode("writedoc0/subnothing").addMixin("hippo:harddocument");
 
        testData.getNode("nothing0").addNode("subread",  "hippo:ntunstructured").setProperty(HippoNodeType.HIPPO_TYPE, "canread");
        testData.getNode("nothing0").addNode("subwrite",  "hippo:ntunstructured").setProperty(HippoNodeType.HIPPO_TYPE, "canwrite");
        testData.getNode("nothing0").addNode("subnothing",  "hippo:ntunstructured").setProperty(HippoNodeType.HIPPO_TYPE, "nothing");
        testData.getNode("nothing0/subread").addMixin("hippo:harddocument");
        testData.getNode("nothing0/subwrite").addMixin("hippo:harddocument");
        testData.getNode("nothing0/subnothing").addMixin("hippo:harddocument");
         
        // create test nagivation
        testNav = session.getRootNode().addNode(TEST_NAVIGATION_NODE);
        
        // search without namespace
        Node node = testNav.addNode("search", HippoNodeType.NT_FACETSEARCH);
        node.setProperty(HippoNodeType.HIPPO_QUERYNAME, "search");
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, testData.getUUID());
        node.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { HippoNodeType.HIPPO_TYPE });

        // select without namespace
        node = testNav.addNode("select", HippoNodeType.NT_FACETSELECT);
        node.setProperty(HippoNodeType.HIPPO_MODES, new String[] { "stick" });
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, testData.getUUID());
        node.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { HippoNodeType.HIPPO_TYPE });
        node.setProperty(HippoNodeType.HIPPO_VALUES, new String[] { "canread" });
        
        // expose data to user session
        session.save();

        // setup user session
        userSession = server.login(TEST_USER_ID, TEST_USER_PASS.toCharArray());
        testData = userSession.getRootNode().getNode(TEST_DATA_NODE);
        testNav  = userSession.getRootNode().getNode(TEST_NAVIGATION_NODE);
    }

    @After
    public void tearDown() throws Exception {
        cleanup();
        super.tearDown();
    }
    
    @Test
    public void testCannotReadConfiguration() throws RepositoryException {
        try {
            userSession.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH);
            fail("Testuser can read configuration.");
        } catch (PathNotFoundException e) {
            // success
        }
    }

    @Test
    public void testReadsAllowed() throws RepositoryException {
        assertTrue(testData.hasNode("readdoc0"));
        assertTrue(testData.hasNode("writedoc0"));
    }
    
    @Test
    public void testReadsNotAllowed() throws RepositoryException {
        assertFalse(testData.hasNode("nothing0"));
    }

    @Test
    public void testSubReadsAllowed() throws RepositoryException {
        assertTrue(testData.hasNode("readdoc0/subread"));
        assertTrue(testData.hasNode("readdoc0/subwrite"));
        assertTrue(testData.hasNode("writedoc0/subread"));
        assertTrue(testData.hasNode("writedoc0/subwrite"));
        
        // the iterator only sees 4 nodes, but they are directly accessible
        assertTrue(testData.hasNode("nothing0/subread"));
        assertTrue(testData.hasNode("nothing0/subwrite"));
    }
    
    @Test
    public void testSubReadsNotAllowed() throws RepositoryException {
        assertFalse(testData.hasNode("readdoc0/subnothing"));
        assertFalse(testData.hasNode("writedoc0/subnothing"));
    }

    @Test
    public void testWritesAllowed() throws RepositoryException {
        Node node;

        node = testData.getNode("writedoc0");
        node.setProperty("test", "allowed");
        userSession.save();

        node = testData.getNode("readdoc0/subwrite");
        node.addNode("newnode", "hippo:ntunstructured");
        userSession.save();
    }
    
    @Test
    public void testSubWritesAllowed() throws RepositoryException {
        Node node;

        node = testData.getNode("readdoc0/subwrite");
        node.setProperty("test", "allowed");
        userSession.save();

        node = testData.getNode("readdoc0/subwrite");
        node.addNode("newnode", "hippo:ntunstructured");
        userSession.save();
    }

    @Test
    public void testWritesNotAllowed() throws RepositoryException {
        Node node;
        try {
            node = testData.getNode("readdoc0");
            node.setProperty("hippo:name", "nope");
            userSession.save();
            fail("Shouldn't be allowed to add property to node.");
        } catch (AccessDeniedException e) {
            // expected
            userSession.refresh(false);
        }
        try {
            node = testData.getNode("readdoc0");
            node.addNode("notallowednode");
            userSession.save();
            fail("Shouldn't be allowed to add node to node.");
        } catch (AccessDeniedException e) {
            // expected
            userSession.refresh(false);
        }

        try {
            node = testData.getNode("writedoc0");
            node.setProperty(HippoNodeType.HIPPO_TYPE, "nope");
            userSession.save();
            fail("Shouldn't be allowed to change node to non-writeable.");
        } catch (AccessDeniedException e) {
            // expected
            userSession.refresh(false);
        }
    }

    @Test
    public void testSubWritesNotAllowed() throws RepositoryException {
        Node node;

        try {
            node = testData.getNode("writedoc0/subread");
            node.setProperty("hippo:name", "nope");
            userSession.save();
            fail("Shouldn't be allowed to add property to node.");
        } catch (AccessDeniedException e) {
            // expected
            userSession.refresh(false);
        }
        try {
            node = testData.getNode("writedoc0/subread");
            node.addNode("notallowednode");
            userSession.save();
            fail("Shouldn't be allowed to add node to node.");
        } catch (AccessDeniedException e) {
            // expected
            userSession.refresh(false);
        }
        
        // TODO: make a descission what to do in these cases
//        try {
//            // should this be allowed? currently not; write needs read on parent
//            node = testData.getNode("nothing0/subwrite");
//            node.setProperty("test", "allowed");
//
//            Utilities.dump(session.getRootNode());
//            userSession.save();
//            fail("Shouldn't be allowed: read on parent node required for setting property.");
//        } catch (AccessDeniedException e) {
//            // expected
//            userSession.refresh(false);
//        }
//
//        try {
//            // should this be allowed? currently not; write needs read on parent
//            node = testData.getNode("nothing0/subwrite");
//            node.addNode("newnode", "hippo:ntunstructured");
//            userSession.save();
//            fail("Shouldn't be allowed: read on parent node required for adding node.");
//        } catch (AccessDeniedException e) {
//            // expected
//            userSession.refresh(false);
//        }
    }

    @Test
    public void testDeletesAllowed() throws Exception {
        Node node;
        try {
            node = testData.getNode("writedoc0");
            node.setProperty("allowedprop", "test");
            userSession.save();
            node.getProperty("allowedprop").remove();
            userSession.save();
        } catch (AccessDeniedException e) {
            fail("Should be allowed to add and remove property from node.");
        }
        try {
            node = testData.getNode("writedoc0/subwrite");
            node.remove();
            userSession.save();
        } catch (AccessDeniedException e) {
            fail("Should be allowed to delete node.");
        }
    }
    
    public void testDeletesNotAllowed() throws RepositoryException {
        Node node;
        try {
            node = testData.getNode("readdoc0");
            node.getProperty(HippoNodeType.HIPPO_TYPE).remove();
            userSession.save();
            fail("Shouldn't be allowed to remove property from node.");
        } catch (AccessDeniedException e) {
            // expected
            userSession.refresh(false);
        }
        try {
            node = testData.getNode("readdoc0");
            node.getNode("subwrite").remove();
            userSession.save();
            fail("Shouldn't be allowed to remove node.");
        } catch (AccessDeniedException e) {
            // expected
            userSession.refresh(false);
        }
    }

    @Test
    public void testSubDeletesNotAllowed() throws RepositoryException {
        try {
            testData.getNode("readdoc0/subread").remove();
            userSession.save();
            fail("Shouldn't be allowed to remove node.");
        } catch (AccessDeniedException e) {
            // expected
            userSession.refresh(false);
        }
        try {
            testData.getNode("writedoc0/subread").remove();
            userSession.save();
            fail("Shouldn't be allowed to remove node.");
        } catch (AccessDeniedException e) {
            // expected
            userSession.refresh(false);
        }
    }

    @Test
    public void testFacetSearch() throws RepositoryException {
        //Utilities.dump(testNav);
        Node navNode = testNav.getNode("search");
        assertTrue(navNode.hasNode("hippo:resultset/readdoc0"));
        assertTrue(navNode.hasNode("hippo:resultset/writedoc0"));
        NodeIterator iter = navNode.getNode("hippo:resultset").getNodes();
        assertEquals(8L, iter.getSize());
        assertEquals(8L, navNode.getNode("hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
    }

    @Test
    public void testFacetSelect() throws RepositoryException {
        Node navNode = testNav.getNode("select");
        assertTrue(navNode.hasNode("readdoc0"));
        assertTrue(navNode.hasNode("readdoc0/subread"));
        NodeIterator iter = navNode.getNodes();
        assertEquals(1L, iter.getSize());
    }
    
    @Test
    public void testQueryXPath() throws RepositoryException {
        QueryManager queryManager = userSession.getWorkspace().getQueryManager();
        // XPath doesn't like the query from the root
        Query query = queryManager.createQuery("//element(*,hippo:ntunstructured)", Query.XPATH);
        NodeIterator iter = query.execute().getNodes();
        assertEquals(8L, iter.getSize());
    }

    @Test
    public void testQuerySQL() throws RepositoryException {
        QueryManager queryManager = userSession.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery("SELECT * FROM hippo:ntunstructured", Query.SQL);
        NodeIterator iter = query.execute().getNodes();
        assertEquals(8L, iter.getSize());
    }

}
