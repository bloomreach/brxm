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
package org.hippocms.repository.frontend;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.WebPage;
import org.hippocms.repository.frontend.model.JcrNodeModel;
import org.hippocms.repository.frontend.plugin.Plugin;
import org.hippocms.repository.frontend.plugin.PluginFactory;
import org.hippocms.repository.frontend.plugin.config.PluginConfig;
import org.hippocms.repository.frontend.plugin.config.PluginConfigFactory;
import org.hippocms.repository.frontend.plugin.error.ErrorPlugin;

public class Home extends WebPage {
    private static final long serialVersionUID = 1L;

    private PluginConfig pluginConfig;
    public static final String[] WICKET_IDS = new String[] { "menuPanel", "navigationPanel", "contentPanel" };

    public Home() {
        pluginConfig = new PluginConfigFactory().getPluginConfig();
        try {
            UserSession wicketSession = (UserSession) getSession();
            javax.jcr.Session jcrSession = wicketSession.getJcrSession();
            if (jcrSession == null) {
                showError(null, "Session is null, no connection to server.");
            } else {
                Node rootNode = jcrSession.getRootNode();
                addPlugins(new JcrNodeModel(rootNode));
            }
        } catch (RepositoryException e) {
            showError(e, "Failed to retreive root node from repository");
        }
    }

    private void addPlugins(JcrNodeModel model) {
        for (int i = 0; i < WICKET_IDS.length; i++) {
            String classname = pluginConfig.pluginClassname(WICKET_IDS[i]);
            Plugin pluginPanel = new PluginFactory(classname).getPlugin(WICKET_IDS[i], model);
            ((UserSession) getSession()).registerPlugin(pluginPanel);
            add(pluginPanel);
        }
    }

    private void showError(Exception e, String message) {
        for (int i = 0; i < WICKET_IDS.length; i++) {
            add(new ErrorPlugin(WICKET_IDS[i], null, e, message));
        }
    }
}
