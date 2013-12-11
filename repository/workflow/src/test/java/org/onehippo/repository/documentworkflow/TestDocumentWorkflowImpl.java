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
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.scxml2.SCXMLExecutor;
import org.apache.jackrabbit.util.ISO8601;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.RepositoryMap;
import org.hippoecm.repository.reviewedactions.HippoStdPubWfNodeType;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.documentworkflow.action.ArchiveDelegatingAction;
import org.onehippo.repository.documentworkflow.action.CopyDocumentDelegatingAction;
import org.onehippo.repository.documentworkflow.action.CopyVariantDelegatingAction;
import org.onehippo.repository.documentworkflow.action.HintDelegatingAction;
import org.onehippo.repository.documentworkflow.action.IsModifiedDelegatingAction;
import org.onehippo.repository.documentworkflow.action.MoveDocumentDelegatingAction;
import org.onehippo.repository.documentworkflow.action.RenameDocumentDelegatingAction;
import org.onehippo.repository.documentworkflow.action.RequestDelegatingAction;
import org.onehippo.repository.documentworkflow.action.ScheduleRequestDelegatingAction;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.mock.MockValue;
import org.onehippo.repository.mock.MockVersion;
import org.onehippo.repository.scxml.MockRepositorySCXMLRegistry;
import org.onehippo.repository.scxml.RepositorySCXMLExecutorFactory;
import org.onehippo.repository.scxml.SCXMLExecutorFactory;
import org.onehippo.repository.scxml.SCXMLRegistry;
import org.onehippo.repository.scxml.SCXMLUtils;
import org.onehippo.repository.util.JcrConstants;

public class TestDocumentWorkflowImpl {

    @BeforeClass
    public static void beforeClass() throws Exception {
        MockRepositorySCXMLRegistry registry = new MockRepositorySCXMLRegistry();
        String scxml = IOUtils.toString(TestDocumentWorkflowImpl.class.getResourceAsStream("test-document-workflow.scxml"));
        MockNode scxmlConfigNode = registry.createConfigNode();
        MockNode scxmlNode = registry.addScxmlNode(scxmlConfigNode, "document-workflow", scxml);
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "copyvariant", CopyVariantDelegatingAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "request", RequestDelegatingAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "archive", ArchiveDelegatingAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "ismodified", IsModifiedDelegatingAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "schedulerequest", ScheduleRequestDelegatingAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "copydocument", CopyDocumentDelegatingAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "movedocument", MoveDocumentDelegatingAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "renamedocument", RenameDocumentDelegatingAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "hint", HintDelegatingAction.class.getName());
        registry.setUp(scxmlConfigNode);

        HippoServiceRegistry.registerService(registry, SCXMLRegistry.class);
        HippoServiceRegistry.registerService(new RepositorySCXMLExecutorFactory(), SCXMLExecutorFactory.class);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        HippoServiceRegistry.unregisterService(HippoServiceRegistry.getService(SCXMLExecutorFactory.class), SCXMLExecutorFactory.class);
        HippoServiceRegistry.unregisterService(HippoServiceRegistry.getService(SCXMLRegistry.class), SCXMLRegistry.class);
    }

    protected MockNode addVariant(MockNode handle, String state) throws RepositoryException {
        MockNode variant = handle.addMockNode(handle.getName(), HippoStdPubWfNodeType.HIPPOSTDPUBWF_DOCUMENT);
        variant.setProperty(HippoStdNodeType.HIPPOSTD_STATE, state);
        return variant;
    }

    protected MockNode addRequest(MockNode handle, String type) throws RepositoryException {
        MockNode variant = handle.addMockNode(PublicationRequest.HIPPO_REQUEST, HippoNodeType.NT_REQUEST);
        variant.setProperty(PublicationRequest.HIPPOSTDPUBWF_TYPE, type);
        return variant;
    }

    protected Set<String> set(String... vargs) {
        Set<String> set = new TreeSet<>();
        for (String arg : vargs) {
            set.add(arg);
        }
        return set;
    }

    protected void assertMatchingStateIds(SCXMLExecutor executor, String ... ids) {
        Set<String> stateIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        if (ids.length == stateIds.size()) {
            for (String id : ids) {
                if (!stateIds.contains(id))
                    Assert.fail("Current SCXML states "+stateIds+" not matching expected states "+set(ids)+"");
            }
            return;
        }
        Assert.fail("Current SCXML states ["+stateIds+"] not matching expected states "+set(ids)+"");
    }

    protected void assertContainsStateIds(SCXMLExecutor executor, String ... ids) {
        Set<String> stateIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        if (ids.length <= stateIds.size()) {
            for (String id : ids) {
                if (!stateIds.contains(id))
                    Assert.fail("Current SCXML states "+stateIds+" not containing expected states "+set(ids)+"");
            }
            return;
        }
        Assert.fail("Current SCXML states ["+stateIds+"] not containing expected states "+set(ids)+"");
    }

    protected void assertNotContainsStateIds(SCXMLExecutor executor, String ... ids) {
        Set<String> stateIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        if (ids.length <= stateIds.size()) {
            for (String id : ids) {
                if (stateIds.contains(id))
                    Assert.fail("Current SCXML states "+stateIds+" containing not expected states "+set(ids)+"");
            }
            return;
        }
        Assert.fail("Current SCXML states ["+stateIds+"] not containing expected states "+set(ids)+"");
    }

    protected void assertContainsHint(Map<String, Serializable> hints, String hint, Object value) {
        if (!hints.containsKey(hint) || ! hints.get(hint).equals(value)) {
            Assert.fail("Current hints "+hints+" not containing expected hint ["+hint+"] with value ["+value+"]");
        }
    }

    protected void assertNotContainsHint(Map<String, Serializable> hints, String hint) {
        if (hints.containsKey(hint)) {
            Assert.fail("Current hints "+hints+" contains not expected hint ["+hint+"]");
        }
    }

    @Test
    public void testStatusState() throws Exception {

        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        RepositoryMap workflowConfig = workflowContext.getWorkflowConfiguration();
        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);

        MockNode handleNode = MockNode.root().addMockNode("test", HippoNodeType.NT_HANDLE);
        MockNode draftVariant, unpublishedVariant, publishedVariant = null;

        draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);
        unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);
        publishedVariant = addVariant(handleNode, HippoStdNodeType.PUBLISHED);

        workflowConfig.put("workflow.supportedFeatures", DocumentWorkflow.SupportedFeatures.request.name());
        wf.setNode(unpublishedVariant);

        assertContainsStateIds(wf.getScxmlExecutor(), "status");
        assertNotContainsHint(wf.hints(), "status");
        assertNotContainsHint(wf.hints(), "checkModified");

        workflowConfig.put("workflow.supportedFeatures", DocumentWorkflow.SupportedFeatures.document.name());
        wf.setNode(unpublishedVariant);

        assertContainsHint(wf.hints(), "status", true);
        assertContainsHint(wf.hints(), "checkModified", false);

        wf.setNode(draftVariant);

        assertContainsHint(wf.hints(), "status", false);
        assertContainsHint(wf.hints(), "checkModified", true);

        wf.setNode(publishedVariant);

        assertContainsHint(wf.hints(), "status", false);
        assertContainsHint(wf.hints(), "checkModified", false);

        unpublishedVariant.remove();
        wf.setNode(publishedVariant);

        assertContainsHint(wf.hints(), "status", true);
        assertContainsHint(wf.hints(), "checkModified", false);

        wf.setNode(draftVariant);

        assertContainsHint(wf.hints(), "status", false);
        assertContainsHint(wf.hints(), "checkModified", false);

        publishedVariant.remove();
        wf.setNode(draftVariant);

        assertContainsHint(wf.hints(), "status", true);

        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "testuser");
        wf.setNode(draftVariant);

        assertContainsHint(wf.hints(), "status", true);

        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "otheruser");
        wf.setNode(draftVariant);

        assertContainsHint(wf.hints(), "status", false);

        unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);
        wf.setNode(unpublishedVariant);

        assertContainsHint(wf.hints(), "status", false);

        draftVariant.remove();
        wf.setNode(unpublishedVariant);

        assertContainsHint(wf.hints(), "status", true);
    }

        @Test
    public void testEditState() throws Exception {

        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        RepositoryMap workflowConfig = workflowContext.getWorkflowConfiguration();
        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);

        MockNode handleNode = MockNode.root().addMockNode("test", HippoNodeType.NT_HANDLE);
        MockNode draftVariant, unpublishedVariant, publishedVariant, rejectedRequest, publishRequest = null;

        draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);
        workflowConfig.put("workflow.supportedFeatures", DocumentWorkflow.SupportedFeatures.request.name());
        wf.setNode(draftVariant);

        assertContainsStateIds(wf.getScxmlExecutor(), "no-edit");
        assertNotContainsHint(wf.hints(), "obtainEditableInstance");
        assertNotContainsHint(wf.hints(), "commitEditableInstance");
        assertNotContainsHint(wf.hints(), "disposeEditableInstance");
        assertNotContainsHint(wf.hints(), "unlock");

        workflowConfig.remove("workflow.supportedFeatures");
        wf.setNode(draftVariant);

        // test state not-editable
        assertContainsStateIds(wf.getScxmlExecutor(), "editable");

        rejectedRequest = addRequest(handleNode, PublicationRequest.REJECTED);
        wf.setNode(rejectedRequest);

        assertContainsStateIds(wf.getScxmlExecutor(), "no-edit");

        rejectedRequest.remove();
        wf.setNode(draftVariant);

        assertContainsStateIds(wf.getScxmlExecutor(), "editable");

        publishRequest = addRequest(handleNode, PublicationRequest.PUBLISH);
        wf.setNode(draftVariant);

        assertContainsStateIds(wf.getScxmlExecutor(), "not-editable");

        publishRequest.remove();

        // test state not-editing
        wf.setNode(draftVariant);

        assertContainsStateIds(wf.getScxmlExecutor(), "editable");
        assertContainsHint(wf.hints(), "obtainEditableInstance", true);
        assertContainsHint(wf.hints(), "unlock", false);

        workflowConfig.put("workflow.supportedFeatures", DocumentWorkflow.SupportedFeatures.document.name());
        wf.setNode(draftVariant);

        assertContainsHint(wf.hints(), "obtainEditableInstance", true);
        assertNotContainsHint(wf.hints(), "unlock");

        workflowConfig.put("workflow.supportedFeatures", DocumentWorkflow.SupportedFeatures.unlock.name());
        wf.setNode(draftVariant);

        assertContainsHint(wf.hints(), "unlock", false);
        assertNotContainsHint(wf.hints(), "obtainEditableInstance");

        workflowConfig.remove("workflow.supportedFeatures");
        unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);
        wf.setNode(draftVariant);

        assertContainsHint(wf.hints(), "obtainEditableInstance", false);

        wf.setNode(unpublishedVariant);

        assertContainsHint(wf.hints(), "obtainEditableInstance", true);

        unpublishedVariant.remove();
        draftVariant.remove();

        publishedVariant = addVariant(handleNode, HippoStdNodeType.PUBLISHED);
        wf.setNode(publishedVariant);

        assertContainsHint(wf.hints(), "obtainEditableInstance", true);

        rejectedRequest = addRequest(handleNode, PublicationRequest.REJECTED);
        wf.setNode(publishedVariant);

        assertContainsHint(wf.hints(), "obtainEditableInstance", true);
        assertContainsStateIds(wf.getScxmlExecutor(), "editable");

        wf.setNode(rejectedRequest);

        assertContainsStateIds(wf.getScxmlExecutor(), "no-edit");

        // test state editing
        publishedVariant.remove();
        rejectedRequest.remove();

        workflowConfig.put("workflow.supportedFeatures", DocumentWorkflow.SupportedFeatures.document.name());
        draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);
        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "testuser");
        wf.setNode(draftVariant);

        assertContainsStateIds(wf.getScxmlExecutor(), "editing");
        assertContainsHint(wf.hints(), "obtainEditableInstance", true);
        assertContainsHint(wf.hints(), "commitEditableInstance", true);
        assertContainsHint(wf.hints(), "disposeEditableInstance", true);
        assertNotContainsHint(wf.hints(), "unlock");

        workflowConfig.put("workflow.supportedFeatures", DocumentWorkflow.SupportedFeatures.unlock.name());
        wf.setNode(draftVariant);

        assertContainsStateIds(wf.getScxmlExecutor(), "editing");
        assertNotContainsHint(wf.hints(), "obtainEditableInstance");
        assertNotContainsHint(wf.hints(), "commitEditableInstance");
        assertNotContainsHint(wf.hints(), "disposeEditableInstance");
        assertContainsHint(wf.hints(), "unlock", true);

        workflowConfig.remove("workflow.supportedFeatures");
        wf.setNode(draftVariant);

        assertContainsStateIds(wf.getScxmlExecutor(), "editing");
        assertContainsHint(wf.hints(), "obtainEditableInstance", true);
        assertContainsHint(wf.hints(), "commitEditableInstance", true);
        assertContainsHint(wf.hints(), "disposeEditableInstance", true);
        assertContainsHint(wf.hints(), "unlock", true);
        assertNotContainsHint(wf.hints(), "inUseBy");

        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "otheruser");
        session.setPermissions(draftVariant.getPath(), "hippo:admin", false);
        wf.setNode(draftVariant);

        assertContainsStateIds(wf.getScxmlExecutor(), "editing");
        assertContainsHint(wf.hints(), "obtainEditableInstance", false);
        assertContainsHint(wf.hints(), "commitEditableInstance", false);
        assertContainsHint(wf.hints(), "disposeEditableInstance", false);
        assertContainsHint(wf.hints(), "unlock", false);
        assertContainsHint(wf.hints(), "inUseBy", "otheruser");

        session.setPermissions(draftVariant.getPath(), "hippo:admin", true);
        wf.setNode(draftVariant);

        assertContainsStateIds(wf.getScxmlExecutor(), "editing");
        assertContainsHint(wf.hints(), "obtainEditableInstance", false);
        assertContainsHint(wf.hints(), "commitEditableInstance", false);
        assertContainsHint(wf.hints(), "disposeEditableInstance", false);
        assertContainsHint(wf.hints(), "unlock", true);
    }

    @Test
    public void testRequestState() throws Exception {
        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        RepositoryMap workflowConfig = workflowContext.getWorkflowConfiguration();
        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);

        MockNode handleNode = MockNode.root().addMockNode("test", HippoNodeType.NT_HANDLE);
        MockNode unpublishedVariant, rejectedRequest, publishRequest = null;

        // test state not-requested
        unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);
        wf.setNode(unpublishedVariant);

        assertContainsStateIds(wf.getScxmlExecutor(), "not-requested");

        // test state requested
        publishRequest = addRequest(handleNode, PublicationRequest.PUBLISH);
        wf.setNode(unpublishedVariant);

        assertContainsStateIds(wf.getScxmlExecutor(), "requested");

        rejectedRequest = addRequest(handleNode, PublicationRequest.REJECTED);
        wf.setNode(unpublishedVariant);

        assertContainsStateIds(wf.getScxmlExecutor(), "requested");

        wf.setNode(rejectedRequest);

        assertContainsStateIds(wf.getScxmlExecutor(), "request-rejected");

        session.setPermissions(publishRequest.getPath(), "hippo:editor", false);
        wf.setNode(publishRequest);

        assertContainsStateIds(wf.getScxmlExecutor(), "requested");
        assertContainsHint(wf.hints(), "cancelRequest", false);
        assertNotContainsHint(wf.hints(), "acceptRequest");
        assertNotContainsHint(wf.hints(), "rejectRequest");

        workflowConfig.put("workflow.supportedFeatures", DocumentWorkflow.SupportedFeatures.document.name());
        wf.setNode(publishRequest);

        assertContainsStateIds(wf.getScxmlExecutor(), "requested");
        assertContainsHint(wf.hints(), "cancelRequest", false);
        assertNotContainsHint(wf.hints(), "acceptRequest");
        assertNotContainsHint(wf.hints(), "rejectRequest");

        workflowConfig.put("workflow.supportedFeatures", DocumentWorkflow.SupportedFeatures.request.name());
        session.setPermissions(publishRequest.getPath(), "hippo:editor", true);

        wf.setNode(publishRequest);
        assertContainsStateIds(wf.getScxmlExecutor(), "requested");
        assertContainsHint(wf.hints(), "cancelRequest", false);
        assertContainsHint(wf.hints(), "acceptRequest", true);
        assertContainsHint(wf.hints(), "rejectRequest", true);

        publishRequest.setProperty(PublicationRequest.HIPPOSTDPUBWF_USERNAME, "testuser");
        wf.setNode(publishRequest);

        assertContainsHint(wf.hints(), "cancelRequest", true);
        assertContainsHint(wf.hints(), "rejectRequest", false);

        publishRequest.setProperty(PublicationRequest.HIPPOSTDPUBWF_USERNAME, "otheruser");
        wf.setNode(publishRequest);

        assertContainsHint(wf.hints(), "cancelRequest", false);
        assertContainsHint(wf.hints(), "rejectRequest", true);

        session.setPermissions(publishRequest.getPath(), "hippo:editor", false);
        wf.setNode(publishRequest);

        assertContainsHint(wf.hints(), "cancelRequest", false);
        assertNotContainsHint(wf.hints(), "acceptRequest");
        assertNotContainsHint(wf.hints(), "rejectRequest");

        publishRequest.setProperty(PublicationRequest.HIPPOSTDPUBWF_USERNAME, "testuser");

        wf.setNode(publishRequest);

        assertContainsHint(wf.hints(), "cancelRequest", true);
        assertNotContainsHint(wf.hints(), "acceptRequest");
        assertNotContainsHint(wf.hints(), "rejectRequest");

        // test state request-rejected

        session.setPermissions(rejectedRequest.getPath(), "hippo:editor", false);
        wf.setNode(rejectedRequest);

        assertContainsStateIds(wf.getScxmlExecutor(), "request-rejected");
        assertContainsHint(wf.hints(), "cancelRequest", false);
        assertContainsHint(wf.hints(), "acceptRequest", false);
        assertContainsHint(wf.hints(), "rejectRequest", false);

        workflowConfig.put("workflow.supportedFeatures", DocumentWorkflow.SupportedFeatures.document.name());
        wf.setNode(rejectedRequest);

        assertContainsStateIds(wf.getScxmlExecutor(), "request-rejected");

        workflowConfig.remove("workflow.supportedFeatures");
        rejectedRequest.setProperty(PublicationRequest.HIPPOSTDPUBWF_USERNAME, "testuser");
        wf.setNode(rejectedRequest);

        assertContainsHint(wf.hints(), "cancelRequest", true);
        assertContainsHint(wf.hints(), "acceptRequest", false);
        assertContainsHint(wf.hints(), "rejectRequest", false);

        rejectedRequest.setProperty(PublicationRequest.HIPPOSTDPUBWF_USERNAME, "otheruser");
        wf.setNode(rejectedRequest);

        assertContainsHint(wf.hints(), "cancelRequest", false);
        assertContainsHint(wf.hints(), "acceptRequest", false);
        assertContainsHint(wf.hints(), "rejectRequest", false);

        session.setPermissions(rejectedRequest.getPath(), "hippo:editor", true);
        wf.setNode(rejectedRequest);

        assertContainsHint(wf.hints(), "cancelRequest", true);
        assertContainsHint(wf.hints(), "acceptRequest", false);
        assertContainsHint(wf.hints(), "rejectRequest", false);

        publishRequest.getProperty(PublicationRequest.HIPPOSTDPUBWF_USERNAME).remove();
        wf.setNode(rejectedRequest);

        assertContainsHint(wf.hints(), "cancelRequest", true);
        assertContainsHint(wf.hints(), "acceptRequest", false);
        assertContainsHint(wf.hints(), "rejectRequest", false);

        session.setPermissions(rejectedRequest.getPath(), "hippo:editor", false);
        wf.setNode(rejectedRequest);

        assertContainsHint(wf.hints(), "cancelRequest", false);
        assertContainsHint(wf.hints(), "acceptRequest", false);
        assertContainsHint(wf.hints(), "rejectRequest", false);
    }

    @Test
    public void testPublishState() throws Exception {
        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        RepositoryMap workflowConfig = workflowContext.getWorkflowConfiguration();
        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);

        MockNode handleNode = MockNode.root().addMockNode("test", HippoNodeType.NT_HANDLE);
        MockNode draftVariant, unpublishedVariant, publishedVariant, publishRequest = null;

        unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);
        workflowConfig.put("workflow.supportedFeatures", DocumentWorkflow.SupportedFeatures.request.name());
        wf.setNode(unpublishedVariant);

        assertContainsStateIds(wf.getScxmlExecutor(), "no-publish");
        assertNotContainsHint(wf.hints(), "publish");

        workflowConfig.remove("workflow.supportedFeatures");
        wf.setNode(unpublishedVariant);

        assertContainsStateIds(wf.getScxmlExecutor(), "publishable");
        assertContainsHint(wf.hints(), "publish", true);

        unpublishedVariant.remove();
        publishRequest = addRequest(handleNode, PublicationRequest.PUBLISH);
        wf.setNode(publishRequest);

        assertContainsStateIds(wf.getScxmlExecutor(), "no-publish");
        assertNotContainsHint(wf.hints(), "publish");

        unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);
        wf.setNode(unpublishedVariant);

        assertContainsHint(wf.hints(), "publish", false);

        workflowContext.setUserIdentity("system");
        wf.setNode(unpublishedVariant);

        assertContainsHint(wf.hints(), "publish", true);

        workflowContext.setUserIdentity("testuser");
        publishRequest.remove();
        draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);
        wf.setNode(draftVariant);

        assertContainsHint(wf.hints(), "publish", false);

        wf.setNode(unpublishedVariant);

        assertContainsHint(wf.hints(), "publish", true);

        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "testuser");
        wf.setNode(unpublishedVariant);

        assertContainsStateIds(wf.getScxmlExecutor(), "not-publishable");

        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "otheruser");
        wf.setNode(unpublishedVariant);

        assertContainsStateIds(wf.getScxmlExecutor(), "not-publishable");

        draftVariant.getProperty(HippoStdNodeType.HIPPOSTD_HOLDER).remove();

        wf.setNode(unpublishedVariant);

        assertContainsHint(wf.hints(), "publish", true);

        publishedVariant = addVariant(handleNode, HippoStdNodeType.PUBLISHED);
        wf.setNode(unpublishedVariant);

        assertContainsHint(wf.hints(), "publish", false);

        Calendar publishedModified = Calendar.getInstance();
        Calendar unpublishedModified = Calendar.getInstance();
        publishedModified.setTimeInMillis(unpublishedModified.getTimeInMillis()-1000);
        unpublishedVariant.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE, new MockValue(PropertyType.DATE, ISO8601.format(unpublishedModified)));
        publishedVariant.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE, new MockValue(PropertyType.DATE, ISO8601.format(publishedModified)));

        wf.setNode(unpublishedVariant);

        assertContainsHint(wf.hints(), "publish", true);

        publishedVariant.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE, new MockValue(PropertyType.DATE, ISO8601.format(unpublishedModified)));

        wf.setNode(unpublishedVariant);

        assertContainsHint(wf.hints(), "publish", false);
    }

    @Test
    public void testDePublishState() throws Exception {
        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        RepositoryMap workflowConfig = workflowContext.getWorkflowConfiguration();
        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);

        MockNode handleNode = MockNode.root().addMockNode("test", HippoNodeType.NT_HANDLE);
        MockNode draftVariant, unpublishedVariant, publishedVariant, publishRequest = null;

        publishedVariant = addVariant(handleNode, HippoStdNodeType.PUBLISHED);
        workflowConfig.put("workflow.supportedFeatures", DocumentWorkflow.SupportedFeatures.request.name());
        wf.setNode(publishedVariant);

        assertContainsStateIds(wf.getScxmlExecutor(), "no-depublish");
        assertNotContainsHint(wf.hints(), "depublish");

        workflowConfig.remove("workflow.supportedFeatures");
        wf.setNode(publishedVariant);

        // note: empty/no availability means allways available
        assertContainsStateIds(wf.getScxmlExecutor(), "depublishable");
        assertContainsHint(wf.hints(), "depublish", true);

        publishedVariant.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[]{"foo"});

        wf.setNode(publishedVariant);

        assertContainsStateIds(wf.getScxmlExecutor(), "not-depublishable");
        assertContainsHint(wf.hints(), "depublish", false);

        publishedVariant.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[]{"foo","live"});

        wf.setNode(publishedVariant);

        assertContainsStateIds(wf.getScxmlExecutor(), "depublishable");
        assertContainsHint(wf.hints(), "depublish", true);

        draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);
        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "testuser");
        wf.setNode(publishedVariant);

        assertContainsStateIds(wf.getScxmlExecutor(), "not-depublishable");

        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "otheruser");
        wf.setNode(publishedVariant);

        assertContainsStateIds(wf.getScxmlExecutor(), "not-depublishable");

        draftVariant.getProperty(HippoStdNodeType.HIPPOSTD_HOLDER).remove();
        publishRequest = addRequest(handleNode, PublicationRequest.PUBLISH);
        wf.setNode(publishRequest);

        assertContainsStateIds(wf.getScxmlExecutor(), "no-depublish");

        workflowContext.setUserIdentity("system");
        wf.setNode(draftVariant);

        assertContainsStateIds(wf.getScxmlExecutor(), "depublishable");
        assertContainsHint(wf.hints(), "depublish", true);

        workflowContext.setUserIdentity("testuser");

        wf.setNode(draftVariant);

        assertContainsStateIds(wf.getScxmlExecutor(), "depublishable");
        assertContainsHint(wf.hints(), "depublish", false);

        publishRequest.remove();

        wf.setNode(draftVariant);

        assertContainsStateIds(wf.getScxmlExecutor(), "depublishable");
        assertContainsHint(wf.hints(), "depublish", true);

        unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);

        wf.setNode(draftVariant);

        assertContainsStateIds(wf.getScxmlExecutor(), "depublishable");
        assertContainsHint(wf.hints(), "depublish", false);

        wf.setNode(publishedVariant);

        assertContainsStateIds(wf.getScxmlExecutor(), "depublishable");
        assertContainsHint(wf.hints(), "depublish", false);

        wf.setNode(unpublishedVariant);

        assertContainsStateIds(wf.getScxmlExecutor(), "depublishable");
        assertContainsHint(wf.hints(), "depublish", true);

        unpublishedVariant.remove();
        wf.setNode(publishedVariant);

        assertContainsStateIds(wf.getScxmlExecutor(), "depublishable");
        assertContainsHint(wf.hints(), "depublish", false);

        draftVariant.remove();
        wf.setNode(publishedVariant);

        assertContainsStateIds(wf.getScxmlExecutor(), "depublishable");
        assertContainsHint(wf.hints(), "depublish", true);
    }

    @Test
    public void testVersioningState() throws Exception {
        MockAccessManagedSession session = new MockAccessManagedSession(MockNode.root());
        MockWorkflowContext workflowContext = new MockWorkflowContext("testuser", session);
        RepositoryMap workflowConfig = workflowContext.getWorkflowConfiguration();
        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        wf.setWorkflowContext(workflowContext);

        MockNode handleNode = MockNode.root().addMockNode("test", HippoNodeType.NT_HANDLE);
        MockNode unpublishedVariant, frozenNode = null;

        unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);

        MockVersion versionNode = new MockVersion("1.0", JcrConstants.NT_VERSION);
        ((MockNode)unpublishedVariant.getParent()).addNode(versionNode);
        frozenNode = versionNode.addMockNode(JcrConstants.JCR_FROZEN_NODE, JcrConstants.NT_FROZEN_NODE);
        frozenNode.setProperty(JcrConstants.JCR_FROZEN_UUID, unpublishedVariant.getIdentifier());

        wf.setNode(unpublishedVariant);

        assertContainsStateIds(wf.getScxmlExecutor(), "version");
        assertContainsHint(wf.hints(), "version", true);
        assertContainsHint(wf.hints(), "listVersions", true);
        assertContainsHint(wf.hints(), "restoreVersion", true);
        assertContainsHint(wf.hints(), "revertVersion", true);
        assertNotContainsHint(wf.hints(), "restoreVersionTo");

        workflowConfig.put("workflow.supportedFeatures", DocumentWorkflow.SupportedFeatures.document.name());
        wf.setNode(unpublishedVariant);

        assertContainsStateIds(wf.getScxmlExecutor(), "no-versioning");
        assertNotContainsHint(wf.hints(), "version");
        assertNotContainsHint(wf.hints(), "restoreVersionTo");

        wf.setNode(frozenNode);
        assertContainsStateIds(wf.getScxmlExecutor(), "version");
        assertContainsHint(wf.hints(), "restoreVersionTo", true);
        assertNotContainsHint(wf.hints(), "version");
    }
}