/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.quartz.HippoSchedJcrConstants;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.documentworkflow.action.ArchiveDocumentAction;
import org.onehippo.repository.documentworkflow.action.ConfigVariantAction;
import org.onehippo.repository.documentworkflow.action.CopyDocumentAction;
import org.onehippo.repository.documentworkflow.action.CopyVariantAction;
import org.onehippo.repository.documentworkflow.action.DeleteRequestAction;
import org.onehippo.repository.documentworkflow.action.IsModifiedAction;
import org.onehippo.repository.documentworkflow.action.ListVersionsVariantAction;
import org.onehippo.repository.scxml.LogEventAction;
import org.onehippo.repository.documentworkflow.action.MoveDocumentAction;
import org.onehippo.repository.documentworkflow.action.RejectRequestAction;
import org.onehippo.repository.documentworkflow.action.RenameDocumentAction;
import org.onehippo.repository.documentworkflow.action.RequestActionAction;
import org.onehippo.repository.documentworkflow.action.RestoreVersionAction;
import org.onehippo.repository.documentworkflow.action.RetrieveVersionAction;
import org.onehippo.repository.documentworkflow.action.ScheduleWorkflowAction;
import org.onehippo.repository.documentworkflow.action.SetHolderAction;
import org.onehippo.repository.documentworkflow.action.VersionRestoreToAction;
import org.onehippo.repository.documentworkflow.action.VersionVariantAction;
import org.onehippo.repository.documentworkflow.action.WorkflowRequestAction;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.scxml.ActionAction;
import org.onehippo.repository.scxml.FeedbackAction;
import org.onehippo.repository.scxml.MockRepositorySCXMLRegistry;
import org.onehippo.repository.scxml.RepositorySCXMLExecutorFactory;
import org.onehippo.repository.scxml.ResultAction;
import org.onehippo.repository.scxml.SCXMLExecutorFactory;
import org.onehippo.repository.scxml.SCXMLRegistry;
import org.onehippo.repository.scxml.WorkflowExceptionAction;

/**
 * Base class for DocumentWorkflow based tests
 */
public class BaseDocumentWorkflowTest {

    protected static String loadSCXML() throws Exception {
        return IOUtils.toString(DocumentWorkflowTest.class.getResourceAsStream("/documentworkflow.scxml"));
    }

    protected static void createDocumentWorkflowSCXMLRegistry() throws Exception {
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
        registry.addCustomAction(scxmlNode, "http://www.onehippo.org/cms7/repository/scxml", "logEvent", LogEventAction.class.getName());
        registry.setUp(scxmlConfigNode);

        HippoServiceRegistry.registerService(registry, SCXMLRegistry.class);
        HippoServiceRegistry.registerService(new RepositorySCXMLExecutorFactory(), SCXMLExecutorFactory.class);
    }

    protected static void destroyDocumentWorkflowSCXMLRegistry() throws Exception {
        HippoServiceRegistry.unregisterService(HippoServiceRegistry.getService(SCXMLExecutorFactory.class), SCXMLExecutorFactory.class);
        HippoServiceRegistry.unregisterService(HippoServiceRegistry.getService(SCXMLRegistry.class), SCXMLRegistry.class);
    }

    protected static MockNode addVariant(MockNode handle, String state) throws RepositoryException {
        MockNode variant = handle.addNode(handle.getName(), HippoStdPubWfNodeType.HIPPOSTDPUBWF_DOCUMENT);
        variant.setProperty(HippoStdNodeType.HIPPOSTD_STATE, state);
        return variant;
    }

    protected static MockNode addRequest(MockNode handle, String type, boolean workflowRequest) throws RepositoryException {
        MockNode variant = handle.addNode(HippoStdPubWfNodeType.HIPPO_REQUEST,
                workflowRequest ? HippoStdPubWfNodeType.NT_HIPPOSTDPUBWF_REQUEST : HippoSchedJcrConstants.HIPPOSCHED_WORKFLOW_JOB);
        variant.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_TYPE, type);
        variant.addMixin(HippoNodeType.NT_REQUEST);
        return variant;
    }
}
