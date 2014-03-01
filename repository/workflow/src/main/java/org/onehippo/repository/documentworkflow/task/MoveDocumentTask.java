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

import static org.hippoecm.repository.util.WorkflowUtils.getContainingFolder;

import java.rmi.RemoteException;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.RepositoryMap;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.documentworkflow.DocumentVariant;

/**
 * Custom workflow task for moving document.
 */
public class MoveDocumentTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;

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

        DocumentVariant document = dm.getDocuments().get(HippoStdNodeType.UNPUBLISHED);

        if (document == null) {
            document = dm.getDocuments().get(HippoStdNodeType.PUBLISHED);
        }

        if (document == null) {
            document = dm.getDocuments().get(HippoStdNodeType.DRAFT);
        }

        if (document == null) {
            throw new WorkflowException("No source document found.");
        }

        Document folder = getContainingFolder(document, getWorkflowContext().getInternalWorkflowSession());
        String folderWorkflowCategory = "internal";
        RepositoryMap config = getWorkflowContext().getWorkflowConfiguration();

        if (config != null && config.exists() && config.get("folder-workflow-category") instanceof String) {
            folderWorkflowCategory = (String) config.get("folder-workflow-category");
        }

        Workflow workflow = getWorkflowContext().getWorkflow(folderWorkflowCategory, folder);

        if (workflow instanceof FolderWorkflow) {
            ((FolderWorkflow) workflow).move(document, destination, newName);
        } else {
            throw new WorkflowException("cannot move document which is not contained in a folder");
        }

        return null;
    }

}
