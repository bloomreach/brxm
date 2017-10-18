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
package org.onehippo.cms.channelmanager.content.document.util;

import java.io.Serializable;
import java.util.Map;

import javax.jcr.Session;

import org.hippoecm.repository.api.DocumentWorkflowAction;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowTransition;

import static org.hippoecm.repository.api.DocumentWorkflowAction.OBTAIN_EDITABLE_INSTANCE;

public class WorkflowContext {

    private final Workflow workflow;
    private final Map<String, Serializable> contextPayload;
    private Session session;

    public WorkflowContext(final Workflow workflow, final Session session, final Map<String, Serializable> contextPayload) {

        this.workflow = workflow;
        this.session = session;
        this.contextPayload = contextPayload;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public Session getSession() {
        return session;
    }

    public Map<String, Serializable> getContextPayload() {
        return contextPayload;
    }

    public WorkflowTransition createWorkflowTransition(final DocumentWorkflowAction action) {
       return new WorkflowTransition.Builder().action(action.getAction()).contextPayload(contextPayload).build();
    }
}