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
package org.onehippo.repository.documentworkflow;

import static org.hippoecm.repository.util.WorkflowUtils.getContainingFolder;

import java.rmi.RemoteException;
import java.util.Collection;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.scxml2.ErrorReporter;
import org.apache.commons.scxml2.EventDispatcher;
import org.apache.commons.scxml2.SCInstance;
import org.apache.commons.scxml2.SCXMLExpressionException;
import org.apache.commons.scxml2.TriggerEvent;
import org.apache.commons.scxml2.model.ModelException;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.RepositoryMap;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom action for moving document.
 */
public class MoveDocumentAction extends AbstractDocumentAction {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(MoveDocumentAction.class);

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
    protected void doExecute(EventDispatcher evtDispatcher, ErrorReporter errRep, SCInstance scInstance, Log appLog,
            Collection<TriggerEvent> derivedEvents) throws ModelException, SCXMLExpressionException,
            RepositoryException {

        Document destination = null;
        String newName = null;

        if (getDestinationExpr() != null) {
            destination = eval(scInstance, getDestinationExpr());
        }

        if (getNewNameExpr() != null) {
            newName = eval(scInstance, getNewNameExpr());
        }

        if (destination == null) {
            throw new ModelException("Destination is null.");
        }

        if (StringUtils.isBlank(newName)) {
            throw new ModelException("New document name is blank.");
        }

        DocumentHandle handle = getDocumentHandle(scInstance);

        PublishableDocument document = null;

        if (handle.getUnpublished() != null) {
            document = handle.getUnpublished();
        }

        if (document == null) {
            if (handle.getPublished() != null) {
                document = handle.getPublished();
            }
        }

        if (document == null) {
            if (handle.getDraft() != null) {
                document = handle.getDraft();
            }
        }

        if (document == null) {
            throw new ModelException("No source document found.");
        }

        try {
            Document folder = getContainingFolder(document);
            String folderWorkflowCategory = "internal";
            RepositoryMap config = getWorkflowContext(scInstance).getWorkflowConfiguration();
    
            if (config != null && config.exists() && config.get("folder-workflow-category") instanceof String) {
                folderWorkflowCategory = (String) config.get("folder-workflow-category");
            }
    
            Workflow workflow = getWorkflowContext(scInstance).getWorkflow(folderWorkflowCategory, folder);
    
            if (workflow instanceof FolderWorkflow) {
                ((FolderWorkflow) workflow).move(document, destination, newName);
            } else {
                throw new WorkflowException("cannot move document which is not contained in a folder");
            }
        } catch (WorkflowException e) {
            throw new ModelException(e);
        } catch (RemoteException e) {
            throw new ModelException(e);
        }
    }

}
