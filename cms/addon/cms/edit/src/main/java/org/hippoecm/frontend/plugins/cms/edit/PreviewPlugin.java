/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.plugins.cms.edit;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.IClusterable;
import org.apache.wicket.Session;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.IJcrNodeModelListener;
import org.hippoecm.frontend.model.IModelListener;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IPluginControl;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.service.IEditService;
import org.hippoecm.frontend.service.IFactoryService;
import org.hippoecm.frontend.service.IJcrService;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreviewPlugin implements IPlugin, IModelListener, IJcrNodeModelListener, IDetachable {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(PreviewPlugin.class);

    private IPluginContext context;
    private IPluginConfig config;
    private IPluginControl viewer;
    private IFactoryService factory;
    private String clusterEditorId;
    private JcrNodeModel previewHandle;

    public PreviewPlugin(IPluginContext context, IPluginConfig config) {
        this.context = context;
        this.config = config;

        context.registerService(this, IJcrService.class.getName());

        if (config.getString(RenderService.MODEL_ID) != null) {
            context.registerService(this, config.getString(RenderService.MODEL_ID));
        } else {
            log.warn("No model defined ({})", RenderService.MODEL_ID);
        }
    }

    public void detach() {
        config.detach();
    }

    public void updateModel(IModel handle) {
        JcrNodeModel handleModel = (JcrNodeModel) handle;
        if (handleModel != null && handleModel.getNode() != null) {
            JcrNodeModel draft = getDraftModel(handleModel);
            if (draft != null) {
                stopCluster();
                openEditor(draft);
                return;
            }
            JcrNodeModel preview = getPreviewModel(handleModel);
            if (preview != null) {
                stopCluster();
                openPreview(preview);
            } else {
                log.error("No preview version found of document");
            }
        }
    }

    public void onFlush(JcrNodeModel nodeModel) {
        if (previewHandle != null) {
            if (previewHandle.equals(nodeModel)) {
                updateModel(previewHandle);
            }
        }
    }

    void stopCluster() {
        if (viewer != null) {
            viewer.stopPlugin();
            context.unregisterService(factory, clusterEditorId);

            viewer = null;
            factory = null;
            previewHandle = null;
        }
    }

    void openEditor(JcrNodeModel model) {
        String editorId = config.getString("editor.id");
        IEditService editService = context.getService(editorId, IEditService.class);
        if (editService != null) {
            editService.edit(model);
        }
    }

    void openPreview(JcrNodeModel preview) {
        previewHandle = preview.getParentModel();

        IPluginConfigService pluginConfigService = context.getService(IPluginConfigService.class.getName(),
                IPluginConfigService.class);
        IClusterConfig clusterConfig = pluginConfigService.getCluster(config.getString("cluster.name"));

        // register self as edit service
        final String editorId = clusterConfig.getString("editor.id");
        final IEditService editService = new IEditService() {
            private static final long serialVersionUID = 1L;

            public void edit(IModel model) {
                stopCluster();

                context.unregisterService(this, editorId);

                openEditor((JcrNodeModel) model);
            }
        };
        context.registerService(editService, editorId);

        viewer = context.start(clusterConfig);

        String viewerId = clusterConfig.getString("viewer.id");
        IEditService viewService = context.getService(viewerId, IEditService.class);

        // register as the factory for the view service
        clusterEditorId = context.getReference(viewService).getServiceId();
        factory = new IFactoryService() {
            private static final long serialVersionUID = 1L;

            public void delete(IClusterable service) {
                stopCluster();

                context.unregisterService(editService, editorId);

                // also delete the cluster that contains this plugin
                String myId = context.getReference(PreviewPlugin.this).getServiceId();
                IFactoryService container = context.getService(myId, IFactoryService.class);
                container.delete(PreviewPlugin.this);
            }
        };
        context.registerService(factory, clusterEditorId);

        viewService.edit(preview);

        // look up the render service that is created by the cluster
        final String wicketId = clusterConfig.getString("wicket.id");
        List<IRenderService> targetServices = context.getServices(wicketId, IRenderService.class);
        List<IRenderService> clusterServices = context.getServices(context.getReference(viewer).getServiceId(),
                IRenderService.class);
        for (IRenderService target : targetServices) {
            if (clusterServices.contains(target)) {
                // found it!
                target.focus(null);
                break;
            }
        }
    }

    JcrNodeModel getPreviewModel(JcrNodeModel handle) {
        try {
            Node handleNode = handle.getNode();
            if (handleNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                for (NodeIterator iter = handleNode.getNodes(); iter.hasNext();) {
                    Node child = iter.nextNode();
                    if (child.getName().equals(handleNode.getName())) {
                        // FIXME: This has knowledge of hippostd reviewed actions, which here is not fundamentally wrong, but could raise hairs
                        if (child.hasProperty("hippostd:state")
                                && child.getProperty("hippostd:state").getString().equals("unpublished")) {
                            return new JcrNodeModel(child);
                        }
                    }
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    JcrNodeModel getDraftModel(JcrNodeModel handle) {
        String user = ((UserSession) Session.get()).getCredentials().getString("username");
        try {
            Node handleNode = handle.getNode();
            if (handleNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                for (NodeIterator iter = handleNode.getNodes(); iter.hasNext();) {
                    Node child = iter.nextNode();
                    if (child.getName().equals(handleNode.getName())) {
                        // FIXME: This has knowledge of hippostd reviewed actions, which here is not fundamentally wrong, but could raise hairs
                        if (child.hasProperty("hippostd:state")
                                && child.getProperty("hippostd:state").getString().equals("draft")
                                && child.getProperty("hippostd:holder").getString().equals(user)) {
                            return new JcrNodeModel(child);
                        }
                    }
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

}