/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.frontend.editor;

import java.util.Map;
import java.util.Objects;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.branch.BranchConstants;
import org.onehippo.repository.branch.BranchHandle;
import org.onehippo.repository.documentworkflow.BranchHandleImpl;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.documentworkflow.DocumentVariant;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.HippoStdNodeType.DRAFT;
import static org.hippoecm.repository.HippoStdNodeType.PUBLISHED;
import static org.hippoecm.repository.HippoStdNodeType.UNPUBLISHED;

/**
 * Maps a {@link javax.jcr.Node} to a {@link Document}.
 */
public class DocumentBuilder {

    public static final Logger log = LoggerFactory.getLogger(DocumentBuilder.class);

    private DocumentImpl document;
    private Node node;
    private String branchId;
    private String userId;
    private Node handleNode;
    private DocumentHandle documentHandle;
    private BranchHandle branchHandle;

    private DocumentBuilder() {
        branchId = BranchConstants.MASTER_BRANCH_ID;
        document = new DocumentImpl();
        userId = StringUtils.EMPTY;
    }

    public static DocumentBuilder create() {
        return new DocumentBuilder();
    }

    public DocumentBuilder branchId(final String branchId) {
        Objects.requireNonNull(branchId);
        this.branchId = branchId;
        return this;
    }

    public DocumentBuilder node(final Node node) {
        Objects.requireNonNull(node);
        this.node = node;
        return this;
    }

    public DocumentBuilder userId(String userId) {
        Objects.requireNonNull(userId);
        this.userId = userId;
        return this;
    }

    /**
     * Build {@link Document} based on node.
     *
     * @return {@link Document} representation of the supplied node ( See {@link #node(Node)}
     * @throws EditorException
     */
    public Document build() throws EditorException {
        Objects.requireNonNull(node);
        Objects.requireNonNull(userId);
        Objects.requireNonNull(branchId);
        log.debug("Building document for node : { path : {} }", JcrUtils.getNodePathQuietly(node));
        try {
            buildRevision();
            buildHandle();
        } catch (RepositoryException | WorkflowException e) {
            throw new EditorException("Something went wrong  when reading the document", e);
        }
        log.debug("Node : { path : {} } maps to Document : {}", JcrUtils.getNodePathQuietly(node), document);
        return document;

    }

    protected void buildRevision() throws RepositoryException, EditorException {
        if (isVersion()) {
            log.debug("Node : { path : {} } is a revision, building revision", node.getPath());
            final Node frozenNode = node.getNode(JcrConstants.JCR_FROZEN_NODE);
            document.setRevision(frozenNode.getPath());
            handleNode = getVersionHandle(frozenNode);
        } else {
            handleNode = node;
        }
    }

    protected boolean isVersion() throws RepositoryException {
        return node.isNodeType(JcrConstants.NT_VERSION);
    }

    protected Node getVersionHandle(final Node frozenNode) throws RepositoryException, EditorException {
        final String uuid = frozenNode.getProperty(JcrConstants.JCR_FROZEN_UUID).getString();
        try {
            final Node variant = node.getSession().getNodeByIdentifier(uuid);
            final Node parent = variant.getParent();
            log.debug("Found associated unpublished variant : { path : {} } for frozen Node : { path : {} }"
                    , parent.getPath(), frozenNode.getPath());
            return parent;
        } catch (ItemNotFoundException e) {
            String message = "Associated unpublished variant: { identifier : %s } or its handle " +
                    "for frozenNode : { path : %s } cannot be found";
            throw new EditorException(String.format(message, uuid, frozenNode.getPath()));
        }
    }

    protected void buildHandle() throws WorkflowException, RepositoryException {
        documentHandle = new DocumentHandle(handleNode);
        documentHandle.initialize(branchId);
        branchHandle = new BranchHandleImpl(branchId, documentHandle);
        final Map<String, DocumentVariant> documents = documentHandle.getDocuments();
        for (String key : documents.keySet()) {
            final DocumentVariant documentVariant = documents.get(key);
            if (documentVariant == null ){
                break;
            }
            if (DRAFT.equals(key) ){
                buildDraft(documentVariant.getNode());
                final Boolean transferable = documentVariant.isTransferable();
                document.setTransferable(transferable == null ? false : transferable);
                log.debug("Transferable: {}", document.isTransferable());
                document.setHolder(this.userId.equals(documentVariant.getHolder()));
                log.debug("Holder is current user: {}", document.isHolder());
            }
            if (UNPUBLISHED.equals(key)){
                buildUnpublished(documentVariant.getNode());
            }
            if (PUBLISHED.equals(key)){
                buildPublished(documentVariant.getNode());
            }
        }
    }

    protected void buildDraft(final Node child)
            throws RepositoryException {
        log.debug("Build draft variant : { path : {} }", child.getPath());
        if (hasRevision()) {
            log.debug("The document represents a revision, so the document does not have draft, " +
                    "the published and/or unpublished variants are relevant because they are used " +
                    "to compare the revision with the (un)published variant");
            return;
        }
        log.debug("The document does not represent a revision,  building draft");
        Node draft = branchHandle.getDraft();
        if (draft == null) {
            draft = child;
        }
        document.setDraft(draft.getPath());


    }

    private boolean hasRevision() {
        return !StringUtils.EMPTY.equals(document.getRevision());
    }

    protected void buildPublished(final Node child)
            throws RepositoryException {
        // if there is no published for the branch we fallback to master published and if there is
        // no master published we just use child
        log.debug("Build published variant : { path : {} }", child.getPath());
        Node published = branchHandle.getPublished();
        if (published == null) {
            published = branchHandle.getPublishedMaster();
        }
        if (published == null) {
            published = child;
        }
        document.setPublished(published.getPath());
    }

    protected void buildUnpublished(final Node child)
            throws RepositoryException {
        log.debug("Build unpublished variant : { path : {} }", child.getPath());
        Node unpublished = branchHandle.getUnpublished();
        if (unpublished == null) {
            unpublished = child;
        }
        document.setUnpublished(unpublished.getPath());
    }


}
