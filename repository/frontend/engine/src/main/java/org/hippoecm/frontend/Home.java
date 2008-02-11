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

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.WebPage;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.PluginFactory;
import org.hippoecm.frontend.plugin.PluginManager;
import org.hippoecm.frontend.plugin.config.PluginConfig;
import org.hippoecm.frontend.plugin.config.PluginConfigFactory;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.template.config.RepositoryTemplateConfig;
import org.hippoecm.frontend.template.config.TemplateConfig;
import org.hippoecm.repository.api.HippoNode;

public class Home extends WebPage {
    private static final long serialVersionUID = 1L;

    public Home() {
        UserSession session = (UserSession) getSession();
        HippoNode rootNode = session.getRootNode();
        if (rootNode == null) {
            throw new RestartResponseException(LoginPage.class);
        }

        PluginConfig pluginConfig = new PluginConfigFactory().getPluginConfig();
        TemplateConfig templateConfig = new RepositoryTemplateConfig(session.getJcrSessionModel());
        PluginManager pluginManager = new PluginManager(pluginConfig, templateConfig);
        PluginFactory pluginFactory = new PluginFactory(pluginManager);

        PluginDescriptor rootPluginDescriptor = pluginConfig.getRoot();
        JcrNodeModel rootModel = new JcrNodeModel(rootNode);
        Plugin rootPlugin = pluginFactory.createPlugin(rootPluginDescriptor, rootModel, null);
        rootPlugin.setPluginManager(pluginManager);

        add(rootPlugin);
        rootPlugin.addChildren();

    }

}
