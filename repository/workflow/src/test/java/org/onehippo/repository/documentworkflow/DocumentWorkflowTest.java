/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.repository.documentworkflow;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.scxml2.model.TransitionTarget;
import org.apache.jackrabbit.util.ISO8601;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.hippoecm.repository.quartz.HippoSchedJcrConstants;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.documentworkflow.action.ArchiveDocumentAction;
import org.onehippo.repository.documentworkflow.action.ConfigVariantAction;
import org.onehippo.repository.documentworkflow.action.CopyDocumentAction;
import org.onehippo.repository.documentworkflow.action.CopyVariantAction;
import org.onehippo.repository.documentworkflow.action.DeleteRequestAction;
import org.onehippo.repository.documentworkflow.action.IsModifiedAction;
import org.onehippo.repository.documentworkflow.action.ListVersionsVariantAction;
import org.onehippo.repository.documentworkflow.action.MoveDocumentAction;
import org.onehippo.repository.documentworkflow.action.RejectRequestAction;
import org.onehippo.repository.documentworkflow.action.RenameDocumentAction;
import org.onehippo.repository.documentworkflow.action.RequestActionAction;
import org.onehippo.repository.documentworkflow.action.ScheduleWorkflowAction;
import org.onehippo.repository.documentworkflow.action.VersionRestoreToAction;
import org.onehippo.repository.documentworkflow.action.WorkflowRequestAction;
import org.onehippo.repository.documentworkflow.action.RestoreVersionAction;
import org.onehippo.repository.documentworkflow.action.RetrieveVersionAction;
import org.onehippo.repository.documentworkflow.action.SetHolderAction;
import org.onehippo.repository.documentworkflow.action.VersionVariantAction;
import org.onehippo.repository.scxml.WorkflowExceptionAction;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.mock.MockValue;
import org.onehippo.repository.mock.MockVersion;
import org.onehippo.repository.scxml.ActionAction;
import org.onehippo.repository.scxml.FeedbackAction;
import org.onehippo.repository.scxml.MockRepositorySCXMLRegistry;
import org.onehippo.repository.scxml.RepositorySCXMLExecutorFactory;
import org.onehippo.repository.scxml.ResultAction;
import org.onehippo.repository.scxml.SCXMLExecutorFactory;
import org.onehippo.repository.scxml.SCXMLRegistry;
import org.onehippo.repository.scxml.SCXMLWorkflowExecutor;
import org.onehippo.repository.util.JcrConstants;

import static org.junit.Assert.assertEquals;

public class DocumentWorkflowTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        MockRepositorySCXMLRegistry registry = new MockRepositorySCXMLRegistry();
        MockNode scxmlConfigNode = registry.createConfigNode();
        MockNode scxmlNode = registry.addScxmlNode(scxmlConfigNode, "documentworkflow", loadSCXML());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "action", ActionAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "result", ResultAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "feedback", FeedbackAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "copyVariant", CopyVariantAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "configVariant", ConfigVariantAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "workflowRequest", WorkflowRequestAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "archiveDocument", ArchiveDocumentAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "isModified", IsModifiedAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "scheduleWorkflow", ScheduleWorkflowAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "copyDocument", CopyDocumentAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "moveDocument", MoveDocumentAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "renameDocument", RenameDocumentAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "setHolder", SetHolderAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "deleteRequest", DeleteRequestAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "workflowException", WorkflowExceptionAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "version", VersionVariantAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "listVersions", ListVersionsVariantAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "retrieveVersion", RetrieveVersionAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "restoreVersion", RestoreVersionAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "versionRestoreTo", VersionRestoreToAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "requestAction", RequestActionAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "rejectRequest", RejectRequestAction.class.getName());
        registry.setUp(scxmlConfigNode);

        HippoServiceRegistry.registerService(registry, SCXMLRegistry.class);
        HippoServiceRegistry.registerService(new RepositorySCXMLExecutorFactory(), SCXMLExecutorFactory.class);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        HippoServiceRegistry.unregisterService(HippoServiceRegistry.getService(SCXMLExecutorFactory.class), SCXMLExecutorFactory.class);
        HippoServiceRegistry.unregisterService(HippoServiceRegistry.getService(SCXMLRegistry.class), SCXMLRegistry.class);
    }

    protected static String loadSCXML() throws Exception {
        return IOUtils.toString(DocumentWorkflowTest.class.getResourceAsStream("/documentworkflow.scxml"));
    }

    protected MockNode addVariant(MockNode handle, String state) throws RepositoryException {
        MockNode variant = (MockNode)handle.addNode(handle.getName(), HippoStdPubWfNodeType.HIPPOSTDPUBWF_DOCUMENT);
        variant.setProperty(HippoStdNodeType.HIPPOSTD_STATE, state);
        return variant;
    }

    protected MockNode addRequest(MockNode handle, String type, boolean workflowRequest) throws RepositoryException {
        MockNode variant = (MockNode)handle.addNode(HippoStdPubWfNodeType.HIPPO_REQUEST,
                workflowRequest ? HippoStdPubWfNodeType.NT_HIPPOSTDPUBWF_REQUEST : HippoSchedJcrConstants.HIPPOSCHED_WORKFLOW_JOB);
        variant.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_TYPE, type);
        variant.addMixin(HippoNodeType.NT_REQUEST);
        return variant;
    }

    protected Set<String> set(String... vargs) {
        Set<String> set = new TreeSet<>();
        Collections.addAll(set, vargs);
        return set;
    }

    protected Set<String> getCurrentStateIds(SCXMLWorkflowExecutor executor) {
        Set<TransitionTarget> targets = executor.getSCXMLExecutor().getCurrentStatus().getStates();

        if (targets.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> ids = new TreeSet<>();

        for (TransitionTarget target : targets) {
            ids.add(target.getId());
        }

        return ids;
    }

    protected void assertMatchingStateIds(SCXMLWorkflowExecutor executor, String... ids) {
        Set<String> stateIds = getCurrentStateIds(executor);
        if (ids.length == stateIds.size()) {
            for (String id : ids) {
                if (!stateIds.contains(id)) {
                    Assert.fail("Current SCXML states " + stateIds + " not matching expected states " + set(ids) + "");
                }
            }
            return;
        }
        Assert.fail("Current SCXML states [" + stateIds + "] not matching expected states " + set(ids) + "");
    }

    protected void assertContainsStateIds(SCXMLWorkflowExecutor executor, String... ids) {
        Set<String> stateIds = getCurrentStateIds(executor);
        if (ids.length <= stateIds.size()) {
            for (String id : ids) {
                if (!stateIds.contains(id)) {
                    Assert.fail("Current SCXML states " + stateIds + " not containing expected states " + set(ids) + "");
                }
            }
            return;
        }
        Assert.fail("Current SCXML states [" + stateIds + "] not containing expected states " + set(ids) + "");
    }

    protected void assertContainsHint(Map<String, Serializable> hints, String hint, Object value) {
        if (!hints.containsKey(hint) || !hints.get(hint).toString().equals(value.toString())) {
            Assert.fail("Current hints " + hints + " not containing expected hint [" + hint + "] with value [" + value + "]");
        }
    }

    protected void assertNotContainsHint(Map<String, Serializable> hints, String hint) {
        if (hints.containsKey(hint)) {
            Assert.fail("Current hints " + hints + " contains not expected hint [" + hint + "]");
        }
    }

    @Test
    public void testInitializeSCXML() throws Exception {

        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);

        MockNode handleNode = (MockNode)session.getRootNode().addNode("test", HippoNodeType.NT_HANDLE);

        addVariant(handleNode, HippoStdNodeType.DRAFT);
        wf.setNode(handleNode);
        wf.getWorkflowExecutor().start();
    }

    @Test
    public void testStatusState() throws Exception {

        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);

        MockNode handleNode = MockNode.root().addMockNode("test", HippoNodeType.NT_HANDLE);

        MockNode draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);
        MockNode unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);
        MockNode publishedVariant = addVariant(handleNode, HippoStdNodeType.PUBLISHED);

        wf.setNode(handleNode);
        wf.getWorkflowExecutor().start();

        assertContainsStateIds(wf.getWorkflowExecutor(), "status");
        assertContainsHint(wf.hints(), "status", true);
        assertContainsHint(wf.hints(), "checkModified", true);

        unpublishedVariant.remove();
        wf.setNode(handleNode);
        wf.getWorkflowExecutor().start();

        assertContainsHint(wf.hints(), "status", true);
        assertContainsHint(wf.hints(), "checkModified", false);

        publishedVariant.remove();
        wf.setNode(handleNode);
        wf.getWorkflowExecutor().start();

        assertContainsHint(wf.hints(), "status", true);

        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "testuser");
        wf.setNode(handleNode);
        wf.getWorkflowExecutor().start();

        assertContainsHint(wf.hints(), "status", true);

        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "otheruser");
        wf.setNode(handleNode);
        wf.getWorkflowExecutor().start();

        assertContainsHint(wf.hints(), "status", false);

        unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);
        wf.setNode(handleNode);
        wf.getWorkflowExecutor().start();

        assertContainsHint(wf.hints(), "status", false);

        draftVariant.remove();
        wf.setNode(handleNode);
        wf.getWorkflowExecutor().start();

        assertContainsHint(wf.hints(), "status", true);
    }

    @Test
    public void testEditState() throws Exception {

        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);

        MockNode handleNode = (MockNode)session.getRootNode().addNode("test", HippoNodeType.NT_HANDLE);
        MockNode draftVariant, unpublishedVariant, publishedVariant, rejectedRequest, publishRequest;

        draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);
        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "no-edit");
        assertNotContainsHint(wf.hints(), "obtainEditableInstance");
        assertNotContainsHint(wf.hints(), "commitEditableInstance");
        assertNotContainsHint(wf.hints(), "disposeEditableInstance");
        assertNotContainsHint(wf.hints(), "unlock");

        wf.setNode(handleNode);

        // test state not-editable
        assertContainsStateIds(wf.getWorkflowExecutor(), "editable");

        rejectedRequest = addRequest(handleNode, HippoStdPubWfNodeType.REJECTED, true);
        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "no-edit");

        rejectedRequest.remove();
        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "editable");

        publishRequest = addRequest(handleNode, HippoStdPubWfNodeType.PUBLISH, true);
        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "not-editable");

        publishRequest.remove();

        // test state not-editing
        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "editable");
        assertContainsHint(wf.hints(), "obtainEditableInstance", true);
        assertContainsHint(wf.hints(), "unlock", false);

        wf.setNode(handleNode);

        assertContainsHint(wf.hints(), "obtainEditableInstance", true);
        assertNotContainsHint(wf.hints(), "unlock");

        wf.setNode(handleNode);

        assertContainsHint(wf.hints(), "unlock", false);
        assertNotContainsHint(wf.hints(), "obtainEditableInstance");

        unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);
        wf.setNode(handleNode);

        assertContainsHint(wf.hints(), "obtainEditableInstance", true);

        unpublishedVariant.remove();
        draftVariant.remove();

        publishedVariant = addVariant(handleNode, HippoStdNodeType.PUBLISHED);
        wf.setNode(handleNode);

        assertContainsHint(wf.hints(), "obtainEditableInstance", true);

        rejectedRequest = addRequest(handleNode, HippoStdPubWfNodeType.REJECTED, true);
        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "no-edit");


        // test state editing
        publishedVariant.remove();
        rejectedRequest.remove();

        draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);
        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "testuser");
        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "editing");
        assertContainsHint(wf.hints(), "obtainEditableInstance", true);
        assertContainsHint(wf.hints(), "commitEditableInstance", true);
        assertContainsHint(wf.hints(), "disposeEditableInstance", true);
        assertNotContainsHint(wf.hints(), "unlock");

        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "editing");
        assertNotContainsHint(wf.hints(), "obtainEditableInstance");
        assertNotContainsHint(wf.hints(), "commitEditableInstance");
        assertNotContainsHint(wf.hints(), "disposeEditableInstance");
        assertContainsHint(wf.hints(), "unlock", true);

        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "editing");
        assertContainsHint(wf.hints(), "obtainEditableInstance", true);
        assertContainsHint(wf.hints(), "commitEditableInstance", true);
        assertContainsHint(wf.hints(), "disposeEditableInstance", true);
        assertContainsHint(wf.hints(), "unlock", true);
        assertNotContainsHint(wf.hints(), "inUseBy");

        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "otheruser");
        session.setPermissions(draftVariant.getPath(), "hippo:admin", false);
        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "editing");
        assertContainsHint(wf.hints(), "obtainEditableInstance", false);
        assertContainsHint(wf.hints(), "commitEditableInstance", false);
        assertContainsHint(wf.hints(), "disposeEditableInstance", false);
        assertContainsHint(wf.hints(), "unlock", false);
        assertContainsHint(wf.hints(), "inUseBy", "otheruser");

        session.setPermissions(draftVariant.getPath(), "hippo:admin", true);
        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "editing");
        assertContainsHint(wf.hints(), "obtainEditableInstance", false);
        assertContainsHint(wf.hints(), "commitEditableInstance", false);
        assertContainsHint(wf.hints(), "disposeEditableInstance", false);
        assertContainsHint(wf.hints(), "unlock", true);
    }

    @Test
    public void testRequestState() throws Exception {
        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);

        MockNode handleNode = (MockNode)session.getRootNode().addNode("test", HippoNodeType.NT_HANDLE);
        MockNode unpublishedVariant, rejectedRequest, publishRequest;

        // test state not-requested
        unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);
        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "not-requested");

        // test state requested
        publishRequest = addRequest(handleNode, HippoStdPubWfNodeType.PUBLISH, true);
        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "requested");

        rejectedRequest = addRequest(handleNode, HippoStdPubWfNodeType.REJECTED, true);
        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "request-rejected");

        session.setPermissions(publishRequest.getPath(), "hippo:editor", false);
        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "requested");
        assertContainsHint(wf.hints(), "cancelRequest", false);
        assertNotContainsHint(wf.hints(), "acceptRequest");
        assertNotContainsHint(wf.hints(), "rejectRequest");

        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "requested");
        assertContainsHint(wf.hints(), "cancelRequest", false);
        assertNotContainsHint(wf.hints(), "acceptRequest");
        assertNotContainsHint(wf.hints(), "rejectRequest");

        session.setPermissions(publishRequest.getPath(), "hippo:editor", true);

        wf.setNode(handleNode);
        assertContainsStateIds(wf.getWorkflowExecutor(), "requested");
        assertContainsHint(wf.hints(), "cancelRequest", false);
        assertContainsHint(wf.hints(), "acceptRequest", true);
        assertContainsHint(wf.hints(), "rejectRequest", true);

        publishRequest.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_USERNAME, "testuser");
        wf.setNode(handleNode);

        assertContainsHint(wf.hints(), "cancelRequest", true);
        assertContainsHint(wf.hints(), "rejectRequest", false);

        publishRequest.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_USERNAME, "otheruser");
        wf.setNode(handleNode);

        assertContainsHint(wf.hints(), "cancelRequest", false);
        assertContainsHint(wf.hints(), "rejectRequest", true);

        session.setPermissions(publishRequest.getPath(), "hippo:editor", false);
        wf.setNode(handleNode);

        assertContainsHint(wf.hints(), "cancelRequest", false);
        assertNotContainsHint(wf.hints(), "acceptRequest");
        assertNotContainsHint(wf.hints(), "rejectRequest");

        publishRequest.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_USERNAME, "testuser");

        wf.setNode(handleNode);

        assertContainsHint(wf.hints(), "cancelRequest", true);
        assertNotContainsHint(wf.hints(), "acceptRequest");
        assertNotContainsHint(wf.hints(), "rejectRequest");

        // test state request-rejected

        session.setPermissions(rejectedRequest.getPath(), "hippo:editor", false);
        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "request-rejected");
        assertContainsHint(wf.hints(), "cancelRequest", false);
        assertContainsHint(wf.hints(), "acceptRequest", false);
        assertContainsHint(wf.hints(), "rejectRequest", false);

        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "request-rejected");

        rejectedRequest.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_USERNAME, "testuser");
        wf.setNode(handleNode);

        assertContainsHint(wf.hints(), "cancelRequest", true);
        assertContainsHint(wf.hints(), "acceptRequest", false);
        assertContainsHint(wf.hints(), "rejectRequest", false);

        rejectedRequest.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_USERNAME, "otheruser");
        wf.setNode(handleNode);

        assertContainsHint(wf.hints(), "cancelRequest", false);
        assertContainsHint(wf.hints(), "acceptRequest", false);
        assertContainsHint(wf.hints(), "rejectRequest", false);

        session.setPermissions(rejectedRequest.getPath(), "hippo:editor", true);
        wf.setNode(handleNode);

        assertContainsHint(wf.hints(), "cancelRequest", true);
        assertContainsHint(wf.hints(), "acceptRequest", false);
        assertContainsHint(wf.hints(), "rejectRequest", false);

        publishRequest.getProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_USERNAME).remove();
        wf.setNode(handleNode);

        assertContainsHint(wf.hints(), "cancelRequest", true);
        assertContainsHint(wf.hints(), "acceptRequest", false);
        assertContainsHint(wf.hints(), "rejectRequest", false);

        session.setPermissions(rejectedRequest.getPath(), "hippo:editor", false);
        wf.setNode(handleNode);

        assertContainsHint(wf.hints(), "cancelRequest", false);
        assertContainsHint(wf.hints(), "acceptRequest", false);
        assertContainsHint(wf.hints(), "rejectRequest", false);
    }

    @Test
    public void testPublishState() throws Exception {
        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);

        MockNode handleNode = (MockNode)session.getRootNode().addNode("test", HippoNodeType.NT_HANDLE);
        MockNode draftVariant, unpublishedVariant, publishedVariant, publishRequest;

        unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);
        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "no-publish");
        assertNotContainsHint(wf.hints(), "publish");

        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "publishable");
        assertContainsHint(wf.hints(), "publish", true);

        unpublishedVariant.remove();
        publishRequest = addRequest(handleNode, HippoStdPubWfNodeType.PUBLISH, true);
        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "not-publishable");
        assertContainsHint(wf.hints(), "publish", false);

        unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);
        wf.setNode(handleNode);

        assertContainsHint(wf.hints(), "publish", false);

        workflowContext.setUserIdentity("system");
        wf.setNode(handleNode);

        assertContainsHint(wf.hints(), "publish", true);

        workflowContext.setUserIdentity("testuser");
        publishRequest.remove();
        draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);
        wf.setNode(handleNode);

        assertContainsHint(wf.hints(), "publish", false);

        wf.setNode(handleNode);

        assertContainsHint(wf.hints(), "publish", true);

        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "testuser");
        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "not-publishable");

        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "otheruser");
        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "not-publishable");

        draftVariant.getProperty(HippoStdNodeType.HIPPOSTD_HOLDER).remove();

        wf.setNode(handleNode);

        assertContainsHint(wf.hints(), "publish", true);

        publishedVariant = addVariant(handleNode, HippoStdNodeType.PUBLISHED);
        wf.setNode(handleNode);

        assertContainsHint(wf.hints(), "publish", false);

        publishedVariant.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[0]);
        wf.setNode(handleNode);

        assertContainsHint(wf.hints(), "publish", true);

        publishedVariant.getProperty(HippoNodeType.HIPPO_AVAILABILITY).remove();

        Calendar publishedModified = Calendar.getInstance();
        Calendar unpublishedModified = Calendar.getInstance();
        publishedModified.setTimeInMillis(unpublishedModified.getTimeInMillis() - 1000);
        unpublishedVariant.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE, new MockValue(PropertyType.DATE, ISO8601.format(unpublishedModified)));
        publishedVariant.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE, new MockValue(PropertyType.DATE, ISO8601.format(publishedModified)));

        wf.setNode(handleNode);

        assertContainsHint(wf.hints(), "publish", true);

        publishedVariant.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE, new MockValue(PropertyType.DATE, ISO8601.format(unpublishedModified)));

        wf.setNode(handleNode);

        assertContainsHint(wf.hints(), "publish", false);
    }

    @Test
    public void testDePublishState() throws Exception {
        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);

        MockNode handleNode = (MockNode)session.getRootNode().addNode("test", HippoNodeType.NT_HANDLE);
        MockNode draftVariant, unpublishedVariant, publishedVariant, publishRequest;

        publishedVariant = addVariant(handleNode, HippoStdNodeType.PUBLISHED);
        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "no-depublish");
        assertNotContainsHint(wf.hints(), "depublish");

        wf.setNode(handleNode);

        // note: empty/no availability means allways available
        assertContainsStateIds(wf.getWorkflowExecutor(), "depublishable");
        assertContainsHint(wf.hints(), "depublish", true);

        publishedVariant.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[]{"foo"});

        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "not-depublishable");
        assertContainsHint(wf.hints(), "depublish", false);

        publishedVariant.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[]{"foo", "live"});

        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "depublishable");
        assertContainsHint(wf.hints(), "depublish", true);

        draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);
        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "testuser");
        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "depublishable");

        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "otheruser");
        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "not-depublishable");

        draftVariant.getProperty(HippoStdNodeType.HIPPOSTD_HOLDER).remove();
        publishRequest = addRequest(handleNode, HippoStdPubWfNodeType.PUBLISH, true);
        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "no-depublish");

        workflowContext.setUserIdentity("system");
        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "depublishable");
        assertContainsHint(wf.hints(), "depublish", true);

        workflowContext.setUserIdentity("testuser");

        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "depublishable");
        assertContainsHint(wf.hints(), "depublish", false);

        publishRequest.remove();

        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "depublishable");
        assertContainsHint(wf.hints(), "depublish", true);

        unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);

        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "depublishable");
        assertContainsHint(wf.hints(), "depublish", false);

        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "depublishable");
        assertContainsHint(wf.hints(), "depublish", false);

        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "depublishable");
        assertContainsHint(wf.hints(), "depublish", true);

        unpublishedVariant.remove();
        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "depublishable");
        assertContainsHint(wf.hints(), "depublish", false);

        draftVariant.remove();
        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "depublishable");
        assertContainsHint(wf.hints(), "depublish", true);
    }

    @Test
    public void testVersioningState() throws Exception {
        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);

        MockNode handleNode = (MockNode)session.getRootNode().addNode("test", HippoNodeType.NT_HANDLE);
        MockNode unpublishedVariant, frozenNode;

        unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);

        MockVersion versionNode = new MockVersion("1.0", JcrConstants.NT_VERSION);
        ((MockNode) unpublishedVariant.getParent()).addNode(versionNode);
        frozenNode = versionNode.addMockNode(JcrConstants.JCR_FROZEN_NODE, JcrConstants.NT_FROZEN_NODE);
        frozenNode.setProperty(JcrConstants.JCR_FROZEN_UUID, unpublishedVariant.getIdentifier());

        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "version");
        assertContainsHint(wf.hints(), "version", true);
        assertContainsHint(wf.hints(), "listVersions", true);
        assertContainsHint(wf.hints(), "restoreVersion", true);
        assertContainsHint(wf.hints(), "revertVersion", true);
        assertNotContainsHint(wf.hints(), "restoreVersionTo");

        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "no-versioning");
        assertNotContainsHint(wf.hints(), "version");
        assertNotContainsHint(wf.hints(), "restoreVersionTo");

        wf.setNode(handleNode);
        assertContainsStateIds(wf.getWorkflowExecutor(), "version");
        assertContainsHint(wf.hints(), "restoreVersionTo", true);
        assertNotContainsHint(wf.hints(), "version");
    }

    @Test
    public void testTerminateState() throws Exception {
        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);

        MockNode handleNode = (MockNode)session.getRootNode().addNode("test", HippoNodeType.NT_HANDLE);
        MockNode draftVariant, unpublishedVariant, publishedVariant, publishRequest;

        unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);

        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "terminateable");
        assertContainsHint(wf.hints(), "delete", true);
        assertContainsHint(wf.hints(), "move", true);
        assertContainsHint(wf.hints(), "rename", true);

        draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);
        publishedVariant = addVariant(handleNode, HippoStdNodeType.PUBLISHED);
        publishedVariant.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[0]);

        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "terminateable");
        assertContainsHint(wf.hints(), "delete", true);
        assertContainsHint(wf.hints(), "move", true);
        assertContainsHint(wf.hints(), "rename", true);

        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "not-terminateable");
        assertContainsHint(wf.hints(), "delete", false);
        assertContainsHint(wf.hints(), "move", false);
        assertContainsHint(wf.hints(), "rename", false);

        session.setPermissions(publishedVariant.getPath(), "hippo:editor", false);
        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "not-terminateable");
        assertContainsHint(wf.hints(), "delete", false);
        assertNotContainsHint(wf.hints(), "move");
        assertNotContainsHint(wf.hints(), "rename");

        session.setPermissions(unpublishedVariant.getPath(), "hippo:editor", false);
        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "terminateable");
        assertContainsHint(wf.hints(), "delete", true);
        assertNotContainsHint(wf.hints(), "move");
        assertNotContainsHint(wf.hints(), "rename");

        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "no-terminate");

        publishedVariant.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[]{"live"});
        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "no-terminate");

        publishedVariant.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[0]);
        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "testuser");
        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "no-terminate");

        draftVariant.getProperty(HippoStdNodeType.HIPPOSTD_HOLDER).remove();
        publishRequest = addRequest(handleNode, HippoStdPubWfNodeType.PUBLISH, true);
        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "no-terminate");

        publishRequest.remove();
        unpublishedVariant.remove();

        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "terminateable");
        assertContainsHint(wf.hints(), "delete", true);

        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "not-terminateable");
        assertContainsHint(wf.hints(), "delete", false);

        publishedVariant.remove();

        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "terminateable");
        assertContainsHint(wf.hints(), "delete", true);
    }

    @Test
    public void testCopyState() throws Exception {
        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);

        MockNode handleNode = (MockNode)session.getRootNode().addNode("test", HippoNodeType.NT_HANDLE);
        MockNode draftVariant, unpublishedVariant, publishedVariant, publishRequest;

        draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);
        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "otheruser");
        unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);
        publishedVariant = addVariant(handleNode, HippoStdNodeType.PUBLISHED);
        publishRequest = addRequest(handleNode, HippoStdPubWfNodeType.PUBLISH, true);

        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "copyable");
        assertContainsHint(wf.hints(), "copy", true);

        wf.setNode(handleNode);

        unpublishedVariant.remove();
        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "copyable");
        assertContainsHint(wf.hints(), "copy", true);

        session.setPermissions(publishedVariant.getPath(), "hippo:editor", false);
        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "no-copy");
        assertNotContainsHint(wf.hints(), "copy");

        session.setPermissions(publishedVariant.getPath(), "hippo:editor", true);
        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "no-copy");
        assertNotContainsHint(wf.hints(), "copy");

        wf.setNode(handleNode);

        assertContainsStateIds(wf.getWorkflowExecutor(), "copyable");
        assertContainsHint(wf.hints(), "copy", true);
    }

    @Test
    public void testNoState() throws Exception {
        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);

        MockNode handleNode = (MockNode)session.getRootNode().addNode("test", HippoNodeType.NT_HANDLE);
        MockNode unpublishedVariant, frozenNode;

        unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);

        MockVersion versionNode = new MockVersion("1.0", JcrConstants.NT_VERSION);
        ((MockNode) unpublishedVariant.getParent()).addNode(versionNode);
        frozenNode = versionNode.addMockNode(JcrConstants.JCR_FROZEN_NODE, JcrConstants.NT_FROZEN_NODE);
        frozenNode.setProperty(JcrConstants.JCR_FROZEN_UUID, unpublishedVariant.getIdentifier());

        unpublishedVariant.remove();
        wf.setNode(handleNode);

        assertMatchingStateIds(wf.getWorkflowExecutor(), "status", "no-edit", "no-request", "no-publish", "no-depublish", "no-versioning", "no-terminate", "no-copy");
        assertEquals(0, wf.hints().size());
    }
}