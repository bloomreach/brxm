/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.repository.impl;

import java.rmi.RemoteException;
import java.util.Set;
import javax.jcr.InvalidItemStateException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.WorkflowEventWorkflow;
import org.hippoecm.repository.standardworkflow.WorkflowEventsWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class is not part of a public accessible API or extensible interface */
class WorkflowPostActionsBoundMethod implements WorkflowPostActions {

    static final Logger log = LoggerFactory.getLogger(WorkflowPostActionsBoundMethod.class);

    WorkflowManagerImpl workflowManager;
    String sourceIdentity;
    boolean isDocumentResult;
    Node wfSubject;
    WorkflowDefinition wfNode;
    Set<String> preconditionSet;
    String workflowCategory;
    String workflowMethod;

    WorkflowPostActionsBoundMethod(WorkflowManagerImpl workflowManager, Node wfSubject, boolean isDocumentResult, WorkflowDefinition wfNode, String workflowCategory, String workflowMethod) throws RepositoryException {
        this.workflowManager = workflowManager;
        this.sourceIdentity = wfSubject.getIdentifier();
        this.wfSubject = wfSubject;
        this.isDocumentResult = isDocumentResult;
        this.wfNode = wfNode;
        this.workflowCategory = workflowCategory;
        this.workflowMethod = workflowMethod;
        Node eventDocument = wfNode.getEventDocument();
        if (eventDocument != null) {
            this.wfSubject = eventDocument;
        }
    }

    public void execute(Object returnObject) {
        try {
            try {
                wfSubject.getPath();
            } catch (InvalidItemStateException ex) {
                /*
                 * Workflow was invoked on deleted subject, the simple query post action cannot be invoked on these kind of actions,
                 * and although configuring this can of action is useless, we will silently ignore any of such actions.
                 */
                log.debug("silently ignoring the workflow event on deleted item");
                return;
            }
            if (!wfNode.matchesEventCondition(workflowCategory, workflowMethod)) {
                return;
            }

            Workflow workflow = workflowManager.createProxiedWorkflow(wfNode, wfSubject);
            if (workflow instanceof WorkflowEventWorkflow) {
                WorkflowEventWorkflow event = (WorkflowEventWorkflow)workflow;
                if (event instanceof WorkflowEventsWorkflow) {
                    ((WorkflowEventsWorkflow)event).setWorkflowCategory(workflowCategory);
                    ((WorkflowEventsWorkflow)event).setWorkflowMethod(workflowMethod);
                }
                try {
                    if (isDocumentResult) {
                        event.fire((Document)returnObject);
                    } else {
                        event.fire();
                    }
                } catch (WorkflowException ex) {
                    log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                } catch (MappingException ex) {
                    log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                } catch (RemoteException ex) {
                    log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
        }
    }

    public void dispose() {
    }
}
