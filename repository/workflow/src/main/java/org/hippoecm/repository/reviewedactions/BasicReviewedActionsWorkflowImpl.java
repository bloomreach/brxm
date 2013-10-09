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

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.PropertyIterable;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicReviewedActionsWorkflowImpl extends WorkflowImpl implements BasicReviewedActionsWorkflow {

    private static final Logger log = LoggerFactory.getLogger(BasicReviewedActionsWorkflowImpl.class);

    private static final long serialVersionUID = 1L;

    private static final String[] PROTECTED_MIXINS = new String[]{
            JcrConstants.MIX_VERSIONABLE,
            JcrConstants.MIX_REFERENCEABLE,
            HippoNodeType.NT_HARDDOCUMENT,
            HippoStdNodeType.NT_PUBLISHABLE,
            HippoStdNodeType.NT_PUBLISHABLESUMMARY,
            HippoStdPubWfNodeType.HIPPOSTDPUBWF_DOCUMENT,
            HippoNodeType.NT_SKIPINDEX
    };
    private static final String[] PROTECTED_PROPERTIES = new String[]{
            HippoNodeType.HIPPO_AVAILABILITY,
            HippoNodeType.HIPPO_RELATED,
            HippoNodeType.HIPPO_PATHS,
            HippoStdNodeType.HIPPOSTD_STATE,
            HippoStdNodeType.HIPPOSTD_HOLDER,
            HippoStdNodeType.HIPPOSTD_STATESUMMARY,
            HippoStdPubWfNodeType.HIPPOSTDPUBWF_PUBLICATION_DATE,
            HippoStdPubWfNodeType.HIPPOSTDPUBWF_CREATED_BY,
            HippoStdPubWfNodeType.HIPPOSTDPUBWF_CREATION_DATE,
            HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_BY,
            HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE
    };
    static {
        Arrays.sort(PROTECTED_PROPERTIES);
        Arrays.sort(PROTECTED_MIXINS);
    }

    protected PublishableDocument draftDocument;

    protected PublishableDocument unpublishedDocument;

    protected PublishableDocument publishedDocument;

    protected PublicationRequest current;

    public BasicReviewedActionsWorkflowImpl() throws RemoteException {
    }

    public void setNode(Node node) throws RepositoryException {
        super.setNode(node);

        Node parent = node.getParent();

        draftDocument = unpublishedDocument = publishedDocument = null;
        for (Node sibling : new NodeIterable(parent.getNodes(node.getName()))) {
            String state = JcrUtils.getStringProperty(sibling, HippoStdNodeType.HIPPOSTD_STATE, "");
            if ("draft".equals(state)) {
                draftDocument = new PublishableDocument(sibling);
            } else if ("unpublished".equals(state)) {
                unpublishedDocument = new PublishableDocument(sibling);
            } else if ("published".equals(state)) {
                publishedDocument = new PublishableDocument(sibling);
            }
        }
        current = null;
        for (Node request : new NodeIterable(parent.getNodes("hippo:request"))) {
            String requestType = JcrUtils.getStringProperty(request, "hippostdpubwf:type", "");
            if (!("rejected".equals(requestType))) {
                current = new PublicationRequest(request);
            }
        }
    }

    @Override
    public Map<String, Serializable> hints() {
        Map<String, Serializable> info = super.hints();
        boolean editable = false;
        boolean publishable = false;
        boolean depublishable = false;
        Boolean deleteable = false;
        boolean status = false;
        boolean pendingRequest;
        if (current != null) {
            pendingRequest = true;
        } else {
            pendingRequest = false;
        }
        try {
            final String userIdentity = super.getWorkflowContext().getUserIdentity();
            final String state = JcrUtils.getStringProperty(getNode(), HippoStdNodeType.HIPPOSTD_STATE, "");

            boolean draftInUse = draftDocument != null && draftDocument.getOwner() != null && !draftDocument.getOwner().equals(userIdentity);
            boolean unpublishedDirty = unpublishedDocument != null && unpublishedDocument.isAvailable("preview");
            boolean publishedLive = publishedDocument != null && publishedDocument.isAvailable("live");

            status = !draftInUse;
            editable = !draftInUse && !pendingRequest;
            publishable = unpublishedDirty && !pendingRequest;
            depublishable = publishedLive && !pendingRequest;
            deleteable = !publishedLive;

            // put everything on the unpublished; unless it doesn't exist
            if (unpublishedDocument != null && !PublishableDocument.UNPUBLISHED.equals(state)) {
                status = editable = publishable = depublishable = false;
                deleteable = null;
            } else if (unpublishedDocument == null) {
                // unpublished is null
                // put edit, publish actions on draft, depublish, delete on published.
                if (PublishableDocument.DRAFT.equals(state)) {
                    if (publishedDocument != null) {
                        depublishable = status = false;
                        deleteable = null;
                    }
                } else if (PublishableDocument.PUBLISHED.equals(state)) {
                    if (draftDocument != null) {
                        editable = publishable = false;
                    }
                }
            }

            if (!editable && PublishableDocument.DRAFT.equals(state) && draftInUse) {
                info.put("inUseBy", draftDocument.getOwner());
            }
            info.put("obtainEditableInstance", editable);
            info.put("publish", publishable);
            info.put("depublish", depublishable);
            if (deleteable != null) {
                info.put("delete", deleteable);
            }
            info.put("status", status);
        } catch (RepositoryException ex) {
            // TODO DEJDO: ignore?
        }
        return info;
    }

    protected Node cloneDocumentNode(Document document) throws RepositoryException {
        Node srcNode = document.getNode();
        final Node parent = srcNode.getParent();
        JcrUtils.ensureIsCheckedOut(parent, true);

        Node destNode = parent.addNode(srcNode.getName(), srcNode.getPrimaryNodeType().getName());
        if (!destNode.isNodeType(HippoStdPubWfNodeType.HIPPOSTDPUBWF_DOCUMENT)) {
            destNode.addMixin(HippoStdPubWfNodeType.HIPPOSTDPUBWF_DOCUMENT);
        }
        if (srcNode.isNodeType(HippoStdNodeType.NT_PUBLISHABLESUMMARY) && !destNode.isNodeType(HippoStdNodeType.NT_PUBLISHABLESUMMARY)) {
            destNode.addMixin(HippoStdNodeType.NT_PUBLISHABLESUMMARY);
        }
        return copyTo(srcNode, destNode);
    }

    protected void deleteDocument(Document document) throws RepositoryException {
        JcrUtils.ensureIsCheckedOut(document.getNode(), true);
        JcrUtils.ensureIsCheckedOut(document.getNode().getParent(), true);
        document.getNode().remove();
    }

    protected void copyDocumentTo(Document source, Document target) throws RepositoryException {
        clearDocument(target);
        copyTo(source.getNode(), target.getNode());
    }

    protected void clearDocument(Document document) throws RepositoryException {
        final Node node = document.getNode();
        JcrUtils.ensureIsCheckedOut(node, true);

        for (Property property : new PropertyIterable(node.getProperties())) {
            if (property.getDefinition().isProtected()) {
                continue;
            }
            String name = property.getName();
            if (Arrays.binarySearch(PROTECTED_PROPERTIES, name) >= 0) {
                continue;
            }
            property.remove();
        }

        for (Node child : new NodeIterable(node.getNodes())) {
            if (child.getDefinition().isProtected()) {
                continue;
            }
            child.remove();
        }

        final NodeType[] mixins = node.getMixinNodeTypes();
        for (NodeType mixin : mixins) {
            String name = mixin.getName();
            if (Arrays.binarySearch(PROTECTED_MIXINS, name) >= 0) {
                continue;
            }
            node.removeMixin(mixin.getName());
        }

    }

    /**
     * Copies {@link Node} {@code srcNode} to {@code destNode}.
     * Special properties and mixins are filtered out; those are actively maintained by the workflow.
     *
     * @param srcNode the node to copy
     * @param destNode the node that the contents of srcNode will be copied to
     * @return destNode
     * @throws RepositoryException
     */
    protected Node copyTo(final Node srcNode, Node destNode) throws RepositoryException {
        for (NodeType mixin : srcNode.getMixinNodeTypes()) {
            String name = mixin.getName();
            if (Arrays.binarySearch(PROTECTED_MIXINS, name) >= 0) {
                continue;
            }
            destNode.addMixin(mixin.getName());
        }

        final PropertyIterator properties = srcNode.getProperties();
        while (properties.hasNext()) {
            final Property property = properties.nextProperty();
            if (!property.getDefinition().isProtected()) {
                String name = property.getName();
                if (Arrays.binarySearch(PROTECTED_PROPERTIES, name) >= 0) {
                    continue;
                }
                if (property.isMultiple()) {
                    destNode.setProperty(property.getName(), property.getValues(), property.getType());
                } else {
                    destNode.setProperty(property.getName(), property.getValue());
                }
            }
        }

        copyMetaData(srcNode, destNode);

        final NodeIterator nodes = srcNode.getNodes();
        while (nodes.hasNext()) {
            final Node child = nodes.nextNode();
            JcrUtils.copy(child, child.getName(), destNode);
        }
        return destNode;
    }

    private void copyMetaData(final Node srcNode, final Node destNode) throws RepositoryException {
        if (srcNode.isNodeType(HippoStdPubWfNodeType.HIPPOSTDPUBWF_DOCUMENT)
                && destNode.isNodeType(HippoStdPubWfNodeType.HIPPOSTDPUBWF_DOCUMENT)) {
            for (String propertyName : new String[] {
                    HippoStdPubWfNodeType.HIPPOSTDPUBWF_PUBLICATION_DATE,
                    HippoStdPubWfNodeType.HIPPOSTDPUBWF_CREATED_BY,
                    HippoStdPubWfNodeType.HIPPOSTDPUBWF_CREATION_DATE,
                    HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_BY,
                    HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE }) {
                if (srcNode.hasProperty(propertyName)) {
                    destNode.setProperty(propertyName, srcNode.getProperty(propertyName).getValue());
                } else if (destNode.hasProperty(propertyName)) {
                    destNode.getProperty(propertyName).remove();
                }
            }
        }
    }

    public Document obtainEditableInstance() throws WorkflowException {
        log.info("obtain editable instance on document ");
        try {
            if (draftDocument == null) {
                if (current != null) {
                    throw new WorkflowException("unable to edit document with pending operation");
                }
                createDraft();
            } else if (draftDocument.getOwner() != null) {
                if (!getWorkflowContext().getUserIdentity().equals(draftDocument.getOwner())) {
                    throw new WorkflowException("document already being edited");
                }
            }
            draftDocument.setOwner(getWorkflowContext().getUserIdentity());
            // make sure drafts nor their descendant nodes do not get indexed
            if (!draftDocument.getNode().isNodeType(HippoNodeType.NT_SKIPINDEX)) {
                draftDocument.getNode().addMixin(HippoNodeType.NT_SKIPINDEX);
            }
        } catch (RepositoryException ex) {
            throw new WorkflowException("Failed to obtain an editable instance", ex);
        }
        return draftDocument;
    }

    public Document commitEditableInstance() throws WorkflowException {
        log.info("commit editable instance of document ");
        try {
            if (draftDocument == null) {
                throw new WorkflowException("no draft version of publication");
            }
            draftDocument.setOwner(null);

            if (unpublishedDocument == null) {
                createUnpublished(draftDocument);
            } else {
                copyDocumentTo(draftDocument, unpublishedDocument);
            }

            unpublishedDocument.setAvailability(new String[]{"preview"});
            if (publishedDocument != null && publishedDocument.isAvailable("live")) {
                publishedDocument.setAvailability(new String[]{"live"});
            }
            unpublishedDocument.setModified(getWorkflowContext().getUserIdentity());
            return unpublishedDocument;
        } catch (RepositoryException ex) {
            throw new WorkflowException("failed to commit editable instance", ex);
        }
    }

    public Document disposeEditableInstance() throws WorkflowException {
        log.info("dispose editable instance on document ");
        try {
            if (draftDocument != null) {
                draftDocument.setOwner(null);
            }
            if (unpublishedDocument != null && unpublishedDocument.isAvailable("preview")) {
                return unpublishedDocument;
            } else if (publishedDocument != null) {
                return publishedDocument;
            } else {
                return null;
            }
        } catch (RepositoryException ex) {
            throw new WorkflowException("failed to dispose editable instance", ex);
        }
    }

    protected void createDraft() throws RepositoryException {
        Node draftNode = cloneDocumentNode(unpublishedDocument != null ? unpublishedDocument : publishedDocument);
        if (!draftNode.isNodeType(JcrConstants.MIX_REFERENCEABLE)) {
            draftNode.addMixin(JcrConstants.MIX_REFERENCEABLE);
        }
        draftDocument = new PublishableDocument(draftNode);
        draftDocument.setState(PublishableDocument.DRAFT);
        draftDocument.setAvailability(null);
        draftDocument.setModified(getWorkflowContext().getUserIdentity());
    }

    protected void createUnpublished(Document from) throws RepositoryException {
        final Node node = cloneDocumentNode(from);
        unpublishedDocument = new PublishableDocument(node);
        unpublishedDocument.setState(PublishableDocument.UNPUBLISHED);
        node.addMixin(HippoNodeType.NT_HARDDOCUMENT);
    }

    protected void createPublished() throws RepositoryException {
        final Node node = cloneDocumentNode(unpublishedDocument);
        if (!node.isNodeType(JcrConstants.MIX_REFERENCEABLE)) {
            node.addMixin(JcrConstants.MIX_REFERENCEABLE);
        }
        publishedDocument = new PublishableDocument(node);
        publishedDocument.setState(PublishableDocument.PUBLISHED);
    }

    public void requestDeletion() throws WorkflowException {
        log.info("deletion request on document ");
        if (current == null) {
            try {
                current = new PublicationRequest(PublicationRequest.DELETE, getNode(), unpublishedDocument, getWorkflowContext()
                        .getUserIdentity());
            } catch (RepositoryException e) {
                throw new WorkflowException("request deletion failure", e);
            }
        } else {
            throw new WorkflowException("request deletion failure");
        }
    }

    public void requestPublication() throws WorkflowException {
        log.info("publication request on document ");
        if (current == null) {
            try {
                current = new PublicationRequest(PublicationRequest.PUBLISH, getNode(), unpublishedDocument, getWorkflowContext()
                        .getUserIdentity());
            } catch (RepositoryException e) {
                throw new WorkflowException("request publication failure", e);
            }
        } else {
            throw new WorkflowException("publication request already pending");
        }
    }

    public void requestDepublication() throws WorkflowException {
        log.info("depublication request on document ");
        if (current == null) {
            try {
                current = new PublicationRequest(PublicationRequest.DEPUBLISH, getNode(), publishedDocument, getWorkflowContext()
                        .getUserIdentity());
            } catch (RepositoryException e) {
                throw new WorkflowException("request de-publication failure", e);
            }
        } else {
            throw new WorkflowException("publication request already pending");
        }
    }

    public void requestPublication(Date publicationDate) throws WorkflowException {
        log.info("publication request on document ");
        if (current == null) {
            try {
                current = new PublicationRequest(PublicationRequest.SCHEDPUBLISH, getNode(), unpublishedDocument, getWorkflowContext()
                        .getUserIdentity(), publicationDate);
            } catch (RepositoryException e) {
                throw new WorkflowException("request publication failure", e);
            }
        } else {
            throw new WorkflowException("publication request already pending");
        }
    }

    public void requestPublication(Date publicationDate, Date depublicationDate) throws WorkflowException {
        log.info("publication request on document ");
        throw new WorkflowException("unsupported");
    }

    public void requestDepublication(Date depublicationDate) throws WorkflowException {
        log.info("depublication request on document ");
        if (current == null) {
            try {
                current = new PublicationRequest(PublicationRequest.SCHEDDEPUBLISH, getNode(), publishedDocument, getWorkflowContext()
                        .getUserIdentity(), depublicationDate);
            } catch (RepositoryException e) {
                throw new WorkflowException("request de-publication failure", e);
            }
        } else {
            throw new WorkflowException("publication request already pending");
        }
    }
}
