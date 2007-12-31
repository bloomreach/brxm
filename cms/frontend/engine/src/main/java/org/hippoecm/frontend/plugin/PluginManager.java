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
package org.hippoecm.frontend.plugin;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.wicket.IClusterable;
import org.apache.wicket.Session;
import org.hippoecm.frontend.UserSession;
import org.hippoecm.frontend.plugin.channel.ChannelFactory;
import org.hippoecm.frontend.plugin.config.PluginConfig;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginManager implements IClusterable {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(PluginManager.class);

    private PluginConfig pluginConfig;
    private List<Plugin> pluginRegistry;

    public PluginManager(PluginConfig pluginConfig) {
        this.pluginRegistry = new ArrayList<Plugin>();
        this.pluginConfig = pluginConfig;
    }

    public void registerPlugin(Plugin plugin) {
        pluginRegistry.add(plugin);
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public WorkflowManager getWorkflowManager() {
        try {
            javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();
            return ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public ChannelFactory getChannelFactory() {
        return pluginConfig.getChannelFactory();
    }
}
