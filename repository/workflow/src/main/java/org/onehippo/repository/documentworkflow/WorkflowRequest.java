/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.reviewedactions.HippoStdPubWfNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.util.JcrConstants;

public class WorkflowRequest extends Document {

    public static final String HIPPO_REQUEST = "hippo:request";
    public static final String NT_HIPPOSTDPUBWF_REQUEST = "hippostdpubwf:request";
    public static final String HIPPOSTDPUBWF_TYPE = "hippostdpubwf:type";
    public static final String HIPPOSTDPUBWF_USERNAME = "hippostdpubwf:username";
    public static final String HIPPOSTDPUBWF_REQDATE = "hippostdpubwf:reqdate";
    public static final String HIPPOSTDPUBWF_REASON = "hippostdpubwf:reason";
    public static final String REJECTED = "rejected"; // zombie
    public static final String PUBLISH = "publish";
    public static final String DEPUBLISH = "depublish";
    public static final String SCHEDPUBLISH = "scheduledpublish";
    public static final String SCHEDDEPUBLISH = "scheduleddepublish";
    public static final String DELETE = "delete";
    public static final String COLLECTION = "collection";

    public WorkflowRequest() {}

    public WorkflowRequest(Node node) throws RepositoryException {
        super(node);
    }

    private static Node newRequestNode(Node parent) throws RepositoryException {
        JcrUtils.ensureIsCheckedOut(parent);
        Node requestNode = parent.addNode(HIPPO_REQUEST, NT_HIPPOSTDPUBWF_REQUEST);
        requestNode.addMixin(JcrConstants.MIX_REFERENCEABLE);
        return requestNode;
    }

    public WorkflowRequest(String type, Node sibling, DocumentVariant document, String username) throws RepositoryException {
        super(newRequestNode(sibling.getParent()));
        setStringProperty(HIPPOSTDPUBWF_TYPE, type);
        setStringProperty(HIPPOSTDPUBWF_USERNAME, username);
        if (document != null) {
            getCheckedOutNode().setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_DOCUMENT, document.getNode());
        }
    }

    public WorkflowRequest(String type, Node sibling, DocumentVariant document, String username, Date scheduledDate) throws RepositoryException {
        this(type, sibling, document, username);
        setDateProperty(HIPPOSTDPUBWF_REQDATE, scheduledDate);
    }

    public String getType() throws RepositoryException {
        return getStringProperty(HIPPOSTDPUBWF_TYPE);
    }

    public String getOwner() throws RepositoryException {
        return getStringProperty(HIPPOSTDPUBWF_USERNAME);
    }

    public Date getScheduledDate() throws RepositoryException  {
        return getDateProperty(HIPPOSTDPUBWF_REQDATE);
    }

    public void setRejected(DocumentVariant stale, String reason) throws RepositoryException  {
        setStringProperty(HIPPOSTDPUBWF_TYPE, REJECTED);
        if (stale != null) {
            setNodeProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_DOCUMENT, stale.getNode());
        }
        else {
            setNodeProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_DOCUMENT, null);
        }
        setStringProperty(HIPPOSTDPUBWF_REASON, reason);
    }

    public void setRejected(String reason) throws RepositoryException  {
        setRejected(null, reason);
    }

    public Document getReference() throws RepositoryException  {
        if (hasNode() && getNode().hasProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_DOCUMENT)) {
            return new Document(getNode().getProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_DOCUMENT).getNode());
        }
        return null;
    }
}
