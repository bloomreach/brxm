/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.handleworkflow;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.RepositoryMap;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.documentworkflow.HandleDocumentWorkflow;
import org.onehippo.repository.scxml.SCXMLWorkflowExecutor;

public class HandleDocumentWorkflowImpl extends WorkflowImpl implements HandleDocumentWorkflow {

    private static final long serialVersionUID = 1L;

    public static final String SCXML_DEFINITION_KEY = "scxml-definition";

    private SCXMLWorkflowExecutor workflowExecutor;
    private DocumentHandle dm;

    public HandleDocumentWorkflowImpl() throws RemoteException {
    }

    SCXMLWorkflowExecutor getWorkflowExecutor() {
        return workflowExecutor;
    }

    // Workflow implementation / WorkflowImpl override

    @Override
    public void setNode(final Node node) throws RepositoryException {
        super.setNode(node);

        // Critical: MUST use getNonChainingWorkflowContext() or getWorkflowContext(null), NOT getWorkflowContext()!
        dm = new DocumentHandle(getNonChainingWorkflowContext(), node);

        try {
            String definition = "document-workflow";
            final RepositoryMap workflowConfiguration = getWorkflowContext().getWorkflowConfiguration();
            if (workflowConfiguration != null && workflowConfiguration.exists() && workflowConfiguration.get(SCXML_DEFINITION_KEY) instanceof String) {
                definition = (String) workflowConfiguration.get(SCXML_DEFINITION_KEY);
            }

            workflowExecutor = new SCXMLWorkflowExecutor(definition, dm);
            workflowExecutor.start();
        }
        catch (WorkflowException wfe) {
            if (wfe.getCause() != null && wfe.getCause() instanceof RepositoryException) {
                throw (RepositoryException)wfe.getCause();
            }
            throw new RepositoryException(wfe);
        }
    }

    @Override
    public Map<String, Serializable> hints() {
        Map<String, Serializable> hints = super.hints();
        if (workflowExecutor.isStarted()) {
            hints.putAll(dm.getInfo());
            hints.putAll(dm.getActions());
        }
        return hints;
    }

    @Override
    public Map<String, Serializable> getInfo() {
        if (workflowExecutor.isStarted()) {
            return dm.getInfo();
        } else {
            return Collections.emptyMap();
        }
    }

    @Override
    public Map<String, Boolean> getActions() {
        if (workflowExecutor.isStarted()) {
            return dm.getActions();
        } else {
            return Collections.emptyMap();
        }
    }

    @Override
    public Object triggerAction(final String action) throws WorkflowException {
        return workflowExecutor.triggerAction(action);
    }

    @Override
    public Object triggerAction(final String action, final Object payload) throws WorkflowException {
        return workflowExecutor.triggerAction(action, payload);
    }

}
