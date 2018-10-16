/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.model;

import java.util.Objects;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.branch.BranchConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_NAME;
import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;
import static org.hippoecm.repository.util.WorkflowUtils.Variant.PUBLISHED;
import static org.hippoecm.repository.util.WorkflowUtils.Variant.UNPUBLISHED;
import static org.hippoecm.repository.util.WorkflowUtils.getDocumentVariantNode;
import static org.onehippo.repository.branch.BranchConstants.MASTER_BRANCH_ID;

public class BranchIdModel implements IModel<IModelReference<Pair<String, String>>> {

    private static final Pair<String, String> UNDEFINED_BRANCH_INFO = new ImmutablePair<>("undefined", "undefined");
    private static final Logger log = LoggerFactory.getLogger(BranchIdModel.class);

    private IModelReference<Pair<String, String>> branchIdModelReference;

    public BranchIdModel() {
    }

    public BranchIdModel(final IPluginContext context, final String handleIdentifier) {
        this();
        Objects.requireNonNull(context);
        Objects.requireNonNull(handleIdentifier);
        this.branchIdModelReference = getOrCreateModelReference(context, getReferenceModelIdentifier(handleIdentifier));
    }

    private static String getUserId() {
        return UserSession.get().getJcrSession().getUserID();
    }

    @Override
    public IModelReference<Pair<String, String>> getObject() {
        return branchIdModelReference;
    }

    @Override
    public void setObject(final IModelReference<Pair<String, String>> object) {
        this.branchIdModelReference = object;
    }

    public String getBranchId() {
        return isDefined() ? getBranchInfo().getLeft() : BranchConstants.MASTER_BRANCH_ID;
    }

    public String getBranchName() {
        return isDefined() ? getBranchInfo().getRight() : "core";
    }

    public Pair<String, String> getBranchInfo() {
        return branchIdModelReference.getModel().getObject();
    }

    public void setBranchInfo(final String branchId, final String branchName) {
        log.debug("Setting branch id and name to :{}, {}", branchId, branchName);
        this.branchIdModelReference.setModel(new Model<>(new ImmutablePair<>(branchId, branchName)));
    }

    public void setInitialBranchInfo(final String branchId, final String branchName) {
        if (UNDEFINED_BRANCH_INFO == branchIdModelReference.getModel().getObject()) {
            log.debug("Setting initial branch id and name to :{}, {}", branchId, branchName);
            setBranchInfo(branchId, branchName);
        }
    }

    public void destroy() {
        log.debug("Destroy branch id model reference");
        ((ModelReference) branchIdModelReference).destroy();
    }

    /**
     * Detaches model after use. This is generally used to null out transient references that can be
     * re-attached later.
     */
    @Override
    public void detach() {
    }

    public static BranchIdModel initialize(IPluginContext context, Node documentHandleNode) throws RepositoryException {
        Objects.requireNonNull(context);
        Objects.requireNonNull(documentHandleNode);
        if (!documentHandleNode.isNodeType(NT_HANDLE)) {
            throw new IllegalArgumentException("Node with id '" + documentHandleNode.getIdentifier() + "' is not of type " + NT_HANDLE);
        }
        final BranchIdModel branchIdModel = new BranchIdModel(context, documentHandleNode.getIdentifier());
        if (!branchIdModel.isDefined()) {
            log.debug("Initializing branch id model for identifier:{}", documentHandleNode.getIdentifier());
            branchIdModel.setInitialBranchInfo(MASTER_BRANCH_ID, "Core");
            final Node variant = getDocumentVariantNode(documentHandleNode, UNPUBLISHED)
                    .orElseGet(() -> getDocumentVariantNode(documentHandleNode, PUBLISHED).orElse(null));
            if (variant != null && variant.isNodeType(HippoNodeType.HIPPO_MIXIN_BRANCH_INFO)) {
                final String branchId = JcrUtils.getStringProperty(variant, HIPPO_PROPERTY_BRANCH_ID, MASTER_BRANCH_ID);
                final String branchName = JcrUtils.getStringProperty(variant, HIPPO_PROPERTY_BRANCH_NAME, "");
                branchIdModel.setBranchInfo(branchId, branchName);
            }
        }
        return branchIdModel;
    }

    @SuppressWarnings("unchecked")
    private IModelReference<Pair<String, String>> getOrCreateModelReference(IPluginContext context, String referenceModelIdentifier) {
        return Optional
                .ofNullable(getBranchIdModelReference(context, referenceModelIdentifier))
                .orElseGet(() -> createBranchIdModelReference(context, referenceModelIdentifier));
    }

    private IModelReference<Pair<String, String>> getBranchIdModelReference(final IPluginContext context, final String referenceModelIdentifier) {
        log.debug("Getting model reference for a model contain the branchId for identifier:{}", referenceModelIdentifier);
        return (IModelReference<Pair<String, String>>) context.getService(referenceModelIdentifier, IModelReference.class);
    }

    private IModelReference<Pair<String, String>> createBranchIdModelReference(final IPluginContext context, final String referenceModelIdentifier) {
        log.debug("Creating model reference for a model containing the branchId for identifier:{}", referenceModelIdentifier);
        ModelReference<Pair<String, String>> modelReference = new ModelReference<>(referenceModelIdentifier, new Model<>(UNDEFINED_BRANCH_INFO));
        modelReference.init(context);
        return modelReference;
    }

    private String getReferenceModelIdentifier(final String identifier) {
        return identifier + "." + getUserId();
    }


    public boolean isDefined() {
        return !UNDEFINED_BRANCH_INFO.equals(getBranchInfo());
    }
}
