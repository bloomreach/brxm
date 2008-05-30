/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.cms.edit.sa;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.IClusterable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.sa.dialog.ExceptionDialog;
import org.hippoecm.frontend.sa.dialog.IDialogService;
import org.hippoecm.frontend.sa.plugin.IPlugin;
import org.hippoecm.frontend.sa.plugin.IPluginContext;
import org.hippoecm.frontend.sa.plugin.IPluginControl;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.hippoecm.frontend.sa.plugin.config.impl.JavaClusterConfig;
import org.hippoecm.frontend.sa.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.sa.service.IFactoryService;
import org.hippoecm.frontend.sa.service.IRenderService;
import org.hippoecm.frontend.sa.service.IViewService;
import org.hippoecm.frontend.sa.service.render.RenderService;
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiEditorPlugin implements IPlugin, IViewService, IFactoryService, IClusterable {
    private static final long serialVersionUID = 1L;

    public static final Logger log = LoggerFactory.getLogger(MultiEditorPlugin.class);

    public static final String EDITOR_ID = "editor.id";
    public static final String EDITOR_CLASS = "editor.class";

    private static class PluginEntry implements IClusterable {
        private static final long serialVersionUID = 1L;
        String id;
        IPluginControl plugin;
        IViewService viewer;
    }

    private IPluginContext context;
    private IPluginConfig config;
    private String editorClass;
    private Map<IModel, PluginEntry> editors;
    private int editCount;

    public MultiEditorPlugin(IPluginContext context, IPluginConfig config) {
        editors = new HashMap<IModel, PluginEntry>();
        editCount = 0;

        this.context = context;
        this.config = config;

        if (config.get(EDITOR_CLASS) != null) {
            editorClass = config.getString(EDITOR_CLASS);
            try {
                Class clazz = Class.forName(editorClass);
                if (!IViewService.class.isAssignableFrom(clazz)) {
                    log.error("Specified editor class does not implement IEditService");
                }
                if (!IRenderService.class.isAssignableFrom(clazz)) {
                    log.error("Specified editor class does not implement IRenderService");
                }
            } catch (ClassNotFoundException ex) {
                log.error(ex.getMessage());
            }
        } else {
            log.error("No editor class ({}) defined", EDITOR_CLASS);
        }

        if (config.getString(EDITOR_ID) != null) {
            context.registerService(this, config.getString(EDITOR_ID));
        } else {
            log.error("No editor id ({}) defined under which to register");
        }
    }

    public void view(final IModel model) {
        IViewService viewer;
        if (!editors.containsKey(model)) {
            IPluginConfig editConfig = new JavaPluginConfig();
            editConfig.put(IPlugin.CLASSNAME, editorClass);
            editConfig.put(RenderService.WICKET_ID, config.get(RenderService.WICKET_ID));
            editConfig.put(RenderService.DIALOG_ID, config.get(RenderService.DIALOG_ID));

            JavaClusterConfig clusterConfig = new JavaClusterConfig();
            clusterConfig.addPlugin(editConfig);
            IPluginControl plugin = context.start(clusterConfig);

            // register as the factory for the view service
            String pluginId = context.getReference(plugin).getServiceId();
            viewer = context.getService(pluginId, IViewService.class);

            String editorId = context.getReference(viewer).getServiceId();
            context.registerService(this, editorId);

            PluginEntry entry = new PluginEntry();
            entry.plugin = plugin;
            entry.id = editorId;
            entry.viewer = viewer;
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
            String editorId = entry.getValue().id;
            context.unregisterService(this, editorId);

            entry.getValue().plugin.stopPlugin();
            editors.remove(entry.getKey());
        } else {
            log.error("unknown editor " + service + " delete is ignored");
        }
    }

    public void delete(IClusterable service) {
        IViewService viewer = (IViewService) service;
        Map.Entry<IModel, PluginEntry> entry = getPluginEntry(viewer);
        if (entry != null) {
            IDialogService dialogService = context.getService(config.getString(RenderService.DIALOG_ID),
                    IDialogService.class);

            try {
                Node node = ((JcrNodeModel) entry.getKey()).getNode();
                HippoSession session = (HippoSession) node.getSession();
                if (dialogService != null && session.pendingChanges(node, "nt:base").hasNext()) {
                    dialogService.show(new OnCloseDialog(context, dialogService, (JcrNodeModel) entry.getKey(), this,
                            viewer));
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
        }
    }

    private Map.Entry<IModel, PluginEntry> getPluginEntry(IViewService service) {
        for (Map.Entry<IModel, PluginEntry> entry : editors.entrySet()) {
            if (entry.getValue().plugin.equals(service)) {
                return entry;
            }
        }
        return null;
    }
}
