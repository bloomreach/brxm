/*
 *  Copyright 2008 Hippo.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.AccessControlException;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.security.Privilege;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class FacetedAuthorizationTest extends TestCase {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

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
    private static final String TEST_GROUP_ID = "testgroup";

    // predefined action constants in checkPermission
    public static final String READ_ACTION = Privilege.JCR_READ;
    public static final String REMOVE_ACTION = Privilege.JCR_REMOVE_NODE;
    public static final String ADD_NODE_ACTION = Privilege.JCR_ADD_CHILD_NODES;
    public static final String SET_PROPERTY_ACTION = Privilege.JCR_MODIFY_PROPERTIES;
    public static final String[] JCR_ACTIONS = new String[] { READ_ACTION, REMOVE_ACTION, ADD_NODE_ACTION,
        SET_PROPERTY_ACTION };

    public void cleanup() throws RepositoryException {
        Node config = session.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH);
        Node domains = config.getNode(HippoNodeType.DOMAINS_PATH);
        Node users = config.getNode(HippoNodeType.USERS_PATH);
        Node groups = config.getNode(HippoNodeType.GROUPS_PATH);

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
        if (groups.hasNode(TEST_GROUP_ID)) {
            groups.getNode(TEST_GROUP_ID).remove();
        }
        
        if (session.getRootNode().hasNode(TEST_DATA_NODE)) {
            session.getRootNode().getNode(TEST_DATA_NODE).remove();
        }
        if (session.getRootNode().hasNode(TEST_NAVIGATION_NODE)) {
            session.getRootNode().getNode(TEST_NAVIGATION_NODE).remove();
        }
        session.save();
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        cleanup();
        Node config = session.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH);
        Node domains = config.getNode(HippoNodeType.DOMAINS_PATH);
        Node users = config.getNode(HippoNodeType.USERS_PATH);
        Node groups = config.getNode(HippoNodeType.GROUPS_PATH);

        // create test user
        testUser = users.addNode(TEST_USER_ID, HippoNodeType.NT_USER);
        testUser.setProperty(HippoNodeType.HIPPO_PASSWORD, TEST_USER_PASS);

        // create test group with member test
        Node testGroup = groups.addNode(TEST_GROUP_ID, HippoNodeType.NT_GROUP);
        testGroup.setProperty(HippoNodeType.HIPPO_MEMBERS, new String[] { TEST_USER_ID });

        // create hippodoc domain
        hipDocDomain = domains.addNode(DOMAIN_DOC_NODE, HippoNodeType.NT_DOMAIN);
        Node ar = hipDocDomain.addNode("hippo:authrole", HippoNodeType.NT_AUTHROLE);
        Node dr  = hipDocDomain.addNode("hippo:domainrule", HippoNodeType.NT_DOMAINRULE);
        Node fr  = dr.addNode("hippo:facetrule", HippoNodeType.NT_FACETRULE);
        ar.setProperty(HippoNodeType.HIPPO_ROLE, "readonly");
        ar.setProperty(HippoNodeType.HIPPO_USERS, new String[] {TEST_USER_ID});
        fr.setProperty(HippoNodeType.HIPPO_FACET, "nodetype");
        fr.setProperty(HippoNodeType.HIPPOSYS_VALUE, "hippo:testdocument");
        fr.setProperty(HippoNodeType.HIPPOSYS_TYPE, "Name");

        // create read domain
        readDomain = domains.addNode(DOMAIN_READ_NODE, HippoNodeType.NT_DOMAIN);
        ar = readDomain.addNode("hippo:authrole", HippoNodeType.NT_AUTHROLE);
        dr  = readDomain.addNode("hippo:domainrule", HippoNodeType.NT_DOMAINRULE);
        fr  = dr.addNode("hippo:facetrule", HippoNodeType.NT_FACETRULE);
        ar.setProperty(HippoNodeType.HIPPO_ROLE, "readonly");
        ar.setProperty(HippoNodeType.HIPPO_USERS, new String[] {TEST_USER_ID});
        fr.setProperty(HippoNodeType.HIPPO_FACET, "authtest");
        fr.setProperty(HippoNodeType.HIPPOSYS_VALUE, "canread");
        fr.setProperty(HippoNodeType.HIPPOSYS_TYPE, "String");

        dr  = readDomain.addNode("hippo:domainrule", HippoNodeType.NT_DOMAINRULE);
        fr  = dr.addNode("hippo:facetrule", HippoNodeType.NT_FACETRULE);
        ar.setProperty(HippoNodeType.HIPPO_ROLE, "readonly");
        ar.setProperty(HippoNodeType.HIPPO_USERS, new String[] {TEST_USER_ID});
        fr.setProperty(HippoNodeType.HIPPO_FACET, "user");
        fr.setProperty(HippoNodeType.HIPPOSYS_VALUE, "__user__");
        fr.setProperty(HippoNodeType.HIPPOSYS_TYPE, "String");

        dr  = readDomain.addNode("hippo:domainrule", HippoNodeType.NT_DOMAINRULE);
        fr  = dr.addNode("hippo:facetrule", HippoNodeType.NT_FACETRULE);
        ar.setProperty(HippoNodeType.HIPPO_ROLE, "readonly");
        ar.setProperty(HippoNodeType.HIPPO_USERS, new String[] {TEST_USER_ID});
        fr.setProperty(HippoNodeType.HIPPO_FACET, "group");
        fr.setProperty(HippoNodeType.HIPPOSYS_VALUE, "__group__");
        fr.setProperty(HippoNodeType.HIPPOSYS_TYPE, "String");

        dr  = readDomain.addNode("hippo:domainrule", HippoNodeType.NT_DOMAINRULE);
        fr  = dr.addNode("hippo:facetrule", HippoNodeType.NT_FACETRULE);
        ar.setProperty(HippoNodeType.HIPPO_ROLE, "readonly");
        ar.setProperty(HippoNodeType.HIPPO_USERS, new String[] {TEST_USER_ID});
        fr.setProperty(HippoNodeType.HIPPO_FACET, "role");
        fr.setProperty(HippoNodeType.HIPPOSYS_VALUE, "__role__");
        fr.setProperty(HippoNodeType.HIPPOSYS_TYPE, "String");

        // create write domain
        writeDomain = domains.addNode(DOMAIN_WRITE_NODE, HippoNodeType.NT_DOMAIN);
        ar = writeDomain.addNode("hippo:authrole", HippoNodeType.NT_AUTHROLE);
        dr  = writeDomain.addNode("hippo:domainrule", HippoNodeType.NT_DOMAINRULE);
        fr  = dr.addNode("hippo:facetrule", HippoNodeType.NT_FACETRULE);
        ar.setProperty(HippoNodeType.HIPPO_ROLE, "readwrite");
        ar.setProperty(HippoNodeType.HIPPO_USERS, new String[] {TEST_USER_ID});
        fr.setProperty(HippoNodeType.HIPPO_FACET, "authtest");
        fr.setProperty(HippoNodeType.HIPPOSYS_VALUE, "canwrite");
        fr.setProperty(HippoNodeType.HIPPOSYS_TYPE, "String");

        // create test data
        testData = session.getRootNode().addNode(TEST_DATA_NODE);
        testData.addMixin("mix:referenceable");

        testData.addNode("readdoc0",  "hippo:ntunstructured").setProperty("authtest", "canread");
        testData.addNode("writedoc0", "hippo:ntunstructured").setProperty("authtest", "canwrite");
        testData.addNode("nothing0",  "hippo:ntunstructured").setProperty("authtest", "nothing");
        testData.getNode("readdoc0").addMixin("hippo:harddocument");
        testData.getNode("writedoc0").addMixin("hippo:harddocument");
        testData.getNode("nothing0").addMixin("hippo:harddocument");

        testData.getNode("readdoc0").addNode("subread",  "hippo:ntunstructured").setProperty("authtest", "canread");
        testData.getNode("readdoc0").addNode("subwrite",  "hippo:ntunstructured").setProperty("authtest", "canwrite");
        testData.getNode("readdoc0").addNode("subnothing",  "hippo:ntunstructured").setProperty("authtest", "nothing");
        testData.getNode("readdoc0/subread").addMixin("hippo:harddocument");
        testData.getNode("readdoc0/subwrite").addMixin("hippo:harddocument");
        testData.getNode("readdoc0/subnothing").addMixin("hippo:harddocument");

        testData.getNode("writedoc0").addNode("subread",  "hippo:ntunstructured").setProperty("authtest", "canread");
        testData.getNode("writedoc0").addNode("subwrite",  "hippo:ntunstructured").setProperty("authtest", "canwrite");
        testData.getNode("writedoc0").addNode("subnothing",  "hippo:ntunstructured").setProperty("authtest", "nothing");
        testData.getNode("writedoc0/subread").addMixin("hippo:harddocument");
        testData.getNode("writedoc0/subwrite").addMixin("hippo:harddocument");
        testData.getNode("writedoc0/subnothing").addMixin("hippo:harddocument");

        testData.getNode("nothing0").addNode("subread",  "hippo:ntunstructured").setProperty("authtest", "canread");
        testData.getNode("nothing0").addNode("subwrite",  "hippo:ntunstructured").setProperty("authtest", "canwrite");
        testData.getNode("nothing0").addNode("subnothing",  "hippo:ntunstructured").setProperty("authtest", "nothing");
        testData.getNode("nothing0/subread").addMixin("hippo:harddocument");
        testData.getNode("nothing0/subwrite").addMixin("hippo:harddocument");
        testData.getNode("nothing0/subnothing").addMixin("hippo:harddocument");

        // expander test data
        testData.addNode("expanders",  "hippo:ntunstructured").setProperty("authtest", "canread");
        testData.getNode("expanders").addMixin("hippo:harddocument");

        testData.getNode("expanders").addNode("usertest",  "hippo:ntunstructured").setProperty("user", TEST_USER_ID);
        testData.getNode("expanders/usertest").addMixin("hippo:harddocument");
        testData.getNode("expanders").addNode("useradmin",  "hippo:ntunstructured").setProperty("user", "admin");
        testData.getNode("expanders/useradmin").addMixin("hippo:harddocument");

        testData.getNode("expanders").addNode("grouptest",  "hippo:ntunstructured").setProperty("group", TEST_GROUP_ID);
        testData.getNode("expanders/grouptest").addMixin("hippo:harddocument");
        testData.getNode("expanders").addNode("groupadmin",  "hippo:ntunstructured").setProperty("group", "admin");
        testData.getNode("expanders/groupadmin").addMixin("hippo:harddocument");

        testData.getNode("expanders").addNode("roletest",  "hippo:ntunstructured").setProperty("role", "readonly");
        testData.getNode("expanders/roletest").addMixin("hippo:harddocument");
        testData.getNode("expanders").addNode("roleadmin",  "hippo:ntunstructured").setProperty("group", "admin");
        testData.getNode("expanders/roleadmin").addMixin("hippo:harddocument");

        // save content
        session.save();
        
        // refresh session to be sure uuids are refreshed on all nodes
        session.refresh(false);
        
        // create test navigation
        testNav = session.getRootNode().addNode(TEST_NAVIGATION_NODE);

        // search without namespace
        Node node = testNav.addNode("search", HippoNodeType.NT_FACETSEARCH);
        node.setProperty(HippoNodeType.HIPPO_QUERYNAME, "search");
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, testData.getIdentifier());
        node.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { "authtest" });

        // select without namespace
        node = testNav.addNode("select", HippoNodeType.NT_FACETSELECT);
        node.setProperty(HippoNodeType.HIPPO_MODES, new String[] { "stick" });
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, testData.getIdentifier());
        node.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { "authtest" });
        node.setProperty(HippoNodeType.HIPPO_VALUES, new String[] { "canread" });

        // expose data to user session
        session.save();
        session.refresh(false);

        // setup user session
        userSession = server.login(TEST_USER_ID, TEST_USER_PASS.toCharArray());
        testData = userSession.getRootNode().getNode(TEST_DATA_NODE);
        testNav  = userSession.getRootNode().getNode(TEST_NAVIGATION_NODE);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        cleanup();
        super.tearDown();
    }

    @Test
    public void testReadsAllowed() throws RepositoryException {
        assertTrue(testData.hasNode("readdoc0"));
        assertTrue(testData.hasNode("writedoc0"));
        assertTrue(testData.hasNode("expanders"));
        assertTrue(testData.hasNode("expanders/usertest"));
        assertTrue(testData.hasNode("expanders/grouptest"));
        assertTrue(testData.hasNode("expanders/roletest"));
        userSession.checkPermission(testData.getPath() + "/" + "readdoc0" ,  READ_ACTION);
        userSession.checkPermission(testData.getPath() + "/" + "writedoc0" , READ_ACTION);
        userSession.checkPermission(testData.getPath() + "/" + "expanders" , READ_ACTION);
        userSession.checkPermission(testData.getPath() + "/" + "expanders/usertest" ,  READ_ACTION);
        userSession.checkPermission(testData.getPath() + "/" + "expanders/grouptest" , READ_ACTION);
        userSession.checkPermission(testData.getPath() + "/" + "expanders/roletest" ,  READ_ACTION);
    }

    @Test
    public void testReadsNotAllowed() throws RepositoryException {
        assertFalse(testData.hasNode("nothing0"));
        assertFalse(testData.hasNode("expanders/useradmin"));
        assertFalse(testData.hasNode("expanders/groupadmin"));
        assertFalse(testData.hasNode("expanders/roleadmin"));
    }

    @Test
    public void testSubReadsAllowed() throws RepositoryException {
        assertTrue(testData.hasNode("readdoc0/subread"));
        assertTrue(testData.hasNode("readdoc0/subwrite"));
        assertTrue(testData.hasNode("writedoc0/subread"));
        assertTrue(testData.hasNode("writedoc0/subwrite"));
    }

    @Test
    public void testSubReadsNotAllowed() throws RepositoryException {
        assertFalse(testData.hasNode("readdoc0/subnothing"));
        assertFalse(testData.hasNode("writedoc0/subnothing"));
        assertFalse(testData.hasNode("nothing0/subread"));
        assertFalse(testData.hasNode("nothing0/subwrite"));
    }

    @Test
    public void testWritesAllowed() throws RepositoryException {
        Node node;

        userSession.checkPermission(testData.getPath() + "/" + "writedoc0" , SET_PROPERTY_ACTION);
        node = testData.getNode("writedoc0");
        node.setProperty("test", "allowed");
        userSession.save();

        userSession.checkPermission(testData.getPath() + "/" + "writedoc0" , ADD_NODE_ACTION);
        node = testData.getNode("writedoc0");
        node.addNode("newnode", "hippo:ntunstructured").setProperty("authtest", "canwrite");
        userSession.save();
    }

    @Test
    public void testSubWritesAllowed() throws RepositoryException {
        Node node;

        node = testData.getNode("readdoc0/subwrite");
        node.setProperty("test", "allowed");
        userSession.save();

        node = testData.getNode("readdoc0/subwrite");
        node.addNode("newnode", "hippo:ntunstructured").setProperty("authtest", "canwrite");
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
    }

    @Ignore
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
            node = testData.getNode("writedoc0");
            node.setProperty("allowedprop", "test");
            userSession.save();
            userSession.checkPermission(testData.getPath() + "/" + "writedoc0/subwrite", REMOVE_ACTION);
            node = testData.getNode("writedoc0/subwrite");
            node.remove();
            userSession.save();
        } catch (AccessDeniedException e) {
            fail("Should be allowed to delete node.");
        } catch (AccessControlException ex) {
            fail("Should be allowed to delete node");
        }
    }

    @Test
    public void testDeletesNotAllowed() throws RepositoryException {
        Node node;
        try {
            node = testData.getNode("readdoc0");
            node.getProperty("authtest").remove();
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
        // FIXME HREPTWO-2126: Should not be allowed to remove nodes on which the 
        // user only has read permisssions
        try {
            testData.getNode("writedoc0/subread").remove();
            userSession.save();
            fail("Shouldn't be allowed to remove node.");
        } catch (AccessDeniedException e) {
            // expected
            userSession.refresh(false);
        }
    }

    @Ignore
    public void testSelfExclusionNotAllowed() throws RepositoryException {
        Node node;
        try {
            node = testData.getNode("writedoc0");
            node.setProperty("authtest", "none");
            userSession.save();
            fail("Shouldn't be allowed to exclude yourself from reading.");
        } catch (AccessDeniedException e) {
            // expected
            userSession.refresh(false);
        }
        try {
            node = testData.getNode("writedoc0");
            node.setProperty("authtest", "read");
            userSession.save();
            fail("Shouldn't be allowed to exclude yourself from writing.");
        } catch (AccessDeniedException e) {
            // expected
            userSession.refresh(false);
        }


        try {
            node = testData.getNode("writedoc0");
            node.addNode("mynode1", "hippo:ntunstructured").setProperty("authtest", "none");
            userSession.save();
            
            // JackRabbit swallows the AccessDeniedException, so check manually for node
            try {
                userSession.refresh(false);
                testData.getNode("writedoc0/mynode1");
                fail("Shouldn't be allowed to add node you can't read.");
            } catch (PathNotFoundException e) {
                // expected
            }
        } catch (AccessDeniedException e) {
            // expected
            userSession.refresh(false);
        }
        try {
            node = testData.getNode("writedoc0");
            node.addNode("mynode2", "hippo:ntunstructured").setProperty("authtest", "canread");
            userSession.save();
            
            // JackRabbit swallows the AccessDeniedException, so check manually for node
            try {
                userSession.refresh(false);
                testData.getNode("writedoc0/mynode2");
                fail("Shouldn't be allowed to add node you can't write.");
            } catch (PathNotFoundException e) {
                // expected
            }
        } catch (AccessDeniedException e) {
            // expected
            userSession.refresh(false);
        }
    }

    @Test
    public void testFacetSearch() throws RepositoryException {
        Node navNode = testNav.getNode("search");
        assertTrue(navNode.hasNode("hippo:resultset/readdoc0"));
        assertTrue(navNode.hasNode("hippo:resultset/writedoc0"));
        NodeIterator iter = navNode.getNode("hippo:resultset").getNodes();
        int count = 0;
        while(iter.hasNext()) {
            Node n = iter.nextNode();
            try {
                Node c = ((HippoNode)n).getCanonicalNode();
                if(c == null) {
                    fail("Item "+n.getPath()+" should not have appeared in the resultset");
                } 
            } catch(Exception ex) {
                fail("Item "+n.getPath()+" should not have appeared in the resultset");
            }
            ++count;
        }
        assertEquals(10, count);
        assertEquals(10L, iter.getSize());
        // FIXME HREPTWO-3554 assertEquals(10L, navNode.getNode("hippo:resultset").getProperty(HippoNodeType.HIPPO_COUNT).getLong());
    }

    @Test
    public void testFacetSelect() throws RepositoryException {
        Node navNode = testNav.getNode("select");
        assertTrue(navNode.hasNode("readdoc0"));
        assertTrue(navNode.hasNode("readdoc0/subread"));

        NodeIterator directIter = testData.getNodes();
        while (directIter.hasNext()) {
            directIter.nextNode().getName();
        }
        
        
        NodeIterator iter = navNode.getNodes();
        
        while (iter.hasNext()) {
            iter.nextNode().getName();
        }
        assertEquals(directIter.getSize(), iter.getSize());
    }

    @Test
    public void testQueryXPath() throws RepositoryException {
        QueryManager queryManager = userSession.getWorkspace().getQueryManager();
        // XPath doesn't like the query from the root
        Query query = queryManager.createQuery("//element(*,hippo:ntunstructured) order by @jcr:score", Query.XPATH);
        NodeIterator iter = query.execute().getNodes();
        assertEquals(10L, iter.getSize());
    }

    @Test
    public void testQuerySQL() throws RepositoryException {
        QueryManager queryManager = userSession.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery("SELECT * FROM hippo:ntunstructured ORDER BY jcr:score", Query.SQL);
        NodeIterator iter = query.execute().getNodes();
        assertEquals(10L, iter.getSize());
    }

    @Test
    public void testCheckPermissionPrivilegeJcrRead() throws RepositoryException {
        userSession.checkPermission(testData.getPath() + "/readdoc0", "jcr:read");
        userSession.checkPermission(testData.getPath() + "/readdoc0/subread", "jcr:read");
        userSession.checkPermission(testData.getPath() + "/writedoc0/subread", "jcr:read");
    }

    @Test
    public void testCheckPermissionPrivilegeJcrWrite() throws RepositoryException {
        userSession.checkPermission(testData.getPath() + "/writedoc0", "jcr:write");
        userSession.checkPermission(testData.getPath() + "/writedoc0/subwrite", "jcr:write");
        userSession.checkPermission(testData.getPath() + "/readdoc0/subwrite", "jcr:write");
    }


    @Test
    public void testCheckPermissionNotPrivilegeJcrRead() throws RepositoryException {
        try {
            userSession.checkPermission(testData.getPath() + "/readdoc0/subnothing", "jcr:read");
            fail("User shouldn't be allowed action 'jcrread' on : " + testData.getPath() + "/readdoc0/subnothing");
        } catch (AccessControlException e) {
            // expected
        }
        try {
            userSession.checkPermission(testData.getPath() + "/writedoc0/subnothing", "jcr:read");
            fail("User shouldn't be allowed action 'read' on : " + testData.getPath() + "/writedoc0/subnothing");
        } catch (AccessControlException e) {
            // expected
        }
        try {
            userSession.checkPermission(testData.getPath() + "/nothing0", "jcr:read");
            fail("User shouldn't be allowed action 'read' on : " + testData.getPath() + "/nothing0");
        } catch (AccessControlException e) {
            // expected
        }
        try {
            userSession.checkPermission(testData.getPath() + "/nothing0/subnothing", "jcr:read");
            fail("User shouldn't be allowed action 'read' on : " + testData.getPath() + "/nothing0/subnothing");
        } catch (AccessControlException e) {
            // expected
        }
        try {
            userSession.checkPermission(testData.getPath() + "/nothing0/subread", "jcr:read");
            fail("User shouldn't be allowed action 'read' on : " + testData.getPath() + "/nothing0/subread");
        } catch (AccessControlException e) {
            // expected
        }
        try {
            userSession.checkPermission(testData.getPath() + "/nothing0/subwrite", "jcr:read");
            fail("User shouldn't be allowed action 'read' on : " + testData.getPath() + "/nothing0/subwrite");
        } catch (AccessControlException e) {
            // expected
        }
    }

    @Test
    public void testCheckPermissionNotPrivilegeJcrWrite() throws RepositoryException {
        try {
            userSession.checkPermission(testData.getPath() + "/readdoc0", "jcr:write");
            fail("User shouldn't be allowed action 'jcrall' on : " + testData.getPath() + "/readdoc0");
        } catch (AccessControlException e) {
            // expected
        }
        try {
            userSession.checkPermission(testData.getPath() + "/readdoc0/subread", "jcr:write");
            fail("User shouldn't be allowed action 'jcrall' on : " + testData.getPath() + "/readdoc0/subread");
        } catch (AccessControlException e) {
            // expected
        }
        try {
            userSession.checkPermission(testData.getPath() + "/readdoc0/subnothing", "jcr:write");
            fail("User shouldn't be allowed action 'jcrall' on : " + testData.getPath() + "/readdoc0/subnothing");
        } catch (AccessControlException e) {
            // expected
        }
        try {
            userSession.checkPermission(testData.getPath() + "/nothing0", "jcr:write");
            fail("User shouldn't be allowed action 'read' on : " + testData.getPath() + "/nothing0");
        } catch (AccessControlException e) {
            // expected
        }
        try {
            userSession.checkPermission(testData.getPath() + "/nothing0/subread", "jcr:write");
            fail("User shouldn't be allowed action 'read' on : " + testData.getPath() + "/nothing0/subread");
        } catch (AccessControlException e) {
            // expected
        }
        try {
            userSession.checkPermission(testData.getPath() + "/nothing0/subnothing", "jcr:write");
            fail("User shouldn't be allowed action 'read' on : " + testData.getPath() + "/nothing0/subnothing");
        } catch (AccessControlException e) {
            // expected
        }
    }
}
