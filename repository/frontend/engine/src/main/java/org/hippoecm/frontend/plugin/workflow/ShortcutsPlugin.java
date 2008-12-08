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
package org.hippoecm.frontend.plugin.workflow;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.IJcrNodeModelListener;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelService;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IPluginControl;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaClusterConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.hippoecm.frontend.service.IJcrService;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShortcutsPlugin extends Panel implements IPlugin, IJcrNodeModelListener, IDetachable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ShortcutsPlugin.class);

    private static final String PLUGINSQUERY = "shortcuts.query";
    public static final String SHORTCUTS_ID = "shortcuts.id";

    private IPluginContext context;
    private IPluginConfig config;
    private String factoryId;
    private Map<String, IPluginControl> plugins;
    private Map<String, ModelService> models;
    private int pluginCount;
    private String pluginsQuery = "";

    public ShortcutsPlugin(IPluginContext context, IPluginConfig config) {
        super("id");

        this.context = context;
        this.config = config;

        plugins = new HashMap<String, IPluginControl>();
        models = new HashMap<String, ModelService>();
        pluginCount = 0;

        if (config.get(PLUGINSQUERY) != null) {
            pluginsQuery = config.getString(PLUGINSQUERY);
        } else {
            log.error("No query defined for {}", factoryId);
        }

        context.registerService(this, IJcrService.class.getName());
        refresh();
    }

    public void refresh() {
        closePlugins();
        try {
            QueryManager qmgr = ((UserSession) getSession()).getJcrSession().getWorkspace().getQueryManager();
            Query query = qmgr.createQuery(pluginsQuery, Query.XPATH);
            QueryResult result = query.execute();

            for (NodeIterator iter = result.getNodes(); iter.hasNext(); ) {
                Node pluginNode = iter.nextNode();

                String pluginId = config.getString(SHORTCUTS_ID) + (pluginCount++);

                IPluginConfig pluginConfig = new JavaPluginConfig(new JcrPluginConfig(new JcrNodeModel(pluginNode)));
                pluginConfig.put(RenderService.WICKET_ID, config.get(RenderService.WICKET_ID));

                JavaClusterConfig clusterConfig = new JavaClusterConfig();
                clusterConfig.addPlugin(pluginConfig);

                IPluginControl plugin = context.start(clusterConfig);

                // look up render service
                String controlId = context.getReference(plugin).getServiceId();
                IRenderService renderer = context.getService(controlId, IRenderService.class);

                // register as the factory for the render service
                context.registerService(this, context.getReference(renderer).getServiceId());

                plugins.put(controlId, plugin);
            }
        } catch (RepositoryException ex) {
            log.error("could not setup plugin", ex);
        }
    }

    private void closePlugins() {
        for (Map.Entry<String, IPluginControl> entry : plugins.entrySet()) {
            String controlId = entry.getKey();

            // unregister as the factory for the render service
            IRenderService renderer = context.getService(controlId, IRenderService.class);
            context.registerService(this, context.getReference(renderer).getServiceId());

            entry.getValue().stopPlugin();
        }
        plugins = new HashMap<String, IPluginControl>();

        for (Map.Entry<String, ModelService> entry : models.entrySet()) {
            ModelService modelService = entry.getValue();
            modelService.destroy();
        }
        models = new HashMap<String, ModelService>();
    }

    public void onFlush(JcrNodeModel nodeModel) {
        refresh();
    }

    @Override
    public void onDetach() {
        config.detach();
        super.onDetach();
    }

}
