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

import org.onehippo.repository.documentworkflow.task.IsModifiedTask;

/**
 * IsModifiedAction is a custom DocumentWorkflow SCXML state machine action which compares the current document draft
 * variant with the unpublished variant and stores the boolean result under the "modified" key in the
 * {@link org.onehippo.repository.scxml.SCXMLWorkflowContext#getFeedback() feedback} map.
 * <p>
 * If either the draft or unpublished variant does not exists, the "modified" key is removed from the feedback map.
 * </p>
 * <p>
 * The execution of the task to compare the two variants is delegated to the corresponding {@link IsModifiedTask}.
 * </p>
 */
public class IsModifiedAction extends AbstractDocumentTaskAction<IsModifiedTask> {

    private static final long serialVersionUID = 1L;

    @Override
    protected IsModifiedTask createWorkflowTask() {
        return new IsModifiedTask();
    }

    protected void processTaskResult(Object taskResult) {
        if (taskResult != null) {
            getSCXMLWorkflowContext().getFeedback().put("modified", (Serializable)taskResult);
        }
        else {
            getSCXMLWorkflowContext().getFeedback().remove("modified");
        }
    }
}
