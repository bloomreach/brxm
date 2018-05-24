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
import org.onehippo.repository.documentworkflow.task.LabelTask;

/**
 *
 * </p>
 */
public class LabelAction extends AbstractDocumentTaskAction<LabelTask> {

    private static final long serialVersionUID = 1L;

    public String getVariant() {
        return getParameter("variantExpr");
    }

    @SuppressWarnings("unused")
    public void setVariant(String variant) {
        setParameter("variantExpr", variant);
    }

    @SuppressWarnings("unused")
    public String getAddLabel() {
        return getParameter("addLabelExpr");
    }

    @SuppressWarnings("unused")
    public void setAddLabel(String addLabel) {
        setParameter("addLabelExpr", addLabel);
    }

    @SuppressWarnings("unused")
    public String getOnLabel() {
        return getParameter("onLabelExpr");
    }

    @SuppressWarnings("unused")
    public void setOnLabel(String onLabel) {
        setParameter("onLabelExpr", onLabel);
    }

    @SuppressWarnings("unused")
    public String getRemoveLabel() {
        return getParameter("removeLabelExpr");
    }

    @SuppressWarnings("unused")
    public void setRemoveLabel(String onLabel) {
        setParameter("removeLabelExpr", onLabel);
    }

    @Override
    protected LabelTask createWorkflowTask() {
        return new LabelTask();
    }

    @Override
    protected void initTask(LabelTask task) throws ModelException, SCXMLExpressionException {
        super.initTask(task);
        task.setVariant(eval(getVariant()));
        task.setAddLabel(eval(getAddLabel()));
        task.setOnLabel(eval(getOnLabel()));
        task.setRemoveLabel(eval(getRemoveLabel()));
    }
}
