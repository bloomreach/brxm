/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
 *
 */
package org.hippoecm.hst.pagecomposer.jaxrs.services.experiencepage;

import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Optional;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpSession;

import org.apache.jackrabbit.JcrConstants;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.core.internal.BranchSelectionService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.DocumentWorkflowAction;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.util.WorkflowUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.jackrabbit.JcrConstants.JCR_FROZENUUID;
import static org.apache.jackrabbit.JcrConstants.NT_FROZENNODE;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED;
import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_STATE;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_BY;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.util.JcrUtils.getNodePathQuietly;
import static org.hippoecm.repository.util.JcrUtils.getStringProperty;
import static org.hippoecm.repository.util.JcrUtils.isAncestor;
import static org.hippoecm.repository.util.WorkflowUtils.Variant.UNPUBLISHED;
import static org.onehippo.repository.branch.BranchConstants.MASTER_BRANCH_ID;
import static org.onehippo.repository.util.JcrConstants.NT_FROZEN_NODE;

public class XPageUtils {

    private static final Logger log = LoggerFactory.getLogger(XPageUtils.class);

    private XPageUtils() {

    }

    /**
     * <p>
     *     Returns the {@link DocumentWorkflow} for the current Experience Page document BUT if the current page is not
     *     editable by the {@code userSession} then a ClientException is thrown
     * </p>
     * @throws ClientException in case the current user is not allowed to edit the current document, for example because
     * someone else is already editing it
     *
     */
    public static DocumentWorkflow getObtainEditableInstanceWorkflow(final HippoSession userSession,
                                                                     final PageComposerContextService contextService) throws RepositoryException, WorkflowException {

        final DocumentWorkflow documentWorkflow = getDocumentWorkflow(userSession, contextService);
        try {
            if (Boolean.FALSE.equals(documentWorkflow.hints(contextService.getSelectedBranchId()).get(DocumentWorkflowAction.obtainEditableInstance().getAction()))) {
                throw new ClientException("Document not editable", ClientError.ITEM_ALREADY_LOCKED);
            }
        } catch (RemoteException e) {
            log.error("Exception while checking hints", e);
            throw new WorkflowException(e.getMessage());
        }

        return documentWorkflow;
    }

    /**
     * Returns the {@link DocumentWorkflow} for the current Experience Page document
     */
    public static DocumentWorkflow getDocumentWorkflow(final HippoSession userSession,
                                                       final PageComposerContextService contextService) throws RepositoryException {
        // userSession is allowed to read the node since has XPAGE_REQUIRED_PRIVILEGE_NAME on the node
        final Node handle = userSession.getNodeByIdentifier(contextService.getExperiencePageHandleUUID());
        final HippoWorkspace workspace = userSession.getWorkspace();
        final WorkflowManager workflowManager = workspace.getWorkflowManager();
        return (DocumentWorkflow) workflowManager.getWorkflow("default", handle);
    }

    public static boolean isXPageDocument(final Node node) {
        final Optional<Node> unpublished = WorkflowUtils.getDocumentVariantNode(node, WorkflowUtils.Variant.UNPUBLISHED);
        if (!unpublished.isPresent()) {
            return false;
        }

        try {
            return unpublished.get().isNodeType(HstNodeTypes.MIXINTYPE_HST_XPAGE_MIXIN);
        } catch (RepositoryException e) {
            return false;
        }
    }

    /**
     * we need to write with the workflowSession. Make sure to use this workflowSession and not impersonate to a
     * workflowSession : This way we can make sure that the workflow manager also persists the changes since the
     * workflow manager will handle the  workflow session save (when we invoke the document workflow
     */
    static Session getInternalWorkflowSession(final DocumentWorkflow documentWorkflow) {
        return documentWorkflow.getWorkflowContext().getInternalWorkflowSession();
    }

    /**
     * optionally checks out the right branch if the branch to be changed is in version history and not the unpublished
     * variant
     * Returns TRUE if a branch was checked out, FALSE if it wasn't needed
     */
    static boolean checkoutCorrectBranch(final DocumentWorkflow documentWorkflow,
                                         final PageComposerContextService contextService) throws WorkflowException, RepositoryException {

        try {

            if (!Boolean.TRUE.equals(documentWorkflow.hints(contextService.getSelectedBranchId()).get("checkoutBranch"))) {
                // there is only master branch, so no need to check out a branch
                return false;
            }
        } catch (RemoteException | RepositoryException e) {
            throw new WorkflowException(e.getMessage());
        }

        final HttpSession httpSession = contextService.getRequestContext().getServletRequest().getSession();
        final CmsSessionContext cmsSessionContext = CmsSessionContext.getContext(httpSession);

        final String targetBranchId = getBranchId(cmsSessionContext);

        // validate that the targetBranchId is the SAME as the branch ID belonging to the request identifier node (this
        // can be a workspace container item or a container item in version history! This check is to avoid that for
        // some reason, there is a mismatch between the container item branch and the CMS session context branch

        final Optional<Node> unpublished = WorkflowUtils.getDocumentVariantNode(documentWorkflow.getNode(), UNPUBLISHED);

        if (!unpublished.isPresent()) {
            throw new WorkflowException("Expected unpublished variant to be present");
        }

        final String currentBranchId = getStringProperty(unpublished.get(), HIPPO_PROPERTY_BRANCH_ID, MASTER_BRANCH_ID);

        final Session workflowSession = getInternalWorkflowSession(documentWorkflow);
        final Node xpageComponent = workflowSession.getNodeByIdentifier(contextService.getRequestConfigIdentifier());

        if (xpageComponent.isNodeType(NT_FROZENNODE)) {

            // FIND OUT whether the FROZEN NODE is also the LATEST version for branch, otherwise there has been made
            // changes by someone else *after* the current cms user loaded the page in the CM : Optimistic locking should
            // then kick in! TODO cast this behavior in concrete in an integration test
            final Document current = documentWorkflow.getBranch(targetBranchId, UNPUBLISHED);
            if (!isAncestor(current.getNode(workflowSession), xpageComponent)) {
                String msg = String.format("Node '%s' is not the most recent version for '%s' anymore. Someone else might have " +
                                "made concurrent changes, page must be reloaded. This is optimistic locking",
                        getNodePathQuietly(xpageComponent), targetBranchId);
                log.info(msg);
                throw new ClientException(msg, ClientError.ITEM_CHANGED);
            }


            Node documentVariant = xpageComponent;
            while (documentVariant.getParent().isNodeType(NT_FROZENNODE)) {
                documentVariant = documentVariant.getParent();
            }
            final String xPageComponentBranchId = getStringProperty(documentVariant, HIPPO_PROPERTY_BRANCH_ID, MASTER_BRANCH_ID);
            if (!targetBranchId.equals(xPageComponentBranchId)) {
                throw new WorkflowException(String.format("Expected target branch id '%s' to be the same as the branch " +
                        "id '%s' to which the XPage component '%s' belongs", targetBranchId, xPageComponentBranchId,
                        contextService.getRequestConfigIdentifier()));
            }
        }

        if (currentBranchId.equals(targetBranchId)) {
            log.debug(String.format("target branch '%s' is current unpublished, no need to invoke checkoutBranch workflow",
                    targetBranchId ));
            return false;
        }

        documentWorkflow.checkoutBranch(targetBranchId);
        return true;
    }

    /**
     * @return
     */
    static String getBranchId(final CmsSessionContext cmsSessionContext) {
        final BranchSelectionService branchSelectionService = HippoServiceRegistry.getService(BranchSelectionService.class);
        final String selectedBranchId = branchSelectionService.getSelectedBranchId(cmsSessionContext.getContextPayload());
        return selectedBranchId == null ? MASTER_BRANCH_ID : selectedBranchId;
    }


    /**
     * @throws ClientException in case  {@code versionStamp} is not 0, the {@code container} has the property
     * {@code GENERAL_PROPERTY_LAST_MODIFIED} and the value is not equal to {@code versionStamp}
     */
    static void validateTimestamp(final long versionStamp, final Node container, final String userId) throws RepositoryException, ClientException {
        if (versionStamp == 0) {
            // no timestamp to validate against, ignore
        }

        // sometimes the UI has not yet been update with the timestamp if two calls are done after each other, therefore
        // we now first check if the unpublished last modified by is equal to cmsUserId, and if so, we do not require
        // the same timestamp, this is similar to checking the lock in
        // org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.LockHelper.doLock. Admittedly it would had been better
        // if the UI always sends the updated timestamp but as long as it doesn't, this helps
        final Node xPageUnpublishedVariant = getXPageUnpublishedVariant(container);
        if (xPageUnpublishedVariant == null) {
            throw new RepositoryException("Expected to find an unpublished variant");
        }
        final String lastModifiedBy = getStringProperty(xPageUnpublishedVariant, HIPPOSTDPUBWF_LAST_MODIFIED_BY, null);
        if (userId.equals(lastModifiedBy)) {
            return;
        }

        if (versionStamp != 0 && container.hasProperty(GENERAL_PROPERTY_LAST_MODIFIED)) {
            long existingStamp = container.getProperty(GENERAL_PROPERTY_LAST_MODIFIED).getLong();
            if (existingStamp != versionStamp) {
                String msg = String.format("Node '%s' has been modified wrt versionStamp. Someone else might have " +
                                "made concurrent changes, page must be reloaded. This can happen due to optimistic locking",
                        getNodePathQuietly(container));
                log.info(msg);
                throw new ClientException(msg, ClientError.ITEM_CHANGED);
            }
        }
    }


    /**
     * Returns the WORKSPACE node for {@code identifier}
     *
     * Note that 'pageComposerContextService.getRequestConfigIdentifier()' can return a frozen node : this method returns
     * the 'workspace' version of that node, and if not found, a ItemNotFoundException will be thrown
     */
    static Node getWorkspaceNode(final Session session, final String identifier) throws RepositoryException {
        // note this can be a frozen node
        final Node node = session.getNodeByIdentifier(identifier);

        if (node.isNodeType(JcrConstants.NT_FROZENNODE)) {
            // get hold of the workspace container item! Since 'checkoutCorrectBranch' did already do the checkout,
            // and a restore from version history restores the frozen uuid, we can safely get hold of that one
            final String workspaceUUID = node.getProperty(JcrConstants.JCR_FROZENUUID).getString();
            return session.getNodeByIdentifier(workspaceUUID);
        } else {
            return node;
        }
    }


    /**
     * if belongs to XPage, returns the unpublished variant. If it is an XPage but there is no unpublished variant,
     * we throw an ClientException for now, see CMS-13262.
     * Note that the {@code node} can also be a frozen XPage node: In that case we try to get hold of the
     * unpubished variant from the frozen node.
     */
    public static Node getXPageUnpublishedVariant(Node node) throws RepositoryException {
        if (node.getSession().getRootNode().isSame(node)) {
            return null;
        }

        if (node.isNodeType(NT_FROZEN_NODE)) {
            Node current = node;
            while (current.getParent().isNodeType(NT_FROZEN_NODE)) {
                current = current.getParent();
            }
            // expected that current is now the frozen node of the unpublished variant
            final String workspaceUUID = current.getProperty(JCR_FROZENUUID).getString();
            try {
                final Node unpublished = node.getSession().getNodeByIdentifier(workspaceUUID);
                if (unpublished.getParent().isNodeType(HippoNodeType.NT_HANDLE) &&
                        HippoStdNodeType.UNPUBLISHED.equals(getStringProperty(unpublished, HIPPOSTD_STATE, null))) {
                    return unpublished;
                } else {
                    throw new ClientException(String.format("Could not find unpublished variant of Experience Page for '%s'.",
                            node.getPath()), ClientError.INVALID_UUID);
                }
            } catch (ItemNotFoundException e) {
                throw new ClientException(String.format("Could not find unpublished variant workspace node for versioned node '%s'",
                        node.getPath()), ClientError.INVALID_UUID);
            }

        }

        if (node.getName().equals(HstNodeTypes.NODENAME_HST_XPAGE) && node.getParent().isNodeType(HstNodeTypes.MIXINTYPE_HST_XPAGE_MIXIN)) {
            // found hst:page, parent must be hippo:document and must be unpublished variant
            Node unpublished = node.getParent();
            if (unpublished.getParent().isNodeType(HippoNodeType.NT_HANDLE) &&
                    HippoStdNodeType.UNPUBLISHED.equals(getStringProperty(unpublished, HIPPOSTD_STATE, null))) {
                return unpublished;
            } else {
                throw new ClientException(String.format("'%s' Does not belong to unpublished variant of Experience Page.",
                        node.getPath()), ClientError.INVALID_UUID);
            }
        }

        return getXPageUnpublishedVariant(node.getParent());
    }


    public static Calendar updateTimestamp(final Node containerNode) throws RepositoryException {
        // update last modified for optimistic locking
        final Calendar updatedTimestamp = Calendar.getInstance();
        containerNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED, updatedTimestamp);
        return updatedTimestamp;
    }
}
