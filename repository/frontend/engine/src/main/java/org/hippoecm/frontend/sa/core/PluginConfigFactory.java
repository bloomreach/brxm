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
package org.hippoecm.frontend.sa.core;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Application;
import org.apache.wicket.Session;
import org.hippoecm.frontend.Main;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.sa.core.impl.JavaPluginConfig;
import org.hippoecm.frontend.sa.core.impl.JcrPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;

public class PluginConfigFactory {

    private IPluginConfig pluginConfig;
    private String style;

    public PluginConfigFactory() {
        try {
            javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();
            String config = ((Main) Application.get()).getConfigurationParameter("config", null);

            //TODO: define constant
            String basePath = "/" + HippoNodeType.CONFIGURATION_PATH + "/hippo:";
            Node baseNode = (Node) session.getItem(basePath);

            if (config == null && baseNode.hasNodes()) {
                //Use the first frontend configuration node
                Node configNode = baseNode.getNodes().nextNode();
                pluginConfig = new JcrPluginConfig(new JcrNodeModel(configNode));
                if (configNode.hasProperty("hippo:style")) {
                    style = configNode.getProperty("hippo:style").getString();
                }

            } else if (baseNode.hasNode(config)) {
                //Use specified configuration
                Node configNode = baseNode.getNode(config);
                pluginConfig = new JcrPluginConfig(new JcrNodeModel(configNode));
                if (configNode.hasProperty("hippo:style")) {
                    style = configNode.getProperty("hippo:style").getString();
                }

            } else {
                //Fall back to builtin configuration
                pluginConfig = new JavaPluginConfig();
            }
        } catch (RepositoryException e) {
            //Fall back to builtin configuration
            pluginConfig = new JavaPluginConfig();
        }

    }

    public IPluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public String getStyle() {
        return style;
    }

}
