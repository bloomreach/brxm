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
package org.hippoecm.frontend;

import org.apache.wicket.markup.html.WebPage;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.JcrTreeNode;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.PluginFactory;
import org.hippoecm.frontend.plugin.PluginManager;
import org.hippoecm.frontend.plugin.config.PluginConfig;
import org.hippoecm.frontend.plugin.config.PluginConfigFactory;
import org.hippoecm.frontend.plugin.error.ErrorPlugin;
import org.hippoecm.repository.api.HippoNode;

public class Home extends WebPage {
    private static final long serialVersionUID = 1L;

    public Home() {
        UserSession session = (UserSession)getSession();
        HippoNode rootNode = session.getRootNode();
        if (rootNode == null) {        
            String message = "Cannot find repository root, no connection to server.";
            PluginDescriptor errorDescriptor = new PluginDescriptor("rootPlugin", null);
            add(new ErrorPlugin(errorDescriptor, null, message));
        } else {
            PluginConfig pluginConfig = new PluginConfigFactory().getPluginConfig();
            PluginManager pluginManager = new PluginManager(pluginConfig);
            PluginFactory pluginFactory = new PluginFactory(pluginManager);
            
            PluginDescriptor rootPluginDescriptor = pluginConfig.getRoot();
            JcrNodeModel rootModel = new JcrTreeNode(null, rootNode);
            Plugin rootPlugin = pluginFactory.createPlugin(rootPluginDescriptor, rootModel, null);
            rootPlugin.setPluginManager(pluginManager);

            add(rootPlugin);
            rootPlugin.addChildren();
        }
    }
    
}
