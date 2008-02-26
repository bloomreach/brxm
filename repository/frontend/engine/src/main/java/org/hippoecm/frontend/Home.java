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
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.PluginFactory;
import org.hippoecm.frontend.plugin.PluginManager;
import org.hippoecm.frontend.plugin.config.PluginConfig;
import org.hippoecm.frontend.plugin.config.PluginConfigFactory;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.template.config.RepositoryTemplateConfig;
import org.hippoecm.frontend.template.config.RepositoryTypeConfig;
import org.hippoecm.frontend.template.config.TemplateConfig;
import org.hippoecm.frontend.template.config.TypeConfig;
import org.hippoecm.repository.api.HippoNode;

public class Home extends WebPage {
    private static final long serialVersionUID = 1L;

    public static final ValueMap ANONYMOUS_CREDENTIALS = new ValueMap("username=,password=");
    public static final String ROOT_PLUGIN = "rootPlugin";
    public static final String LOGIN_PLUGIN = "loginPlugin";

    public Home() {

        UserSession session = getValidUserSession();
        HippoNode rootNode = session.getRootNode();

        TypeConfig typeConfig = new RepositoryTypeConfig(session.getJcrSessionModel());
        TemplateConfig templateConfig = new RepositoryTemplateConfig(session.getJcrSessionModel());
        PluginConfig pluginConfig = new PluginConfigFactory().getPluginConfig();

        PluginManager pluginManager = new PluginManager(pluginConfig, typeConfig, templateConfig);
        PluginFactory pluginFactory = new PluginFactory(pluginManager);

        PluginDescriptor rootPluginDescriptor;
        if (session.getCredentials().equals(ANONYMOUS_CREDENTIALS)) {
            //logged in as anonymous, present user with loginPlugin
            rootPluginDescriptor = pluginConfig.getPlugin(LOGIN_PLUGIN);
            rootPluginDescriptor.setWicketId(ROOT_PLUGIN);
        } else {
            rootPluginDescriptor = pluginConfig.getPlugin(ROOT_PLUGIN);
        }
        JcrNodeModel rootModel = new JcrNodeModel(rootNode);

        Plugin rootPlugin = pluginFactory.createPlugin(rootPluginDescriptor, rootModel, null);
        rootPlugin.setPluginManager(pluginManager);

        add(rootPlugin);
        rootPlugin.addChildren();
    }

    private UserSession getValidUserSession() {
        UserSession session = (UserSession) getSession();
        if (session.getRootNode() == null) {
            session.setJcrCredentials(ANONYMOUS_CREDENTIALS);
        }
        return session;
    }

}
