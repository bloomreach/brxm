/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.reviewedactions;

import java.util.Date;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.util.JcrUtils;

/**
 * @deprecated since CMS 7.9, no longer used by {@link org.onehippo.repository.documentworkflow.DocumentWorkflowImpl} replacing the reviewedactions workflow
 */
@Deprecated
public class PublicationRequest extends Document {

    public static final String REJECTED = "rejected"; // zombie
    public static final String PUBLISH = "publish";
    public static final String DEPUBLISH = "depublish";
    public static final String SCHEDPUBLISH = "scheduledpublish";
    public static final String SCHEDDEPUBLISH = "scheduleddepublish";
    public static final String DELETE = "delete";
    public static final String COLLECTION = "collection";

    public PublicationRequest() {}

    public PublicationRequest(Node node) throws RepositoryException {
        super(node);
    }

    private static Node newRequestNode(Node parent) throws RepositoryException {
        JcrUtils.ensureIsCheckedOut(parent);
        Node requestNode = parent.addNode("hippo:request", "hippostdpubwf:request");
        requestNode.addMixin("mix:referenceable");
        return requestNode;
    }

    public PublicationRequest(String type, Node sibling, PublishableDocument document, String username) throws RepositoryException {
        super(newRequestNode(sibling.getParent()));
        setStringProperty("hippostdpubwf:type", type);
        setStringProperty("hippostdpubwf:username", username);
        if (document != null) {
            getCheckedOutNode().setProperty("hippostdpubwf:document", document.getNode());
        }
    }

    public PublicationRequest(String type, Node sibling, PublishableDocument document, String username, Date scheduledDate) throws RepositoryException {
        this(type, sibling, document, username);
        setDateProperty("hippostdpubwf:reqdate", scheduledDate);
    }

    String getType() throws RepositoryException {
        return getStringProperty("hippostdpubwf:type");
    }

    String getOwner() throws RepositoryException {
        return getStringProperty("hippostdpubwf:username");
    }

    Date getScheduledDate() throws RepositoryException  {
        return getDateProperty("hippostdpubwf:reqdate");
    }

    void setRejected(PublishableDocument stale, String reason) throws RepositoryException  {
        setStringProperty("hippostdpubwf:type", REJECTED);
        if (stale != null) {
            setNodeProperty("hippostdpubwf:document", stale.getNode());
        }
        else {
            setNodeProperty("hippostdpubwf:document", null);
        }
        setStringProperty("hippostdpubwf:reason", reason);
    }

    void setRejected(String reason) throws RepositoryException  {
        setRejected(null, reason);
    }

    Document getReference() throws RepositoryException  {
        if (hasNode() && getNode().hasProperty("hippostdpubwf:document")) {
            return new Document(getNode().getProperty("hippostdpubwf:document").getNode());
        }
        return null;
    }
}
