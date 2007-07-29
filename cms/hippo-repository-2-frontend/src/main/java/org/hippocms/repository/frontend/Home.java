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

public class Home extends WebPage {
    private static final long serialVersionUID = 1L;

    public Home() throws RepositoryException {
        UserSession session = (UserSession) getSession();
        Node rootNode = session.getJcrSession().getRootNode();
        JcrNodeModel model = new JcrNodeModel(rootNode);
        
        addPlugin("navigationPanel", "org.hippocms.repository.plugins.admin.browser.BrowserPlugin", model);
        addPlugin("menuPanel", "org.hippocms.repository.plugins.admin.menu.MenuPlugin", model);
        addPlugin("contentPanel", "org.hippocms.repository.plugins.admin.editor.EditorPlugin", model);
        //addPlugin("contentPanel", "org.hippocms.repository.plugins.samples.SamplesPlugin", model);
    }

    public void addPlugin(String id, String classname, JcrNodeModel model) {
        Plugin pluginPanel = new PluginFactory(classname).getPlugin(id, model);
        ((UserSession) getSession()).registerPlugin(pluginPanel);
        add(pluginPanel);
    }

}
