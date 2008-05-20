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
package org.hippoecm.frontend.sa.plugin.editor;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.IClusterable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.sa.core.IPlugin;
import org.hippoecm.frontend.sa.core.IPluginConfig;
import org.hippoecm.frontend.sa.core.IPluginContext;
import org.hippoecm.frontend.sa.core.impl.PluginConfig;
import org.hippoecm.frontend.sa.dialog.ExceptionDialog;
import org.hippoecm.frontend.sa.plugin.RenderPlugin;
import org.hippoecm.frontend.sa.service.IDialogService;
import org.hippoecm.frontend.sa.service.IFactoryService;
import org.hippoecm.frontend.sa.service.IRenderService;
import org.hippoecm.frontend.sa.service.IViewService;
import org.hippoecm.frontend.sa.service.render.RenderService;
import org.hippoecm.frontend.sa.util.ServiceTracker;
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiEditorPlugin implements IPlugin, IViewService, IFactoryService, IClusterable {
    private static final long serialVersionUID = 1L;

    public static final Logger log = LoggerFactory.getLogger(MultiEditorPlugin.class);

    public static final String EDITOR_ID = "editor";
    public static final String EDITOR_CLASS = "editor.class";

    private static class PluginEntry implements IClusterable {
        private static final long serialVersionUID = 1L;

        String id;
        IPlugin plugin;
    }

    private IPluginContext context;
    private IPluginConfig config;
    private ServiceTracker<IDialogService> dialogTracker;
    private String editorClass;
    private Map<IModel, PluginEntry> editors;
    private int editCount;

    public MultiEditorPlugin() {
        editors = new HashMap<IModel, PluginEntry>();
        editCount = 0;
        dialogTracker = new ServiceTracker(IDialogService.class);
    }

    public void start(IPluginContext context) {
        this.context = context;
        config = context.getProperties();

        if (config.get(RenderService.DIALOG_ID) != null) {
            dialogTracker.open(context, config.getString(RenderService.DIALOG_ID));
        } else {
            log.warn("No dialog service ({}) defined", RenderService.DIALOG_ID);
        }

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

        if (config.get(IPlugin.SERVICE_ID) != null) {
            context.registerService(this, config.getString(IPlugin.SERVICE_ID));
        } else {
            log.warn("No service id defined");
        }
    }

    public void stop() {
        for (Map.Entry<IModel, PluginEntry> entry : editors.entrySet()) {
            entry.getValue().plugin.stop();
            editors.remove(entry.getKey());
        }
        dialogTracker.close();
    }

    public String getServiceId() {
        if (config.get(IPlugin.SERVICE_ID) != null) {
            return config.getString(IPlugin.SERVICE_ID);
        }
        return null;
    }

    public void view(final IModel model) {
        IPlugin plugin;
        if (!editors.containsKey(model)) {
            PluginConfig editConfig = new PluginConfig();
            String editorId = config.getString(EDITOR_ID) + editCount;
            editConfig.put(IPlugin.SERVICE_ID, editorId);
            editConfig.put(IPlugin.CLASSNAME, editorClass);

            editConfig.put(RenderPlugin.WICKET_ID, config.get(RenderPlugin.WICKET_ID));
            editConfig.put(RenderPlugin.DIALOG_ID, config.get(RenderPlugin.DIALOG_ID));

            String factoryId = editorId + ".factory";
            context.registerService(this, factoryId);

            plugin = context.start(editConfig);
            if (plugin instanceof IViewService) {
                ((IViewService) plugin).view(model);
            }

            PluginEntry entry = new PluginEntry();
            entry.plugin = plugin;
            entry.id = editorId;
            editors.put(model, entry);

            editCount++;
        } else {
            plugin = editors.get(model).plugin;
        }
        if (plugin instanceof IRenderService) {
            ((IRenderService) plugin).focus(null);
        }
    }

    public void deleteEditor(IViewService service) {
        Map.Entry<IModel, PluginEntry> entry = getPluginEntry(service);
        if (entry != null) {
            String editorId = entry.getValue().id;
            context.unregisterService(this, editorId + ".factory");

            entry.getValue().plugin.stop();
            editors.remove(entry.getKey());
        } else {
            log.error("unknown editor " + service + " delete is ignored");
        }
    }

    public void delete(IClusterable service) {
        IViewService viewer = (IViewService) service;
        Map.Entry<IModel, PluginEntry> entry = getPluginEntry(viewer);
        if (entry != null) {
            IDialogService dialogService = null;
            if (dialogTracker.getServices().size() > 0) {
                dialogService = dialogTracker.getServices().get(0);
            }

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
