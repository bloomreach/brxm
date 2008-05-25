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
package org.hippoecm.frontend.sa.plugin.config.impl;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.Application;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.Main;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfigService;
import org.hippoecm.repository.api.HippoNodeType;

public class PluginConfigFactory {

    private static final ValueMap ANONYMOUS_CREDENTIALS = new ValueMap("username=,password=");    

    private String style;
    private IPluginConfigService pluginConfigService;

    public PluginConfigFactory(JcrSessionModel sessionModel) {
        if (sessionModel.getCredentials().equals(ANONYMOUS_CREDENTIALS)) {
            pluginConfigService = new LoginConfigService();
        } else {
            try {
                Session session = sessionModel.getSession();
                String config = ((Main) Application.get()).getConfigurationParameter("config", null);

                String basePath = "/" + HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.FRONTEND_PATH;
                Node baseNode = (Node) session.getItem(basePath);

                if (config == null && baseNode.hasNodes()) {
                    //Use the first frontend configuration node
                    Node configNode = baseNode.getNodes().nextNode();
                    pluginConfigService = new RepositoryConfigService(new JcrNodeModel(configNode));
                    if (configNode.hasProperty("hippo:style")) {
                        style = configNode.getProperty("hippo:style").getString();
                    }

                } else if (baseNode.hasNode(config)) {
                    //Use specified configuration
                    Node configNode = baseNode.getNode(config);
                    pluginConfigService = new RepositoryConfigService(new JcrNodeModel(configNode));
                    if (configNode.hasProperty("hippo:style")) {
                        style = configNode.getProperty("hippo:style").getString();
                    }

                } else {
                    //Fall back to builtin configuration
                    //pluginConfigService = new ExperimentalConfigService();
                    pluginConfigService = new ConsoleConfigService();
                    //pluginConfigService = new CmsConfigService();
                }
            } catch (RepositoryException e) {
                //Fall back to builtin configuration
                //pluginConfigService = new ExperimentalConfigService();
                pluginConfigService = new ConsoleConfigService();
                //pluginConfigService = new CmsConfigService();
            }
        }
    }

    public IPluginConfigService getPluginConfigService() {
        return pluginConfigService;
    }

    public String getStyle() {
        return style;
    }

}
