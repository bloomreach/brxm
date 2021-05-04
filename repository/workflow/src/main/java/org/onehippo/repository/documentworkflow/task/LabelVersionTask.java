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

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.campaign.VersionLabel;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.documentworkflow.DocumentVariant;
import org.onehippo.repository.documentworkflow.campaign.JcrVersionsMetaUtils;

import static java.lang.String.format;
import static org.onehippo.repository.util.JcrConstants.NT_FROZEN_NODE;

public class LabelVersionTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;

    private String frozenNodeId;
    private String versionLabel;

    public void setFrozenNodeId(final String frozenNodeId) {
        this.frozenNodeId = frozenNodeId;
    }

    public void setVersionLabel(final String versionLabel) {
        this.versionLabel = versionLabel;
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

            if (StringUtils.isEmpty(versionLabel)) {
                JcrVersionsMetaUtils.removeVersionLabel(handle, frozenNodeId);
                return new DocumentVariant(frozenNode);
            }

            JcrVersionsMetaUtils.setVersionLabel(handle, new VersionLabel(frozenNodeId, versionLabel));

            return new DocumentVariant(frozenNode);
        } catch (ItemNotFoundException e) {
            throw new WorkflowException(format("No node found for '%'s", frozenNodeId));
        }

    }
}
