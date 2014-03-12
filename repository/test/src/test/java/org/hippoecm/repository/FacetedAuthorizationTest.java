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

import java.security.AccessControlException;
import java.util.Arrays;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.RowIterator;
import javax.jcr.security.Privilege;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeIterator;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.repository.security.domain.DomainRuleExtension;
import org.onehippo.repository.security.domain.FacetRule;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FacetedAuthorizationTest extends RepositoryTestCase {

    Node hipDocDomain;
    Node readDomain;
    Node writeDomain;
    Node testUser;
    
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

        cleanupDomains(domains);

        cleanupUserAndGroup(users, groups);

        cleanupTestData();

        cleanupTestNavigation();

        session.save();
    }

    private void cleanupDomains(final Node domains) throws RepositoryException {
        if (domains.hasNode(DOMAIN_DOC_NODE)) {
            domains.getNode(DOMAIN_DOC_NODE).remove();
        }
        if (domains.hasNode(DOMAIN_READ_NODE)) {
            domains.getNode(DOMAIN_READ_NODE).remove();
        }
        if (domains.hasNode(DOMAIN_WRITE_NODE)) {
            domains.getNode(DOMAIN_WRITE_NODE).remove();
        }
    }

    private void cleanupUserAndGroup(final Node users, final Node groups) throws RepositoryException {
        if (users.hasNode(TEST_USER_ID)) {
            users.getNode(TEST_USER_ID).remove();
        }
        if (groups.hasNode(TEST_GROUP_ID)) {
            groups.getNode(TEST_GROUP_ID).remove();
        }
    }

    private void cleanupTestData() throws RepositoryException {
        if (session.getRootNode().hasNode(TEST_DATA_NODE)) {
            session.getRootNode().getNode(TEST_DATA_NODE).remove();
        }
    }

    private void cleanupTestNavigation() throws RepositoryException {
        if (session.getRootNode().hasNode(TEST_NAVIGATION_NODE)) {
            session.getRootNode().getNode(TEST_NAVIGATION_NODE).remove();
        }
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
        createUserAndGroup(users, groups);

        createDomains(domains);

        // create test data
        Node testData = session.getRootNode().addNode(TEST_DATA_NODE);
        testData.addMixin("mix:referenceable");

        createTestData(testData);

        // expander test data
        createExpanderData(testData);

        // save content
        session.save();
        
        // refresh session to be sure uuids are refreshed on all nodes
        session.refresh(false);
        
        // create test navigation
        createTestNavigation(testData);

        // expose data to user session
        session.save();
        session.refresh(false);

        // setup user session
        userSession = server.login(TEST_USER_ID, TEST_USER_PASS.toCharArray());
    }

    private void createUserAndGroup(final Node users, final Node groups) throws RepositoryException {
        testUser = users.addNode(TEST_USER_ID, HippoNodeType.NT_USER);
        testUser.setProperty(HippoNodeType.HIPPO_PASSWORD, TEST_USER_PASS);

        // create test group with member test
        Node testGroup = groups.addNode(TEST_GROUP_ID, HippoNodeType.NT_GROUP);
        testGroup.setProperty(HippoNodeType.HIPPO_MEMBERS, new String[] { TEST_USER_ID });
    }

    private void createDomains(final Node domains) throws RepositoryException {
        // create hippodoc domain
        hipDocDomain = domains.addNode(DOMAIN_DOC_NODE, HippoNodeType.NT_DOMAIN);
        Node ar = hipDocDomain.addNode("hippo:authrole", HippoNodeType.NT_AUTHROLE);
        ar.setProperty(HippoNodeType.HIPPO_ROLE, "readonly");
        ar.setProperty(HippoNodeType.HIPPO_USERS, new String[]{TEST_USER_ID});

        Node dr = hipDocDomain.addNode("hippo:domainrule", HippoNodeType.NT_DOMAINRULE);
        Node fr = dr.addNode("hippo:facetrule", HippoNodeType.NT_FACETRULE);
        fr.setProperty(HippoNodeType.HIPPO_FACET, "nodetype");
        fr.setProperty(HippoNodeType.HIPPOSYS_VALUE, "hippo:testdocument");
        fr.setProperty(HippoNodeType.HIPPOSYS_TYPE, "Name");

        // create read domain
        readDomain = domains.addNode(DOMAIN_READ_NODE, HippoNodeType.NT_DOMAIN);
        ar = readDomain.addNode("hippo:authrole", HippoNodeType.NT_AUTHROLE);
        ar.setProperty(HippoNodeType.HIPPO_ROLE, "readonly");
        ar.setProperty(HippoNodeType.HIPPO_USERS, new String[]{TEST_USER_ID});

        dr = readDomain.addNode("hippo:domainrule", HippoNodeType.NT_DOMAINRULE);
        fr = dr.addNode("hippo:facetrule", HippoNodeType.NT_FACETRULE);
        fr.setProperty(HippoNodeType.HIPPO_FACET, "authtest");
        fr.setProperty(HippoNodeType.HIPPOSYS_VALUE, "canread");
        fr.setProperty(HippoNodeType.HIPPOSYS_TYPE, "String");

        dr = readDomain.addNode("hippo:domainrule", HippoNodeType.NT_DOMAINRULE);
        fr = dr.addNode("hippo:facetrule", HippoNodeType.NT_FACETRULE);
        fr.setProperty(HippoNodeType.HIPPO_FACET, "user");
        fr.setProperty(HippoNodeType.HIPPOSYS_VALUE, "__user__");
        fr.setProperty(HippoNodeType.HIPPOSYS_TYPE, "String");

        dr = readDomain.addNode("hippo:domainrule", HippoNodeType.NT_DOMAINRULE);
        fr = dr.addNode("hippo:facetrule", HippoNodeType.NT_FACETRULE);
        fr.setProperty(HippoNodeType.HIPPO_FACET, "group");
        fr.setProperty(HippoNodeType.HIPPOSYS_VALUE, "__group__");
        fr.setProperty(HippoNodeType.HIPPOSYS_TYPE, "String");

        dr = readDomain.addNode("hippo:domainrule", HippoNodeType.NT_DOMAINRULE);
        fr = dr.addNode("hippo:facetrule", HippoNodeType.NT_FACETRULE);
        fr.setProperty(HippoNodeType.HIPPO_FACET, "role");
        fr.setProperty(HippoNodeType.HIPPOSYS_VALUE, "__role__");
        fr.setProperty(HippoNodeType.HIPPOSYS_TYPE, "String");

        // create write domain
        writeDomain = domains.addNode(DOMAIN_WRITE_NODE, HippoNodeType.NT_DOMAIN);
        ar = writeDomain.addNode("hippo:authrole", HippoNodeType.NT_AUTHROLE);
        ar.setProperty(HippoNodeType.HIPPO_ROLE, "readwrite");
        ar.setProperty(HippoNodeType.HIPPO_USERS, new String[]{TEST_USER_ID});

        dr = writeDomain.addNode("hippo:domainrule", HippoNodeType.NT_DOMAINRULE);
        fr = dr.addNode("hippo:facetrule", HippoNodeType.NT_FACETRULE);
        fr.setProperty(HippoNodeType.HIPPO_FACET, "authtest");
        fr.setProperty(HippoNodeType.HIPPOSYS_VALUE, "canwrite");
        fr.setProperty(HippoNodeType.HIPPOSYS_TYPE, "String");
    }

    private void createTestData(final Node testData) throws RepositoryException {
        testData.addNode("readdoc0", "hippo:authtestdocument").setProperty("authtest", "canread");
        testData.addNode("writedoc0", "hippo:authtestdocument").setProperty("authtest", "canwrite");
        testData.addNode("nothing0", "hippo:authtestdocument").setProperty("authtest", "nothing");

        testData.getNode("readdoc0").addNode("subread", "hippo:authtestdocument").setProperty("authtest", "canread");
        testData.getNode("readdoc0").addNode("subwrite", "hippo:authtestdocument").setProperty("authtest", "canwrite");
        testData.getNode("readdoc0").addNode("subnothing", "hippo:authtestdocument").setProperty("authtest", "nothing");

        testData.getNode("writedoc0").addNode("subread", "hippo:authtestdocument").setProperty("authtest", "canread");
        testData.getNode("writedoc0").addNode("subwrite", "hippo:authtestdocument").setProperty("authtest", "canwrite");
        testData.getNode("writedoc0").addNode("subnothing", "hippo:authtestdocument").setProperty("authtest", "nothing");

        testData.getNode("nothing0").addNode("subread", "hippo:authtestdocument").setProperty("authtest", "canread");
        testData.getNode("nothing0").addNode("subwrite", "hippo:authtestdocument").setProperty("authtest", "canwrite");
        testData.getNode("nothing0").addNode("subnothing", "hippo:authtestdocument").setProperty("authtest", "nothing");
    }

    private void createExpanderData(final Node testData) throws RepositoryException {
        final Node expanders = testData.addNode("expanders", "hippo:authtestdocument");
        expanders.setProperty("authtest", "canread");

        expanders.addNode("usertest", "hippo:authtestdocument").setProperty("user", TEST_USER_ID);
        expanders.addNode("useradmin", "hippo:authtestdocument").setProperty("user", "admin");

        expanders.addNode("grouptest", "hippo:authtestdocument").setProperty("group", TEST_GROUP_ID);
        expanders.addNode("groupadmin", "hippo:authtestdocument").setProperty("group", "admin");

        expanders.addNode("roletest", "hippo:authtestdocument").setProperty("role", "readonly");
        expanders.addNode("roleadmin", "hippo:authtestdocument").setProperty("group", "admin");
    }

    private void createTestNavigation(final Node testData) throws RepositoryException {
        Node testNav = session.getRootNode().addNode(TEST_NAVIGATION_NODE);

        // search without namespace
        Node node = testNav.addNode("search", HippoNodeType.NT_FACETSEARCH);
        node.setProperty(HippoNodeType.HIPPO_QUERYNAME, "search");
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, testData.getIdentifier());
        node.setProperty(HippoNodeType.HIPPO_FACETS, new String[]{"authtest"});

        // select without namespace
        node = testNav.addNode("select", HippoNodeType.NT_FACETSELECT);
        node.setProperty(HippoNodeType.HIPPO_MODES, new String[]{"stick"});
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, testData.getIdentifier());
        node.setProperty(HippoNodeType.HIPPO_FACETS, new String[]{"authtest"});
        node.setProperty(HippoNodeType.HIPPO_VALUES, new String[]{"canread"});
    }

    @Override
    @After
    public void tearDown() throws Exception {
        userSession.logout();
        userSession = null;

        cleanup();
        super.tearDown();
    }

    @Test
    public void testReadsAllowed() throws RepositoryException {
        Node testData = userSession.getRootNode().getNode(TEST_DATA_NODE);
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
        Node testData = userSession.getRootNode().getNode(TEST_DATA_NODE);
        assertFalse(testData.hasNode("nothing0"));
        assertFalse(testData.hasNode("expanders/useradmin"));
        assertFalse(testData.hasNode("expanders/groupadmin"));
        assertFalse(testData.hasNode("expanders/roleadmin"));
    }

    @Test
    public void testDocumentsAreOrderedBelowHandle() throws RepositoryException {
        Node testRoot = session.getRootNode().getNode(TEST_DATA_NODE);
        final Node handle = testRoot.addNode("doc", "hippo:handle");
        handle.addMixin("hippo:hardhandle");

        Node doc = handle.addNode("doc", "hippo:authtestdocument");
        doc = handle.addNode("doc", "hippo:authtestdocument");
        doc.setProperty("authtest", "canread");
        String identifierSecondDoc = doc.getIdentifier();
        session.save();

        Node testData = userSession.getRootNode().getNode(TEST_DATA_NODE);
        Node userDoc = testData.getNode("doc/doc");
        assertEquals(testData.getPath() + "/doc/doc", userDoc.getPath());
        // even though it is as-if we fetch the first doc, it is actually the second doc below the handle. It is just the
        // first doc that the userSession is allowed to read. Hence, the identifier should be equal to identifierSecondDoc
        assertEquals("Identifiers expected to be equal", testData.getNode("doc/doc").getIdentifier(), identifierSecondDoc);
        assertFalse(userSession.hasPendingChanges());
    }

    @Test
    public void testDocumentsAreOrderedBelowHandleWithOtherNodesPresent() throws RepositoryException {
        Node testRoot = session.getRootNode().getNode(TEST_DATA_NODE);
        final Node handle = testRoot.addNode("doc", "hippo:handle");
        handle.addMixin("hippo:hardhandle");
        handle.addMixin("hippo:translated");

        Node doc = handle.addNode("doc", "hippo:authtestdocument");

        Node translation = handle.addNode("hippo:translation", "hippo:translation");
        translation.setProperty("hippo:language", "lang");
        translation.setProperty("hippo:message", "ignored");

        doc = handle.addNode("doc", "hippo:authtestdocument");
        doc.setProperty("authtest", "canread");
        String identifierSecondDoc = doc.getIdentifier();
        session.save();

        Node testData = userSession.getRootNode().getNode(TEST_DATA_NODE);
        Node userDoc = testData.getNode("doc/doc");
        assertEquals(testData.getPath() + "/doc/doc", userDoc.getPath());
        // even though it is as-if we fetch the first doc, it is actually the second doc below the handle. It is just the
        // first doc that the userSession is allowed to read. Hence, the identifier should be equal to identifierSecondDoc
        assertEquals("Identifiers expected to be equal", testData.getNode("doc/doc").getIdentifier(), identifierSecondDoc);
        assertFalse(userSession.hasPendingChanges());
    }

    @Test
    public void testDocumentsAreOrderedAfterModification() throws RepositoryException {
        Node testData = session.getRootNode().getNode(TEST_DATA_NODE);
        final Node handle = testData.addNode("doc", "hippo:handle");
        handle.addMixin("hippo:hardhandle");

        Node doc = handle.addNode("doc", "hippo:authtestdocument");
        doc = handle.addNode("doc", "hippo:authtestdocument");
        doc.setProperty("authtest", "canread");

        session.save();

        Node userTestData = userSession.getRootNode().getNode(TEST_DATA_NODE);
        Node userHandle = userTestData.getNode("doc");

        // doc node is still coupled to the SYSTEMUSER_ID based jcr session
        doc = handle.addNode("doc", "hippo:authtestdocument");
        session.save();
        assertEquals("Number of child nodes below doc handle was not 3 ", 3, session.getRootNode().getNode(TEST_DATA_NODE).getNode("doc").getNodes().getSize());

        assertTrue(userHandle.hasNode("doc"));
        assertEquals(1, userHandle.getNodes("doc").getSize());
        Node userDoc = userHandle.getNode("doc");
        assertEquals(userTestData.getPath() + "/doc/doc", userDoc.getPath());

        assertFalse(userSession.hasPendingChanges());
    }

    @Test
    public void testAccessIsNotCached() throws RepositoryException {
        Node testData = session.getRootNode().getNode(TEST_DATA_NODE);
        final Node handle = testData.addNode("doc", "hippo:handle");
        handle.addMixin("hippo:hardhandle");

        Node doc = handle.addNode("doc", "hippo:authtestdocument");
        doc = handle.addNode("doc", "hippo:authtestdocument");
        doc.setProperty("authtest", "canread");
        session.save();

        Node userTestData = userSession.getRootNode().getNode(TEST_DATA_NODE);
        Node userDoc = userTestData.getNode("doc/doc");
        assertEquals(userTestData.getPath() + "/doc/doc", userDoc.getPath());

        doc.getProperty("authtest").remove();
        session.save();

        userTestData = userSession.getRootNode().getNode(TEST_DATA_NODE);
        assertFalse("User can still read node while authorization was revoked", userTestData.hasNode("doc/doc"));
        try {
            userTestData.getNode("doc/doc");
            fail("User should not be able to see doc/doc");
        } catch (PathNotFoundException expected) {}
    }

    @Test
    public void testAccessIsNotCachedWhenHandleChanges() throws RepositoryException {
        Node testData = session.getRootNode().getNode(TEST_DATA_NODE);
        final Node handle = testData.addNode("doc", "hippo:handle");
        handle.addMixin("hippo:hardhandle");

        Node doc = handle.addNode("doc", "hippo:authtestdocument");
        doc.setProperty("authtest", "canread");
        session.save();

        Node userTestData = userSession.getRootNode().getNode(TEST_DATA_NODE);
        Node userDoc = userTestData.getNode("doc/doc");
        assertEquals(userTestData.getPath() + "/doc/doc", userDoc.getPath());

        doc.getProperty("authtest").remove();
        doc = handle.addNode("doc", "hippo:authtestdocument");
        doc.setProperty("authtest", "canread");
        String newId = doc.getIdentifier();
        session.save();

        userTestData = userSession.getRootNode().getNode(TEST_DATA_NODE);
        assertTrue("User cannot read new doc", userTestData.hasNode("doc/doc"));

        assertEquals(newId, userTestData.getNode("doc/doc").getIdentifier());
    }

    @Test
    public void testDocumentAccessIsSwitched() throws RepositoryException {
        Node testData = session.getRootNode().getNode(TEST_DATA_NODE);
        final Node handle = testData.addNode("doc", "hippo:handle");
        handle.addMixin("hippo:hardhandle");

        Node doc1 = handle.addNode("doc", "hippo:authtestdocument");
        Node doc2 = handle.addNode("doc", "hippo:authtestdocument");
        doc2.setProperty("authtest", "canread");
        session.save();

        Node userTestData = userSession.getRootNode().getNode(TEST_DATA_NODE);
        Node userDoc = userTestData.getNode("doc/doc");
        assertEquals(userTestData.getPath() + "/doc/doc", userDoc.getPath());

        doc1.setProperty("authtest", "canread");
        doc2.getProperty("authtest").remove();
        String accessibleDocId = doc1.getIdentifier();
        session.save();

        assertTrue("User cannot read new doc", userTestData.hasNode("doc/doc"));

        assertEquals(accessibleDocId, userTestData.getNode("doc/doc").getIdentifier());
    }

    @Test
    public void testAccessChangesBasedWhenPropertyIsUpdated() throws RepositoryException {
        Node testData = session.getRootNode().getNode(TEST_DATA_NODE);
        final Node handle = testData.addNode("doc", "hippo:handle");
        handle.addMixin("hippo:hardhandle");

        Node doc1 = handle.addNode("doc", "hippo:authtestdocument");
        doc1.setProperty("authtest", "cannotread");
        Node doc2 = handle.addNode("doc", "hippo:authtestdocument");
        doc2.setProperty("authtest", "canread");
        session.save();

        Node userTestData = userSession.getRootNode().getNode(TEST_DATA_NODE);
        Node userDoc = userTestData.getNode("doc/doc");
        assertEquals(userTestData.getPath() + "/doc/doc", userDoc.getPath());

        doc1.setProperty("authtest", "canread");
        doc2.setProperty("authtest", "cannotread");
        String accessibleDocId = doc1.getIdentifier();
        session.save();

        assertTrue("User cannot read new doc", userTestData.hasNode("doc/doc"));

        assertEquals(accessibleDocId, userTestData.getNode("doc/doc").getIdentifier());
    }

    @Test
    public void testAccessForChildNodeFollowsParent() throws RepositoryException {
        Node testData = session.getRootNode().getNode(TEST_DATA_NODE);
        final Node handle = testData.addNode("doc", "hippo:handle");
        handle.addMixin("hippo:hardhandle");

        Node doc = handle.addNode("doc", "hippo:authtestdocument");
        doc.addMixin("hippo:container");
        final Node childNode = doc.addNode("link", "hippo:mirror");
        childNode.setProperty("hippo:docbase", session.getRootNode().getIdentifier());
        session.save();

        Node userTestData = userSession.getRootNode().getNode(TEST_DATA_NODE);
        try {
            userTestData.getNode("doc/doc/link");
            fail("User should not be able to read doc/doc/link");
        } catch (PathNotFoundException expected) {}

        doc.setProperty("authtest", "canread");
        session.save();

        userTestData = userSession.getRootNode().getNode(TEST_DATA_NODE);
        assertTrue("User can still not read node while authorization was granted", userTestData.hasNode("doc/doc/link"));
        userTestData.getNode("doc/doc/link");
    }

    @Test
    public void testNodenameExpanders() throws RepositoryException {

        // count the number of docs in resultset before new doc is added:

        final long count = userSession.getRootNode().getNode(TEST_NAVIGATION_NODE).getNode("search").getNode("hippo:resultset").getProperty("hippo:count").getLong();
        // now refresh userSession to clean up virtual nodes
        userSession.refresh(false);

        // create a node which has as name the id of the userSession id
        {
            Node testData = session.getRootNode().getNode(TEST_DATA_NODE);
            testData.getNode("expanders").addNode(TEST_USER_ID, "hippo:authtestdocument");
            session.save();
        }
        // since the userSession is only allowed to read nodes that have the property [authtest = canread] the userSession
        // should not see this node
        assertFalse(userSession.getRootNode().getNode(TEST_DATA_NODE).hasNode("expanders/" + TEST_USER_ID));
        {
            Node testData = userSession.getRootNode().getNode(TEST_DATA_NODE);
            Node testNav  = userSession.getRootNode().getNode(TEST_NAVIGATION_NODE);
            // the node is *not* readable currently for userSession
            // THUS
            // 1 hasNode returns false
            assertFalse(testData.hasNode("expanders/" + TEST_USER_ID));
            // 2 no search hit
            QueryManager queryManager = userSession.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery("//element("+TEST_USER_ID+",hippo:authtestdocument) order by @jcr:score", Query.XPATH);
            HippoNodeIterator iter = (HippoNodeIterator) query.execute().getNodes();
            assertEquals(0L, iter.getSize());
            assertEquals(0L, iter.getTotalSize());
            //3 not in the faceted navigation result
            // count should be still the same
            assertEquals("Unexpected count for resultset", count, testNav.getNode("search").getNode("hippo:resultset").getProperty("hippo:count").getLong());
            assertFalse(testNav.getNode("search").getNode("hippo:resultset").hasNode(TEST_USER_ID));
        }
        
        // Add a domain rule with facetrule such that the userSession can now read the node it cannot read above by
        // setting a facetrule that nodes of name __user__ can be read. user is expanded by the userId of the userSession
        Node dr  = readDomain.addNode("hippo:domainrule", HippoNodeType.NT_DOMAINRULE);
        Node fr  = dr.addNode("hippo:facetrule", HippoNodeType.NT_FACETRULE);
        fr.setProperty(HippoNodeType.HIPPO_FACET, "nodename");
        fr.setProperty(HippoNodeType.HIPPOSYS_VALUE, "__user__");
        fr.setProperty(HippoNodeType.HIPPOSYS_TYPE, "Name");
        session.save();

        // setup CLEAN user session
        userSession.logout();
        userSession = server.login(TEST_USER_ID, TEST_USER_PASS.toCharArray());

        {
            Node testData = userSession.getRootNode().getNode(TEST_DATA_NODE);
            Node testNav  = userSession.getRootNode().getNode(TEST_NAVIGATION_NODE);
            // the node is now readable currently for userSession
            // THUS
            // 1 hasNode returns true
            assertTrue(testData.hasNode("expanders/" + TEST_USER_ID));
            // 2 there is a search hit
            QueryManager queryManager = userSession.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery("//element("+TEST_USER_ID+",hippo:authtestdocument) order by @jcr:score", Query.XPATH);
            HippoNodeIterator iter = (HippoNodeIterator) query.execute().getNodes();
            assertEquals(1L, iter.getSize());
            assertEquals(1L, iter.getTotalSize());
            //3 It is in the faceted navigation result
            // we should now have count + 1
            assertEquals("Unexpected count for resultset", count + 1, testNav.getNode("search").getNode("hippo:resultset").getProperty("hippo:count").getLong());
            assertTrue(testNav.getNode("search").getNode("hippo:resultset").hasNode(TEST_USER_ID));
        }
    }

    @Test
    public void testNonDocumentTypeNodesAreAuthorized() throws RepositoryException {
        Node dr  = readDomain.addNode("hippo:domainrule", HippoNodeType.NT_DOMAINRULE);
        Node fr  = dr.addNode("hippo:facetrule", HippoNodeType.NT_FACETRULE);
        fr.setProperty(HippoNodeType.HIPPO_FACET, "jcr:primaryType");
        fr.setProperty(HippoNodeType.HIPPOSYS_VALUE, "hippo:authtestdocument");
        fr.setProperty(HippoNodeType.HIPPOSYS_TYPE, "Name");
        {
            Node testData = session.getRootNode().getNode(TEST_DATA_NODE);
            testData.addNode("readable", "hippo:authtestdocument");
        }
        session.save();

        // setup user session
        userSession.logout();
        userSession = server.login(TEST_USER_ID, TEST_USER_PASS.toCharArray());

        assertTrue(userSession.getRootNode().getNode(TEST_DATA_NODE).hasNode("readable"));

        QueryManager queryManager = userSession.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery("//element(readable,hippo:authtestdocument) order by @jcr:score", Query.XPATH);
        HippoNodeIterator iter = (HippoNodeIterator) query.execute().getNodes();
        assertEquals(1L, iter.getSize());
        assertEquals(1L, iter.getTotalSize());
    }

    @Test
    public void testSubReadsAllowed() throws RepositoryException {
        Node testData = userSession.getRootNode().getNode(TEST_DATA_NODE);
        assertTrue(testData.hasNode("readdoc0/subread"));
        assertTrue(testData.hasNode("readdoc0/subwrite"));
        assertTrue(testData.hasNode("writedoc0/subread"));
        assertTrue(testData.hasNode("writedoc0/subwrite"));
    }

    @Test
    public void testSubReadsNotAllowed() throws RepositoryException {
        Node testData = userSession.getRootNode().getNode(TEST_DATA_NODE);
        assertFalse(testData.hasNode("readdoc0/subnothing"));
        assertFalse(testData.hasNode("writedoc0/subnothing"));
        assertFalse(testData.hasNode("nothing0/subread"));
        assertFalse(testData.hasNode("nothing0/subwrite"));
    }

    @Test
    public void testWritesAllowed() throws RepositoryException {
        Node node;
        Node testData = userSession.getRootNode().getNode(TEST_DATA_NODE);
        userSession.checkPermission(testData.getPath() + "/" + "writedoc0" , SET_PROPERTY_ACTION);
        node = testData.getNode("writedoc0");
        node.setProperty("test", "allowed");
        userSession.save();

        userSession.checkPermission(testData.getPath() + "/" + "writedoc0" , ADD_NODE_ACTION);
        node = testData.getNode("writedoc0");
        node.addNode("newnode", "hippo:authtestdocument").setProperty("authtest", "canwrite");
        userSession.save();
    }

    @Test
    public void testSubWritesAllowed() throws RepositoryException {
        Node node;
        Node testData = userSession.getRootNode().getNode(TEST_DATA_NODE);
        node = testData.getNode("readdoc0/subwrite");
        node.setProperty("test", "allowed");
        userSession.save();

        node = testData.getNode("readdoc0/subwrite");
        node.addNode("newnode", "hippo:authtestdocument").setProperty("authtest", "canwrite");
        userSession.save();
    }

    @Test
    public void testWritesNotAllowed() throws RepositoryException {
        Node node;
        Node testData = userSession.getRootNode().getNode(TEST_DATA_NODE);
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
        Node testData = userSession.getRootNode().getNode(TEST_DATA_NODE);
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
        Node testData = userSession.getRootNode().getNode(TEST_DATA_NODE);
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
        Node testData = userSession.getRootNode().getNode(TEST_DATA_NODE);
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
        Node testData = userSession.getRootNode().getNode(TEST_DATA_NODE);
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
        Node testData = userSession.getRootNode().getNode(TEST_DATA_NODE);
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
            node.addNode("mynode1", "hippo:authtestdocument").setProperty("authtest", "none");
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
            node.addNode("mynode2", "hippo:authtestdocument").setProperty("authtest", "canread");
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
        Node testNav  = userSession.getRootNode().getNode(TEST_NAVIGATION_NODE);
        Node navNode = testNav.getNode("search");
        Node resultSetNode = navNode.getNode("hippo:resultset");
        assertTrue(resultSetNode.hasNode("readdoc0"));
        assertTrue(resultSetNode.hasNode("writedoc0"));
        NodeIterator iter = resultSetNode.getNodes();
        int count = 0;
        while(iter.hasNext()) {
            Node n = iter.nextNode();
            if (n == null) {
                fail("null node in resultset child node iterator");
            }
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

        // Nodes 'nothing0/subread' and 'nothing0/subwrite' are counted but not instantiated.
        // The hierarchical constraint (can read parent) is not taken into account.
        assertEquals(12L, resultSetNode.getProperty(HippoNodeType.HIPPO_COUNT).getLong());
    }

    @Test
    public void testFacetSelect() throws RepositoryException {
        Node testData = userSession.getRootNode().getNode(TEST_DATA_NODE);
        Node testNav  = userSession.getRootNode().getNode(TEST_NAVIGATION_NODE);
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
        Query query = queryManager.createQuery("//element(*,hippo:authtestdocument) order by @jcr:score", Query.XPATH);
        NodeIterator iter = query.execute().getNodes();
        assertEquals(12L, iter.getSize());

        // explicitly authorize nodes by fetching them
        while (iter.hasNext()) {
            iter.nextNode();
        }
        assertEquals(10L, iter.getSize());

        // Nodes 'nothing0/subread' and 'nothing0/subwrite' are counted but not instantiated.
        // The hierarchical constraint (can read parent) is not taken into account.
        assertEquals(12L, ((HippoNodeIterator) iter).getTotalSize());
    }

    @Test
    public void testQueryFilterIsInvalidated() throws RepositoryException {
        // make sure the authorization filter is loaded
        {
            QueryManager queryManager = userSession.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery("//element(*,hippo:authtestdocument) order by @jcr:score", Query.XPATH);
            query.execute().getNodes();
        }

        Node testData = session.getRootNode().getNode(TEST_DATA_NODE);
        testData.getNode("nothing0").setProperty("authtest", "canread");
        session.save();

        userSession.logout();
        userSession = server.login(TEST_USER_ID, TEST_USER_PASS.toCharArray());

        QueryManager queryManager = userSession.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery("//element(*,hippo:authtestdocument) order by @jcr:score", Query.XPATH);
        NodeIterator iter = query.execute().getNodes();
        assertEquals(13L, iter.getSize());
        assertEquals(13L, ((HippoNodeIterator) iter).getTotalSize());
    }

    @Test
    public void testQuerySQL2() throws RepositoryException {
        final QueryManager queryManager = userSession.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery("SELECT * FROM [hippo:authtestdocument]", Query.JCR_SQL2);
        NodeIterator iter = query.execute().getNodes();
        assertEquals(10L, iter.getSize());

        query = queryManager.createQuery("SELECT * FROM [hippo:authtestdocument] WHERE [jcr:name] = 'subread'", Query.JCR_SQL2);
        iter = query.execute().getNodes();
        assertEquals(2L, iter.getSize());

        query = queryManager.createQuery("SELECT child.[jcr:uuid] AS identifier FROM [nt:base] AS child " +
                "INNER JOIN [hippo:authtestdocument] AS parent ON ISCHILDNODE(child,parent)", Query.JCR_SQL2);
        RowIterator rows = query.execute().getRows();
        assertEquals(7L, rows.getSize());

        query = queryManager.createQuery("SELECT child.[jcr:uuid] AS identifier FROM [nt:base] AS child " +
                "INNER JOIN [hippo:authtestdocument] AS parent ON ISCHILDNODE(child,parent) " +
                "WHERE parent.[jcr:name] = 'readdoc0'", Query.JCR_SQL2);
        rows = query.execute().getRows();
        assertEquals(2L, rows.getSize());
    }

    @Test
    public void testQueryWithNodenameFilter() throws RepositoryException {
        Node testData = session.getRootNode().getNode(TEST_DATA_NODE);
        testData.getNode("readdoc0").addNode("readable", "hippo:authtestdocument");
        testData.getNode("readdoc0").addNode("writable", "hippo:authtestdocument");
        testData.getNode("readdoc0").addNode("invisible", "hippo:authtestdocument");

        Node dr = readDomain.addNode("hippo:domainrule", HippoNodeType.NT_DOMAINRULE);
        Node fr = dr.addNode("hippo:facetrule", HippoNodeType.NT_FACETRULE);
        fr.setProperty(HippoNodeType.HIPPO_FACET, "nodename");
        fr.setProperty(HippoNodeType.HIPPOSYS_VALUE, "readable");
        fr.setProperty(HippoNodeType.HIPPOSYS_TYPE, "Name");

        dr  = writeDomain.addNode("hippo:domainrule", HippoNodeType.NT_DOMAINRULE);
        fr  = dr.addNode("hippo:facetrule", HippoNodeType.NT_FACETRULE);
        fr.setProperty(HippoNodeType.HIPPO_FACET, "nodename");
        fr.setProperty(HippoNodeType.HIPPOSYS_VALUE, "writable");
        fr.setProperty(HippoNodeType.HIPPOSYS_TYPE, "Name");

        session.save();

        Session userSession = server.login(TEST_USER_ID, TEST_USER_PASS.toCharArray());
        testData = userSession.getRootNode().getNode(TEST_DATA_NODE);
        assertTrue(testData.hasNode("readdoc0/readable"));
        assertTrue(testData.hasNode("readdoc0/writable"));
        assertFalse(testData.hasNode("readdoc0/invisible"));

        QueryManager queryManager = userSession.getWorkspace().getQueryManager();
        // XPath doesn't like the query from the root
        Query query = queryManager.createQuery("//element(*,hippo:authtestdocument) order by @jcr:score", Query.XPATH);
        NodeIterator iter = query.execute().getNodes();

        // Nodes 'nothing0/subread' and 'nothing0/subwrite' are counted but not instantiated.
        // The hierarchical constraint (can read parent) is not taken into account.
        assertEquals(14L, iter.getSize());
        // explicitly authorize
        while (iter.hasNext()) {
            iter.nextNode();
        }
        assertEquals(12L, iter.getSize());
        assertEquals(14L, ((HippoNodeIterator) iter).getTotalSize());
    }

    @Test
    public void testOptionalFacetPlain() throws RepositoryException {
        testOptionalFacet("optionalvalue", "optionalvalue");
    }

    @Test
    public void testOptionalFacetWithExpander() throws RepositoryException {
        testOptionalFacet("__group__", TEST_GROUP_ID);
    }

    private void testOptionalFacet(final String optionalFacetValue, final String optionalPropertyValue) throws RepositoryException {
        Node testData = session.getRootNode().getNode(TEST_DATA_NODE);
        testData.getNode("readdoc0").getNode("subread").setProperty("optionalfacet", optionalPropertyValue);

        final Node otherDoc = testData.getNode("readdoc0").addNode("subhidden", "hippo:authtestdocument");
        otherDoc.setProperty("optionalfacet", "incorrectvalue");
        otherDoc.setProperty("authtest", "canread");

        Node dr = readDomain.getNode("hippo:domainrule");
        Node fr = dr.addNode("hippo:facetrule", HippoNodeType.NT_FACETRULE);
        fr.setProperty(HippoNodeType.HIPPO_FACET, "optionalfacet");
        fr.setProperty(HippoNodeType.HIPPOSYS_VALUE, optionalFacetValue);
        fr.setProperty(HippoNodeType.HIPPOSYS_TYPE, "String");
        fr.setProperty(HippoNodeType.HIPPOSYS_FILTER, true);

        session.save();

        Session userSession = server.login(TEST_USER_ID, TEST_USER_PASS.toCharArray());
        testData = userSession.getRootNode().getNode(TEST_DATA_NODE);
        assertTrue(testData.hasNode("readdoc0/subread"));
        assertFalse(testData.hasNode("readdoc0/subhidden"));

        QueryManager queryManager = userSession.getWorkspace().getQueryManager();
        // XPath doesn't like the query from the root
        Query query = queryManager.createQuery("//element(*,hippo:authtestdocument) order by @jcr:score", Query.XPATH);
        NodeIterator iter = query.execute().getNodes();
        assertEquals(12L, iter.getSize());

        boolean found = false;
        while (iter.hasNext()) {
            if ((testData.getPath() + "/readdoc0/subread").equals(iter.nextNode().getPath())) {
                found = true;
            }
        }
        assertEquals(10L, iter.getSize());
        assertTrue("readable node with optional facet was not found in result set", found);

        // Nodes 'nothing0/subread' and 'nothing0/subwrite' are counted but not instantiated.
        // The hierarchical constraint (can read parent) is not taken into account.
        assertEquals(12L, ((HippoNodeIterator) iter).getTotalSize());
    }

    @Test
    public void testQuerySQL() throws RepositoryException {
        QueryManager queryManager = userSession.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery("SELECT * FROM hippo:authtestdocument ORDER BY jcr:score", Query.SQL);
        NodeIterator iter = query.execute().getNodes();

        // Nodes 'nothing0/subread' and 'nothing0/subwrite' are counted but not instantiated.
        // The hierarchical constraint (can read parent) is not taken into account.
        assertEquals(12L, iter.getSize());

        // explicitly authorize
        while (iter.hasNext()) {
            iter.nextNode();
        }
        assertEquals(10L, iter.getSize());

        assertEquals(12L, ((HippoNodeIterator) iter).getTotalSize());
    }

    @Test
    public void testCheckPermissionPrivilegeJcrRead() throws RepositoryException {
        Node testData = userSession.getRootNode().getNode(TEST_DATA_NODE);
        userSession.checkPermission(testData.getPath() + "/readdoc0", "jcr:read");
        userSession.checkPermission(testData.getPath() + "/readdoc0/subread", "jcr:read");
        userSession.checkPermission(testData.getPath() + "/writedoc0/subread", "jcr:read");
    }

    @Test
    public void testCheckPermissionPrivilegeJcrWrite() throws RepositoryException {
        Node testData = userSession.getRootNode().getNode(TEST_DATA_NODE);
        userSession.checkPermission(testData.getPath() + "/writedoc0", "jcr:write");
        userSession.checkPermission(testData.getPath() + "/writedoc0/subwrite", "jcr:write");
        userSession.checkPermission(testData.getPath() + "/readdoc0/subwrite", "jcr:write");
    }


    @Test
    public void testCheckPermissionNotPrivilegeJcrRead() throws RepositoryException {
        Node testData = userSession.getRootNode().getNode(TEST_DATA_NODE);
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
        Node testData = userSession.getRootNode().getNode(TEST_DATA_NODE);
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

    @Test
    public void testDelegatedSessionConcatenatesUserIDs() throws RepositoryException {
        final Session adminSession = userSession.impersonate(new SimpleCredentials("admin", "admin".toCharArray()));
        Session extendedSession = ((HippoSession) userSession).createSecurityDelegate(adminSession);

        assertEquals("admin," + TEST_USER_ID, extendedSession.getUserID());

        extendedSession.logout();
        adminSession.logout();
    }

    @Test
    public void testDelegatedSessionWithoutDomainRuleExtensions() throws RepositoryException {
        final Session adminSession = userSession.impersonate(new SimpleCredentials("admin", "admin".toCharArray()));
        Session extendedSession = ((HippoSession) userSession).createSecurityDelegate(adminSession);
        QueryManager queryManager = extendedSession.getWorkspace().getQueryManager();

        // XPath doesn't like the query from the root
        Query query = queryManager.createQuery("//element(*,hippo:authtestdocument) order by @jcr:score", Query.XPATH);
        NodeIterator iter = query.execute().getNodes();

        // all nodes can be read
        assertEquals(19L, iter.getSize());

        extendedSession.logout();

        adminSession.logout();
    }

    @Test
    public void testDelegatedSessionWithNamedDomainRuleExtension() throws RepositoryException {
        final Session adminSession = userSession.impersonate(new SimpleCredentials("admin", "admin".toCharArray()));
        final FacetRule facetRule = new FacetRule("authtest", "canread", true, false, PropertyType.STRING);

        Session extendedSession = ((HippoSession) userSession).createSecurityDelegate(adminSession,
                new DomainRuleExtension("everywhere", "all-nodes", Arrays.asList(facetRule)),
                new DomainRuleExtension("hippodocuments", "hippo-document", Arrays.asList(facetRule)));
        QueryManager queryManager = extendedSession.getWorkspace().getQueryManager();

        // XPath doesn't like the query from the root
        Query query = queryManager.createQuery("//element(*,hippo:authtestdocument) order by @jcr:score", Query.XPATH);
        NodeIterator iter = query.execute().getNodes();

        // Nodes 'nothing0/subread' and 'nothing0/subwrite' are counted but not instantiated.
        // The hierarchical constraint (can read parent) is not taken into account.
        assertEquals(15L, iter.getSize());
        // fetch results for forced authorization
        while (iter.hasNext()) {
            iter.nextNode();
        }
        assertEquals(13L, iter.getSize());
        assertEquals(15L, ((HippoNodeIterator) iter).getTotalSize());

        extendedSession.logout();

        adminSession.logout();
    }

    @Test
    public void testDelegatedSessionWithWildcardDomainRuleExtension() throws RepositoryException {
        final Session adminSession = userSession.impersonate(new SimpleCredentials("admin", "admin".toCharArray()));
        final FacetRule facetRule = new FacetRule("authtest", "canread", true, false, PropertyType.STRING);

        Session extendedSession = ((HippoSession) userSession).createSecurityDelegate(adminSession,
                new DomainRuleExtension("everywhere", "*", Arrays.asList(facetRule)),
                new DomainRuleExtension("hippodocuments", "*", Arrays.asList(facetRule)));
        QueryManager queryManager = extendedSession.getWorkspace().getQueryManager();

        // XPath doesn't like the query from the root
        Query query = queryManager.createQuery("//element(*,hippo:authtestdocument) order by @jcr:score", Query.XPATH);
        NodeIterator iter = query.execute().getNodes();

        // Nodes 'nothing0/subread' and 'nothing0/subwrite' are counted but not instantiated.
        // The hierarchical constraint (can read parent) is not taken into account.
        assertEquals(15L, iter.getSize());
        // fetch results for forced authorization
        while (iter.hasNext()) {
            iter.nextNode();
        }
        assertEquals(13L, iter.getSize());
        assertEquals(15L, ((HippoNodeIterator) iter).getTotalSize());

        extendedSession.logout();

        adminSession.logout();
    }
}
