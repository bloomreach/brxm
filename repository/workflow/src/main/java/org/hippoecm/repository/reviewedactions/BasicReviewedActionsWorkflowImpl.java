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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.hippoecm.repository.util.CopyHandler;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeInfo;
import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.OverwritingCopyHandler;
import org.hippoecm.repository.util.PropInfo;
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
            HippoStdPubWfNodeType.HIPPOSTDPUBWF_PUBLICATION_DATE
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
        try {
            final String userIdentity = super.getWorkflowContext().getUserIdentity();
            final String state = JcrUtils.getStringProperty(getNode(), HippoStdNodeType.HIPPOSTD_STATE, "");

            boolean draftInUse = draftDocument != null && draftDocument.getOwner() != null && !draftDocument.getOwner().equals(userIdentity);
            boolean unpublishedDirty = unpublishedDocument != null &&
                    (publishedDocument == null
                            || publishedDocument.getLastModificationDate().getTime() == 0
                            || !publishedDocument.getLastModificationDate().equals(unpublishedDocument.getLastModificationDate()));
            boolean publishedLive = publishedDocument != null && publishedDocument.isAvailable("live");
            boolean pendingRequest = current != null;

            boolean status = !draftInUse;
            boolean editable = !draftInUse && !pendingRequest;
            boolean publishable = unpublishedDirty && !pendingRequest;
            boolean depublishable = publishedLive && !pendingRequest;
            Boolean deleteable = !publishedLive;

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

            if (PublishableDocument.DRAFT.equals(state) && unpublishedDocument != null) {
                Node draftNode = getWorkflowContext().getUserSession().getNodeByIdentifier(draftDocument.getIdentity());
                Node unpublishedNode = unpublishedDocument.getNode();
                info.put("modified", !equals(draftNode, unpublishedNode));
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
        JcrUtils.ensureIsCheckedOut(parent);

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
        JcrUtils.ensureIsCheckedOut(document.getNode());
        JcrUtils.ensureIsCheckedOut(document.getNode().getParent());
        document.getNode().remove();
    }

    protected void copyDocumentTo(Document source, Document target) throws RepositoryException {
        copyTo(source.getNode(), target.getNode());
    }

    protected boolean equals(Node a, Node b) throws RepositoryException {
        final boolean virtualA = JcrUtils.isVirtual(a);
        if (virtualA != JcrUtils.isVirtual(b)) {
            return false;
        } else if (virtualA) {
            return true;
        }

        final PropertyIterator aProperties = a.getProperties();
        final PropertyIterator bProperties = b.getProperties();

        Map<String, Property> properties = new HashMap<>();
        for (Property property : new PropertyIterable(aProperties)) {
            final String name = property.getName();
            if (Arrays.binarySearch(PROTECTED_PROPERTIES, name) >= 0) {
                continue;
            }
            if (property.getDefinition().isProtected()) {
                continue;
            }
            if (!b.hasProperty(name)) {
                return false;
            }

            properties.put(name, property);
        }
        for (Property bProp : new PropertyIterable(bProperties)) {
            final String name = bProp.getName();
            if (Arrays.binarySearch(PROTECTED_PROPERTIES, name) >= 0) {
                continue;
            }
            if (bProp.getDefinition().isProtected()) {
                continue;
            }
            if (!properties.containsKey(name)) {
                return false;
            }

            Property aProp = properties.get(name);
            if (!equals(bProp, aProp)) {
                return false;
            }
        }

        NodeIterator aIter = a.getNodes();
        NodeIterator bIter = b.getNodes();
        if (aIter.getSize() != bIter.getSize()) {
            return false;
        }
        while (aIter.hasNext()) {
            Node aChild = aIter.nextNode();
            Node bChild = bIter.nextNode();
            if (!equals(aChild, bChild)) {
                return false;
            }
        }
        return true;
    }

    private boolean equals(final Property bProp, final Property aProp) throws RepositoryException {
        if (aProp.isMultiple() != bProp.isMultiple() || aProp.getType() != bProp.getType()) {
            return false;
        }

        if (aProp.isMultiple()) {
            Value[] aValues = aProp.getValues();
            Value[] bValues = bProp.getValues();
            if (aValues.length != bValues.length) {
                return false;
            }
            for (int i = 0; i < aValues.length; i++) {
                if (!equals(aValues[i], bValues[i])) {
                    return false;
                }
            }
        } else {
            Value aValue = aProp.getValue();
            Value bValue = bProp.getValue();
            if (!equals(aValue, bValue)) {
                return false;
            }
        }
        return true;
    }

    private boolean equals(final Value aValue, final Value bValue) throws RepositoryException {
        return aValue.getString().equals(bValue.getString());
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
        final CopyHandler chain = new OverwritingCopyHandler(destNode) {

            @Override
            public void startNode(final NodeInfo nodeInfo) throws RepositoryException {
                String[] oldMixins = nodeInfo.getMixinNames();
                Set<String> mixins = new HashSet<>();
                for (String mixin : oldMixins) {
                    if (Arrays.binarySearch(PROTECTED_MIXINS, mixin) >= 0) {
                        continue;
                    }
                    mixins.add(mixin);
                }
                String[] newMixins = mixins.toArray(new String[mixins.size()]);
                final NodeInfo newInfo = new NodeInfo(nodeInfo.getName(), nodeInfo.getIndex(), nodeInfo.getNodeTypeName(), newMixins);
                super.startNode(newInfo);
            }

            @Override
            protected void removeProperties(final Node node) throws RepositoryException {
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
            }

            @Override
            protected void replaceMixins(final Node node, final NodeInfo nodeInfo) throws RepositoryException {
                Set<String> mixinSet = new TreeSet<>();
                Collections.addAll(mixinSet, nodeInfo.getMixinNames());
                for (NodeType nodeType : node.getMixinNodeTypes()) {
                    final String mixinName = nodeType.getName();
                    if (!mixinSet.contains(mixinName)) {
                        if (Arrays.binarySearch(PROTECTED_MIXINS, mixinName) < 0) {
                            node.removeMixin(mixinName);
                        }
                    } else {
                        mixinSet.remove(mixinName);
                    }
                }
                for (String mixinName : mixinSet) {
                    node.addMixin(mixinName);
                }
            }

            @Override
            public void setProperty(final PropInfo propInfo) throws RepositoryException {
                String name = propInfo.getName();
                if (Arrays.binarySearch(PROTECTED_PROPERTIES, name) >= 0) {
                    return;
                }
                super.setProperty(propInfo);
            }
        };
        JcrUtils.copyTo(srcNode, chain);

        return destNode;
    }

    public Document obtainEditableInstance() throws WorkflowException {
        log.info("obtain editable instance on document ");
        try {
            if (draftDocument == null) {
                if (current != null) {
                    throw new WorkflowException("unable to edit document with pending operation");
                }
                createDraft();
            } else {
                if (draftDocument.getOwner() != null && !getWorkflowContext().getUserIdentity().equals(draftDocument.getOwner())) {
                    throw new WorkflowException("document already being edited");
                }
                if (unpublishedDocument != null) {
                    copyDocumentTo(unpublishedDocument, draftDocument);
                }
            }
            draftDocument.setOwner(getWorkflowContext().getUserIdentity());
            // make sure drafts nor their descendant nodes get indexed
            final Node draftNode = draftDocument.getNode();
            if (!draftNode.isNodeType(HippoNodeType.NT_SKIPINDEX)) {
                draftNode.addMixin(HippoNodeType.NT_SKIPINDEX);
            }
            if (draftNode.isNodeType(HippoNodeType.NT_HARDDOCUMENT)) {
                draftNode.removeMixin(HippoNodeType.NT_HARDDOCUMENT);
                draftNode.addMixin(JcrConstants.MIX_REFERENCEABLE);
            }
            return toUserDocument(draftDocument);
        } catch (RepositoryException ex) {
            throw new WorkflowException("Failed to obtain an editable instance", ex);
        }
    }

    protected Document toUserDocument(Document document) throws RepositoryException {
        return new Document(getWorkflowContext().getUserSession().getNodeByIdentifier(document.getIdentity()));
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
            } else if (!unpublishedDocument.isAvailable("preview")) {
                unpublishedDocument.setAvailability(new String[] {"preview"});
            }

            if (!equals(draftDocument.getNode(), unpublishedDocument.getNode())) {
                copyDocumentTo(draftDocument, unpublishedDocument);
                unpublishedDocument.setModified(getWorkflowContext().getUserIdentity());
            }

            return toUserDocument(unpublishedDocument);
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
                return toUserDocument(unpublishedDocument);
            } else if (publishedDocument != null) {
                return toUserDocument(publishedDocument);
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
        draftNode.getSession().save();
    }

    protected void createUnpublished(Document from) throws RepositoryException {
        final Node node = cloneDocumentNode(from);
        unpublishedDocument = new PublishableDocument(node);
        unpublishedDocument.setState(PublishableDocument.UNPUBLISHED);
        unpublishedDocument.setAvailability(new String[]{"preview"});
        if (publishedDocument != null) {
            if (publishedDocument.isAvailable("live")) {
                publishedDocument.setAvailability(new String[]{"live"});
            } else {
                publishedDocument.setAvailability(new String[]{});
            }
        }
        node.addMixin(JcrConstants.MIX_VERSIONABLE);
        node.getSession().save();
    }

    protected void createPublished() throws RepositoryException {
        final Node node = cloneDocumentNode(unpublishedDocument);
        if (!node.isNodeType(JcrConstants.MIX_REFERENCEABLE)) {
            node.addMixin(JcrConstants.MIX_REFERENCEABLE);
        }
        publishedDocument = new PublishableDocument(node);
        publishedDocument.setState(PublishableDocument.PUBLISHED);
        node.getSession().save();
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
