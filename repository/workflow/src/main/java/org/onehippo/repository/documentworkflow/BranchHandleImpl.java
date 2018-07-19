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

import static org.hippoecm.repository.util.WorkflowUtils.Variant.DRAFT;
import static org.hippoecm.repository.util.WorkflowUtils.Variant.PUBLISHED;
import static org.hippoecm.repository.util.WorkflowUtils.Variant.UNPUBLISHED;

public class BranchHandleImpl implements BranchHandle {

    private static final Logger log = LoggerFactory.getLogger(BranchHandleImpl.class);

    private final String branchId;
    private final DocumentHandle documentHandle;

    BranchHandleImpl(final String branchId, final DocumentHandle documentHandle) throws WorkflowException {
        this.branchId = branchId;
        this.documentHandle = documentHandle;
        this.documentHandle.initialize();
    }

    public BranchHandleImpl(final String branchId, final Node handle) throws WorkflowException {
        this(branchId, new DocumentHandle(handle));
    }

    @Override
    public Node getPublished() {
        return getNode(getVariant(PUBLISHED)
                .orElse(null));
    }

    @Override
    public Node getUnpublished() {
        return getNode(getVariant(UNPUBLISHED)
                .orElse(null));
    }

    @Override
    public Node getDraft() {
        return getVariant(DRAFT)
                .map(DocumentVariant::getNode)
                .orElse(null);
    }

    @Override
    public boolean isModified() {
        return getVariant(UNPUBLISHED)
                .map(unpublished ->
                        getVariant(PUBLISHED)
                                .map(published -> isModified(unpublished, published))
                                .orElse(false))
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
        return Optional.ofNullable(documentHandle.getDocuments().get(variant.getState()));
    }

    private Node getNode(DocumentVariant variant) {

        if (variant == null) {
            return null;
        }

        if (isMaster(variant) || isBranch(variant)) {
            return variant.getNode();
        }

        return getVariant(UNPUBLISHED)
                .map(unpublished -> getFrozenNode(variant, unpublished))
                .orElse(null);

    }

    private Node getFrozenNode(final DocumentVariant variant, final DocumentVariant unpublished) {
        try {
            final VersionManager versionManager = variant.getNode().getSession().getWorkspace().getVersionManager();
            final VersionHistory versionHistory = versionManager.getVersionHistory(unpublished.getNode().getPath());
            final Version versionByLabel = versionHistory.getVersionByLabel(branchId);
            return versionByLabel.getFrozenNode();
        } catch (RepositoryException e) {
            log.error("Cannot get frozen node of document {} for branch {}, returning null",
                    unpublished.getIdentity(), branchId, e);
            return null;
        }
    }

    private boolean isModified(final DocumentVariant unpublished, final DocumentVariant published) {
        try {
            return unpublished.getLastModified().after(published.getLastModified());
        } catch (RepositoryException e) {
            log.error("Cannot determine if document {} is modified, returning false", unpublished.getIdentity(), e);
            return false;
        }
    }

    private boolean isLive(final DocumentVariant published) {
        try {
            return Stream
                    .of(published.getAvailability())
                    .anyMatch(a -> a.equals("live"));
        } catch (RepositoryException e) {
            log.error("Cannot determine if document {} is live, returning false", published.getIdentity(), e);
            return false;
        }
    }

    private boolean isMaster(final DocumentVariant unpublished) {
        try {
            return unpublished.isMaster();
        } catch (RepositoryException e) {
            log.error("Cannot determine if document {} is master, returning false", unpublished.getIdentity(), e);
            return false;
        }
    }

    private boolean isBranch(final DocumentVariant unpublished) {
        try {
            return unpublished.isBranch(branchId);
        } catch (RepositoryException e) {
            log.error("Cannot determine if document {} is branch {}, returning false", unpublished.getIdentity(), branchId, e);
            return false;
        }
    }
}
