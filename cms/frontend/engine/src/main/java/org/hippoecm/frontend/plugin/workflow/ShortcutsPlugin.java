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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.query.Query;

import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.JcrQueryModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
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

public class ShortcutsPlugin implements IPlugin, IDetachable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ShortcutsPlugin.class);

    private static final String PLUGINSQUERY = "shortcuts.query";
    public static final String SHORTCUTS_ID = "shortcuts.id";

    private IPluginContext context;
    private IPluginConfig config;
    private IClusterControl pluginControl;

    private JcrQueryModel query;

    public ShortcutsPlugin(IPluginContext context, IPluginConfig config) {
        this.context = context;
        this.config = config;

        // FIXME: throw exception when no query is defined?
        if (config.get(PLUGINSQUERY) != null) {
            query = new JcrQueryModel(config.getString(PLUGINSQUERY), Query.XPATH);
            context.registerService(new IObserver() {
                private static final long serialVersionUID = 1L;

                public IObservable getObservable() {
                    return query;
                }

                public void onEvent(IEvent event) {
                    refresh();
                }

            }, IObserver.class.getName());
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
        if (query != null) {
            JavaClusterConfig clusterConfig = new JavaClusterConfig();
            Iterator<Node> iter = query.iterator(0, query.size());
            while (iter.hasNext()) {
                JcrNodeModel model = (JcrNodeModel) query.model(iter.next());

                IPluginConfig pluginConfig = new JavaPluginConfig(new JcrPluginConfig(model));
                pluginConfig.put(RenderService.WICKET_ID, config.getString(RenderService.WICKET_ID));
                clusterConfig.addPlugin(pluginConfig);
            }
            pluginControl = context.newCluster(clusterConfig, null);
            pluginControl.start();
        }
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();

        refresh();
    }

    public void detach() {
        if (query != null) {
            query.detach();
        }
    }

}
