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
package org.hippoecm.frontend.plugin.config.impl;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.Application;
import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.Main;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.repository.api.HippoNodeType;

public class PluginConfigFactory implements IClusterable {
    private static final long serialVersionUID = 1L;

    private IPluginConfigService pluginConfigService;

    public PluginConfigFactory(JcrSessionModel sessionModel) {
        try {
            Session session = sessionModel.getSession();
            String config = ((Main) Application.get()).getConfigurationParameter("config", null);

            String basePath = "/" + HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.FRONTEND_PATH;
            Node baseNode = (Node) session.getItem(basePath);

            final IPluginConfigService baseService;
            if (config == null && baseNode.hasNodes()) {
                Node configNode = baseNode.getNodes().nextNode();
                baseService = new JcrConfigService(new JcrNodeModel(baseNode), configNode.getName());
            } else if (baseNode.hasNode(config)) {
                baseService = new JcrConfigService(new JcrNodeModel(baseNode), config);
            } else {
                //Fall back to builtin configuration
                baseService = new JavaConfigService();
            }

            pluginConfigService = new IPluginConfigService() {
                private static final long serialVersionUID = 1L;

                private int count = 0;

                public IClusterConfig getDefaultCluster() {
                    return new ClusterConfigDecorator(baseService.getDefaultCluster(), "config.cluster." + (count++));
                }

                public IClusterConfig getPlugins(String key) {
                    IClusterConfig upstream = baseService.getPlugins(key);
                    if (upstream != null) {
                        return new ClusterConfigDecorator(upstream, "config.cluster." + (count++));
                    }
                    return null;
                }

                public void detach() {
                    baseService.detach();
                }
            };
        } catch (RepositoryException e) {
            //Fall back to builtin configuration
            pluginConfigService = new JavaConfigService();
        }
    }

    public IPluginConfigService getPluginConfigService() {
        return pluginConfigService;
    }

}
