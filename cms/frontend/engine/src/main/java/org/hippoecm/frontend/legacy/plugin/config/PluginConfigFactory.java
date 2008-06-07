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
package org.hippoecm.frontend.legacy.plugin.config;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Application;
import org.apache.wicket.Session;
import org.hippoecm.frontend.Main;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;

/**
 * @deprecated use org.hippoecm.frontend.sa.core.PluginConfigFactory instead
 */
@Deprecated
public class PluginConfigFactory {

    private PluginConfig pluginConfig;
    private String style;

    public PluginConfigFactory() {
        try {
            javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();
            String config = ((Main) Application.get()).getConfigurationParameter("config", null);

            String basePath = "/" + HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.FRONTEND_PATH + "_deprecated";
            Node baseNode = (Node) session.getItem(basePath);

            if (config == null && baseNode.hasNodes()) {
                //Use the first frontend configuration node
                Node configNode = baseNode.getNodes().nextNode();
                pluginConfig = new PluginRepositoryConfig(configNode);
                if (configNode.hasProperty("hippo:style")) {
                    style = configNode.getProperty("hippo:style").getString();
                }

            } else if (baseNode.hasNode(config)) {
                //Use specified configuration
                Node configNode = baseNode.getNode(config);
                pluginConfig = new PluginRepositoryConfig(configNode);
                if (configNode.hasProperty("hippo:style")) {
                    style = configNode.getProperty("hippo:style").getString();
                }

            } else {
                //Fall back to builtin configuration
                pluginConfig = new PluginJavaConfig();
            }
        } catch (RepositoryException e) {
            //Fall back to builtin configuration
            pluginConfig = new PluginJavaConfig();
        }

    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public String getStyle() {
        return style;
    }

}
