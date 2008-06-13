/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.plugin.config.impl;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.Application;
import org.apache.wicket.IClusterable;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.Main;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.repository.api.HippoNodeType;

public class PluginConfigFactory implements IClusterable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private IPluginConfigService pluginConfigService;

    public PluginConfigFactory(JcrSessionModel sessionModel) {
        IPluginConfigService baseService;
        try {
            Session session = sessionModel.getSession();
            ValueMap credentials = sessionModel.getCredentials();
            if (session == null || !session.isLive() || Main.DEFAULT_CREDENTIALS.equals(credentials)) {
                baseService = new JavaConfigService("login");
            } else {
                String config = ((Main) Application.get()).getConfigurationParameter("config", null);
                String basePath = "/" + HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.FRONTEND_PATH;
                Node baseNode = (Node) session.getItem(basePath);
                if (config == null && baseNode.hasNodes()) {
                    Node configNode = baseNode.getNodes().nextNode();
                    baseService = new JcrConfigService(new JcrNodeModel(baseNode), configNode.getName());
                } else if (baseNode.hasNode(config)) {
                    baseService = new JcrConfigService(new JcrNodeModel(baseNode), config);
                } else {
                    baseService = new JavaConfigService("console");
                }
            }
        } catch (RepositoryException e) {
            baseService = new JavaConfigService("login");
        }
        pluginConfigService = new DecoratedConfigService(baseService);
    }

    public IPluginConfigService getPluginConfigService() {
        return pluginConfigService;
    }

    private class DecoratedConfigService implements IPluginConfigService {
        private static final long serialVersionUID = 1L;

        private int count = 0;
        private IPluginConfigService decoratedService;

        DecoratedConfigService(IPluginConfigService configService) {
            decoratedService = configService;
        }

        public IClusterConfig getDefaultCluster() {
            return new ClusterConfigDecorator(decoratedService.getDefaultCluster(), "config.cluster." + (count++));
        }

        public IClusterConfig getCluster(String key) {
            IClusterConfig upstream = decoratedService.getCluster(key);
            if (upstream != null) {
                return new ClusterConfigDecorator(upstream, "config.cluster." + (count++));
            }
            return null;
        }

        public void detach() {
            decoratedService.detach();
        }
    }

}
