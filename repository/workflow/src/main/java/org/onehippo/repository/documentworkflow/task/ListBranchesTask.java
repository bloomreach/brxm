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
package org.onehippo.repository.documentworkflow.task;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.scxml.SCXMLWorkflowContext;

/**
 * <p>
 *    Returns the available branches available via {@link DocumentHandle#getBranches()} and sets this on the
 *    {@link SCXMLWorkflowContext}. This action is idempotent
 * </p>
 */
public class ListBranchesTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;

    @Override
    protected Object doExecute() throws WorkflowException, RepositoryException {
        return getDocumentHandle().getBranches();
    }
}
