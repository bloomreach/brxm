/**
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
package org.onehippo.repository.test.scxml;

import org.apache.commons.scxml2.Context;
import org.apache.commons.scxml2.Evaluator;
import org.apache.commons.scxml2.SCXMLExpressionException;
import org.apache.commons.scxml2.env.jexl.JexlContext;
import org.apache.commons.scxml2.env.jexl.JexlEvaluator;
import org.apache.commons.scxml2.model.ModelException;
import org.onehippo.repository.api.WorkflowTask;
import org.onehippo.repository.scxml.AbstractTaskAction;

/**
 * MockAbstractWorkflowTaskDelegatingAction
 */
public class MockAbstractWorkflowTaskDelegatingAction<T extends WorkflowTask> extends AbstractTaskAction<WorkflowTask> {

    private static final long serialVersionUID = 1L;

    private Context context = new JexlContext();
    private Evaluator evaluator = new JexlEvaluator();

    public MockAbstractWorkflowTaskDelegatingAction() {
    }

    public void setWorkflowTask(WorkflowTask workflowTask) {
        super.setWorkflowTask(workflowTask);
    }

    @Override
    protected WorkflowTask createWorkflowTask() {
        return null;
    }

    public <T> T getContextAttribute(String name) {
        return (T) context.get(name);
    }

    public void setContextAttribute(String name, Object value) {
        context.set(name, value);
    }

    public <T> T eval(String expr) throws ModelException, SCXMLExpressionException {
        return (T) evaluator.eval(context, expr);
    }
}
