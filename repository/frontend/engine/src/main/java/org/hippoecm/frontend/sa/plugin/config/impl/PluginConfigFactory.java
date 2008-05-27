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
import org.hippoecm.frontend.Main;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfigService;
import org.hippoecm.repository.api.HippoNodeType;

public class PluginConfigFactory {

    private IPluginConfigService pluginConfigService;

    public PluginConfigFactory(JcrSessionModel sessionModel) {
        try {
            Session session = sessionModel.getSession();
            String config = ((Main) Application.get()).getConfigurationParameter("config", null);

            String basePath = "/" + HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.FRONTEND_PATH;
            Node baseNode = (Node) session.getItem(basePath);

            if (config == null && baseNode.hasNodes()) {
                Node configNode = baseNode.getNodes().nextNode();
                pluginConfigService = new JcrConfigService(new JcrNodeModel(baseNode), configNode.getName());
            } else if (baseNode.hasNode(config)) {
                pluginConfigService = new JcrConfigService(new JcrNodeModel(baseNode), config);

            } else {
                //Fall back to builtin configuration
                pluginConfigService = new JavaConfigService();
            }
        } catch (RepositoryException e) {
            //Fall back to builtin configuration
            pluginConfigService = new JavaConfigService();
        }
    }

    public IPluginConfigService getPluginConfigService() {
        return pluginConfigService;
    }

}
