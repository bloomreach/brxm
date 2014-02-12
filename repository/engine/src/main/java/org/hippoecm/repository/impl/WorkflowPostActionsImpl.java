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

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class is not part of a public accessible API or extensible interface */
public class WorkflowPostActionsImpl implements WorkflowPostActions {

    static final Logger log = LoggerFactory.getLogger(WorkflowPostActions.class);

    static final String CATAGORYNAMEPREFIX = "events";

    private List<WorkflowPostActions> actions;
    private boolean isDocumentPathResult;
    private WorkflowManagerImpl workflowManager;
    
    private WorkflowPostActionsImpl(WorkflowManagerImpl workflowManager, List<WorkflowPostActions> actions, boolean isDocumentPathResult) {
        this.actions = actions;
        this.isDocumentPathResult = isDocumentPathResult;
        this.workflowManager = workflowManager;
    }

    static WorkflowPostActions createPostActions(WorkflowManagerImpl workflowManager, String workflowCategory, Method workflowMethod, String sourceIdentity) {
        if (WorkflowManagerImpl.log.isDebugEnabled()) {
            WorkflowManagerImpl.log.debug("inspect workflow for event workflow upon "+workflowCategory+":"+workflowMethod.toString());
        }
        if (workflowMethod.getName().equals("hints") || workflowCategory.startsWith(CATAGORYNAMEPREFIX) || workflowCategory.equals("internal")) {
            return null;
        }
        List<WorkflowPostActions> actions = new LinkedList<WorkflowPostActions>();
        boolean isDocumentResult = false;
        boolean isDocumentPathResult = false;
        if (Document.class.isAssignableFrom(workflowMethod.getReturnType())) {
            isDocumentResult = true;
        } else if (FolderWorkflow.class.isAssignableFrom(workflowMethod.getDeclaringClass()) && workflowMethod.getName().equals("add")) {
            isDocumentResult = true;
            isDocumentPathResult = true;
        }
        try {
            for (NodeIterator categories = workflowManager.rootSession.getNodeByIdentifier(workflowManager.configuration).getNodes(); categories.hasNext();) {
                Node category = categories.nextNode();
                if (category.getName().startsWith(CATAGORYNAMEPREFIX)) {
                    if (WorkflowManagerImpl.log.isDebugEnabled()) {
                        WorkflowManagerImpl.log.debug("inspect workflow for event workflow events in category "+category.getName());
                    }
                    Node wfSubject = workflowManager.rootSession.getNodeByIdentifier(sourceIdentity);
                    try {
                        WorkflowDefinition wfNode = workflowManager.getWorkflowDefinition(category.getName(), wfSubject);
                        if (wfNode != null) {
                            if (WorkflowManagerImpl.log.isDebugEnabled()) {
                                WorkflowManagerImpl.log.debug("inspect workflow for event workflow selected "+wfNode.getPath());
                            }
                            WorkflowPostActions action = null;
                            if (wfNode.isSimpleQueryPostAction()) {
                                action = new WorkflowPostActionSimpleQuery(workflowManager, wfSubject,
                                        isDocumentResult, wfNode,
                                        workflowCategory, workflowMethod.getName());
                            } else if (wfNode.isMethodBoundPostAction()) {
                                action = new WorkflowPostActionsBoundMethod(workflowManager, wfSubject,
                                        isDocumentResult, wfNode,
                                        workflowCategory, workflowMethod.getName());
                            }
                            if (action != null) {
                                actions.add(action);
                            }
                        }
                    } catch (RepositoryException ex) {
                        log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                    }
                }
            }
            return new WorkflowPostActionsImpl(workflowManager, actions, isDocumentPathResult);
        }  catch (RepositoryException ex) {
            log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
            return null;
        }
    }

    @Override
    public void execute(Object returnObject) {
        if (isDocumentPathResult && returnObject instanceof String) {
            try {
                returnObject = new Document(workflowManager.rootSession.getNode((String) returnObject));
            } catch (ItemNotFoundException ex) {
                log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                return;
            } catch (RepositoryException ex) {
                log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                return;
            }
        }
        for (WorkflowPostActions postAction : actions) {
            postAction.execute(returnObject);
        }
    }

    @Override
    public void dispose() {
        for (WorkflowPostActions postAction : actions) {
            postAction.dispose();
        }
    }
}
