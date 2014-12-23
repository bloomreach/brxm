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

package org.onehippo.repository.scxml;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.mock.MockNode;

public class MockWorkflowContext implements WorkflowContext {

    private String userIdentity;
    private Session session;
    private MockRepositoryMap configuration;

    public MockWorkflowContext(String userIdentity) throws RepositoryException {
        this(userIdentity, MockNode.root().getSession());
    }

    public MockWorkflowContext(String userIdentity, Session session) throws RepositoryException {
        this.userIdentity = userIdentity;
        this.session = session;
        this.configuration = new MockRepositoryMap();
    }

    @Override
    public Workflow getWorkflow(final String category) throws WorkflowException, RepositoryException {
        return null;
    }

    @Override
    public Workflow getWorkflow(final String category, final Document document) throws WorkflowException, RepositoryException {
        return null;
    }

    public void setUserIdentity(String userIdentity) {
        this.userIdentity = userIdentity;
    }

    @Override
    public String getUserIdentity() {
        return userIdentity;
    }

    @Override
    public Node getSubject() {
        return null;
    }

    @Override
    public Session getSubjectSession() {
        return session;
    }

    @Override
    public Session getUserSession() {
        return session;
    }

    @Override
    public Session getInternalWorkflowSession() {
        return session;
    }

    @Override
    public MockRepositoryMap getWorkflowConfiguration() {
        return configuration;
    }

    @Override
    public String getInteraction() {
        return null;
    }

    @Override
    public String getInteractionId() {
        return null;
    }
}
