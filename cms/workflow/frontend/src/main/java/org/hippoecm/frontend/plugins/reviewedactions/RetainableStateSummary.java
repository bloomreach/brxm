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
 */

package org.hippoecm.frontend.plugins.reviewedactions;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.branch.BranchHandle;
import org.onehippo.repository.documentworkflow.BranchHandleImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_STATESUMMARY;
import static org.hippoecm.repository.api.HippoNodeType.NT_HIPPO_VERSION_INFO;

public class RetainableStateSummary {


    private static final Logger log = LoggerFactory.getLogger(RetainableStateSummary.class);
    private final Node handle;
    private final String branchId;
    private boolean draftChanges;
    private boolean unpublishedChanges;
    private boolean live;


    public RetainableStateSummary(final Node handle, final String branchId) {
        this.handle = handle;
        this.branchId = branchId;
        try {
            invoke();
        } catch (RepositoryException | WorkflowException e) {
            log.warn("Could not read state summary for node: { path: {} }, please check the validity of the node.", JcrUtils.getNodePathQuietly(handle));
        }
    }


    public boolean hasDraftChanges(){
        return draftChanges;
    }

    public boolean isLive(){
        return live;
    }

    public boolean hasUnpublishedChanges(){
        return unpublishedChanges;
    }

    private void invoke() throws RepositoryException, WorkflowException {
        if (handle.isNodeType(HippoNodeType.NT_HANDLE)) {
            RetainableStateSummary.HandleParser handleParser = new RetainableStateSummary.HandleParser(handle);
            Node stateSummaryNode = handleParser.getStateSummaryVariant();
            final Node draftVariantNode = handleParser.getDraftVariantNode();
            if (draftVariantNode != null && draftVariantNode.hasProperty(HippoStdNodeType.HIPPOSTD_RETAINABLE)) {
                draftChanges = draftVariantNode.getProperty(HippoStdNodeType.HIPPOSTD_RETAINABLE).getBoolean();
            }
            NodeType primaryType = handleParser.getPrimaryType();
            if (stateSummaryNode != null
                    && (primaryType.isNodeType(HippoStdNodeType.NT_PUBLISHABLESUMMARY)
                    || stateSummaryNode.isNodeType(HippoStdNodeType.NT_PUBLISHABLESUMMARY))) {
                final String stateSummaryValue = getState(stateSummaryNode);
                live = !"new".equals(stateSummaryValue);
                unpublishedChanges = "changed".equals(stateSummaryValue);
            }
        }
    }

    private String getState(final Node stateSummaryVariant) throws RepositoryException, WorkflowException {

        if (!handle.isNodeType(NT_HIPPO_VERSION_INFO)) {
            // For performance reasons avoid creating a branch handle if it is not necessary.
            return stateSummaryVariant.getProperty(HIPPOSTD_STATESUMMARY).getString();
        }

        final BranchHandle branchHandle = new BranchHandleImpl(branchId, handle);
        if (!branchHandle.isLiveAvailable()) {
            return "new";
        }

        final Node unpublished = branchHandle.getUnpublished();
        if (unpublished!=null && branchHandle.isModified()) {
            return "changed";
        }
        return "live";
    }

    private static class HandleParser {
        private final Node node;
        private Node stateSummaryVariant;
        private Node draftVariantNode;
        private NodeType primaryType;

        HandleParser(final Node node) throws RepositoryException {
            this.node = node;
            invoke();
        }

        /**
         * @return One of the variants or {@code null} if none is present
         */
        public Node getStateSummaryVariant() {
            return stateSummaryVariant;
        }

        public Node getDraftVariantNode() {
            return draftVariantNode;
        }

        public NodeType getPrimaryType() {
            return primaryType;
        }

        /**
         * <p>
         * Find the draft variant and use that as the stateSummaryVariant or set the stateSummaryVariant to the first
         * (unpublished or published) variant that is found if there is no draft variant
         * </p>
         *
         * @throws RepositoryException If something went wrong
         */
        private void invoke() throws RepositoryException {
            NodeIterator docs = node.getNodes(node.getName());
            while (docs.hasNext() && draftVariantNode == null) {
                Node variantNode = docs.nextNode();
                primaryType = variantNode.getPrimaryNodeType();
                if (variantNode.isNodeType(HippoStdNodeType.NT_PUBLISHABLE)) {
                    String state = variantNode.getProperty(HippoStdNodeType.HIPPOSTD_STATE).getString();
                    if ("draft".equals(state)) {
                        draftVariantNode = variantNode;
                    }
                    stateSummaryVariant = variantNode;
                }
            }
        }
    }
}
