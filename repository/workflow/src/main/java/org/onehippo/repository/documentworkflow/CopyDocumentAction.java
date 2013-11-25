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
import org.hippoecm.repository.reviewedactions.FullReviewedActionsWorkflow;
import org.hippoecm.repository.standardworkflow.EmbedWorkflow;
import org.hippoecm.repository.util.WorkflowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom action for copying document.
 */
public class CopyDocumentAction extends AbstractDocumentAction {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(CopyDocumentAction.class);

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

        String folderWorkflowCategory = "embedded";
        RepositoryMap config = getWorkflowContext(scInstance).getWorkflowConfiguration();

        if (config != null && config.exists() && config.get("folder-workflow-category") instanceof String) {
            folderWorkflowCategory = (String) config.get("folder-workflow-category");
        }

        try {
            if (handle.getUnpublished() == null) {
                Document folder = WorkflowUtils.getContainingFolder(handle.getPublished().getDocument());
                Workflow workflow = getWorkflowContext(null).getWorkflow(folderWorkflowCategory, destination);
    
                if (workflow instanceof EmbedWorkflow) {
                    Document copy = ((EmbedWorkflow) workflow).copyTo(folder, handle.getPublished().getDocument(), newName, null);
                    FullReviewedActionsWorkflow copiedDocumentWorkflow = (FullReviewedActionsWorkflow) getWorkflowContext(null).getWorkflow("default", copy);
                    copiedDocumentWorkflow.depublish();
                } else {
                    throw new ModelException("cannot copy document which is not contained in a folder");
                }
            } else {
                Document folder = WorkflowUtils.getContainingFolder(handle.getUnpublished().getDocument());
                Workflow workflow = getWorkflowContext(scInstance).getWorkflow(folderWorkflowCategory, destination);
    
                if (workflow instanceof EmbedWorkflow) {
                    ((EmbedWorkflow) workflow).copyTo(folder, handle.getUnpublished().getDocument(), newName, null);
                } else {
                    throw new ModelException("cannot copy document which is not contained in a folder");
                }
            }
        } catch (WorkflowException e) {
            throw new ModelException(e);
        } catch (RemoteException e) {
            throw new ModelException(e);
        }
    }

}
