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

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentWorkflowManagerPlugin extends AbstractWorkflowManagerPlugin {

    private static final String NO_MODEL_CONFIGURED = "No model configured";
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(DocumentWorkflowManagerPlugin.class);

    private IModelReference modelReference;
    private boolean updateMenu = true;
    private final IPluginContext context;
    private final IPluginConfig config;



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
                            nodeSet.add(node);
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


}
