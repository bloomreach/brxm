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

package org.onehippo.repository.documentworkflow.task;

import java.io.Serializable;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.documentworkflow.DocumentHandle;

/**
 * HintWorkflowTask sets or removes a DocumentHandle (dm context variable) hints key
 */
public class HintWorkflowTask extends AbstractDocumentWorkflowTask {

    private static final long serialVersionUID = 1L;

    private String hint;
    private String value;

    public String getHint() {
        return hint;
    }

    public void setHint(final String hint) {
        this.hint = hint;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    @Override
    public void execute() throws WorkflowException {
        if (StringUtils.isBlank(getHint())) {
            throw new WorkflowException("No hint specified");
        }

        Serializable attrValue = null;

        if (getValue() != null) {
            attrValue = (Serializable) eval(getValue());
        }

        DocumentHandle dm = getDataModel();

        if (attrValue == null) {
            dm.getHints().remove(getHint());
        } else {
            dm.getHints().put(getHint(), attrValue);
        }
    }

}
