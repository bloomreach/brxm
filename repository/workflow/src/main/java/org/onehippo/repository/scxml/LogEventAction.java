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
package org.onehippo.repository.scxml;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.scxml2.ActionExecutionContext;
import org.apache.commons.scxml2.SCXMLExpressionException;
import org.apache.commons.scxml2.model.ModelException;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.impl.WorkflowLogger;

public class LogEventAction extends AbstractAction {

    public void setActionexpr(final String actionexpr) {
        setParameter("actionexpr", actionexpr);
    }

    public String getActionexpr() {
        return getParameter("actionexpr");
    }

    @Override
    protected void doExecute(final ActionExecutionContext exctx) throws ModelException, SCXMLExpressionException,
            WorkflowException, RepositoryException {
        final WorkflowContext context = this.getSCXMLWorkflowContext().getWorkflowContext();
        final WorkflowLogger logger = new WorkflowLogger(context.getInternalWorkflowSession());
        final Node subject = context.getSubject();
        logger.logWorkflowStep(context.getUserIdentity(), null, eval(getActionexpr()), null, null,
                subject.getIdentifier(), subject.getPath(), context.getInteraction(), context.getInteractionId(),
                null, null, null);
    }
}
