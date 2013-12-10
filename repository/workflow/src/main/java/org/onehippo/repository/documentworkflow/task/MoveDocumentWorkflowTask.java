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
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.RepositoryMap;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.documentworkflow.PublishableDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom workflow task for moving document.
 */
public class MoveDocumentWorkflowTask extends AbstractDocumentWorkflowTask {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(MoveDocumentWorkflowTask.class);

    private String destinationExpr;
    private String newNameExpr;

    public String getDestinationExpr() {
        return destinationExpr;
    }

    public void setDestinationExpr(String destinationExpr) {
        this.destinationExpr = destinationExpr;
    }

    public String getNewNameExpr() {
        return newNameExpr;
    }

    public void setNewNameExpr(String newNameExpr) {
        this.newNameExpr = newNameExpr;
    }

    @Override
    public void doExecute() throws WorkflowException, RepositoryException, RemoteException {

        Document destination = null;
        String newName = null;

        if (getDestinationExpr() != null) {
            destination = eval(getDestinationExpr());
        }

        if (getNewNameExpr() != null) {
            newName = eval(getNewNameExpr());
        }

        if (destination == null) {
            throw new WorkflowException("Destination is null.");
        }

        if (StringUtils.isBlank(newName)) {
            throw new WorkflowException("New document name is blank.");
        }

        DocumentHandle dm = getDataModel();

        PublishableDocument document = null;

        if (dm.getUnpublished() != null) {
            document = dm.getUnpublished();
        }

        if (document == null) {
            if (dm.getPublished() != null) {
                document = dm.getPublished();
            }
        }

        if (document == null) {
            if (dm.getDraft() != null) {
                document = dm.getDraft();
            }
        }

        if (document == null) {
            throw new WorkflowException("No source document found.");
        }

        Document folder = getContainingFolder(document);
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
    }

}
