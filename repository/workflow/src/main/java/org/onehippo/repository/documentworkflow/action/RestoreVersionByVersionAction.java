/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
import org.onehippo.repository.documentworkflow.task.RestoreVersionByVersionTask;

/**
 * Restores the supplied {@link javax.jcr.version.Version} to backing node of the supplied target
 *
 * @see RestoreVersionByVersionTask See RestoreVersionByVersionTask for details
 */
public class RestoreVersionByVersionAction extends AbstractDocumentTaskAction<RestoreVersionByVersionTask> {

    private static final long serialVersionUID = 1L;

    public String getVersion() {
        return getParameter("versionExpr");
    }

    @SuppressWarnings("unused")
    public void setVersion(String version) {
        setParameter("versionExpr", version);
    }

    public String getTarget() {
        return getParameter("targetExpr");
    }

    @SuppressWarnings("unused")
    public void setTarget(String target) {
        setParameter("targetExpr", target);
    }


    @Override
    protected RestoreVersionByVersionTask createWorkflowTask() {
        return new RestoreVersionByVersionTask();
    }

    @Override
    protected void initTask(RestoreVersionByVersionTask task) throws ModelException, SCXMLExpressionException {
        super.initTask(task);
        task.setVersion(eval(getVersion()));
        task.setTarget(eval(getTarget()));
    }
}
