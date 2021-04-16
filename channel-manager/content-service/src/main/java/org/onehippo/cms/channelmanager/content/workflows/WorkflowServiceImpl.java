/*
 * Copyright 2018-2021 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Optional;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.JcrConstants;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.cms.channelmanager.content.document.model.Version;
import org.onehippo.cms.channelmanager.content.document.util.EditingUtils;
import org.onehippo.cms.channelmanager.content.document.util.HintsUtils;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo.Reason;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.ForbiddenException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.cms.channelmanager.content.error.NotFoundException;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.jackrabbit.JcrConstants.NT_FROZENNODE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.util.JcrUtils.getStringProperty;
import static org.onehippo.cms.channelmanager.content.document.util.ContentWorkflowUtils.getDocumentWorkflow;
import static org.onehippo.cms.channelmanager.content.document.util.DocumentHandleUtils.getHandle;
import static org.onehippo.repository.branch.BranchConstants.MASTER_BRANCH_ID;

public class WorkflowServiceImpl implements WorkflowService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowServiceImpl.class);

    @Override
    public void executeDocumentWorkflowAction(final String uuid, final String action, final Session session,
                                              final String branchId) throws ErrorWithPayloadException {
        executeDocumentWorkflowAction(uuid, action, session, branchId, Optional.empty());
    }

    @Override
    public void executeDocumentWorkflowAction(final String uuid, final String action, final Session session, final String branchId,
                                              final Optional<Version> version) throws ErrorWithPayloadException {
        final Node handle = getHandle(uuid, session);
        final DocumentWorkflow documentWorkflow = getDocumentWorkflow(handle);
        final Map<String, Serializable> hints = HintsUtils.getHints(documentWorkflow, branchId);

        if (isRequestPendingAction(action)) {
            final Node requestNode = getRequestNode(handle);
            if (actionIsNotAvailable(action, requestNode, uuid, hints)) {
                log.info("Workflow request action '{}' is not available for document '{}'", action, uuid);
                throw new ForbiddenException(new ErrorInfo(Reason.WORKFLOW_ACTION_NOT_AVAILABLE));
            }
            executeDocumentWorkflowAction(uuid, branchId, getIdentifier(requestNode), documentWorkflow, action, version);
        } else {
            if (!EditingUtils.isActionAvailable(action, hints)) {
                log.info("Workflow action '{}' is not available for document '{}'", action, uuid);
                throw new ForbiddenException(new ErrorInfo(Reason.WORKFLOW_ACTION_NOT_AVAILABLE));
            }
            executeDocumentWorkflowAction(uuid, branchId, null, documentWorkflow, action, version);
        }
    }

    private static void executeDocumentWorkflowAction(final String uuid, final String branchId,
                                                      final String requestIdentifier,
                                                      final DocumentWorkflow documentWorkflow,
                                                      final String action,
                                                      final Optional<Version> version) throws ErrorWithPayloadException {
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

                case "version":
                    version(branchId, documentWorkflow, version);
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
     * <p>
     *     TODO since this feature needs to be backported to a minor version, it is not allowed to modify scxml :
     *     otherwise, we would better have added support for:
     *     <pre>
     *     documentWorkflow.version(branchId)
     *     </pre>
     *     which does *everything* below in one workflow action AND also 'fixes' a potential but very small concurrency
     *     issue: A single workflow action uses a single lock. However, between multiple workflow actions, potentially
     *     another workflow invocation by someone else on the same document can happen, for example replacing the
     *     checked out branch with another one just before calling documentWorkflow.version(). Supporting this in a minor
     *     however is too much of a hassle and odds that it fails are very low and worst case scenario is not even so
     *     problematic : a version of a different branch is made (pointless)
     *     We can improve this in release/saas or 15.0 by introducing 'documentWorkflow.version(branchId)'
     * </p>
     */
    private static void version(final String branchId, final DocumentWorkflow documentWorkflow, final Optional<Version> version) throws WorkflowException, RemoteException, RepositoryException {
        if (!documentWorkflow.listBranches().contains(branchId)) {
            throw new ForbiddenException(new ErrorInfo(Reason.BRANCH_DOES_NOT_EXIST));
        }
        // first make sure the correct branch is checked out before creating a new version: if the workspace
        // variant is already for 'branchId' the #checkoutBranch doesn't do anything
        // checkoutBranch is unfortunately only available if there are branches next to master, hence we need
        // to check this separately instead of just invoking documentWorkflow.checkoutBranch(branchId);
        if (Boolean.TRUE.equals(documentWorkflow.hints(branchId).get("checkoutBranch"))) {
            documentWorkflow.checkoutBranch(branchId);
        }
        final Document jcrVersion = documentWorkflow.version();
        version.ifPresent(v -> {
            try {
                final String frozenNodeId = ((javax.jcr.version.Version) jcrVersion.getNode(documentWorkflow.getWorkflowContext().getInternalWorkflowSession())).getFrozenNode().getIdentifier();
                if (v.getCampaign() != null) {
                    documentWorkflow.campaign(frozenNodeId, branchId, v.getCampaign().getFrom(), v.getCampaign().getTo());
                }
                if (v.getLabel() != null) {
                    documentWorkflow.labelVersion(frozenNodeId, v.getLabel());
                }
            } catch (Exception e) {
                log.warn("Unexpected error when setting campaign or label.", e);
                throw new InternalServerErrorException(new ErrorInfo(Reason.SERVER_ERROR));
            }
        });
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


    @Override
    public void restoreDocumentWorkflowAction(final String uuid, final String frozenNodeId, final Session session,
                                              final String branchId)
            throws ErrorWithPayloadException {
        final Node handle = getHandle(uuid, session);
        try {


            final DocumentWorkflow documentWorkflow = getDocumentWorkflow(handle);
            final Map<String, Serializable> hints = documentWorkflow.hints(branchId);

            if (!EditingUtils.isActionAvailable("restoreVersionToBranch", hints)) {
                log.info("Workflow action '{}' is not available for document '{}' and frozen node id",
                        "restoreVersionToBranch", uuid, frozenNodeId);
                throw new ForbiddenException(new ErrorInfo(Reason.WORKFLOW_ACTION_NOT_AVAILABLE));
            }

            final Node frozenNode = session.getNodeByIdentifier(frozenNodeId);
            if (!frozenNode.isNodeType(NT_FROZENNODE)) {
                log.info("frozenNode '{}' does not belong to a node of type '{}'", frozenNode.getPath(), NT_FROZENNODE);
                throw new BadRequestException(new ErrorInfo(ErrorInfo.Reason.VERSION_DOES_NOT_EXIST));
            }

            // assert the 'frozenNode' is for the 'branchId'
            final String frozenNodeBranchId = getStringProperty(frozenNode, HIPPO_PROPERTY_BRANCH_ID, MASTER_BRANCH_ID);

            if (!frozenNodeBranchId.equals(branchId)) {
                // can only restore versions for 'branchId'
                log.info("frozenNode '{}' belongs to branch '{}' which is not the one from the current cms session context" +
                                " '{}'",  frozenNode.getPath(), frozenNodeBranchId, branchId);
                throw new BadRequestException(new ErrorInfo(ErrorInfo.Reason.INVALID_DATA));
            }

            if (!frozenNode.getParent().isNodeType(JcrConstants.NT_VERSION)) {
                // the frozenNodeId is always expected the unpublished variant below the Version node in version history
                log.info("frozenNode '{}' expected to be a child of a version node",  frozenNode.getPath());
                throw new BadRequestException(new ErrorInfo(ErrorInfo.Reason.INVALID_DATA));
            }

            final javax.jcr.version.Version version = (javax.jcr.version.Version) frozenNode.getParent();
            final String unpublishedWorkspaceVariantUUID = version.getContainingHistory().getVersionableIdentifier();
            // if session cannot read the workspace node, just a ItemNotFoundException is caught below
            final Node unpublishedWorkspaceVariant = session.getNodeByIdentifier(unpublishedWorkspaceVariantUUID);

            if (!unpublishedWorkspaceVariant.getParent().getIdentifier().equals(uuid)) {
                // the versionId does not belong to a versioned node of the unpublished variant below the handle 'uuid'
                log.info("frozenNode '{}' cannot be restored for document '{}' since not part of the version history for " +
                        "that document",  frozenNode.getPath(), handle.getPath());
                throw new BadRequestException(new ErrorInfo(ErrorInfo.Reason.INVALID_DATA));
            }

            documentWorkflow.restoreVersionToBranch(version, branchId);
            log.debug("Succesfully restored frozenNode '{}' for branch '{}' to '{}'", frozenNodeId, branchId,
                    unpublishedWorkspaceVariant.getPath());
        } catch (ItemNotFoundException e) {
            if (log.isDebugEnabled()) {
                log.info("Could not find item", e);
            } else {
                log.info("Could not find item : {}", e.toString());
            }
            throw new NotFoundException(new ErrorInfo(ErrorInfo.Reason.VERSION_DOES_NOT_EXIST));
        } catch (RepositoryException | RemoteException | WorkflowException e) {
            log.warn("Failed to execute workflow action 'restoreVersionToBranch' on document '{}' for version id",
                    uuid, frozenNodeId, e);
            throw new InternalServerErrorException(new ErrorInfo(Reason.SERVER_ERROR));
        }

    }

}
