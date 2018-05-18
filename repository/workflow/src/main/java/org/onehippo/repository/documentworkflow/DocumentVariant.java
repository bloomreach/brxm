/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.repository.documentworkflow;

import java.util.Date;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.HippoStdPubWfNodeType;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MIXIN_BRANCH_INFO;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;

/**
 * DocumentVariant provides a model object for a Hippo Document variant node to the DocumentWorkflow SCXML state machine.
 */
public class DocumentVariant extends Document {

    public static final String CORE_BRANCH_ID = "core";
    public static final String CORE_BRANCH_LABEL_LIVE = "core-live";
    public static final String CORE_BRANCH_LABEL_PREVIEW = "core-preview";

    public DocumentVariant() {
    }

    /**
     * Enabling package access
     * @return backing Node
     */
    protected Node getNode() {
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
        }
        else {
            setStringsProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[0]);
        }
    }

    public String[] getAvailability() throws RepositoryException {
        return getStringsProperty(HippoNodeType.HIPPO_AVAILABILITY);
    }

    @SuppressWarnings("unused")
    public boolean isBranch(final String branch) throws RepositoryException {
        if (branch == null) {
            throw new IllegalArgumentException("Branch is not allowed to be null");
        }
        if (getNode().isNodeType(HIPPO_MIXIN_BRANCH_INFO)) {
            return branch.equals(getNode().getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
        }
        return branch.equals(CORE_BRANCH_ID);
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

    public void setModified(String username) throws RepositoryException{
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
