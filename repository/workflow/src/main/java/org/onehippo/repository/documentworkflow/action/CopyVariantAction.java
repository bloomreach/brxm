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

import org.apache.commons.lang.BooleanUtils;
import org.onehippo.repository.documentworkflow.task.CopyVariantTask;
import org.onehippo.repository.scxml.AbstractTaskAction;

/**
 * CopyVariantAction delegating the execution to CopyVariantTask.
 */
public class CopyVariantAction extends AbstractTaskAction<CopyVariantTask> {

    private static final long serialVersionUID = 1L;

    public String getSourceState() {
        return getPropertiesMap().get("sourceState");
    }

    public void setSourceState(String sourceState) {
        getPropertiesMap().put("sourceState", sourceState);
    }

    public String getTargetState() {
        return getPropertiesMap().get("targetState");
    }

    public void setTargetState(String targetState) {
        getPropertiesMap().put("targetState", targetState);
    }

    public String getAvailabilities() {
        return getPropertiesMap().get("availabilities");
    }

    public void setAvailabilities(String availabilities) {
        getPropertiesMap().put("availabilities", availabilities);
    }

    public boolean isApplyModified() {
        return BooleanUtils.toBoolean(getPropertiesMap().get("applyModified"));
    }

    public void setApplyModified(String applyModified) {
        getPropertiesMap().put("applyModified", applyModified);
    }

    public boolean isSkipIndex() {
        return BooleanUtils.toBoolean(getPropertiesMap().get("skipIndex"));
    }

    public void setSkipIndex(String skipIndex) {
        getPropertiesMap().put("skipIndex", skipIndex);
    }

    public boolean isVersionable() {
        return BooleanUtils.toBoolean(getPropertiesMap().get("versionable"));
    }

    public void setVersionable(String versionable) {
        getPropertiesMap().put("versionable", versionable);
    }

    @Override
    protected CopyVariantTask createWorkflowTask() {
        return new CopyVariantTask();
    }

    @Override
    protected void initTaskBeforeEvaluation(CopyVariantTask task, Map<String, String> propertiesMap) {
        super.initTaskBeforeEvaluation(task, propertiesMap);
        task.setSourceState((String) propertiesMap.get("sourceState"));
        task.setTargetState((String) propertiesMap.get("targetState"));
        task.setAvailabilities((String) propertiesMap.get("availabilities"));
        task.setApplyModified(BooleanUtils.toBoolean(propertiesMap.get("applyModified")));
        task.setSkipIndex(BooleanUtils.toBoolean(propertiesMap.get("skipIndex")));
        task.setVersionable(BooleanUtils.toBoolean(propertiesMap.get("versionable")));
    }

    @Override
    protected void initTaskAfterEvaluation(CopyVariantTask task, Map<String, Object> runtimePropertiesMap) {
        super.initTaskAfterEvaluation(task, runtimePropertiesMap);
    }
}
