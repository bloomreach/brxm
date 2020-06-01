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

import javax.jcr.Node;

import org.apache.commons.scxml2.SCXMLExpressionException;
import org.apache.commons.scxml2.model.ModelException;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.documentworkflow.task.CheckoutBranchTask;

/**
 * <p>
 *     For the supplied {@link #setVariant(String)}, checks out the branch supplied via {@link #setBranchId(String)},
 *     which has the result that the unpublished variant below the handle gets replaced if needed with a the right branch
 *     from version history. The {@link Node} backing the supplied {@link #setVariant(String)} is expected to be the unpublished
 *     variant below the handle
 *     For details about the stateLabel and forceReplace, see {@link CheckoutBranchTask}
 * </p>
 * @see CheckoutBranchTask See CheckoutBranchTask for details
 */
public class CheckoutBranchAction extends AbstractDocumentTaskAction<CheckoutBranchTask> {

    private static final long serialVersionUID = 1L;

    public String getVariant() {
        return getParameter("variantExpr");
    }

    @SuppressWarnings("unused")
    public void setVariant(final String variant) {
        setParameter("variantExpr", variant);
    }

    @SuppressWarnings("unused")
    public String getBranchId() {
        return getParameter("branchIdExpr");
    }

    @SuppressWarnings("unused")
    public void setBranchId(final String branchIdExpr) {
        setParameter("branchIdExpr", branchIdExpr);
    }

    @SuppressWarnings("unused")
    public String getStateLabel() {
        return getParameter("stateLabelExpr");
    }

    @SuppressWarnings("unused")
    public void setStateLabel(final String stateLabelExpr) {
        setParameter("stateLabelExpr", stateLabelExpr);
    }

    @SuppressWarnings("unused")
    public String getForceReplace() {
        return getParameter("forceReplaceExpr");
    }

    public void setForceReplace(final String forceReplace) {
        setParameter("forceReplaceExpr", forceReplace);
    }

    @Override
    protected CheckoutBranchTask createWorkflowTask() {
        return new CheckoutBranchTask();
    }

    @Override
    protected void initTask(CheckoutBranchTask task) throws ModelException, SCXMLExpressionException {
        super.initTask(task);
        task.setVariant(eval(getVariant()));
        final String branchIdArgument = eval(getBranchId());
        task.setBranchId(branchIdArgument == null ? ((DocumentHandle) getSCXMLWorkflowData()).getBranchId() : branchIdArgument);
        task.setStateLabel(eval(getStateLabel()));
        task.setForceReplace(eval(getForceReplace()));
    }

    @Override
    protected void processTaskResult(final Object taskResult) {
        // even when result is null, set the result!
        getSCXMLWorkflowContext().setResult(taskResult);
    }
}
