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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.wicket.Session;
import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.IJcrNodeModelListener;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaClusterConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.hippoecm.frontend.service.IJcrService;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShortcutsPlugin implements IPlugin, IJcrNodeModelListener, IDetachable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ShortcutsPlugin.class);

    private static final String PLUGINSQUERY = "shortcuts.query";
    public static final String SHORTCUTS_ID = "shortcuts.id";

    private IPluginContext context;
    private IPluginConfig config;
    private IClusterControl pluginControl;

    private String pluginsQuery = "";

    public ShortcutsPlugin(IPluginContext context, IPluginConfig config) {
        this.context = context;
        this.config = config;

        context.registerService(this, IJcrService.class.getName());

        if (config.get(PLUGINSQUERY) != null) {
            pluginsQuery = config.getString(PLUGINSQUERY);
        } else {
            log.warn("No query defined for {}", context.getReference(this).getServiceId());
        }

        refresh();
    }

    public void refresh() {
        if (pluginControl != null) {
            pluginControl.stop();
            pluginControl = null;
        }
        try {
            QueryManager qmgr = ((UserSession) Session.get()).getJcrSession().getWorkspace().getQueryManager();
            Query query = qmgr.createQuery(pluginsQuery, Query.XPATH);
            QueryResult result = query.execute();

            JavaClusterConfig clusterConfig = new JavaClusterConfig();
            for (NodeIterator iter = result.getNodes(); iter.hasNext();) {
                Node pluginNode = iter.nextNode();

                IPluginConfig pluginConfig = new JavaPluginConfig(new JcrPluginConfig(new JcrNodeModel(pluginNode)));
                pluginConfig.put(RenderService.WICKET_ID, config.getString(RenderService.WICKET_ID));
                clusterConfig.addPlugin(pluginConfig);
            }
            pluginControl = context.newCluster(clusterConfig, null);
            pluginControl.start();
        } catch (RepositoryException ex) {
            log.error("could not setup plugin", ex);
        }
    }

    public void onFlush(JcrNodeModel nodeModel) {
        refresh();
    }

    public void detach() {
        config.detach();
    }

}
