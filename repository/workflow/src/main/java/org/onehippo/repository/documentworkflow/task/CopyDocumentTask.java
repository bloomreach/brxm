/**
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
package org.onehippo.repository.documentworkflow.task;

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.RepositoryMap;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.EmbedWorkflow;
import org.hippoecm.repository.util.WorkflowUtils;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.documentworkflow.DocumentVariant;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom workflow task for copying document.
 */
public class CopyDocumentTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(CopyDocumentTask.class);

    private Document destination;
    private String newName;

    public Document getDestination() {
        return destination;
    }

    public void setDestination(Document destination) {
        this.destination = destination;
    }

    public String getNewName() {
        return newName;
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }

    @Override
    public Object doExecute() throws WorkflowException, RepositoryException, RemoteException {
        if (destination == null) {
            throw new WorkflowException("Destination is null.");
        }

        if (StringUtils.isBlank(newName)) {
            throw new WorkflowException("New document name is blank.");
        }

        DocumentHandle dm = getDocumentHandle();

        String folderWorkflowCategory = "embedded";
        RepositoryMap config = getWorkflowContext().getWorkflowConfiguration();

        if (config != null && config.exists() && config.get("folder-workflow-category") instanceof String) {
            folderWorkflowCategory = (String) config.get("folder-workflow-category");
        }

        DocumentVariant unpublished = dm.getDocuments().get(HippoStdNodeType.UNPUBLISHED);

        if (unpublished == null) {
            DocumentVariant published = dm.getDocuments().get(HippoStdNodeType.PUBLISHED);
            Document folder = WorkflowUtils.getContainingFolder(published, getWorkflowContext().getInternalWorkflowSession());
            Workflow workflow = getWorkflowContext().getWorkflow(folderWorkflowCategory, destination);

            if (workflow instanceof EmbedWorkflow) {
                Document copy = ((EmbedWorkflow) workflow).copyTo(folder, published, newName, null);
                Node copyHandle = copy.getNode(getWorkflowContext().getInternalWorkflowSession()).getParent();
                DocumentWorkflow copiedDocumentWorkflow = (DocumentWorkflow) getWorkflowContext().getWorkflow("default", new Document(copyHandle));
                copiedDocumentWorkflow.depublish();
            } else {
                throw new WorkflowException("cannot copy document which is not contained in a folder");
            }
        } else {
            Document folder = WorkflowUtils.getContainingFolder(unpublished, getWorkflowContext().getInternalWorkflowSession());
            Workflow workflow = getWorkflowContext().getWorkflow(folderWorkflowCategory, destination);

            if (workflow instanceof EmbedWorkflow) {
                ((EmbedWorkflow) workflow).copyTo(folder, unpublished, newName, null);
            } else {
                throw new WorkflowException("cannot copy document which is not contained in a folder");
            }
        }

        return null;
    }

}
