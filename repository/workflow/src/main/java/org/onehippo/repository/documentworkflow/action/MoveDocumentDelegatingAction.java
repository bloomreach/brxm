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

import org.onehippo.repository.documentworkflow.task.MoveDocumentWorkflowTask;
import org.onehippo.repository.scxml.AbstractWorkflowTaskDelegatingAction;

/**
 * MoveDocumentDelegatingAction delegating the execution to MoveDocumentWorkflowTask.
 * <P>
 * Note: All the setters must be redefined to delegate to the MoveDocumentWorkflowTask.
 * </P>
 */
public class MoveDocumentDelegatingAction extends AbstractWorkflowTaskDelegatingAction<MoveDocumentWorkflowTask> {

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
    protected MoveDocumentWorkflowTask createWorkflowTask() {
        return new MoveDocumentWorkflowTask();
    }

}
