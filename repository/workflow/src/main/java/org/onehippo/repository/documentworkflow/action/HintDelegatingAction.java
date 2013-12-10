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

import java.util.Map;

import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.documentworkflow.task.HintWorkflowTask;
import org.onehippo.repository.scxml.AbstractWorkflowTaskDelegatingAction;

/**
 * HintDelegatingAction delegates the execution to HintWorkflowTask.
 * <P>
 * Note: All the setters must be redefined to delegate to the HintWorkflowTask.
 * </P>
 */
public class HintDelegatingAction extends AbstractWorkflowTaskDelegatingAction<HintWorkflowTask> {

    private static final long serialVersionUID = 1L;

    public String getHint() {
        return getWorkflowTask().getHint();
    }

    public void setHint(final String hint) {
        getWorkflowTask().setHint(hint);
    }

    public String getValue() {
        return (String) getProperties().get("value");
    }

    public void setValue(final String value) {
        getProperties().put("value", value);
    }

    @Override
    protected HintWorkflowTask createWorkflowTask() {
        return new HintWorkflowTask();
    }

    @Override
    protected void initTaskBeforeEvaluation(Map<String, Object> properties) {
        super.initTaskBeforeEvaluation(properties);
        DocumentHandle dm = getContextAttribute("dm");
        getWorkflowTask().setDataModel(dm);
    }

}
