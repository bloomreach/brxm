/*
 * Copyright 2021 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Date;

import org.apache.commons.scxml2.SCXMLExpressionException;
import org.apache.commons.scxml2.model.ModelException;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.documentworkflow.task.CampaignTask;

public class CampaignAction extends AbstractDocumentTaskAction<CampaignTask> {

    private static final long serialVersionUID = 1L;

    public String getFrozenNodeId() {
        return getParameter("frozenNodeIdExpr");
    }

    @SuppressWarnings("unused")
    public void setFrozenNodeId(String frozenNodeId) {
        setParameter("frozenNodeIdExpr", frozenNodeId);
    }

    public String getFrom() {
        return getParameter("fromExpr");
    }

    @SuppressWarnings("unused")
    public void setFrom(String from) {
        setParameter("fromExpr", from);
    }


    public String getTo() {
        return getParameter("toExpr");
    }

    @SuppressWarnings("unused")
    public void setTo(String to) {
        setParameter("toExpr", to);
    }

    @Override
    protected CampaignTask createWorkflowTask() {
        return new CampaignTask();
    }

    @Override
    protected void initTask(CampaignTask task) throws ModelException, SCXMLExpressionException {
        super.initTask(task);
        task.setBranchId(((DocumentHandle) getSCXMLWorkflowData()).getBranchId());
        task.setFrozenNodeId(eval(getFrozenNodeId()));
        task.setFrom(eval(getFrom()));
        task.setTo(eval(getTo()));
    }
}
