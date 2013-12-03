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
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.RepositoryException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.io.IOUtils;
import org.apache.commons.scxml2.SCXMLExecutor;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.reviewedactions.HippoStdPubWfNodeType;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.scxml.MockRepositorySCXMLRegistry;
import org.onehippo.repository.scxml.RepositorySCXMLExecutorFactory;
import org.onehippo.repository.scxml.SCXMLExecutorFactory;
import org.onehippo.repository.scxml.SCXMLRegistry;
import org.onehippo.repository.scxml.SCXMLUtils;

public class TestDocumentWorkflowImpl {

    @BeforeClass
    public static void beforeClass() throws Exception {
        MockRepositorySCXMLRegistry registry = new MockRepositorySCXMLRegistry();
        String scxml = IOUtils.toString(TestDocumentWorkflowImpl.class.getResourceAsStream("test-document-workflow.scxml"));
        MockNode scxmlConfigNode = registry.createConfigNode();
        MockNode scxmlNode = registry.addScxmlNode(scxmlConfigNode, "document-workflow", scxml);
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "copyvariant", CopyVariantAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "request", RequestAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "archive", ArchiveAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "ismodified", IsModifiedAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "schedulerequest", ScheduleRequestAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "copydocument", CopyDocumentAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "movedocument", MoveDocumentAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "renamedocument", RenameDocumentAction.class.getName());
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "hint", HintAction.class.getName());
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
        MockNode variant = handle.addMockNode(PublicationRequest.HIPPO_REQUEST, PublicationRequest.NT_HIPPOSTDPUBWF_REQUEST);
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
    public void testWorkflowFeatures() throws Exception {
        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        MockWorkflowContext context = new MockWorkflowContext("testuser");
        wf.setWorkflowContext(context);
        MockNode handleNode = MockNode.root().addMockNode("test", HippoNodeType.NT_HANDLE);
        MockNode draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);

        wf.setNode(draftVariant);
        assertEquals(DocumentWorkflow.Features.all, wf.getDocumentModel().getFeatures());

        context.getWorkflowConfiguration().put("workflow.features", DocumentWorkflow.Features.document.name());
        wf.setNode(draftVariant);
        assertEquals(DocumentWorkflow.Features.document, wf.getDocumentModel().getFeatures());

        context.getWorkflowConfiguration().put("workflow.features", "undefined");
        wf.setNode(draftVariant);
        assertEquals(DocumentWorkflow.Features.all, wf.getDocumentModel().getFeatures());
    }

    @Test
    public void testHandleNewState() throws Exception {

        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        wf.setWorkflowContext(new MockWorkflowContext("testuser"));

        MockNode handleNode = MockNode.root().addMockNode("test", HippoNodeType.NT_HANDLE);
        MockNode draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);

        wf.setNode(draftVariant);
        assertMatchingStateIds(wf.getScxmlExecutor(), "new.not-edit");

        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "testuser");

        wf.setNode(draftVariant);
        assertMatchingStateIds(wf.getScxmlExecutor(), "new.edit");

        assertTrue(wf.hints().isEmpty());
    }

    @Test
    public void testHandleNotNewEditableState() throws Exception {

        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        wf.setWorkflowContext(new MockWorkflowContext("testuser"));

        MockNode draftVariant, unpublishedVariant, publishedVariant = null;
        MockNode handleNode = MockNode.root().addMockNode("test", HippoNodeType.NT_HANDLE);
        publishedVariant = addVariant(handleNode, HippoStdNodeType.PUBLISHED);

        wf.setNode(publishedVariant);
        assertMatchingStateIds(wf.getScxmlExecutor(), "not-new.not-edit", "other");
        publishedVariant.remove();
        unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);
        wf.setNode(unpublishedVariant);
        assertMatchingStateIds(wf.getScxmlExecutor(), "not-new.not-edit", "other");
        publishedVariant = addVariant(handleNode, HippoStdNodeType.PUBLISHED);
        wf.setNode(publishedVariant);
        assertMatchingStateIds(wf.getScxmlExecutor(), "not-new.not-edit", "other");
        draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);
        wf.setNode(draftVariant);
        assertMatchingStateIds(wf.getScxmlExecutor(), "not-new.not-edit", "other");
        publishedVariant.remove();
        unpublishedVariant.remove();
        wf.setNode(draftVariant);
        assertMatchingStateIds(wf.getScxmlExecutor(), "new.not-edit");
        unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);
        publishedVariant = addVariant(handleNode, HippoStdNodeType.PUBLISHED);
        wf.setNode(draftVariant);
        assertMatchingStateIds(wf.getScxmlExecutor(), "not-new.not-edit", "other");
    }

    @Test
    public void testHandleNotNewEditableInitialState() throws Exception {

        DocumentWorkflowImpl wf = new DocumentWorkflowImpl();
        wf.setWorkflowContext(new MockWorkflowContext("testuser"));

        MockNode draftVariant, unpublishedVariant, publishedVariant = null;
        MockNode handleNode = MockNode.root().addMockNode("test", HippoNodeType.NT_HANDLE);
        unpublishedVariant = addVariant(handleNode, HippoStdNodeType.UNPUBLISHED);
        publishedVariant = addVariant(handleNode, HippoStdNodeType.PUBLISHED);
        draftVariant = addVariant(handleNode, HippoStdNodeType.DRAFT);
        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "testuser");

        wf.setNode(draftVariant);
        assertMatchingStateIds(wf.getScxmlExecutor(), "not-new.edit", "other");

        draftVariant.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, (String)null);

        wf.setNode(unpublishedVariant);
        assertMatchingStateIds(wf.getScxmlExecutor(), "not-new.not-edit", "other");
        assertContainsHint(wf.hints(), "obtainEditableInstance", true);

        wf.setNode(draftVariant);
        assertNotContainsHint(wf.hints(), "obtainEditableInstance");

        wf.setNode(publishedVariant);
        assertNotContainsHint(wf.hints(), "obtainEditableInstance");

        unpublishedVariant.remove();

        wf.setNode(publishedVariant);
        assertNotContainsHint(wf.hints(), "obtainEditableInstance");

        wf.setNode(draftVariant);
        assertContainsHint(wf.hints(), "obtainEditableInstance", true);

        draftVariant.remove();
        wf.setNode(publishedVariant);
        assertContainsHint(wf.hints(), "obtainEditableInstance", true);

        MockNode rejectedRequest = addRequest(handleNode, PublicationRequest.REJECTED);

        wf.setNode(publishedVariant);
        assertContainsHint(wf.hints(), "obtainEditableInstance", true);

        MockNode publishRequest = addRequest(handleNode, PublicationRequest.PUBLISH);

        wf.setNode(publishedVariant);
        assertNotContainsHint(wf.hints(), "obtainEditableInstance");
    }
}
