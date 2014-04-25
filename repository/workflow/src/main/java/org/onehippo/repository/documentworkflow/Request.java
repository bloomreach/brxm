/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.repository.documentworkflow;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.quartz.HippoSchedJcrConstants;

/**
 * Request provides a base class model object for a Hippo Document request node to the DocumentWorkflow SCXML state machine.
 * <p>
 * {@link ScheduledRequest} and {@link WorkflowRequest} provide concrete implementations for the two different
 * usage types, and this base class provides static factory methods to create the correct instance based on the
 * underlying JCR node type.
 * </p>
 */
public abstract class Request extends Document {

    protected Request() {
    }

    protected Request(final Node node) throws RepositoryException {
        super(node);
    }

    /**
     * Enabling package access
     * @return backing Node
     */
    protected Node getNode() {
        return super.getNode();
    }

    public static Request createRequest(Node requestNode) throws RepositoryException {
        Request request = createWorkflowRequest(requestNode);
        if (request == null) {
            request = createScheduledRequest(requestNode);
        }
        return request;
    }

    public static WorkflowRequest createWorkflowRequest(Node requestNode) throws RepositoryException {
        if (requestNode.isNodeType(HippoStdPubWfNodeType.NT_HIPPOSTDPUBWF_REQUEST)) {
            return new WorkflowRequest(requestNode);
        }
        return null;
    }

    public static ScheduledRequest createScheduledRequest(Node requestNode) throws RepositoryException {
        if (requestNode.isNodeType(HippoSchedJcrConstants.HIPPOSCHED_WORKFLOW_JOB)) {
            return new ScheduledRequest(requestNode);
        }
        return null;
    }

    public final boolean isScheduledRequest() {
        return this instanceof ScheduledRequest;
    }

    public final boolean isWorkflowRequest() {
        return this instanceof WorkflowRequest;
    }
}
