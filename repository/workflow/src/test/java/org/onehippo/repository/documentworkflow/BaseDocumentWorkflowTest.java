/*
 * Copyright 2014-2020 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.assertj.core.api.SoftAssertions;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.quartz.HippoSchedJcrConstants;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.documentworkflow.action.ArchiveDocumentAction;
import org.onehippo.repository.documentworkflow.action.BranchAction;
import org.onehippo.repository.documentworkflow.action.CampaignAction;
import org.onehippo.repository.documentworkflow.action.CheckoutBranchAction;
import org.onehippo.repository.documentworkflow.action.ConfigVariantAction;
import org.onehippo.repository.documentworkflow.action.CopyDocumentAction;
import org.onehippo.repository.documentworkflow.action.CopyVariantAction;
import org.onehippo.repository.documentworkflow.action.DeleteRequestAction;
import org.onehippo.repository.documentworkflow.action.GetBranchAction;
import org.onehippo.repository.documentworkflow.action.IsModifiedAction;
import org.onehippo.repository.documentworkflow.action.LabelAction;
import org.onehippo.repository.documentworkflow.action.ListBranchesAction;
import org.onehippo.repository.documentworkflow.action.ListVersionsVariantAction;
import org.onehippo.repository.documentworkflow.action.MoveDocumentAction;
import org.onehippo.repository.documentworkflow.action.RejectRequestAction;
import org.onehippo.repository.documentworkflow.action.RemoveBranchAction;
import org.onehippo.repository.documentworkflow.action.RenameDocumentAction;
import org.onehippo.repository.documentworkflow.action.RequestActionAction;
import org.onehippo.repository.documentworkflow.action.RestoreVersionAction;
import org.onehippo.repository.documentworkflow.action.RestoreVersionByVersionAction;
import org.onehippo.repository.documentworkflow.action.RetrieveVersionAction;
import org.onehippo.repository.documentworkflow.action.ScheduleWorkflowAction;
import org.onehippo.repository.documentworkflow.action.SetHolderAction;
import org.onehippo.repository.documentworkflow.action.SetPreReintegrationLabelsAction;
import org.onehippo.repository.documentworkflow.action.SetRetainableAction;
import org.onehippo.repository.documentworkflow.action.SetTransferableAction;
import org.onehippo.repository.documentworkflow.action.VersionRestoreToAction;
import org.onehippo.repository.documentworkflow.action.VersionVariantAction;
import org.onehippo.repository.documentworkflow.action.WorkflowRequestAction;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.scxml.ActionAction;
import org.onehippo.repository.scxml.FeedbackAction;
import org.onehippo.repository.scxml.LogEventAction;
import org.onehippo.repository.scxml.MockRepositorySCXMLRegistry;
import org.onehippo.repository.scxml.RepositorySCXMLExecutorFactory;
import org.onehippo.repository.scxml.ResultAction;
import org.onehippo.repository.scxml.SCXMLExecutorFactory;
import org.onehippo.repository.scxml.SCXMLRegistry;
import org.onehippo.repository.scxml.WorkflowExceptionAction;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_AVAILABILITY;

/**
 * Base class for DocumentWorkflow based tests
 */
public class BaseDocumentWorkflowTest {

    private static final MockRepositorySCXMLRegistry registry = new MockRepositorySCXMLRegistry();
    private static final RepositorySCXMLExecutorFactory service = new RepositorySCXMLExecutorFactory();


    protected static String loadSCXML() throws Exception {
        return IOUtils.toString(DocumentWorkflowTest.class.getResourceAsStream("/hcm-config/documentworkflow.scxml"));
    }

    protected static void createDocumentWorkflowSCXMLRegistry() throws Exception {

        MockNode scxmlConfigNode = registry.createConfigNode();
        MockNode scxmlNode = registry.addScxmlNode(scxmlConfigNode, "documentworkflow", loadSCXML());
        getMockNode(scxmlNode, "action", ActionAction.class.getName());
        getMockNode(scxmlNode, "result", ResultAction.class.getName());
        getMockNode(scxmlNode, "feedback", FeedbackAction.class.getName());
        getMockNode(scxmlNode, "copyVariant", CopyVariantAction.class.getName());
        getMockNode(scxmlNode, "configVariant", ConfigVariantAction.class.getName());
        getMockNode(scxmlNode, "workflowRequest", WorkflowRequestAction.class.getName());
        getMockNode(scxmlNode, "archiveDocument", ArchiveDocumentAction.class.getName());
        getMockNode(scxmlNode, "isModified", IsModifiedAction.class.getName());
        getMockNode(scxmlNode, "scheduleWorkflow", ScheduleWorkflowAction.class.getName());
        getMockNode(scxmlNode, "copyDocument", CopyDocumentAction.class.getName());
        getMockNode(scxmlNode, "moveDocument", MoveDocumentAction.class.getName());
        getMockNode(scxmlNode, "renameDocument", RenameDocumentAction.class.getName());
        getMockNode(scxmlNode, "setHolder", SetHolderAction.class.getName());
        getMockNode(scxmlNode, "setTransferable", SetTransferableAction.class.getName());
        getMockNode(scxmlNode, "setRetainable", SetRetainableAction.class.getName());
        getMockNode(scxmlNode, "deleteRequest", DeleteRequestAction.class.getName());
        getMockNode(scxmlNode, "workflowException", WorkflowExceptionAction.class.getName());
        getMockNode(scxmlNode, "version", VersionVariantAction.class.getName());
        getMockNode(scxmlNode, "listVersions", ListVersionsVariantAction.class.getName());
        getMockNode(scxmlNode, "retrieveVersion", RetrieveVersionAction.class.getName());
        getMockNode(scxmlNode, "restoreVersion", RestoreVersionAction.class.getName());
        getMockNode(scxmlNode, "versionRestoreTo", VersionRestoreToAction.class.getName());
        getMockNode(scxmlNode, "restoreVersionByVersion", RestoreVersionByVersionAction.class.getName());
        getMockNode(scxmlNode, "requestAction", RequestActionAction.class.getName());
        getMockNode(scxmlNode, "rejectRequest", RejectRequestAction.class.getName());
        getMockNode(scxmlNode, "logEvent", LogEventAction.class.getName());
        getMockNode(scxmlNode, "listBranches", ListBranchesAction.class.getName());
        getMockNode(scxmlNode, "branch", BranchAction.class.getName());
        getMockNode(scxmlNode, "getBranch", GetBranchAction.class.getName());
        getMockNode(scxmlNode, "removeBranch", RemoveBranchAction.class.getName());
        getMockNode(scxmlNode, "checkoutBranch", CheckoutBranchAction.class.getName());
        getMockNode(scxmlNode, "setPreReintegrationLabels", SetPreReintegrationLabelsAction.class.getName());
        getMockNode(scxmlNode, "label", LabelAction.class.getName());
        getMockNode(scxmlNode, "campaign", CampaignAction.class.getName());
        getMockNode(scxmlNode, "removeCampaign", CampaignAction.class.getName());
        registry.setUp(scxmlConfigNode);

        HippoServiceRegistry.registerService(registry, SCXMLRegistry.class);
        HippoServiceRegistry.registerService(service, SCXMLExecutorFactory.class);

    }

    private static MockNode getMockNode(final MockNode scxmlNode, final String action, final String name) throws Exception {
        return registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", action, name);
    }

    protected static void destroyDocumentWorkflowSCXMLRegistry() throws Exception {
        HippoServiceRegistry.unregisterService(registry, SCXMLRegistry.class);
        HippoServiceRegistry.unregisterService(service, SCXMLExecutorFactory.class);
    }

    protected static MockNode addVariant(MockNode handle, String state) throws RepositoryException {
        MockNode variant = handle.addNode(handle.getName(), HippoStdPubWfNodeType.HIPPOSTDPUBWF_DOCUMENT);
        variant.setProperty(HippoStdNodeType.HIPPOSTD_STATE, state);
        if (state.equals("published")) {
            variant.setProperty(HIPPO_AVAILABILITY, new String[]{"live"});
        } else if (state.equals("unpublished")) {
            variant.setProperty(HIPPO_AVAILABILITY, new String[]{"preview"});
        }
        return variant;
    }

    protected static MockNode addRequest(MockNode handle, String type, boolean workflowRequest) throws RepositoryException {
        MockNode variant = handle.addNode(HippoStdPubWfNodeType.HIPPO_REQUEST,
                workflowRequest ? HippoStdPubWfNodeType.NT_HIPPOSTDPUBWF_REQUEST : HippoSchedJcrConstants.HIPPOSCHED_WORKFLOW_JOB);
        variant.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_TYPE, type);
        variant.addMixin(HippoNodeType.NT_REQUEST);
        return variant;
    }

    protected void assertMatchingKeyValues(Map<String, ?> actions, Map<String, ?> expected) {

        SoftAssertions.assertSoftly(softAssertions -> {
                    expected.forEach((key, value) ->
                            softAssertions
                                    .assertThat(actions.get(key))
                                    .as(key)
                                    .isEqualTo(value)
                    );
                    softAssertions.assertThat(actions.keySet())
                            .containsOnlyElementsOf(expected.keySet());

                }
        );
    }

}
