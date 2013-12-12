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

import org.onehippo.repository.documentworkflow.task.ArchiveTask;
import org.onehippo.repository.scxml.AbstractTaskAction;

/**
 * ArchiveAction delegating the execution to ArchiveTask.
 */
public class ArchiveAction extends AbstractTaskAction<ArchiveTask> {

    private static final long serialVersionUID = 1L;

    @Override
    protected ArchiveTask createWorkflowTask() {
        return new ArchiveTask();
    }

    @Override
    protected void initTaskBeforeEvaluation(ArchiveTask task, Map<String, String> propertiesMap) {
        super.initTaskBeforeEvaluation(task, propertiesMap);
    }

    @Override
    protected void initTaskAfterEvaluation(ArchiveTask task, Map<String, Object> runtimePropertiesMap) {
        super.initTaskAfterEvaluation(task, runtimePropertiesMap);
    }
}
