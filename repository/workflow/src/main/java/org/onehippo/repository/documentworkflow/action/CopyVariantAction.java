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

import org.apache.commons.scxml2.SCXMLExpressionException;
import org.apache.commons.scxml2.model.ModelException;
import org.onehippo.repository.documentworkflow.task.CopyVariantTask;

/**
 * CopyVariantAction is a custom DocumentWorkflow SCXML state machine action for creating a new document variant based
 * on another document variant, or overriding/replacing the content of the target variant with that from the source
 * variant.
 * <p>
 * The execution of this task is delegated to its corresponding {@link CopyVariantTask}.
 * </p>
 */
public class CopyVariantAction extends AbstractDocumentTaskAction<CopyVariantTask> {

    private static final long serialVersionUID = 1L;

    public String getSourceState() {
        return getParameter("sourceState");
    }

    @SuppressWarnings("unused")
    public void setSourceState(String sourceState) {
        setParameter("sourceState", sourceState);
    }

    public String getTargetState() {
        return getParameter("targetState");
    }

    @SuppressWarnings("unused")
    public void setTargetState(String targetState) {
        setParameter("targetState", targetState);
    }

    @Override
    protected CopyVariantTask createWorkflowTask() {
        return new CopyVariantTask();
    }

    @Override
    protected void initTask(CopyVariantTask task) throws ModelException, SCXMLExpressionException {
        super.initTask(task);
        task.setSourceState(getSourceState());
        task.setTargetState(getTargetState());
    }
}
