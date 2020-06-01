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
 *
 */
package org.onehippo.repository.documentworkflow;

import java.util.Date;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.branch.BranchConstants;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_NAME;

/**
 * DocumentVariant provides a model object for a Hippo Document variant node to the DocumentWorkflow SCXML state machine.
 */
public class DocumentVariant extends Document {

    public DocumentVariant() {
    }

    /**
     * Enabling package access
     *
     * @return backing Node
     */
    public Node getNode() {
        return super.getNode();
    }

    public DocumentVariant(Node node) throws RepositoryException {
        super(node);
    }

    public void setState(String state) throws RepositoryException {
        setStringProperty(HippoStdNodeType.HIPPOSTD_STATE, state);
    }

    public String getState() throws RepositoryException {
        return getStringProperty(HippoStdNodeType.HIPPOSTD_STATE);
    }

    public String getStateSummary() throws RepositoryException {
        return getStringProperty(HippoStdNodeType.HIPPOSTD_STATESUMMARY);
    }

    public void setPublicationDate(Date date) throws RepositoryException {
        setDateProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_PUBLICATION_DATE, date);
    }

    public Date getPublicationDate() throws RepositoryException {
        return getDateProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_PUBLICATION_DATE);
    }

    public void setHolder(String username) throws RepositoryException {
        setStringProperty(HippoStdNodeType.HIPPOSTD_HOLDER, username);
    }

    public String getHolder() throws RepositoryException {
        return getStringProperty(HippoStdNodeType.HIPPOSTD_HOLDER);
    }

    public void setAvailability(String[] availability) throws RepositoryException {
        if (availability != null) {
            setStringsProperty(HippoNodeType.HIPPO_AVAILABILITY, availability);
        } else {
            setStringsProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[0]);
        }
    }

    public String[] getAvailability() throws RepositoryException {
        return getStringsProperty(HippoNodeType.HIPPO_AVAILABILITY);
    }

    // returns true if this document variant does not represent a branch
    @SuppressWarnings("unused")
    public boolean isMaster() throws RepositoryException {
        // do not nodetype check since getNode can also be a frozenNode, see DocumentWorkflowImpl.getBranch()
        return !getNode().hasProperty(HIPPO_PROPERTY_BRANCH_ID);
    }

    @SuppressWarnings("unused")
    public boolean isBranch(final String branchId) throws RepositoryException {
        if (branchId == null) {
            throw new IllegalArgumentException("Branch is not allowed to be null");
        }
        // do not nodetype check since getNode can also be a frozenNode, see DocumentWorkflowImpl.getBranch()
        final String branchIdProperty = JcrUtils.getStringProperty(getNode(), HIPPO_PROPERTY_BRANCH_ID, null);
        if (branchIdProperty == null) {
            return branchId.equals(BranchConstants.MASTER_BRANCH_ID);
        } else {
            return branchId.equals(branchIdProperty);
        }
    }

    public String getBranchId() throws RepositoryException {
        return JcrUtils.getStringProperty(getNode(), HIPPO_PROPERTY_BRANCH_ID, BranchConstants.MASTER_BRANCH_ID);
    }

    public String getBranchName() throws RepositoryException {
        return JcrUtils.getStringProperty(getNode(), HIPPO_PROPERTY_BRANCH_NAME, null);
    }

    public boolean isAvailable(String environment) throws RepositoryException {
        String[] availability = getAvailability();
        if (availability == null) {
            return true;
        } else {
            for (String env : availability) {
                if (environment.equals(env)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setModified(String username) throws RepositoryException {
        setStringProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_BY, username);
        setDateProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE, new Date());
    }

    public Date getLastModified() throws RepositoryException {
        final Date date = getDateProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE);
        if (date == null) {
            return new Date(0);
        }
        return date;
    }

    public String getLastModifiedBy() throws RepositoryException {
        return getStringProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_BY);
    }
}
