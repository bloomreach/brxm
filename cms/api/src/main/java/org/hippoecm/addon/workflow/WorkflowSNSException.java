/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.addon.workflow;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.WorkflowException;

/**
 * Exception to notify same-name-siblings error
 */
public class WorkflowSNSException extends WorkflowException {
    private final String existedName;

    public WorkflowSNSException(final String message) {
        this(message, StringUtils.EMPTY);
    }

    public WorkflowSNSException(final String message, final RepositoryException e) {
        this(message, StringUtils.EMPTY, e);
    }

    public WorkflowSNSException(final String message, final String existedName) {
        super(message);
        this.existedName = existedName;
    }

    public WorkflowSNSException(final String message, final String existedName, final Exception reason) {
        super(message, reason);
        this.existedName = existedName;
    }

    public String getExistedName(){
        return existedName;
    }
}
