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

import org.apache.commons.lang.StringUtils;
import org.hippoecm.frontend.FrontendNodeType;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import java.security.AccessControlException;
import java.util.LinkedList;
import java.util.List;

import static org.hippoecm.frontend.FrontendNodeType.FRONTEND_SAVEONEXIT;

/**
 * Configuration service for plugin clusters.  Cluster folders are located beneath
 * the application.
 */
public class JcrConfigServiceFactory implements IPluginConfigService {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(JcrConfigServiceFactory.class);

    private static final String PRIVILEGES_CONFIGURATION_PARAM = "frontend:privileges";
    private static final String PRIVILEGES_PATH_CONFIGURATION_PARAM = "frontend:privileges.path";

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
                    } else {
                        log.warn("Expected node of type " + FrontendNodeType.NT_PLUGINCLUSTER + " and name '" + key
                                + "' at " + model.getNode().getPath() + "/" + key
                                + ", or at fallback location " + first.getPath() + "/" + key);
                    }
                } else {
                    log.warn("Expected node of type " + FrontendNodeType.NT_PLUGINCLUSTER + " and name '" + key
                            + "' at " + model.getNode().getPath() + "/" + key);
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
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return results;
    }

    @Override
    public boolean isSaveOnExitEnabled() {
        try {
            final Node applicationNode = model.getNode();
            return JcrUtils.getBooleanProperty(applicationNode, FRONTEND_SAVEONEXIT, true);
        } catch (RepositoryException re) {
            log.error("Could not determine whether save on exit is enabled.  Defaulting to true, save pending changes when session expires.", re);
        }
        return true;
    }

    @Override
    public boolean checkPermission(Session session) {
        try {
            final String aclPrivilege = JcrUtils.getStringProperty(model.getNode(), PRIVILEGES_CONFIGURATION_PARAM, null);
            final String aclPrivilegePath = JcrUtils.getStringProperty(model.getNode(), PRIVILEGES_PATH_CONFIGURATION_PARAM, null);
            if (!StringUtils.isBlank(aclPrivilege) && !StringUtils.isBlank(aclPrivilegePath)) {
                session.checkPermission(aclPrivilegePath, aclPrivilege);
            }
            return true;
        } catch (AccessControlException e) {
            log.info("Permission denied to user {} on application {}", session.getUserID(), JcrUtils.getNodeNameQuietly(model.getNode()));
            return false;
        } catch (RepositoryException e) {
            log.error("Failed to check application permission", e);
            return false;
        }
    }

    public IClusterConfig getDefaultCluster() {
        return getCluster(defaultKey);
    }

    public void detach() {
        model.detach();
    }

}
