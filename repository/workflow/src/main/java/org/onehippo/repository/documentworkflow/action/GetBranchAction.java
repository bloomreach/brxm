/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.repository.documentworkflow.action;

import org.apache.commons.scxml2.SCXMLExpressionException;
import org.apache.commons.scxml2.model.ModelException;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.documentworkflow.DocumentVariant;
import org.onehippo.repository.documentworkflow.task.GetBranchTask;

/**
 * <p>
 *     Returns a {@link DocumentVariant} for the {@link DocumentHandle#getBranchId()}.
 *     The {@link javax.jcr.Node} backing the {@link DocumentVariant} can be a
 *     frozen node. This {@link GetBranchAction} is idempotent and does not change any state
 * </p>
 *
 */
public class GetBranchAction extends AbstractDocumentTaskAction<GetBranchTask> {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    public String getState() {
        return getParameter("stateExpr");
    }

    @SuppressWarnings("unused")
    public void setState(String stateExpr) {
        setParameter("stateExpr", stateExpr);
    }


    public String getUnpublished() {
        return getParameter("unpublishedExpr");
    }

    @SuppressWarnings("unused")
    public void setUnpublished(String unpublished) {
        setParameter("unpublishedExpr", unpublished);
    }

    public String getPublished() {
        return getParameter("publishedExpr");
    }

    @SuppressWarnings("unused")
    public void setPublished(String published) {
        setParameter("publishedExpr", published);
    }

    public String getDraft() {
        return getParameter("draftExpr");
    }

    @SuppressWarnings("unused")
    public void setDraft(String draft) {
        setParameter("draftExpr", draft);
    }

    @Override
    protected GetBranchTask createWorkflowTask() {
        return new GetBranchTask();
    }

    @Override
    protected void initTask(GetBranchTask task) throws ModelException, SCXMLExpressionException {
        super.initTask(task);
        task.setBranchId(((DocumentHandle) getSCXMLWorkflowData()).getBranchId());
        task.setState(eval(getState()));
        task.setUnpublished(eval(getUnpublished()));
        task.setPublished(eval(getPublished()));
        task.setDraft(eval(getDraft()));
    }

    protected void processTaskResult(Object taskResult) {
        // even if the taskResult is null set it because otherwise the previous task result is kept
        getSCXMLWorkflowContext().setResult(taskResult);
    }
}
