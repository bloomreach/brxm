/*
 *  Copyright 2009-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.addon.workflow;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.model.BranchIdModel;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.standardworkflow.DocumentVariant;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MIXIN_BRANCH_INFO;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_NAME;
import static org.hippoecm.repository.standardworkflow.DocumentVariant.MASTER_BRANCH_ID;
import static org.hippoecm.repository.util.WorkflowUtils.Variant.PUBLISHED;
import static org.hippoecm.repository.util.WorkflowUtils.Variant.UNPUBLISHED;
import static org.hippoecm.repository.util.WorkflowUtils.getDocumentVariantNode;
import static org.onehippo.repository.util.JcrConstants.JCR_FROZEN_MIXIN_TYPES;

public class DocumentWorkflowManagerPlugin extends AbstractWorkflowManagerPlugin {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(DocumentWorkflowManagerPlugin.class);
    public static final String NO_MODEL_CONFIGURED = "No model configured";

    private IModelReference modelReference;
    private final IPluginContext context;
    private final IPluginConfig config;

    private boolean updateMenu = true;

    public DocumentWorkflowManagerPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        this.context = context;
        this.config = config;
        updateModelOnDocumentModelChange();
        onModelChanged();
    }

    private void updateModelOnDocumentModelChange() {
        if (config.getString(RenderService.MODEL_ID) != null) {
            modelReference = context.getService(config.getString(RenderService.MODEL_ID), IModelReference.class);
            if (modelReference != null) {
                //updateModel(modelReference.getModel());
                context.registerService(new IObserver<IModelReference>() {

                    private static final long serialVersionUID = 1L;

                    public IModelReference getObservable() {
                        return modelReference;
                    }

                    public void onEvent(Iterator<? extends IEvent<IModelReference>> event) {
                        updateModel(modelReference.getModel());
                    }
                }, IObserver.class.getName());
            }
        } else {
            modelReference = null;
            log.warn(NO_MODEL_CONFIGURED);
        }
    }

    @Override
    protected void onModelChanged() {
        super.onModelChanged();
        if (isObserving()) {
            updateMenu = true;
        }
    }

    @Override
    public void render(final PluginRequestTarget target) {
        if (updateMenu && isActive()) {
            updateMenu = false;
            Set<Node> nodeSet = new LinkedHashSet<>();
            try {
                if (getDefaultModel() instanceof JcrNodeModel) {
                    Node node = ((JcrNodeModel) getDefaultModel()).getNode();
                    if (node != null) {
                        if (node.isNodeType(HippoNodeType.NT_DOCUMENT)
                                && node.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                            Node handle = node.getParent();
                            nodeSet.add(handle);
                        } else {
                            nodeSet.add(updateModelForFrozenNodeWithCurrentBranchId(node));
                        }
                    }
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage(), ex);
            }
            MenuHierarchy menu = buildMenu(nodeSet, this.getPluginConfig());
            menu.restructure();
            replace(new MenuBar("menu", menu));

            if (target != null) {
                target.add(this);
            }
        }
        super.render(target);
    }

    private Node updateModelForFrozenNodeWithCurrentBranchId(final Node node) {
        try {
            final Node handle = getHandle(node);
            final BranchIdModel branchIdModel = new BranchIdModel(context, handle.getIdentifier());
            final String currentBranchId = branchIdModel.getBranchId();
            log.debug("Current branch id:{}", currentBranchId);
            if (isFrozenNode(node)) {
                final DocumentVariant documentVariant = new DocumentVariant(node);
                final String frozenBranchId = documentVariant.getBranchId();
                log.debug("Branch id of frozen node:{} is {}", handle.getPath(), frozenBranchId);
                if (branchIdModel.isDefined() && currentBranchId.equals(frozenBranchId)) {
                    log.debug("The documentModel(handle:{}) contains a frozen node:{} that has the same branchId as" +
                                    " the current branch id: {}, updating the documentModel with the associated handle."
                            , currentBranchId);
                    return handle;
                }
            }
        } catch (RepositoryException e) {
            log.warn(e.getMessage(), e);
        }
        return node;
    }

    private Node getHandle(final Node node) throws RepositoryException {
        return isFrozenNode(node) ? getVersionHandle(node) : node;
    }

    private Node getVersionHandle(final Node frozenNode) {
        try {
            final String uuid = frozenNode.getProperty(JcrConstants.JCR_FROZEN_UUID).getString();
            final Node variant = frozenNode.getSession().getNodeByIdentifier(uuid);
            return variant.getParent();
        } catch (final RepositoryException e) {
            log.warn(e.getMessage(), e);
            return frozenNode;
        }
    }

    private boolean isFrozenNode(final Node node) throws RepositoryException {
        return node.isNodeType(JcrConstants.NT_FROZEN_NODE);
    }

}
