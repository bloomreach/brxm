/*
 * Copyright 2021 Hippo B.V. (http://www.onehippo.com)
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

import java.rmi.RemoteException;
import java.util.Calendar;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.documentworkflow.DocumentVariant;
import org.onehippo.repository.campaign.Campaign;
import org.onehippo.repository.documentworkflow.campaign.JcrVersionsMetaUtils;

import static java.lang.String.format;
import static org.apache.commons.lang3.ArrayUtils.add;
import static org.apache.commons.lang3.ArrayUtils.contains;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.util.JcrUtils.getStringProperty;
import static org.onehippo.repository.branch.BranchConstants.MASTER_BRANCH_ID;
import static org.onehippo.repository.util.JcrConstants.NT_FROZEN_NODE;

public class CampaignTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;

    private String branchId;
    private Calendar from;
    private Calendar to;
    private String frozenNodeId;

    public void setBranchId(final String branchId) {
        this.branchId = branchId;
    }

    public void setFrozenNodeId(final String frozenNodeId) {
        this.frozenNodeId = frozenNodeId;
    }

    public void setFrom(final Calendar from) {
        this.from = from;
    }

    public void setTo(final Calendar to) {
        this.to = to;
    }

    @Override
    protected Object doExecute() throws WorkflowException, RepositoryException, RemoteException {

        final DocumentHandle documentHandle = getDocumentHandle();

        if (frozenNodeId == null) {
            throw new WorkflowException("frozenNodeId is not allowed to be null");
        }

        try {
            final Node frozenNode = getWorkflowContext().getInternalWorkflowSession().getNodeByIdentifier(frozenNodeId);
            if (!frozenNode.isNodeType(NT_FROZEN_NODE)) {
                throw new WorkflowException(format("Node for '%s' is not a node of type '%s'", frozenNodeId, NT_FROZEN_NODE));
            }

            Node handle = documentHandle.getHandle();

            if (to == null && from == null) {
                JcrVersionsMetaUtils.removeCampaign(handle, frozenNodeId);
                // return the unpublished document variant for the right workflow events
                return documentHandle.getDocuments().get(HippoStdNodeType.UNPUBLISHED);
            } else if (from == null) {
                // not allowed that one value is null
                throw new WorkflowException("'from' date has to be set when setting a campaign");
            }

            if (!getStringProperty(frozenNode, HIPPO_PROPERTY_BRANCH_ID, MASTER_BRANCH_ID).equals(branchId)) {
                throw new WorkflowException(format("Node for '%s' is not for branch '%s' hence not allowed to be a " +
                        "campaign for that branch", frozenNodeId, branchId));
            }

            if (to != null && to.before(from)) {
                throw new WorkflowException(format("Not allowed to have a 'to' date '%s' being before the 'from' date '%s'",
                        to, from));
            }

            JcrVersionsMetaUtils.setCampaign(handle, new Campaign(frozenNodeId, from, to));

            // return the unpublished document variant for the right workflow events
            return documentHandle.getDocuments().get(HippoStdNodeType.UNPUBLISHED);
        } catch (ItemNotFoundException e) {
            throw new WorkflowException(format("No node found for '%s'", frozenNodeId));
        }

    }

}
