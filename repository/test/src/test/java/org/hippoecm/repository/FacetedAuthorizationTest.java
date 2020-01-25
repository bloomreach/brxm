/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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

import java.math.BigDecimal;
import java.security.AccessControlException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.RowIterator;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.core.query.lucene.DecimalField;
import org.assertj.core.api.Assertions;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeIterator;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.security.HippoAccessManager;
import org.hippoecm.repository.util.JcrUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.repository.security.DomainInfoPrivilege;
import org.onehippo.repository.security.domain.DomainRuleExtension;
import org.onehippo.repository.security.domain.FacetRule;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hippoecm.repository.HippoStdNodeType.NT_RELAXED;
import static org.hippoecm.repository.api.HippoNodeType.CONFIGURATION_PATH;
import static org.hippoecm.repository.api.HippoNodeType.DOMAINS_PATH;
import static org.hippoecm.repository.api.HippoNodeType.GROUPS_PATH;
import static org.hippoecm.repository.api.HippoNodeType.USERS_PATH;
import static org.hippoecm.repository.util.JcrUtils.getMultipleStringProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.onehippo.repository.security.StandardPermissionNames.JCR_ADD_CHILD_NODES;
import static org.onehippo.repository.security.StandardPermissionNames.JCR_MODIFY_PROPERTIES;
import static org.onehippo.repository.security.StandardPermissionNames.JCR_READ;
import static org.onehippo.repository.security.StandardPermissionNames.JCR_REMOVE_CHILD_NODES;
import static org.onehippo.repository.security.StandardPermissionNames.JCR_REMOVE_NODE;
import static org.onehippo.repository.security.StandardPermissionNames.JCR_WRITE;

public class FacetedAuthorizationTest extends RepositoryTestCase {

    Node testDataDomain;
    Node hipDocDomain;
    Node readDomain;
    Node writeDomain;
    Node testUser;

    HippoSession userSession;

    // nodes that have to be cleaned up
    private static final String DOMAIN_HANDLE_NODE = "test-domain-handle";
    private static final String DOMAIN_TEST_DATA_NODE = "testdata";
//    private static final String DOMAIN_HIPPO_UNSTRUCTURED_NODE = "test-domain-hippo-unstructured";
    private static final String DOMAIN_NAVIGATION = "navigation";
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
        Node config = session.getRootNode().getNode(CONFIGURATION_PATH);
        Node domains = config.getNode(DOMAINS_PATH);
        Node users = config.getNode(USERS_PATH);
        Node groups = config.getNode(GROUPS_PATH);

        groups.getNode("admin").setProperty("hipposys:members", new String[]{});

        cleanupDomains(domains);

        cleanupUserAndGroup(users, groups);

        cleanupTestData();

        cleanupTestNavigation();

        session.save();
    }

    private void cleanupDomains(final Node domains) throws RepositoryException {

        for (String domain : new String[]{DOMAIN_DOC_NODE, DOMAIN_READ_NODE, DOMAIN_WRITE_NODE, DOMAIN_NAVIGATION,
                "defaultread/" + DOMAIN_HANDLE_NODE, "defaultread/" + DOMAIN_TEST_DATA_NODE }) {
            if (domains.hasNode(domain)) {
                domains.getNode(domain).remove();
            }
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
        Node config = session.getRootNode().getNode(CONFIGURATION_PATH);
        Node domains = config.getNode(DOMAINS_PATH);
        Node users = config.getNode(USERS_PATH);
        Node groups = config.getNode(GROUPS_PATH);
        /*   Temporarily adding admin user to admin group to pass the
             testDelegatedSessionWithWildcardDomainRuleExtension() and
             testDelegatedSessionWithNamedDomainRuleExtension() tests
         */
        groups.getNode("admin").setProperty("hipposys:members", new String[]{"admin"});

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
        userSession = (HippoSession)server.login(TEST_USER_ID, TEST_USER_PASS.toCharArray());
    }

    private void createUserAndGroup(final Node users, final Node groups) throws RepositoryException {
        testUser = users.addNode(TEST_USER_ID, HippoNodeType.NT_USER);
        testUser.setProperty(HippoNodeType.HIPPO_PASSWORD, TEST_USER_PASS);

        // create test group with member test
        Node testGroup = groups.addNode(TEST_GROUP_ID, HippoNodeType.NT_GROUP);
        testGroup.setProperty(HippoNodeType.HIPPO_MEMBERS, new String[] { TEST_USER_ID });
    }

    private void createDomains(final Node domains) throws RepositoryException {

        final Node defaultRead = session.getNode("/hippo:configuration/hippo:domains/defaultread");
        testDataDomain = defaultRead.addNode(DOMAIN_TEST_DATA_NODE, "hipposys:domainrule");

        final Node facetRule = testDataDomain.addNode("read-to-test-node", "hipposys:facetrule");
        facetRule.setProperty("hipposys:equals", true);
        facetRule.setProperty("hipposys:facet", "jcr:uuid");
        facetRule.setProperty("hipposys:type", "Reference");
        facetRule.setProperty("hipposys:value", "/testdata");

        // create hippodoc domain
        hipDocDomain = domains.addNode(DOMAIN_DOC_NODE, HippoNodeType.NT_DOMAIN);
        Node ar = hipDocDomain.addNode("readonly", HippoNodeType.NT_AUTHROLE);
        ar.setProperty(HippoNodeType.HIPPO_ROLE, "readonly");
        ar.setProperty(HippoNodeType.HIPPO_USERS, new String[]{TEST_USER_ID});

        Node dr = hipDocDomain.addNode("testdocument-type", HippoNodeType.NT_DOMAINRULE);
        Node fr = dr.addNode("testdocument-type-rule", HippoNodeType.NT_FACETRULE);
        fr.setProperty(HippoNodeType.HIPPO_FACET, "nodetype");
        fr.setProperty(HippoNodeType.HIPPOSYS_VALUE, "hippo:testdocument");
        fr.setProperty(HippoNodeType.HIPPOSYS_TYPE, "Name");

        // add default read for 'hippo:handle' and 'DOMAIN_NAVIGATION' since these tests rely on logic that
        // handle and 'DOMAIN_NAVIGATION' nodes can be read
        readDomain = domains.getNode("defaultread").addNode(DOMAIN_HANDLE_NODE, HippoNodeType.NT_DOMAINRULE);
        fr = readDomain.addNode("match-handle-node", HippoNodeType.NT_FACETRULE);
        fr.setProperty(HippoNodeType.HIPPO_FACET, "nodetype");
        fr.setProperty(HippoNodeType.HIPPO_EQUALS, true);
        fr.setProperty(HippoNodeType.HIPPOSYS_VALUE, "hippo:handle");
        fr.setProperty(HippoNodeType.HIPPOSYS_TYPE, "String");

        // create read domain
        readDomain = domains.addNode(DOMAIN_NAVIGATION, HippoNodeType.NT_DOMAIN);
        ar = readDomain.addNode("readonly", HippoNodeType.NT_AUTHROLE);
        ar.setProperty(HippoNodeType.HIPPO_ROLE, "readonly");
        ar.setProperty(HippoNodeType.HIPPO_GROUPS, new String[]{"everybody"});
        dr = readDomain.addNode("read-below-navigation", HippoNodeType.NT_DOMAINRULE);
        fr = dr.addNode("read-below-navigation-rule", HippoNodeType.NT_FACETRULE);
        fr.setProperty(HippoNodeType.HIPPO_FACET, "jcr:path");
        fr.setProperty(HippoNodeType.HIPPOSYS_VALUE, "/navigation");
        fr.setProperty(HippoNodeType.HIPPOSYS_TYPE, "Reference");
        fr.setProperty(HippoNodeType.HIPPO_EQUALS, true);

        readDomain = domains.addNode(DOMAIN_READ_NODE, HippoNodeType.NT_DOMAIN);
        ar = readDomain.addNode("readonly", HippoNodeType.NT_AUTHROLE);
        ar.setProperty(HippoNodeType.HIPPO_ROLE, "readonly");
        ar.setProperty(HippoNodeType.HIPPO_USERS, new String[]{TEST_USER_ID});

        dr = readDomain.addNode("authtest-read", HippoNodeType.NT_DOMAINRULE);
        fr = dr.addNode("authtest-read-rule", HippoNodeType.NT_FACETRULE);
        fr.setProperty(HippoNodeType.HIPPO_FACET, "authtest");
        fr.setProperty(HippoNodeType.HIPPOSYS_VALUE, "canread");
        fr.setProperty(HippoNodeType.HIPPOSYS_TYPE, "String");

        dr = readDomain.addNode("user", HippoNodeType.NT_DOMAINRULE);
        fr = dr.addNode("user-rule", HippoNodeType.NT_FACETRULE);
        fr.setProperty(HippoNodeType.HIPPO_FACET, "user");
        fr.setProperty(HippoNodeType.HIPPOSYS_VALUE, "__user__");
        fr.setProperty(HippoNodeType.HIPPOSYS_TYPE, "String");

        dr = readDomain.addNode("group", HippoNodeType.NT_DOMAINRULE);
        fr = dr.addNode("group-rule", HippoNodeType.NT_FACETRULE);
        fr.setProperty(HippoNodeType.HIPPO_FACET, "group");
        fr.setProperty(HippoNodeType.HIPPOSYS_VALUE, "__group__");
        fr.setProperty(HippoNodeType.HIPPOSYS_TYPE, "String");

        dr = readDomain.addNode("role", HippoNodeType.NT_DOMAINRULE);
        fr = dr.addNode("role-rule", HippoNodeType.NT_FACETRULE);
        fr.setProperty(HippoNodeType.HIPPO_FACET, "role");
        fr.setProperty(HippoNodeType.HIPPOSYS_VALUE, "__role__");
        fr.setProperty(HippoNodeType.HIPPOSYS_TYPE, "String");

        // create write domain
        writeDomain = domains.addNode(DOMAIN_WRITE_NODE, HippoNodeType.NT_DOMAIN);
        ar = writeDomain.addNode("readwrite", HippoNodeType.NT_AUTHROLE);
        ar.setProperty(HippoNodeType.HIPPO_ROLE, "readwrite");
        ar.setProperty(HippoNodeType.HIPPO_USERS, new String[]{TEST_USER_ID});

        dr = writeDomain.addNode("authtest-write", HippoNodeType.NT_DOMAINRULE);
        fr = dr.addNode("authtest-write-rule", HippoNodeType.NT_FACETRULE);
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
        handle.addMixin("mix:referenceable");


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
        handle.addMixin("mix:referenceable");

        handle.addNode("hippo:request", "hippo:request");

        handle.addNode("doc", "hippo:authtestdocument");

        Node doc = handle.addNode("doc", "hippo:authtestdocument");
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
        handle.addMixin("mix:referenceable");


        Node doc = handle.addNode("doc", "hippo:authtestdocument");
        doc = handle.addNode("doc", "hippo:authtestdocument");
        doc.setProperty("authtest", "canread");

        session.save();

        Node userTestData = userSession.getRootNode().getNode(TEST_DATA_NODE);
        Node userHandle = userTestData.getNode("doc");

        // doc node is still coupled to the ADMIN_ID based jcr session
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
        handle.addMixin("mix:referenceable");


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
        handle.addMixin("mix:referenceable");


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
        handle.addMixin("mix:referenceable");


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
        handle.addMixin("mix:referenceable");


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
    public void testAccessForChildNodeOfDocument() throws RepositoryException {
        Node testData = session.getRootNode().getNode(TEST_DATA_NODE);
        final Node handle = testData.addNode("doc", "hippo:handle");
        handle.addMixin("mix:referenceable");


        Node doc = handle.addNode("doc", "hippo:authtestdocument");
        doc.addMixin("hippo:container");
        final Node childNode = doc.addNode("level1", "hippo:testcomposite");
        childNode.addNode("level2", "hippo:testcomposite");
        {
            final Node link = childNode.addNode("link", "hippo:facetselect");
            link.setProperty(HippoNodeType.HIPPO_FACETS, new String[]{});
            link.setProperty(HippoNodeType.HIPPO_MODES, new String[]{});
            link.setProperty(HippoNodeType.HIPPO_VALUES, new String[]{});
            link.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getIdentifier());
        }
        {
            final Node mirror = childNode.addNode("mirror", "hippo:mirror");
            mirror.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getIdentifier());
        }

        session.save();

        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc", Session.ACTION_READ));
        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc", Session.ACTION_ADD_NODE));

        doc.setProperty("authtest", "canread");
        session.save();

        // read should now be allowed to read 'doc/doc'
        assertTrue(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc", Session.ACTION_READ));
        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc", Session.ACTION_ADD_NODE));

        doc.setProperty("authtest", "canwrite");
        session.save();
        assertTrue(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc", Session.ACTION_READ));

        // See JCR Spec 16.6.2 table: The Session Actions vs Privileges
        assertTrue(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc", JCR_MODIFY_PROPERTIES));
        assertTrue(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/newprop", Session.ACTION_SET_PROPERTY));
        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/newprop", Session.ACTION_SET_PROPERTY));
        assertTrue("REPO-1971 Session.ACTION_REMOVE check on non-existing document is allowed",
                userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/dummy", Session.ACTION_REMOVE));
        try {
            userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/dummy", JCR_REMOVE_NODE);
            fail("REPO-1971: jcr:removeNode permission check on a non-existing path should fail with PathNotFoundException");
        } catch (PathNotFoundException ignore) {
        }
        assertTrue(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc", JCR_REMOVE_CHILD_NODES));

        assertTrue(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/newnode", Session.ACTION_ADD_NODE));
        assertTrue(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc", JCR_ADD_CHILD_NODES));
        userSession.getNode("/" + TEST_DATA_NODE + "/doc/doc").addNode("foo", "hippo:testcomposite");
        userSession.save();

        doc.setProperty("authtest", "canread");
        session.save();
        assertTrue(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc", Session.ACTION_READ));
        // READ is NOT inherited *any more since 14.0* to descendants of a document!
        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1", Session.ACTION_READ));

        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1", JCR_MODIFY_PROPERTIES));
        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1/newprop", Session.ACTION_SET_PROPERTY));
        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/newprop", Session.ACTION_SET_PROPERTY));
        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1/dummy", Session.ACTION_REMOVE));
        try {
            userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1/dummy", JCR_REMOVE_NODE);
            fail("REPO-1971: jcr:removeNode permission check on a non-existing path should fail with PathNotFoundException");
        } catch (PathNotFoundException ignore) {
        }
        assertFalse("REPO-1971 'level1/level2' does exist and should not be allowed to be removed by the userSession",
                userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1/level2", JCR_REMOVE_NODE));
        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1", JCR_REMOVE_CHILD_NODES));

        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1/newnode", Session.ACTION_ADD_NODE));
        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1", JCR_ADD_CHILD_NODES));

        doc.setProperty("authtest", "canwrite");
        session.save();
        assertTrue("'canwrite' results in role 'readwrite' which implies also read",
                userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc", Session.ACTION_READ));

        // jcr:write *IS* inherited *only* to *READABLE* descendants of a document!
        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1", Session.ACTION_READ));

        // jcr:write *IS* inherited to *only* READABLE descendants of a document
        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1", JCR_MODIFY_PROPERTIES));
        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1/newprop", Session.ACTION_SET_PROPERTY));
        assertTrue("user should have write access to properties of document",
                userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/newprop", Session.ACTION_SET_PROPERTY));
        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1/dummy", Session.ACTION_REMOVE));
        try {
            userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1/dummy", JCR_REMOVE_NODE);
            fail("REPO-1971: jcr:removeNode permission check on a non-existing path should fail with PathNotFoundException");
        } catch (PathNotFoundException ignore) {
        }
        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1/level2", JCR_REMOVE_NODE));
        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1", JCR_REMOVE_CHILD_NODES));

        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1/newnode", Session.ACTION_ADD_NODE));
        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1", JCR_ADD_CHILD_NODES));

        // make /doc/doc/level1 node readable. Now the permissions should be inherited from /doc/doc
        doc.getNode("level1").setProperty("authtest", "canread");
        session.save();

        assertTrue(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1", Session.ACTION_READ));
        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1/level2", Session.ACTION_READ));

        // Permissions should be implicitly inherited on READABLE descendant nodes
        assertTrue(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1", JCR_MODIFY_PROPERTIES));
        assertTrue(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1/newprop", Session.ACTION_SET_PROPERTY));
        assertTrue(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1/dummy", Session.ACTION_REMOVE));
        try {
            userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1/dummy", JCR_REMOVE_NODE);
            fail("REPO-1971: jcr:removeNode permission check on a non-existing path should fail with PathNotFoundException");
        } catch (PathNotFoundException ignore) {
        }
        assertFalse("'level2' node is still not readable and thus should not inherit jcr:write",
                userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1/level2", JCR_REMOVE_NODE));

        assertTrue(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1", JCR_REMOVE_CHILD_NODES));

        assertTrue(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1/newnode", Session.ACTION_ADD_NODE));
        assertTrue(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1", JCR_ADD_CHILD_NODES));

        // make /doc/doc/level1/level2 node readable. Now the permissions should be inherited from /doc/doc on level2 as well
        doc.getNode("level1/level2").setProperty("authtest", "canread");
        session.save();

        assertTrue(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1", Session.ACTION_READ));
        assertTrue(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1/level2", Session.ACTION_READ));

        // Permissions should be implicitly inherited on READABLE descendant nodes
        assertTrue(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1/level2", JCR_MODIFY_PROPERTIES));
        assertTrue(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1/level2/newprop", Session.ACTION_SET_PROPERTY));
        assertTrue(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1/level2/dummy", Session.ACTION_REMOVE));
        try {
            userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1/level2/dummy", JCR_REMOVE_NODE);
            fail("REPO-1971: jcr:removeNode permission check on a non-existing path should fail with PathNotFoundException");
        } catch (PathNotFoundException ignore) {
        }
        assertTrue("'level2' node is still not readable and thus should not inherit jcr:write",
                userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1/level2", JCR_REMOVE_NODE));
        assertTrue(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1/level2", JCR_REMOVE_CHILD_NODES));

        assertTrue(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1/level2/newnode", Session.ACTION_ADD_NODE));
        assertTrue(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1/level2", JCR_ADD_CHILD_NODES));


        // MIRROR NODETYPE TESTS

        // 'level1/mirror' is not readable
        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1/mirror", Session.ACTION_READ));
        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1/mirror", JCR_REMOVE_NODE));

        // make 'mirror' readable, now it should also receive the write access from level1
        final Node mirror = session.getNode("/" + TEST_DATA_NODE + "/doc/doc/level1/mirror");
        mirror.addMixin(NT_RELAXED);
        mirror.setProperty("authtest", "canread");
        session.save();

        // because of mirror #^#^#$^&*%^#$ blah logic we need refresh user session, sigh
        userSession.refresh(false);

        assertTrue(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1/mirror", Session.ACTION_READ));
        // 'level1/mirror' is now readable and thus should inherit write access
        assertTrue(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1/mirror", JCR_REMOVE_NODE));

        // FACETSELECT TESTS

        // 'level1/link' is not readable
        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1/link", Session.ACTION_READ));
        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1/link", JCR_REMOVE_NODE));

        // make 'link' readable, now it should also receive the write access from level1
        final Node link = session.getNode("/" + TEST_DATA_NODE + "/doc/doc/level1/link");
        link.addMixin(NT_RELAXED);
        link.setProperty("authtest", "canread");
        session.save();

        // because of facetselect #^#^#$^&*%^#$ blah logic we need refresh user session, sigh
        userSession.refresh(false);


        assertTrue(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1/link", Session.ACTION_READ));
        // 'level1/link' is now readable and thus should inherit write access
        assertTrue(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc/level1/link", JCR_REMOVE_NODE));
    }

    @Test
    public void test_FacetRule_on_Boolean_Property_works() throws RepositoryException {

        final Node testData = session.getRootNode().getNode(TEST_DATA_NODE);
        final Node handle = testData.addNode("doc", "hippo:handle");
        handle.addMixin("mix:referenceable");
        final Node doc = handle.addNode("doc", "hippo:authtestdocument");
        doc.setProperty("authtest", Boolean.TRUE);

        assertThat(doc.getProperty("authtest").getType()).isEqualTo(PropertyType.BOOLEAN);

        session.save();

        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc", Session.ACTION_READ));
        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc", Session.ACTION_ADD_NODE));

        final Node authtestReadRule = readDomain.getNode("authtest-read/authtest-read-rule");

        final Property authtest = authtestReadRule.setProperty(HippoNodeType.HIPPOSYS_VALUE, Boolean.TRUE);

        assertThat(authtest.getType())
                .as("The hipposys:value is always expressed as a String, even if a Boolean is supplied")
                .isEqualTo(PropertyType.STRING);
        assertThat(authtest.getString()).isEqualTo("true");

        session.save();
        userSession.logout();
        userSession = (HippoSession)server.login(TEST_USER_ID, TEST_USER_PASS.toCharArray());

        assertTrue(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc", Session.ACTION_READ));
        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc", Session.ACTION_ADD_NODE));

        // assert search (and thus authorization query) also works
        final Query query = userSession.getWorkspace().getQueryManager()
                .createQuery("//element(*, hippo:authtestdocument)[@authtest = 'true']", "xpath");

        final NodeIterator result = query.execute().getNodes();
        assertThat(result.getSize()).isEqualTo(1);
        assertThat(result.nextNode().getPath()).isEqualTo(doc.getPath());

        // query on true instead of 'true' does not work (did not invest the reason but does not relate to authorization)

        assertThat(userSession.getWorkspace().getQueryManager()
                .createQuery("//element(*, hippo:authtestdocument)[@authtest = true]", "xpath")
                .execute().getNodes().getSize()).isEqualTo(0);
    }

    @Test
    public void test_FacetRule_on_Long_Property_works() throws RepositoryException {

        final Node testData = session.getRootNode().getNode(TEST_DATA_NODE);
        final Node handle = testData.addNode("doc", "hippo:handle");
        handle.addMixin("mix:referenceable");
        final Node doc = handle.addNode("doc", "hippo:authtestdocument");
        // set a Long value
        doc.setProperty("authtest", 10L);

        assertThat(doc.getProperty("authtest").getType()).isEqualTo(PropertyType.LONG);

        session.save();

        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc", Session.ACTION_READ));
        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc", Session.ACTION_ADD_NODE));

        final Node authtestReadRule = readDomain.getNode("authtest-read/authtest-read-rule");

        final Property authtest = authtestReadRule.setProperty(HippoNodeType.HIPPOSYS_VALUE, 10L);

        assertThat(authtest.getType())
                .as("The hipposys:value is always expressed as a String, even if a Long is supplied")
                .isEqualTo(PropertyType.STRING);
        assertThat(authtest.getString()).isEqualTo("10");

        session.save();
        userSession.logout();
        userSession = (HippoSession)server.login(TEST_USER_ID, TEST_USER_PASS.toCharArray());

        assertTrue(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc", Session.ACTION_READ));
        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc", Session.ACTION_ADD_NODE));

        // assert search (and thus authorization query) also works
        // both 10 and '10' works for long
        for (String value : new String[]{ "'10'", "10"} ) {
            final Query query = userSession.getWorkspace().getQueryManager()
                    .createQuery("//element(*, hippo:authtestdocument)[@authtest = " + value + "]", "xpath");

            final NodeIterator result = query.execute().getNodes();
            assertThat(result.getSize()).isEqualTo(1);
            assertThat(result.nextNode().getPath()).isEqualTo(doc.getPath());
        }

    }

    @Test
    public void test_FacetRule_on_Double_Property_works() throws RepositoryException {

        final Node testData = session.getRootNode().getNode(TEST_DATA_NODE);
        final Node handle = testData.addNode("doc", "hippo:handle");
        handle.addMixin("mix:referenceable");
        final Node doc = handle.addNode("doc", "hippo:authtestdocument");
        doc.setProperty("authtest", 10.23);

        assertThat(doc.getProperty("authtest").getType()).isEqualTo(PropertyType.DOUBLE);

        session.save();

        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc", Session.ACTION_READ));
        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc", Session.ACTION_ADD_NODE));

        final Node authtestReadRule = readDomain.getNode("authtest-read/authtest-read-rule");

        final Property authtest = authtestReadRule.setProperty(HippoNodeType.HIPPOSYS_VALUE, 10.23);

        assertThat(authtest.getType())
                .as("The hipposys:value is always expressed as a String, even if a Double is supplied")
                .isEqualTo(PropertyType.STRING);
        assertThat(authtest.getString()).isEqualTo("10.23");

        session.save();
        userSession.logout();
        userSession = (HippoSession)server.login(TEST_USER_ID, TEST_USER_PASS.toCharArray());

        assertTrue(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc", Session.ACTION_READ));
        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc", Session.ACTION_ADD_NODE));

        // assert search (and thus authorization query) also works
        // both 10.23 and '10.23' works for long
        for (String value : new String[]{ "'10.23'", "10.23"} ) {
            final Query query = userSession.getWorkspace().getQueryManager()
                    .createQuery("//element(*, hippo:authtestdocument)[@authtest = " + value + "]", "xpath");

            final NodeIterator result = query.execute().getNodes();
            assertThat(result.getSize()).isEqualTo(1);
            assertThat(result.nextNode().getPath()).isEqualTo(doc.getPath());
        }

    }

    @Test
    public void test_FacetRule_on_BigDecimal_Property_works_in_authorization_but_NOT_in_query_hence_not_fully_supported()
            throws RepositoryException {

        final Node testData = session.getRootNode().getNode(TEST_DATA_NODE);
        final Node handle = testData.addNode("doc", "hippo:handle");
        handle.addMixin("mix:referenceable");
        final Node doc = handle.addNode("doc", "hippo:authtestdocument");
        final BigDecimal bigDecimal = new BigDecimal(325234.324);
        doc.setProperty("authtest", bigDecimal);

        assertThat(doc.getProperty("authtest").getType()).isEqualTo(PropertyType.DECIMAL);

        session.save();

        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc", Session.ACTION_READ));
        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc", Session.ACTION_ADD_NODE));

        final Node authtestReadRule = readDomain.getNode("authtest-read/authtest-read-rule");

        final Property authtest = authtestReadRule.setProperty(HippoNodeType.HIPPOSYS_VALUE, bigDecimal);

        assertThat(authtest.getType())
                .as("The hipposys:value is always expressed as a String, even if a BigDecimal is supplied")
                .isEqualTo(PropertyType.STRING);

        session.save();
        userSession.logout();
        userSession = (HippoSession)server.login(TEST_USER_ID, TEST_USER_PASS.toCharArray());

        assertTrue(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc", Session.ACTION_READ));
        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc", Session.ACTION_ADD_NODE));

        // assert search (and thus authorization query) does NOT work
        for (String value : new String[]{ "'325234.324'", "325234.324", "'" + DecimalField.decimalToString(new BigDecimal(325234.324)) + "'" } ) {
            final Query query = userSession.getWorkspace().getQueryManager()
                    .createQuery("//element(*, hippo:authtestdocument)[@authtest = " + value + "]", "xpath");

            final NodeIterator result = query.execute().getNodes();
            assertThat(result.getSize()).isEqualTo(0);
        }

        // now assert that the ADMIN session actually DOES have a search result : this is because the authorization query
        // for ADMIN is just a match-all query
        final String value = DecimalField.decimalToString(new BigDecimal(325234.324));
        final String xpath = "//element(*, hippo:authtestdocument)[@authtest = '" + value + "']";
        final Query query = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath");

        final NodeIterator result = query.execute().getNodes();
        assertThat(result.getSize()).isEqualTo(1);
    }

    @Test
    public void test_FacetRule_on_Date_Property_works_but_NOT_in_query_hence_not_fully_supported() throws RepositoryException {

        final Node testData = session.getRootNode().getNode(TEST_DATA_NODE);
        final Node handle = testData.addNode("doc", "hippo:handle");
        handle.addMixin("mix:referenceable");
        final Node doc = handle.addNode("doc", "hippo:authtestdocument");
        final Calendar date = Calendar.getInstance();

        doc.setProperty("authtest", date);

        assertThat(doc.getProperty("authtest").getType()).isEqualTo(PropertyType.DATE);

        session.save();

        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc", Session.ACTION_READ));
        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc", Session.ACTION_ADD_NODE));

        final Node authtestReadRule = readDomain.getNode("authtest-read/authtest-read-rule");

        final Property authtest = authtestReadRule.setProperty(HippoNodeType.HIPPOSYS_VALUE, date);

        assertThat(authtest.getType())
                .as("The hipposys:value is always expressed as a String, even if a Date is supplied")
                .isEqualTo(PropertyType.STRING);

        session.save();
        userSession.logout();
        userSession = (HippoSession) server.login(TEST_USER_ID, TEST_USER_PASS.toCharArray());

        assertTrue(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc", Session.ACTION_READ));
        assertFalse(userSession.hasPermission("/" + TEST_DATA_NODE + "/doc/doc", Session.ACTION_ADD_NODE));

        // assert search (and thus authorization query) does NOT work
        final String value = "xs:dateTime('" + session.getValueFactory().createValue(date).getString() + "')";
        final String xpath = "//element(*, hippo:authtestdocument)[@authtest = " + value + "]";
        final Query query = userSession.getWorkspace().getQueryManager()
                .createQuery(xpath, "xpath");

        final NodeIterator result = query.execute().getNodes();
        assertThat(result.getSize()).isEqualTo(0);

        // now assert that the ADMIN session actually DOES have a search result : this is because the authorization query
        // for ADMIN is just a match-all query
        final Query queryAdmin = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath");

        final NodeIterator resultAdmin = queryAdmin.execute().getNodes();
        assertThat(resultAdmin.getSize()).isEqualTo(1);
        assertThat(resultAdmin.nextNode().getPath()).isEqualTo(doc.getPath());
    }


    @Test
    public void test_privileges_on_abs_path() throws RepositoryException {
        final String everywhereDomainPath = "/" + CONFIGURATION_PATH + "/" + DOMAINS_PATH + "/everywhere";
        Node testData = session.getRootNode().getNode(TEST_DATA_NODE);
        final Node handle = testData.addNode("doc", "hippo:handle");
        handle.addMixin("mix:referenceable");
        Node doc = handle.addNode("doc", "hippo:authtestdocument");

        session.save();

        {
            // privileges for admin
            final TreeSet<DomainInfoPrivilege> set = getPrivilegesSortedByName(session, "/" + TEST_DATA_NODE + "/doc/doc");
            assertThat(set)
                    .as("Expected read privilege from ")
                    .hasSize(5);

            final TreeSet<String> expectedPermissions = new TreeSet<>();
            for (String permissions : new String[]{"hippo:admin", "hippo:author", "hippo:editor", "jcr:all", "jcr:read"}) {
                expectedPermissions.add(permissions);
            }

            DomainInfoPrivilege current;
            while ((current = set.pollFirst()) != null) {
                final String expectedPermission = expectedPermissions.pollFirst();
                assertThat(current.getName())
                        .as("Expected '%s' privilege but was '%s'", expectedPermission, current.getName())
                        .isEqualTo(expectedPermission);

                assertThat(current.getDomainPaths())
                        .as("Expected everywhere domain '%s' also giving '%s' privilege but was domain(s) '%s'",
                                everywhereDomainPath, current.getName(), current.getDomainPaths())
                        .containsExactly(everywhereDomainPath);
            }


        }
        assertThat(getPrivileges(userSession, "/" + TEST_DATA_NODE + "/doc/doc"))
                .as("Expected no single privilege for usersSession")
                .isEmpty();


        doc.setProperty("authtest", "canread");
        session.save();

        {
            final DomainInfoPrivilege[] privileges = getPrivileges(userSession, "/" + TEST_DATA_NODE + "/doc/doc");
            assertThat(privileges)
                    .as("Expected read privilege from ")
                    .hasSize(1);
            assertThat(privileges[0].getName())
                    .as("Expected jcr:read privilege")
                    .isEqualTo(JCR_READ);

            assertThat(privileges[0].getDomainPaths())
                    .as("Expected read domain '%s' giving jcr:read privilege but was domain '%s'",
                            readDomain.getPath(), privileges[0].getDomainPaths().first())
                    .containsExactly(readDomain.getPath());
        }

        doc.setProperty("authtest", "canwrite");
        session.save();

        {
            final TreeSet<DomainInfoPrivilege> set = getPrivilegesSortedByName(userSession, "/" + TEST_DATA_NODE + "/doc/doc");

            assertThat(set)
                    .as("Expected privileges jcr:read and jcr:write")
                    .hasSize(2);

            final DomainInfoPrivilege first = set.pollFirst();
            assertThat(first.getName())
                    .as("Expected jcr:read as the first element in the sorted set")
                    .isEqualTo(JCR_READ);
            assertThat(first.getDomainPaths())
                    .as("Expected write domain '%s' giving jcr:read privilege but was domain(s) '%s'",
                            writeDomain.getPath(), first.getDomainPaths())
                    .containsExactly(writeDomain.getPath());

            final DomainInfoPrivilege second = set.pollFirst();
            assertThat(second.getName())
                    .as("Expected jcr:write as the second element in the sorted set")
                    .isEqualTo(JCR_WRITE);
            assertThat(second.getDomainPaths())
                    .as("Expected write domain '%s' giving jcr:write privilege but was domain(s) '%s'",
                            writeDomain.getPath(), second.getDomainPaths())
                    .containsExactly(writeDomain.getPath());
        }

        // VERY Specific use case now : If we copy the 'writeDomain' to a second domain, and log in a new user session
        // we expect that we get the exact same privileges as above BUT this time, the domains providing the privilege
        // should also include the copied domain!

        final String writeDomainPath2 = writeDomain.getPath() + "2";
        JcrUtils.copy(session, writeDomain.getPath(), writeDomainPath2);
        session.save();

        userSession.logout();
        userSession = (HippoSession)server.login(TEST_USER_ID, TEST_USER_PASS.toCharArray());

        try {
            {
                final TreeSet<DomainInfoPrivilege> set = getPrivilegesSortedByName(userSession, "/" + TEST_DATA_NODE + "/doc/doc");

                assertThat(set)
                        .as("Expected privileges jcr:read and jcr:write")
                        .hasSize(2);

                final DomainInfoPrivilege first = set.pollFirst();
                assertThat(first.getName())
                        .as("Expected jcr:read as the first element in the sorted set")
                        .isEqualTo(JCR_READ);
                assertThat(first.getDomainPaths())
                        .as("Expected domain '%s' and '%s' giving jcr:read privilege but was domain(s) '%s'",
                                writeDomain.getPath(), writeDomainPath2, first.getDomainPaths())
                        .containsExactly(writeDomain.getPath(), writeDomainPath2);

                final DomainInfoPrivilege second = set.pollFirst();
                assertThat(second.getName())
                        .as("Expected jcr:write as the second element in the sorted set")
                        .isEqualTo(JCR_WRITE);
                assertThat(second.getDomainPaths())
                        .as("Expected domain '%s' and '%s' giving jcr:write privilege but was domain(s) '%s'",
                                writeDomain.getPath(), writeDomainPath2, second.getDomainPaths())
                        .containsExactly(writeDomain.getPath(), writeDomainPath2);
            }
        } finally {
            // cleanup the second domain
            session.getNode(writeDomainPath2).remove();
            session.save();
        }

    }

    @NotNull
    private TreeSet<DomainInfoPrivilege> getPrivilegesSortedByName(final Session userSession, final String absPath) throws RepositoryException {

        final DomainInfoPrivilege[] privileges = getPrivileges(userSession, absPath);
        TreeSet<DomainInfoPrivilege> set = new TreeSet<>(Comparator.comparing(DomainInfoPrivilege::getName));
        Arrays.stream(privileges).forEach(domainInfoPrivilege -> set.add(domainInfoPrivilege));
        return set;
    }

    private DomainInfoPrivilege[] getPrivileges(final Session session, final String absPath) throws RepositoryException {
        return ((HippoAccessManager) session.getAccessControlManager()).getPrivileges(absPath);
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
        Node dr  = readDomain.addNode("nodename-user", HippoNodeType.NT_DOMAINRULE);
        Node fr  = dr.addNode("nodename-user-rule", HippoNodeType.NT_FACETRULE);
        fr.setProperty(HippoNodeType.HIPPO_FACET, "nodename");
        fr.setProperty(HippoNodeType.HIPPOSYS_VALUE, "__user__");
        fr.setProperty(HippoNodeType.HIPPOSYS_TYPE, "Name");
        session.save();

        // setup CLEAN user session
        userSession.logout();
        userSession = (HippoSession) server.login(TEST_USER_ID, TEST_USER_PASS.toCharArray());

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
        Node dr  = readDomain.addNode("authtestdocument-type", HippoNodeType.NT_DOMAINRULE);
        Node fr  = dr.addNode("authtestdocument-type-rule", HippoNodeType.NT_FACETRULE);
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
        userSession = (HippoSession) server.login(TEST_USER_ID, TEST_USER_PASS.toCharArray());

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


    private void queryAssertions(final Map<String, Long[]> queriesWithExpectedHitSizes) throws RepositoryException {
        for (Map.Entry<String, Long[]> entry : queriesWithExpectedHitSizes.entrySet()) {
            final String xpath = entry.getKey();
            final Long[] expectedSizes = entry.getValue();
            {
                // admin
                QueryManager queryManager = session.getWorkspace().getQueryManager();
                Query query = queryManager.createQuery(xpath, Query.XPATH);
                assertEquals(String.format("Query '%s' should had resulted in %s hits.", xpath, expectedSizes[0].longValue()),
                        expectedSizes[0].longValue(), query.execute().getNodes().getSize());
            }
            {
                // user
                QueryManager queryManager = userSession.getWorkspace().getQueryManager();
                Query query = queryManager.createQuery(xpath, Query.XPATH);
                assertEquals(String.format("Query '%s' should had resulted in %s hits.", xpath, expectedSizes[1].longValue()), expectedSizes[1].longValue(), query.execute().getNodes().getSize());
            }
        }
    }


    @Test
    public void parent_axis_query_result_should_not_count_result_from_unreadable_document() throws Exception {

        // Note below the '/..' at the end, indicating to return parent nodes
        final Map<String, Long[]> queriesWithExpectedHitSizes = new HashMap<>();
        queriesWithExpectedHitSizes.put("//element(*,hippo:authtestdocument)[@authtest='nothing']", new Long[] {new Long(4), new Long(0)});
        queriesWithExpectedHitSizes.put("//element(*,hippo:authtestdocument)[@authtest='nothing'] order by @jcr:score", new Long[] {new Long(4), new Long(0)});
        queriesWithExpectedHitSizes.put("//element(*,hippo:authtestdocument)[@authtest='nothing']/..", new Long[] {new Long(4), new Long(0)});
        queriesWithExpectedHitSizes.put("//element(*,hippo:authtestdocument)[@authtest='nothing']/.. order by @jcr:score", new Long[] {new Long(4), new Long(0)});

        queryAssertions(queriesWithExpectedHitSizes);
    }

    @Test
    public void child_axis_queries_result_should_not_count_children_of_unreadable_document() throws Exception {

        final Map<String, Long[]> queriesWithExpectedHitSizes = new HashMap<>();
        queriesWithExpectedHitSizes.put("/jcr:root/testdata/readdoc0", new Long[] {new Long(1), new Long(1)});
        queriesWithExpectedHitSizes.put("/jcr:root/testdata/readdoc0/*", new Long[] {new Long(3), new Long(2)});
        queriesWithExpectedHitSizes.put("//element(*,hippo:authtestdocument)[@authtest='nothing']/subread", new Long[] {new Long(1), new Long(0)});
        queriesWithExpectedHitSizes.put("//element(*,hippo:authtestdocument)[@authtest='nothing']/subread order by @jcr:score", new Long[] {new Long(1), new Long(0)});
        queriesWithExpectedHitSizes.put("//element(*,hippo:authtestdocument)[@authtest='nothing']/* order by @jcr:score", new Long[] {new Long(3), new Long(0)});
        queriesWithExpectedHitSizes.put("/jcr:root//*/element(*,hippo:authtestdocument)[@authtest='nothing']/*", new Long[] {new Long(3), new Long(0)});
        queriesWithExpectedHitSizes.put("/jcr:root//*/element(*,hippo:authtestdocument)[@authtest='nothing']/* order by @jcr:score", new Long[] {new Long(3), new Long(0)});
        queriesWithExpectedHitSizes.put("/jcr:root/testdata//element(*,hippo:authtestdocument)[@authtest='nothing']/* order by @jcr:score", new Long[] {new Long(3), new Long(0)});
        queriesWithExpectedHitSizes.put("/jcr:root/testdata/element(*,hippo:authtestdocument)[@authtest='nothing']/* order by @jcr:score", new Long[] {new Long(3), new Long(0)});
        queriesWithExpectedHitSizes.put("/jcr:root/testdata[nothing0/@authtest='nothing']/nothing0/*", new Long[] {new Long(3), new Long(0)});
        queriesWithExpectedHitSizes.put("/jcr:root/testdata[nothing0/@authtest='nothing']/*/*", new Long[] {new Long(15), new Long(0)});
        queriesWithExpectedHitSizes.put("/jcr:root/testdata[nothing0/@authtest='nothing']/*[@authtest='nothing']/*", new Long[] {new Long(3), new Long(0)});
        queriesWithExpectedHitSizes.put("/jcr:root/testdata[readdoc0/@authtest='canread']/readdoc0/subread", new Long[] {new Long(1), new Long(1)});

        // for query below, the userSession should be able to read /test/writedoc0/subread and /test/readdoc0/subread
        queriesWithExpectedHitSizes.put("/jcr:root/testdata[readdoc0/@authtest='canread']/*/subread", new Long[] {new Long(3), new Long(2)});

        queryAssertions(queriesWithExpectedHitSizes);

        session.getNode("/testdata/readdoc0/subnothing").addNode("subread", "hippo:authtestdocument").setProperty("authtest", "canread");
        session.save();

        queriesWithExpectedHitSizes.clear();
        queriesWithExpectedHitSizes.put("/jcr:root/testdata/readdoc0/*/subread", new Long[] {new Long(1), new Long(0)});

        queryAssertions(queriesWithExpectedHitSizes);
    }

    @Test
    public void authorization_child_axis_queries_combined_with_parent_axis_queries() throws Exception {
        final Map<String, Long[]> queriesWithExpectedHitSizes = new HashMap<>();
        queriesWithExpectedHitSizes.put("/jcr:root/testdata/readdoc0/..", new Long[] {new Long(1), new Long(1)});
        queriesWithExpectedHitSizes.put("/jcr:root/testdata/readdoc0/*/..", new Long[] {new Long(1), new Long(1)});
        queriesWithExpectedHitSizes.put("//element(*,hippo:authtestdocument)[@authtest='nothing']/subread/..", new Long[] {new Long(1), new Long(0)});
        queriesWithExpectedHitSizes.put("//element(*,hippo:authtestdocument)[@authtest='nothing']/subread/.. order by @jcr:score", new Long[] {new Long(1), new Long(0)});
        queriesWithExpectedHitSizes.put("//element(*,hippo:authtestdocument)[@authtest='nothing']/*/.. order by @jcr:score", new Long[] {new Long(1), new Long(0)});

        queriesWithExpectedHitSizes.put("/jcr:root/testdata/nothing0/*[../@authtest='nothing']", new Long[] {new Long(3), new Long(0)});

        // for query below, the userSession should be able to read /test/writedoc0 and /test/readdoc0
        queriesWithExpectedHitSizes.put("/jcr:root/testdata[readdoc0/@authtest='canread']/*/subread/..", new Long[] {new Long(3), new Long(2)});

        queryAssertions(queriesWithExpectedHitSizes);
    }

    @Test
    public void deref_query_should_not_count_unauthorized_hits() throws RepositoryException {
        final Node readdoc = session.getNode("/testdata/readdoc0");
        final Node nothingdoc = session.getNode("/testdata/nothing0");
        nothingdoc.addMixin("mix:referenceable");
        // create link from 'readdoc0' to 'nothing0'
        readdoc.setProperty("hippo:linkto", nothingdoc);
        session.save();

        final Map<String, Long[]> queriesWithExpectedHitSizes = new HashMap<>();
        queriesWithExpectedHitSizes.put("/jcr:root/testdata/readdoc0/jcr:deref(@hippo:linkto, '*')", new Long[] {new Long(1), new Long(0)});
        queriesWithExpectedHitSizes.put("/jcr:root/testdata/readdoc0/jcr:deref(@hippo:linkto, '*')/*", new Long[] {new Long(3), new Long(0)});
        queriesWithExpectedHitSizes.put("/jcr:root/testdata/readdoc0/jcr:deref(@hippo:linkto, '*')/..", new Long[] {new Long(1), new Long(0)});
        queriesWithExpectedHitSizes.put("/jcr:root/testdata/readdoc0/jcr:deref(@hippo:linkto, '*')/../..", new Long[] {new Long(1), new Long(0)});
        queriesWithExpectedHitSizes.put("//*/jcr:deref(@hippo:linkto, '*')", new Long[] {new Long(1), new Long(0)});
        queriesWithExpectedHitSizes.put("//*/jcr:deref(@hippo:linkto, '*')/*", new Long[] {new Long(3), new Long(0)});
        queriesWithExpectedHitSizes.put("//*/jcr:deref(@hippo:linkto, '*')/..", new Long[] {new Long(1), new Long(0)});

        queryAssertions(queriesWithExpectedHitSizes);

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
        userSession = (HippoSession) server.login(TEST_USER_ID, TEST_USER_PASS.toCharArray());

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

        Node dr = readDomain.addNode("nodename-readable", HippoNodeType.NT_DOMAINRULE);
        Node fr = dr.addNode("nodename-readable-rule", HippoNodeType.NT_FACETRULE);
        fr.setProperty(HippoNodeType.HIPPO_FACET, "nodename");
        fr.setProperty(HippoNodeType.HIPPOSYS_VALUE, "readable");
        fr.setProperty(HippoNodeType.HIPPOSYS_TYPE, "Name");

        dr  = writeDomain.addNode("nodename-writable", HippoNodeType.NT_DOMAINRULE);
        fr  = dr.addNode("nodename-writable-rule", HippoNodeType.NT_FACETRULE);
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

        Node dr = readDomain.getNode("authtest-read");
        Node fr = dr.addNode("optional-rule", HippoNodeType.NT_FACETRULE);
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

        assertEquals(TEST_USER_ID + ",admin", extendedSession.getUserID());

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

    @Ignore
    @Test
    public void propertyBelowUnreadableNodeIsNotReadable() throws Exception {
        // create nodes for which userSession does not have read access
        final String[] extra = {
                "/test-extra", "nt:unstructured",
                "/test-extra/extranet", "nt:unstructured",
                "/test-extra/extranet/doc", "hippo:handle",
                "/test-extra/extranet/doc/doc", "hippo:document",
                "hippo:availability", "live"
        };

        try {
            build(extra, session);
            session.save();
            userSession = (HippoSession)server.login(TEST_USER_ID, TEST_USER_PASS.toCharArray());

            // user session does not have read access below /test-extra
            assertFalse(userSession.nodeExists("/test-extra"));
            assertFalse(userSession.itemExists("/test-extra"));
            assertFalse(userSession.nodeExists("/test-extra/extranet"));
            assertFalse(userSession.nodeExists("/test-extra/extranet/doc"));
            assertFalse(userSession.nodeExists("/test-extra/extranet/doc/doc"));
            assertFalse(userSession.itemExists("/test-extra/extranet/doc/doc/hippo:availability"));

            Assertions.assertThatThrownBy(() -> userSession.getNode("/test-extra"))
                    .as("Expected PathNotFoundException")
                    .isInstanceOf(PathNotFoundException.class);


            Assertions.assertThatThrownBy(() -> userSession.getItem("/test-extra/extranet/doc/doc/hippo:availability"))
                    .as("Expected PathNotFoundException")
                    .isInstanceOf(PathNotFoundException.class);


            assertNull(getMultipleStringProperty(userSession.getRootNode(), "test-extra/extranet/doc/doc/hippo:availability", null));

        } finally {
            if (userSession != null) {
                userSession.logout();
            }
            session.getNode("/test-extra").remove();
            session.save();
        }
    }
}
