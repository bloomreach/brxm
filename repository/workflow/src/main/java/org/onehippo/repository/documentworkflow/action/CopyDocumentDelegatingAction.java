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

import org.onehippo.repository.documentworkflow.task.CopyDocumentWorkflowTask;
import org.onehippo.repository.scxml.AbstractWorkflowTaskDelegatingAction;

/**
 * CopyDocumentDelegatingAction delegating the execution to CopyDocumentWorkflowTask.
 * <P>
 * Note: All the setters must be redefined to delegate to the CopyDocumentWorkflowTask.
 * </P>
 */
public class CopyDocumentDelegatingAction extends AbstractWorkflowTaskDelegatingAction<CopyDocumentWorkflowTask> {

    private static final long serialVersionUID = 1L;

    public String getDestinationExpr() {
        return getWorkflowTask().getDestinationExpr();
    }

    public void setDestinationExpr(String destinationExpr) {
        getWorkflowTask().setDestinationExpr(destinationExpr);
    }

    public String getNewNameExpr() {
        return getWorkflowTask().getNewNameExpr();
    }

    public void setNewNameExpr(String newNameExpr) {
        getWorkflowTask().setNewNameExpr(newNameExpr);
    }

    @Override
    protected CopyDocumentWorkflowTask createWorkflowTask() {
        return new CopyDocumentWorkflowTask();
    }

}
