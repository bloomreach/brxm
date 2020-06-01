/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.query.Query;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.JcrQueryModel;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaClusterConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.hippoecm.frontend.service.render.RenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShortcutsPlugin implements IPlugin {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ShortcutsPlugin.class);

    private static final String PLUGINSQUERY = "shortcuts.query";
    public static final String SHORTCUTS_ID = "shortcuts.id";

    private final IPluginContext context;

    private final IPluginConfig config;

    public ShortcutsPlugin(IPluginContext context, IPluginConfig config) {
        this.context = context;
        this.config = config;

    }

    public void start() {
        // FIXME: throw exception when no query is defined?
        if (config.get(PLUGINSQUERY) != null) {
            JcrQueryModel query = new JcrQueryModel(config.getString(PLUGINSQUERY), Query.XPATH);
            JavaClusterConfig clusterConfig = new JavaClusterConfig();
            Iterator<Node> iter = query.iterator(0, query.size());
            while (iter.hasNext()) {
                JcrNodeModel model = (JcrNodeModel) query.model(iter.next());

                IPluginConfig pluginConfig = new JavaPluginConfig(new JcrPluginConfig(model));
                pluginConfig.put(RenderService.WICKET_ID, config.getString(RenderService.WICKET_ID));
                clusterConfig.addPlugin(pluginConfig);
            }
            IClusterControl pluginControl = context.newCluster(clusterConfig, null);
            pluginControl.start();
        } else {
            log.warn("No query defined for {}", context.getReference(this).getServiceId());
        }
    }

    public void stop() {
    }

}
