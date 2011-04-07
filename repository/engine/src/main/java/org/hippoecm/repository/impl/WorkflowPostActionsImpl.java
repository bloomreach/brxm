/*
 * Copyright 2011 Hippo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.impl;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import org.hippoecm.repository.api.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowPostActionsImpl implements WorkflowPostActions {
    final static Logger log = LoggerFactory.getLogger(WorkflowPostAction.class);

    final static String CATAGORYNAMEPREFIX = "events";

    private List<WorkflowPostActions> actions;
    
    private WorkflowPostActionsImpl(List<WorkflowPostActions> actions) {
        this.actions = actions;
    }

    static WorkflowPostActions createPostActions(WorkflowManagerImpl workflowManager, String workflowCategory, Method workflowMethod, String sourceIdentity) {
	//if (WorkflowManagerImpl.log.isDebugEnabled()) {
        System.err.println("inspect workflow for event workflow upon "+workflowCategory+":"+workflowMethod.toString());
        if (workflowMethod.getName().equals("hints") || workflowCategory.startsWith(CATAGORYNAMEPREFIX) || workflowCategory.equals("internal")) {
            return null;
        }
        List<WorkflowPostActions> actions = new LinkedList<WorkflowPostActions>();
        try {
            for (NodeIterator categories = workflowManager.rootSession.getNodeByIdentifier(workflowManager.configuration).getNodes(); categories.hasNext();) {
                Node category = categories.nextNode();
                if (category.getName().startsWith(CATAGORYNAMEPREFIX)) {
                    System.err.println("inspect workflow for event workflow events in category "+category.getName());
                    Node wfSubject = workflowManager.rootSession.getNodeByIdentifier(sourceIdentity);
                    try {
                        Node wfNode = workflowManager.getWorkflowNode(category.getName(), wfSubject, workflowManager.rootSession);
                        if (wfNode != null) {
                            System.err.println("inspect workflow for event workflow selected "+wfNode.getPath());
                            WorkflowPostActions action = new WorkflowPostAction(workflowManager, wfSubject,
                                    Document.class.isAssignableFrom(workflowMethod.getReturnType()), wfNode,
                                    workflowCategory, workflowMethod.getName());
                            actions.add(action);
                        }
                    } catch (RepositoryException ex) {
                        log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                    }
                }
            }
            return new WorkflowPostActionsImpl(actions);
        }  catch (RepositoryException ex) {
            log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
            return null;
        }
    }

    @Override
    public void execute(Object returnObject) {
        for(WorkflowPostActions postAction : actions) {
            postAction.execute(returnObject);
        }
    }

    @Override
    public void dispose() {
        for(WorkflowPostActions postAction : actions) {
            postAction.dispose();
        }
    }
}
