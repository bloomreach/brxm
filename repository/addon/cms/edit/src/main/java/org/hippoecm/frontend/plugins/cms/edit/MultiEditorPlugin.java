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

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.IClusterable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.dialog.ExceptionDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IPluginControl;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.service.IFactoryService;
import org.hippoecm.frontend.service.IViewService;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiEditorPlugin implements IPlugin, IViewService, IClusterable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public static final Logger log = LoggerFactory.getLogger(MultiEditorPlugin.class);

    public static final String CLUSTER = "cluster.name";

    private static class PluginEntry implements IClusterable {
        private static final long serialVersionUID = 1L;
        String id;
        IPluginControl plugin;
        IViewService viewer;
        IFactoryService factory;
    }

    private IPluginContext context;
    private IPluginConfig config;
    private Map<IModel, PluginEntry> editors;
    private int editCount;

    public MultiEditorPlugin(IPluginContext context, IPluginConfig config) {
        editors = new HashMap<IModel, PluginEntry>();
        editCount = 0;

        this.context = context;
        this.config = config;

        if (config.getString(CLUSTER) == null) {
            log.error("No cluster ({}) defined", CLUSTER);
        }

        if (config.getString(VIEWER_ID) != null) {
            context.registerService(this, config.getString(VIEWER_ID));
        } else {
            log.error("No editor id ({}) defined under which to register", VIEWER_ID);
        }
    }

    public void view(final IModel model) {
        IViewService viewer;
        if (!editors.containsKey(model)) {
            IPluginConfigService pluginConfigService = context.getService("service.plugin.config",
                    IPluginConfigService.class);
            IClusterConfig clusterConfig = pluginConfigService.getCluster(config.getString(CLUSTER));
            String viewerId = clusterConfig.getString(VIEWER_ID);

            clusterConfig.put(RenderService.WICKET_ID, config.getString(RenderService.WICKET_ID));
            clusterConfig.put(RenderService.DIALOG_ID, config.getString(RenderService.DIALOG_ID));
            IPluginControl plugin = context.start(clusterConfig);

            viewer = context.getService(viewerId, IViewService.class);
            String serviceId = context.getReference(viewer).getServiceId();

            // register as the factory for the view service
            IFactoryService factory = new IFactoryService() {
                private static final long serialVersionUID = 1L;

                public void delete(IClusterable service) {
                    IViewService viewer = (IViewService) service;
                    Map.Entry<IModel, PluginEntry> entry = getPluginEntry(viewer);
                    if (entry != null) {
                        IDialogService dialogService = context.getService(config.getString(RenderService.DIALOG_ID),
                                IDialogService.class);

                        JcrNodeModel nodeModel = (JcrNodeModel) entry.getKey();
                        if (nodeModel.getItemModel().exists()) {
                            try {
                                Node node = nodeModel.getNode();
                                HippoSession session = (HippoSession) node.getSession();
                                if (dialogService != null && session.pendingChanges(node, "nt:base").hasNext()) {
                                    dialogService.show(new OnCloseDialog(context, dialogService, (JcrNodeModel) entry
                                            .getKey(), MultiEditorPlugin.this, viewer));
                                } else {
                                    deleteEditor(viewer);
                                }
                            } catch (RepositoryException e) {
                                if (dialogService != null) {
                                    dialogService.show(new ExceptionDialog(context, dialogService, e.getMessage()));
                                    log.error(e.getClass().getName() + ": " + e.getMessage());
                                } else {
                                    log.error(e.getMessage());
                                }
                            }
                        } else {
                            deleteEditor(viewer);
                        }
                    }
                }
            };
            context.registerService(factory, serviceId);

            PluginEntry entry = new PluginEntry();
            entry.plugin = plugin;
            entry.id = serviceId;
            entry.viewer = viewer;
            entry.factory = factory;
            editors.put(model, entry);

            editCount++;
        } else {
            viewer = editors.get(model).viewer;
        }

        if (viewer != null) {
            viewer.view(model);
        } else {
            log.warn("Configured editor does not provide an IViewService");
        }
    }

    public void deleteEditor(IViewService service) {
        Map.Entry<IModel, PluginEntry> entry = getPluginEntry(service);
        if (entry != null) {
            String serviceId = entry.getValue().id;
            context.unregisterService(entry.getValue().factory, serviceId);

            entry.getValue().plugin.stopPlugin();
            editors.remove(entry.getKey());
        } else {
            log.error("unknown editor " + service + ".  Delete is ignored");
        }
    }

    private Map.Entry<IModel, PluginEntry> getPluginEntry(IViewService service) {
        for (Map.Entry<IModel, PluginEntry> entry : editors.entrySet()) {
            if (entry.getValue().viewer.equals(service)) {
                return entry;
            }
        }
        return null;
    }
}
