/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.onehippo.repository.documentworkflow.task;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.documentworkflow.DocumentVariant;

/**
 * Custom workflow task which sets or removes the draft document retainable status.
 *
 */
public class SetRetainableTask extends AbstractDocumentTask {

    private Boolean retainable;

    public Boolean getRetainable() {
        return retainable;
    }

    public void setRetainable(final Boolean retainable) {
        this.retainable = retainable;
    }

    @Override
    public Object doExecute() throws WorkflowException, RepositoryException {

        DocumentHandle dm = getDocumentHandle();

        DocumentVariant draft = dm.getDocuments().get(HippoStdNodeType.DRAFT);
        if (draft != null) {
            draft.setRetainable(retainable);
        } else {
            throw new WorkflowException("Draft document not available");
        }
        return null;
    }
}
