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
package org.hippoecm.repository.frontend;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.WebPage;
import org.hippoecm.repository.frontend.model.JcrNodeModel;
import org.hippoecm.repository.frontend.plugin.PluginManager;
import org.hippoecm.repository.frontend.plugin.config.PluginConfig;
import org.hippoecm.repository.frontend.plugin.config.PluginConfigFactory;
import org.hippoecm.repository.frontend.plugin.error.ErrorPlugin;

public class Home extends WebPage {
    private static final long serialVersionUID = 1L;

    private PluginManager pluginManager;

    public Home() {
        JcrNodeModel rootModel = null;
        try {
            UserSession wicketSession = (UserSession) getSession();
            javax.jcr.Session jcrSession = wicketSession.getJcrSession();
            if (jcrSession == null) {
                String message = "Session is null, no connection to server.";
                add(new ErrorPlugin("homePanel", null, message));
            } else {
                Node rootNode = jcrSession.getRootNode();
                rootModel = new JcrNodeModel(rootNode);
            }
        } catch (RepositoryException e) {
            String message = "Failed to retrieve root node from repository";
            add(new ErrorPlugin("homePanel", null, message));            
        }

        if (rootModel != null) {
            HomePlugin homePlugin = new HomePlugin("homePanel", rootModel);
            PluginConfig pluginConfig = new PluginConfigFactory().getPluginConfig();
            pluginManager = new PluginManager(homePlugin, pluginConfig, rootModel);
            add(homePlugin);
        }
    }

    public PluginManager getPluginManager() {
        return pluginManager;
    }
    
}
