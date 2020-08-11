/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.mock;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;

public class MockWorkflowManager implements WorkflowManager {

    private final Session session;

    public MockWorkflowManager(final Session session) {
        this.session = session;
    }

    @Override
    public Session getSession() throws RepositoryException {
        return session;
    }

    @Override
    public WorkflowDescriptor getWorkflowDescriptor(final String category, final Node item) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public WorkflowDescriptor getWorkflowDescriptor(final String category, final Document document) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Workflow getWorkflow(final String category, final Node item) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Workflow getWorkflow(final String category, final Document document) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Workflow getWorkflow(final WorkflowDescriptor descriptor) throws RepositoryException {
        throw new UnsupportedOperationException();
    }
}
