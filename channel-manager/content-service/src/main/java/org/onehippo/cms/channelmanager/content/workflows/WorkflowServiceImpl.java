/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.channelmanager.content.workflows;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.cms.channelmanager.content.document.util.EditingUtils;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo.Reason;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.ForbiddenException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.cms.channelmanager.content.error.NotFoundException;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cms.channelmanager.content.document.util.ContentWorkflowUtils.getDocumentWorkflow;
import static org.onehippo.cms.channelmanager.content.document.util.DocumentHandleUtils.getHandle;

public class WorkflowServiceImpl implements WorkflowService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowServiceImpl.class);

    @Override
    public void executeDocumentWorkflowAction(final String uuid, final String action, final Session session,
                                              final Map<String, Serializable> contextPayload) throws ErrorWithPayloadException {
        final Node handle = getHandle(uuid, session);
        final DocumentWorkflow documentWorkflow = getDocumentWorkflow(handle);
        final Map<String, Serializable> hints = EditingUtils.getHints(documentWorkflow, contextPayload);

        if (isRequestPendingAction(action)) {
            final Node requestNode = getRequestNode(handle);
            if (actionIsNotAvailable(action, requestNode, uuid, hints)) {
                log.info("Workflow request action '{}' is not available for document '{}'", action, uuid);
                throw new ForbiddenException(new ErrorInfo(Reason.WORKFLOW_ACTION_NOT_AVAILABLE));
            }
            executeDocumentWorkflowAction(uuid, getIdentifier(requestNode), documentWorkflow, action);
        } else {
            if (!EditingUtils.isActionAvailable(action, hints)) {
                log.info("Workflow action '{}' is not available for document '{}'", action, uuid);
                throw new ForbiddenException(new ErrorInfo(Reason.WORKFLOW_ACTION_NOT_AVAILABLE));
            }
            executeDocumentWorkflowAction(uuid, null, documentWorkflow, action);
        }
    }

    private static void executeDocumentWorkflowAction(final String uuid, final String requestIdentifier, final DocumentWorkflow documentWorkflow, final String action) throws ErrorWithPayloadException {
        try {
            switch (action) {
                case "publish":
                    documentWorkflow.publish();
                    break;

                case "requestPublication":
                    documentWorkflow.requestPublication();
                    break;

                case "cancelRequest":
                    documentWorkflow.cancelRequest(requestIdentifier);
                    break;

                default:
                    log.warn("Document workflow action '{}' is not implemented", action);
                    throw new InternalServerErrorException(new ErrorInfo(Reason.WORKFLOW_ACTION_NOT_IMPLEMENTED));
            }
        } catch (WorkflowException | RepositoryException | RemoteException e) {
            log.warn("Failed to execute workflow action '{}' on document '{}'", action, uuid, e);
            throw new InternalServerErrorException(new ErrorInfo(Reason.SERVER_ERROR));
        }
    }

    /**
     * Gets the hippo:request child node of a handle node. If more request nodes exist an error is thrown: technically
     * multiple nodes may exist, but in practice this situation is not expected never occur.
     *
     * @param handle the document handle node
     * @return the one and only request node or null if not found
     * @throws ErrorWithPayloadException if there are multiple request nodes
     */
    private static Node getRequestNode(final Node handle) throws ErrorWithPayloadException {
        Node requestNode = null;
        try {
            final NodeIterator nodeIterator = handle.getNodes("hippo:request");
            if (nodeIterator.hasNext()) {
                requestNode = nodeIterator.nextNode();
                if (nodeIterator.hasNext()) {
                    log.warn("Multiple request nodes found for '{}'. This situation is not supported.", handle.getIdentifier());
                    throw new InternalServerErrorException(new ErrorInfo(Reason.MULTIPLE_REQUESTS));
                }
            }
        } catch (RepositoryException e) {
            log.warn("Unexpected error when reading request child nodes.", e);
            throw new InternalServerErrorException(new ErrorInfo(Reason.SERVER_ERROR));
        }
        return requestNode;
    }

    private static boolean actionIsNotAvailable(final String action, final Node requestNode, final String uuid, final Map<String, Serializable> hints) throws ErrorWithPayloadException {
        if (requestNode != null) {
            try {
                return !EditingUtils.isRequestActionAvailable(action, requestNode.getIdentifier(), hints);
            } catch (RepositoryException e) {
                log.warn("Unexpected error when retrieving node identifier.", e);
                throw new InternalServerErrorException(new ErrorInfo(Reason.SERVER_ERROR));
            }
        } else {
            log.warn("Cancel request received but no pending request found for '{}'.", uuid);
            throw new NotFoundException(new ErrorInfo(Reason.NO_REQUEST_PENDING));
        }
    }

    /**
     * @return True if the action is for a document in request pending status.
     */
    private static boolean isRequestPendingAction(final String action) {
        return "cancelRequest".equals(action);
    }

    private static String getIdentifier(final Node node) throws InternalServerErrorException {
        try {
            return node.getIdentifier();
        } catch (RepositoryException e) {
            log.warn("Unexpected error when retrieving node identifier.", e);
            throw new InternalServerErrorException(new ErrorInfo(Reason.SERVER_ERROR));
        }
    }

}
