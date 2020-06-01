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
import org.onehippo.repository.documentworkflow.task.SetPreReintegrationLabelsTask;

/**
 * <p>
 *     Sets pre-reintegration-labels in version history
 * </p>
 * @see SetPreReintegrationLabelsTask see SetPreReintegrationLabelsTask for more info
 */
public class SetPreReintegrationLabelsAction extends AbstractDocumentTaskAction<SetPreReintegrationLabelsTask> {

    private static final long serialVersionUID = 1L;

    public String getUnpublished() {
        return getParameter("unpublishedExpr");
    }

    @SuppressWarnings("unused")
    public void setUnpublished(String unpublished) {
        setParameter("unpublishedExpr", unpublished);
    }

    @Override
    protected SetPreReintegrationLabelsTask createWorkflowTask() {
        return new SetPreReintegrationLabelsTask();
    }

    @Override
    protected void initTask(SetPreReintegrationLabelsTask task) throws ModelException, SCXMLExpressionException {
        super.initTask(task);
        task.setUnpublished(eval(getUnpublished()));
    }
}
