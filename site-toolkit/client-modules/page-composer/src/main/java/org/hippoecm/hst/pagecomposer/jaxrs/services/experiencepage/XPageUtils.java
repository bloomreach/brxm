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
import java.util.Optional;

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
import org.hippoecm.repository.api.DocumentWorkflowAction;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.WorkflowUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.jackrabbit.JcrConstants.NT_FROZENNODE;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.util.JcrUtils.getNodePathQuietly;
import static org.hippoecm.repository.util.WorkflowUtils.Variant.UNPUBLISHED;
import static org.onehippo.repository.branch.BranchConstants.MASTER_BRANCH_ID;

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
            if (Boolean.FALSE.equals(documentWorkflow.hints().get(DocumentWorkflowAction.obtainEditableInstance().getAction()))) {
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
            if (!Boolean.TRUE.equals(documentWorkflow.hints().get("checkoutBranch"))) {
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

        final String currentBranchId = JcrUtils.getStringProperty(unpublished.get(), HIPPO_PROPERTY_BRANCH_ID, MASTER_BRANCH_ID);

        final Node xpageComponent = getInternalWorkflowSession(documentWorkflow).getNodeByIdentifier(contextService.getRequestConfigIdentifier());

        if (xpageComponent.isNodeType(NT_FROZENNODE)) {
            Node documentVariant = xpageComponent;
            while (documentVariant.getParent().isNodeType(NT_FROZENNODE)) {
                documentVariant = documentVariant.getParent();
            }
            final String xPageComponentBranchId = JcrUtils.getStringProperty(documentVariant, HIPPO_PROPERTY_BRANCH_ID, MASTER_BRANCH_ID);
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


    static void validateTimestamp(final long versionStamp, final Node container) throws RepositoryException {
        if (true) {
            // temporarily ignore validating timestamp for xpages since IF a 'hst:lastmodified' ends up on the
            // container in an xpage, this timestamp is not taking into the HST Model timestamp (since container from
            // XPageLayout is used!! and then this validation always fails
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
     * Returns the WORKSPACE container item for {@code identifier}
     *
     * Note that 'pageComposerContextService.getRequestConfigIdentifier()' can return a frozen node : this method returns
     * the 'workspace' version of that node, and if not found, a ItemNotFoundException will be thrown
     */
    static Node getWorkspaceNode(final Session session, final String identifier) throws RepositoryException {
        // note this can be a frozen node
        final Node containerItem = session.getNodeByIdentifier(identifier);

        final Node workspaceNode;
        if (containerItem.isNodeType(JcrConstants.NT_FROZENNODE)) {
            // get hold of the workspace container item! Since 'checkoutCorrectBranch' did already do the checkout,
            // and a restore from version history restores the frozen uuid, we can safely get hold of that one
            final String workspaceUUID = containerItem.getProperty(JcrConstants.JCR_FROZENUUID).getString();
            return session.getNodeByIdentifier(workspaceUUID);
        } else {
            return containerItem;
        }
    }
}
