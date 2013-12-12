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

import org.hippoecm.repository.api.Document;
import org.onehippo.repository.documentworkflow.task.MoveDocumentTask;
import org.onehippo.repository.scxml.AbstractTaskAction;

/**
 * MoveDocumentAction delegating the execution to MoveDocumentTask.
 */
public class MoveDocumentAction extends AbstractTaskAction<MoveDocumentTask> {

    private static final long serialVersionUID = 1L;

    public String getDestinationExpr() {
        return (String) getRuntimePropertiesMap().get("destination");
    }

    public void setDestinationExpr(String destinationExpr) {
        getRuntimePropertiesMap().put("destination", destinationExpr);
    }

    public String getNewNameExpr() {
        return (String) getRuntimePropertiesMap().get("newName");
    }

    public void setNewNameExpr(String newNameExpr) {
        getRuntimePropertiesMap().put("newName", newNameExpr);
    }

    @Override
    protected MoveDocumentTask createWorkflowTask() {
        return new MoveDocumentTask();
    }

    @Override
    protected void initTaskBeforeEvaluation(MoveDocumentTask task, Map<String, String> propertiesMap) {
        super.initTaskBeforeEvaluation(task, propertiesMap);
    }

    @Override
    protected void initTaskAfterEvaluation(MoveDocumentTask task, Map<String, Object> runtimePropertiesMap) {
        super.initTaskAfterEvaluation(task, runtimePropertiesMap);
        task.setDestination((Document) runtimePropertiesMap.get("destination"));
        task.setNewName((String) runtimePropertiesMap.get("newName"));
    }
}
