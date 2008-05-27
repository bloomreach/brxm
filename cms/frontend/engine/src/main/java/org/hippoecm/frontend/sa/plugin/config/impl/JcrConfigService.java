/*
 * Copyright 2008 Hippo
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

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.sa.plugin.config.IClusterConfig;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrConfigService implements IPluginConfigService {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(JcrConfigService.class);

    private JcrNodeModel model;
    private String defaultKey;

    public JcrConfigService(JcrNodeModel model, String defaultKey) {
        this.model = model;
        this.defaultKey = defaultKey;
    }

    public IClusterConfig getPlugins(String key) {
        IClusterConfig cluster;
        try {
            if (model.getNode().hasNode(key)) {
                Node clusterNode = model.getNode().getNode(key);
                JcrNodeModel clusterNodeModel = new JcrNodeModel(clusterNode);
                cluster = new JcrClusterConfig(clusterNodeModel);
            } else {
                cluster = getDefaultCluster();
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
            cluster = getDefaultCluster();
        }
        return cluster;
    }

    public IClusterConfig getDefaultCluster() {
        return getPlugins(defaultKey);
    }

}
