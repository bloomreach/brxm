/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.frontend.FrontendNodeType;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import java.util.LinkedList;
import java.util.List;

/**
 * Configuration service for plugin clusters.  Cluster folders are located beneath
 * the application, with a fallback to the first defined application.
 */
public class JcrConfigServiceFactory implements IPluginConfigService {

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
            Node clusterNode = null;
            if (model.getNode().hasNode(key)) {
                clusterNode = model.getNode().getNode(key);
            } else {
                Node appFolder = model.getNode().getParent();
                Node first = appFolder.getNodes().nextNode();
                if (!model.getNode().isSame(first)) {
                    if (first.hasNode(key)) {
                        clusterNode = first.getNode(key);
                    }
                }
            }
            if (clusterNode != null) {
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
            if (model.getNode().hasNode(folder)) {
                Node node = model.getNode().getNode(folder);
                NodeIterator iter = node.getNodes();
                while (iter.hasNext()) {
                    Node child = iter.nextNode();
                    if (child.isNodeType(FrontendNodeType.NT_PLUGINCLUSTER)) {
                        results.add(child.getName());
                    }
                }
            }
            Node appFolder = model.getNode().getParent();
            Node first = appFolder.getNodes().nextNode();
            if (!model.getNode().isSame(first)) {
                if (first.hasNode(folder)) {
                    NodeIterator iter = first.getNode(folder).getNodes();
                    while (iter.hasNext()) {
                        Node child = iter.nextNode();
                        if (child.isNodeType(FrontendNodeType.NT_PLUGINCLUSTER)) {
                            if (!results.contains(child.getName())) {
                                results.add(child.getName());
                            }
                        }
                    }
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return results;
    }

    @Override
    public boolean isSaveOnExitEnabled() {
        try {
            final Node applicationNode = model.getNode();
            if (applicationNode.hasProperty(FrontendNodeType.FRONTEND_SAVEONEXIT) && Boolean.FALSE.equals(applicationNode.getProperty(
                    FrontendNodeType.FRONTEND_SAVEONEXIT).getBoolean())) {

                return false;
            }
        } catch (RepositoryException re) {
            log.error("Could not determine whether save on exit is enabled.  Defaulting to true, save pending changes when session expires.", re);
        }
        return true;
    }

    public IClusterConfig getDefaultCluster() {
        return getCluster(defaultKey);
    }

    public void detach() {
        model.detach();
    }

}
