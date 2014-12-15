/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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

import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.impl.WorkflowLogger;

public class LogEventTask extends AbstractDocumentTask {

    private String action;

    public void setAction(final String action) {
        this.action = action;
    }

    @Override
    protected Object doExecute() throws WorkflowException, RepositoryException, RemoteException {
        final WorkflowContext context = getWorkflowContext();
        final WorkflowLogger logger = new WorkflowLogger(context.getInternalWorkflowSession());
        final Node handle = getDocumentHandle().getHandle();
        logger.logWorkflowStep(context.getUserIdentity(), null, action, null, null,
                handle.getIdentifier(), handle.getPath(), context.getInteraction(), context.getInteractionId(),
                null, null, null);
        return null;
    }

}
