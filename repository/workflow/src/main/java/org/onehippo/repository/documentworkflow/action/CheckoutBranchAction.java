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
import org.onehippo.repository.documentworkflow.task.CheckoutBranchTask;

/**
 *
 * </p>
 */
public class CheckoutBranchAction extends AbstractDocumentTaskAction<CheckoutBranchTask> {

    private static final long serialVersionUID = 1L;

    public String getVariant() {
        return getParameter("variantExpr");
    }

    @SuppressWarnings("unused")
    public void setVariant(String variant) {
        setParameter("variantExpr", variant);
    }

    @SuppressWarnings("unused")
    public String getBranchId() {
        return getParameter("branchIdExpr");
    }

    @SuppressWarnings("unused")
    public void setBranchId(String branchIdExpr) {
        setParameter("branchIdExpr", branchIdExpr);
    }

    @Override
    protected CheckoutBranchTask createWorkflowTask() {
        return new CheckoutBranchTask();
    }

    @Override
    protected void initTask(CheckoutBranchTask task) throws ModelException, SCXMLExpressionException {
        super.initTask(task);
        task.setVariant(eval(getVariant()));
        task.setBranchId(eval(getBranchId()));
    }
}
