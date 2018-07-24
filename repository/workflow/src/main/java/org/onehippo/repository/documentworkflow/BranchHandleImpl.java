/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.onehippo.repository.documentworkflow;

import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.DocumentVariant;
import org.hippoecm.repository.util.WorkflowUtils;
import org.onehippo.repository.branch.BranchHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.standardworkflow.DocumentVariant.MASTER_BRANCH_ID;
import static org.hippoecm.repository.util.WorkflowUtils.Variant.DRAFT;
import static org.hippoecm.repository.util.WorkflowUtils.Variant.PUBLISHED;
import static org.hippoecm.repository.util.WorkflowUtils.Variant.UNPUBLISHED;

public class BranchHandleImpl implements BranchHandle {

    private static final Logger log = LoggerFactory.getLogger(BranchHandleImpl.class);

    private final String branchId;
    private final DocumentHandle documentHandle;

    private BranchHandleImpl(final String branchId, final DocumentHandle documentHandle) {
        this.branchId = branchId;
        this.documentHandle = documentHandle;
    }

    public BranchHandleImpl(final String branchId, final Node handle) throws WorkflowException {
        this(branchId, newDocumentHandle(handle));
    }

    @Override
    public String getBranchId() {
        return branchId;
    }

    @Override
    public Node getPublished() {
        return getVariant(PUBLISHED)
                .map(DocumentVariant::getNode)
                .orElseGet(this::getPublishedMaster);
    }

    @Override
    public Node getUnpublished() {
        return getVariant(UNPUBLISHED)
                .map(DocumentVariant::getNode)
                .orElse(null);
    }

    @Override
    public Node getDraft() {
        return Optional.ofNullable(getDocumentVariant(DRAFT))
                .map(DocumentVariant::getNode)
                .orElse(null);
    }

    @Override
    public boolean isModified() {
        return getVariant(UNPUBLISHED)
                .map(unpublished ->
                        getVariant(PUBLISHED)
                                .map(published -> !isLive(published) || isModified(unpublished, published))
                                .orElse(true))
                .orElse(false);
    }

    @Override
    public boolean isMaster() {
        return this.branchId.equals("master");
    }

    @Override
    public boolean isLive() {
        return getVariant(PUBLISHED)
                .map(this::isLive)
                .orElse(false);
    }

    private Optional<DocumentVariant> getVariant(WorkflowUtils.Variant variant) {
        final DocumentVariant documentVariant = getDocumentVariant(variant);
        if (documentVariant == null || !isBranch(documentVariant)) {
            return getFrozenVariant(variant, getDocumentVariant(UNPUBLISHED));
        }
        if (isBranch(documentVariant)) {
            return Optional.of(documentVariant);
        } else {
            return Optional.empty();
        }
    }

    private Optional<DocumentVariant> getFrozenVariant(final WorkflowUtils.Variant variant, final DocumentVariant unpublished) {
        if (unpublished != null) {
            try {
                final VersionManager versionManager = unpublished.getNode().getSession().getWorkspace().getVersionManager();
                final VersionHistory versionHistory = versionManager.getVersionHistory(unpublished.getNode().getPath());
                final String versionLabel = branchId + "-" + variant.getState();
                if (versionHistory.hasVersionLabel(versionLabel)) {
                    final Version versionByLabel = versionHistory.getVersionByLabel(versionLabel);
                    return Optional.of(new DocumentVariant(versionByLabel.getFrozenNode()));
                }
            } catch (RepositoryException e) {
                log.error("Cannot get frozen node of document {} for branch {}, returning null",
                        unpublished.getIdentity(), branchId, e);
            }
        }
        return Optional.empty();
    }

    private boolean isModified(final DocumentVariant unpublished, final DocumentVariant published) {
        try {
            return unpublished.getLastModified().after(published.getLastModified());
        } catch (RepositoryException e) {
            log.error("Cannot determine if document {} is modified, returning false", unpublished.getIdentity(), e);
            return false;
        }
    }

    private boolean isLive(final DocumentVariant variant) {
        try {
            return WorkflowUtils.hasAvailability(variant.getNode(), "live");
        } catch (RepositoryException e) {
            log.error("Cannot determine if document {} is live, returning false", variant.getIdentity(), e);
            return false;
        }
    }

    private boolean isBranch(final DocumentVariant variant) {
        try {
            return variant.isBranch(branchId);
        } catch (RepositoryException e) {
            log.error("Cannot determine if document {} is branch {}, returning false", variant.getIdentity(), branchId, e);
            return false;
        }
    }

    private DocumentVariant getDocumentVariant(WorkflowUtils.Variant variant) {
        return documentHandle.getDocuments().get(variant.getState());
    }

    private Node getPublishedMaster() {
        return new BranchHandleImpl(MASTER_BRANCH_ID, documentHandle)
                .getVariant(PUBLISHED)
                .map(DocumentVariant::getNode)
                .orElse(null);
    }

    private static DocumentHandle newDocumentHandle(Node handleNode) throws WorkflowException {
        final DocumentHandle documentHandle = new DocumentHandle(handleNode);
        documentHandle.initialize();
        return documentHandle;
    }
}
