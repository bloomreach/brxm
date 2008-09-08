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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.IClusterable;
import org.apache.wicket.Session;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IPluginControl;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.service.IEditService;
import org.hippoecm.frontend.service.IFactoryService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentViewPlugin implements IPlugin, IEditService, IDetachable {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(DocumentViewPlugin.class);

    private IPluginContext context;
    private IPluginConfig config;
    private IPluginControl viewer;
    private IFactoryService factory;
    private String clusterEditorId;

    public DocumentViewPlugin(IPluginContext context, IPluginConfig config) {
        this.context = context;
        this.config = config;

        context.registerService(this, config.getString(EDITOR_ID));
    }

    public void detach() {
        config.detach();
    }

    public void edit(IModel handle) {
        JcrNodeModel draft = getDraftModel((JcrNodeModel) handle);
        if (draft != null) {
            openEditor(draft);
        } else {
            JcrNodeModel preview = getPreviewModel((JcrNodeModel) handle);
            if (preview != null) {
                openPreview(preview);
            } else {
                log.error("No draft or unpublished version found of document");
                return;
            }
        }
    }

    void stopCluster() {
        viewer.stopPlugin();
        context.unregisterService(factory, clusterEditorId);

        viewer = null;
        factory = null;
    }

    void openEditor(JcrNodeModel model) {
        IPluginConfigService pluginConfigService = context.getService(IPluginConfigService.class.getName(),
                IPluginConfigService.class);
        IClusterConfig clusterConfig = pluginConfigService.getCluster(config.getString("cluster.editor.name"));
        viewer = context.start(clusterConfig);

        String editorId = clusterConfig.getString("editor.id");
        IEditService editService = context.getService(editorId, IEditService.class);
        clusterEditorId = context.getReference(editService).getServiceId();

        // register as the factory for the view service
        factory = new IFactoryService() {
            private static final long serialVersionUID = 1L;

            public void delete(IClusterable service) {
                stopCluster();

                // also delete the cluster that started this plugin
                String myId = context.getReference(DocumentViewPlugin.this).getServiceId();
                IFactoryService container = context.getService(myId, IFactoryService.class);
                container.delete(DocumentViewPlugin.this);
            }
        };
        context.registerService(factory, clusterEditorId);

        editService.edit(model);
    }

    void openPreview(JcrNodeModel preview) {
        IPluginConfigService pluginConfigService = context.getService(IPluginConfigService.class.getName(),
                IPluginConfigService.class);
        IClusterConfig clusterConfig = pluginConfigService.getCluster(config.getString("cluster.preview.name"));

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
                String myId = context.getReference(DocumentViewPlugin.this).getServiceId();
                IFactoryService container = context.getService(myId, IFactoryService.class);
                container.delete(DocumentViewPlugin.this);
            }
        };
        context.registerService(factory, clusterEditorId);

        viewService.edit(preview);
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
                        if (child.hasProperty("hippostd:state") && child.getProperty("hippostd:state").getString().equals("draft")
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