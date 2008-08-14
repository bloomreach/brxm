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
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.wicket.IClusterable;
import org.apache.wicket.Session;
import org.apache.wicket.model.IDetachable;
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
import org.hippoecm.frontend.service.IEditService;
import org.hippoecm.frontend.service.IFactoryService;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiEditorPlugin implements IPlugin, IEditService, IDetachable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public static final Logger log = LoggerFactory.getLogger(MultiEditorPlugin.class);

    public static final String CLUSTER = "cluster.name";

    private static class PluginEntry implements IClusterable {
        private static final long serialVersionUID = 1L;
        String id;
        IPluginControl plugin;
        IEditService editor;
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

        if (config.getString(EDITOR_ID) != null) {
            context.registerService(this, config.getString(EDITOR_ID));
        } else {
            log.error("No editor id ({}) defined under which to register", EDITOR_ID);
        }

        try {
            String user = ((UserSession) Session.get()).getCredentials().getString("username");
            QueryManager qMgr = ((UserSession) Session.get()).getJcrSession().getWorkspace().getQueryManager();
            Query query = qMgr.createQuery("select * from hippostd:publishable where hippostd:state='draft' "
                    + "and hippostd:holder='" + user + "'", Query.SQL);
            NodeIterator iter = query.execute().getNodes();
            while (iter.hasNext()) {
                Node node = iter.nextNode();
                if (!node.getName().equals("hippo:prototype")) {
                    openEditor(new JcrNodeModel(node));
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    public void edit(final IModel model) {
        PluginEntry entry = openEditor(model);
        focusEditor(entry, model);
    }

    protected PluginEntry openEditor(final IModel model) {
        PluginEntry entry;
        if (!editors.containsKey(model)) {
            IPluginConfigService pluginConfigService = context.getService(IPluginConfigService.class.getName(),
                    IPluginConfigService.class);
            IClusterConfig clusterConfig = pluginConfigService.getCluster(config.getString(CLUSTER));
            String editorId = clusterConfig.getString(EDITOR_ID);

            IPluginControl plugin = context.start(clusterConfig);

            IEditService editService = context.getService(editorId, IEditService.class);
            String serviceId = context.getReference(editService).getServiceId();

            // register as the factory for the view service
            IFactoryService factory = new IFactoryService() {
                private static final long serialVersionUID = 1L;

                public void delete(IClusterable service) {
                    IEditService viewer = (IEditService) service;
                    Map.Entry<IModel, PluginEntry> entry = getPluginEntry(viewer);
                    if (entry != null) {
                        IDialogService dialogService = context.getService(IDialogService.class.getName(),
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

            entry = new PluginEntry();
            entry.plugin = plugin;
            entry.id = serviceId;
            entry.editor = editService;
            entry.factory = factory;
            editors.put(model, entry);

            editCount++;
        } else {
            entry = editors.get(model);
        }
        entry.editor.edit(model);
        return entry;
    }

    protected void focusEditor(PluginEntry entry, IModel model) {
        if (entry != null) {
            // look up the render service that is created by the cluster
            List<IRenderService> targetServices = context.getServices(config.getString(RenderService.WICKET_ID),
                    IRenderService.class);
            List<IRenderService> clusterServices = context.getServices(context.getReference(entry.plugin)
                    .getServiceId(), IRenderService.class);
            for (IRenderService target : targetServices) {
                if (clusterServices.contains(target)) {
                    // found it!
                    target.focus(null);
                    break;
                }
            }
        } else {
            log.warn("Configured editor does not provide an IViewService");
        }
    }

    public void deleteEditor(IEditService service) {
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

    private Map.Entry<IModel, PluginEntry> getPluginEntry(IEditService service) {
        for (Map.Entry<IModel, PluginEntry> entry : editors.entrySet()) {
            if (entry.getValue().editor.equals(service)) {
                return entry;
            }
        }
        return null;
    }

    public void detach() {
        config.detach();
    }

}
