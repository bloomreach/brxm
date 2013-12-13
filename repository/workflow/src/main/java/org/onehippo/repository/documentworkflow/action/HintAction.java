/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

import java.io.Serializable;

import org.apache.commons.scxml2.SCXMLExpressionException;
import org.apache.commons.scxml2.model.ModelException;
import org.onehippo.repository.documentworkflow.task.HintTask;

/**
 * HintAction delegates the execution to HintTask.
 */
public class HintAction extends AbstractDocumentTaskAction<HintTask> {

    private static final long serialVersionUID = 1L;

    public String getHint() {
        return getParameter("hint");
    }

    public void setHint(final String hint) {
        setParameter("hint", hint);
    }

    public String getValue() {
        return getParameter("valueExpr");
    }

    public void setValue(final String value) {
        setParameter("valueExpr", value);
    }

    @Override
    protected HintTask createWorkflowTask() {
        return new HintTask();
    }

    @Override
    protected void initTask(HintTask task) throws ModelException, SCXMLExpressionException {
        super.initTask(task);
        task.setHint(getHint());
        task.setValue((Serializable)eval(getValue()));
    }
}
