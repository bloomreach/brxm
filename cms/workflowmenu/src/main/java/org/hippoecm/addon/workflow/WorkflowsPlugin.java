/*
 *  Copyright 2009 Hippo.
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
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.repository.api.HippoNodeType;

public class WorkflowsPlugin extends AbstractWorkflowPlugin {

    private static final long serialVersionUID = 1L;

    private final IModelReference modelReference;

    private JcrNodeModel handleModel;
    private IObserver handleObserver = null;

    public WorkflowsPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

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
            log.warn("No model configured");
        }

        onModelChanged();
    }

    @Override
    protected void onModelChanged() {
        super.onModelChanged();
        redraw();
    }

    @Override
    protected void onBeforeRender() {
        Set<Node> nodeSet = new LinkedHashSet<Node>();
        MenuHierarchy menu = null;
        if (handleObserver != null) {
            getPluginContext().unregisterService(handleObserver, IObserver.class.getName());
            handleObserver = null;
            handleModel = null;
        }
        try {
            if (getDefaultModel() instanceof JcrNodeModel) {
                Node node = ((JcrNodeModel) getDefaultModel()).getNode();
                if (node != null) {
                    if (node.isNodeType(HippoNodeType.NT_DOCUMENT)
                            && node.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                        Node handle = node.getParent();
                        for (NodeIterator iter = handle.getNodes(); iter.hasNext();) {
                            node = iter.nextNode();
                            if (node != null) {
                                nodeSet.add(node);
                            }
                        }
                        handleModel = new JcrNodeModel(handle);
                        getPluginContext().registerService(handleObserver = new IObserver<JcrNodeModel>() {
                            public JcrNodeModel getObservable() {
                                return handleModel;
                            }

                            public void onEvent(Iterator<? extends IEvent<JcrNodeModel>> event) {
                                onModelChanged();
                            }
                        }, IObserver.class.getName());
                    } else {
                        nodeSet.add(node);
                    }
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage(), ex);
        }
        menu = buildMenu(nodeSet);
        menu.restructure();
        addOrReplace(new MenuBar("menu", menu));

        super.onBeforeRender();
    }

    @Override
    protected void onDetach() {
        if (handleModel != null) {
            handleModel.detach();
        }
        super.onDetach();
    }
}
