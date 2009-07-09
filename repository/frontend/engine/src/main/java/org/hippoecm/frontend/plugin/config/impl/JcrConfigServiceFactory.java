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

import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.FrontendNodeType;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrConfigServiceFactory implements IPluginConfigService {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(JcrConfigServiceFactory.class);

    private JcrNodeModel model;
    private String defaultKey;

    public JcrConfigServiceFactory(JcrNodeModel model, String defaultKey) {
        this.model = model;
        this.defaultKey = defaultKey;
    }

    public IClusterConfig getCluster(String key) {
        IClusterConfig cluster = null;
        try {
            if (model.getNode().hasNode(key)) {
                Node clusterNode = model.getNode().getNode(key);
                JcrNodeModel clusterNodeModel = new JcrNodeModel(clusterNode);
                cluster = new JcrClusterConfig(clusterNodeModel);
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return cluster;
    }

    public List<String> listClusters(String folder) {
        List<String> results = new LinkedList<String>();
        try {
            Node node = model.getNode().getNode(folder);
            NodeIterator iter = node.getNodes();
            while (iter.hasNext()) {
                Node child = iter.nextNode();
                if (child.isNodeType(FrontendNodeType.NT_PLUGINCLUSTER)) {
                    results.add(child.getName());
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return results;
    }

    public IClusterConfig getDefaultCluster() {
        return getCluster(defaultKey);
    }

    public void detach() {
        model.detach();
    }

}
