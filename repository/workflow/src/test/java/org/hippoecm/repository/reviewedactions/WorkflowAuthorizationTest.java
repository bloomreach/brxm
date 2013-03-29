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
package org.hippoecm.repository.reviewedactions;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static junit.framework.Assert.assertNotNull;

public class WorkflowAuthorizationTest extends RepositoryTestCase {

    static final Logger log = LoggerFactory.getLogger(WorkflowAuthorizationTest.class);

    Node readDomain;
    Node testUser;

    Session userSession;

    protected WorkflowManager workflowMgr = null;

    // nodes that have to be cleaned up
    private static final String DOMAIN_READ_NODE = "readme";

    private static final String TEST_USER_ID = "testuser";
    private static final String TEST_USER_PASS = "password";
    private static final String TEST_GROUP_ID = "testgroup";

    private static final String[] DOCUMENT_TEMPLATE = {
        "/${name}", "hippo:handle",
            "jcr:mixinTypes", "hippo:hardhandle",
            "/${name}/${name}", "hippostdpubwf:test",
                "jcr:mixinTypes", "hippo:harddocument",
                "hippostdpubwf:content", "test data",
                "hippostd:holder", "admin",
                "hippostd:state", "unpublished",
                "hippo:availability", "preview",
                "hippostdpubwf:createdBy", "admin",
                "hippostdpubwf:creationDate", "2010-02-04T16:32:28.068+02:00",
                "hippostdpubwf:lastModifiedBy", "admin",
                "hippostdpubwf:lastModificationDate", "2010-02-04T16:32:28.068+02:00"
    };

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        Node config = session.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH);
        Node domains = config.getNode(HippoNodeType.DOMAINS_PATH);
        Node users = config.getNode(HippoNodeType.USERS_PATH);
        Node groups = config.getNode(HippoNodeType.GROUPS_PATH);

        createUserAndGroup(users, groups);

        createDomains(domains);

        createTestData();

        session.save();

        // refresh session to be sure uuids are refreshed on all nodes
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
        Node ar, dr, fr;

        // create read domain
        readDomain = domains.addNode(DOMAIN_READ_NODE, HippoNodeType.NT_DOMAIN);
        ar = readDomain.addNode("hippo:authrole", HippoNodeType.NT_AUTHROLE);
        ar.setProperty(HippoNodeType.HIPPO_ROLE, "readonly");
        ar.setProperty(HippoNodeType.HIPPO_USERS, new String[] {TEST_USER_ID});

        dr  = readDomain.addNode("hippo:domainrule", HippoNodeType.NT_DOMAINRULE);
        fr  = dr.addNode("hippo:facetrule", HippoNodeType.NT_FACETRULE);
        fr.setProperty(HippoNodeType.HIPPO_FACET, HippoNodeType.HIPPO_AVAILABILITY);
        fr.setProperty(HippoNodeType.HIPPOSYS_VALUE, "preview");
        fr.setProperty(HippoNodeType.HIPPOSYS_TYPE, "String");
    }

    private void createTestData() throws RepositoryException {
        Node root = session.getRootNode();
        if (root.hasNode("test")) {
            root.getNode("test").remove();
        }
        root.addNode("test");

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("name", "myarticle");
        build(session, mount("/test", instantiate(DOCUMENT_TEMPLATE, parameters)));
    }

    protected static String[] instantiate(String[] content, Map<String, String> parameters) {
        String[] result = new String[content.length];
        for (int i = 0; i < content.length; i++) {
            String value = content[i];
            while (value.contains("${")) {
                String parameter = value.substring(value.indexOf('{') + 1, value.indexOf('}'));
                if (parameters.containsKey(parameter)) {
                    value = value.substring(0, value.indexOf('$')) + parameters.get(parameter)
                            + value.substring(value.indexOf('}') + 1);
                } else {
                    throw new IllegalArgumentException("parameters does not contain variable " + parameter);
                }
            }
            result[i] = value;
        }
        return result;
    }

    protected static String[] mount(String path, String[] content) {
        String[] result = new String[content.length];
        for (int i = 0; i < content.length; i++) {
            String value = content[i];
            if (value.startsWith("/")) {
                result[i] = path + value;
            } else {
                result[i] = value;
            }
        }
        return result;
    }

    @Override
    @After
    public void tearDown() throws Exception {
        Node config = session.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH);
        Node domains = config.getNode(HippoNodeType.DOMAINS_PATH);
        Node users = config.getNode(HippoNodeType.USERS_PATH);
        Node groups = config.getNode(HippoNodeType.GROUPS_PATH);

        cleanupTestData();

        cleanupDomains(domains);

        cleanupUserAndGroup(users, groups);

        session.save();

        super.tearDown();
    }

    private void cleanupTestData() throws RepositoryException {
        Node root = session.getRootNode();
        while(root.hasNode("test")) {
            root.getNode("test").remove();
        }
        root.save();
    }

    private void cleanupDomains(final Node domains) throws RepositoryException {
        if (domains.hasNode(DOMAIN_READ_NODE)) {
            domains.getNode(DOMAIN_READ_NODE).remove();
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

    @Test
    public void testBasic() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        assertNotNull(getUserNode("/test/myarticle/myarticle"));

        Node node = getNode("test/myarticle/myarticle");
        FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) getWorkflow(node, "default");
        workflow.publish();

        assertNotNull(getUserNode("/test/myarticle/myarticle"));
    }

    @Test
    public void testRepeatedly() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Random random = new Random(0);
        int max = 10;
        int iter = 0;
        while ((iter++ >= 0) && (log.isDebugEnabled() || max-- > 0)) {
            switch (random.nextInt(3)) {
                case 0: {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < 10; i++) {
                        sb.append((char) ('a' + random.nextInt(26)));
                    }
                    String name = sb.toString();

                    log.debug(iter + ": creating " + name);

                    Map<String, String> parameters = new HashMap<String, String>();
                    parameters.put("name", name);
                    build(session, mount("/test", instantiate(DOCUMENT_TEMPLATE, parameters)));
                    session.save();

                    assertNotNull(getUserNode("/test/" + name + "/" + name));

                    Node node = getNode("test/" + name + "/" + name);
                    FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) getWorkflow(node, "default");
                    workflow.publish();

                    assertNotNull(getUserNode("/test/" + name + "/" + name));
                }
                break;
                case 1: {
                    NodeIterator nodes = getNode("/test").getNodes();
                    final int size = (int) nodes.getSize();
                    if (size == 0) {
                        continue;
                    }

                    nodes.skip(random.nextInt(size));

                    Node node = nodes.nextNode();
                    String name = node.getName();

                    log.debug(iter + ": cycling " + name);

                    final String path = "/test/" + name + "/" + name;
                    assertNotNull(getUserNode(path));

                    node = node.getNode(name);
                    FullReviewedActionsWorkflow editWorkflow = (FullReviewedActionsWorkflow) getWorkflow(node, "default");
                    editWorkflow.obtainEditableInstance();

                    Node draft = getNode(path + "[@hippostd:state='draft']");
                    FullReviewedActionsWorkflow saveWorkflow = (FullReviewedActionsWorkflow) getWorkflow(draft, "default");
                    saveWorkflow.commitEditableInstance();

                    assertNotNull(getUserNode(path));

                    Node preview = getNode(path + "[@hippostd:state='unpublished']");
                    FullReviewedActionsWorkflow publishWorkflow = (FullReviewedActionsWorkflow) getWorkflow(preview, "default");
                    publishWorkflow.publish();

                    assertNotNull(getUserNode(path));
                }
                break;
                case 2: {
                    NodeIterator nodes = getNode("/test").getNodes();
                    final int size = (int) nodes.getSize();
                    if (size == 0) {
                        continue;
                    }
                    nodes.skip(random.nextInt(size));

                    Node node = nodes.nextNode();
                    String name = node.getName();

                    log.debug(iter + ": removing " + name);

                    node.remove();
                    session.save();
                }
            }
        }
    }

    protected Workflow getWorkflow(Node node, String category) throws RepositoryException {
        if (workflowMgr == null) {
            HippoWorkspace wsp = (HippoWorkspace) node.getSession().getWorkspace();
            workflowMgr = wsp.getWorkflowManager();
        }
        Node canonicalNode = ((HippoNode) node).getCanonicalNode();
        return workflowMgr.getWorkflow(category, canonicalNode);
    }

    protected Workflow getWorkflow(Document document, String category) throws RepositoryException {
        if (workflowMgr == null) {
            HippoWorkspace wsp = (HippoWorkspace) session.getWorkspace();
            workflowMgr = wsp.getWorkflowManager();
        }
        return workflowMgr.getWorkflow(category, document);
    }

    protected Node getNode(String path) throws RepositoryException {
        return ((HippoWorkspace)session.getWorkspace()).getHierarchyResolver().getNode(session.getRootNode(), path);
    }

    protected Node getUserNode(String path) throws RepositoryException {
        return userSession.getNode(path);
    }
}
