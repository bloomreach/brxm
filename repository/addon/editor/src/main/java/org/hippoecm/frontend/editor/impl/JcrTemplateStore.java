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
package org.hippoecm.frontend.editor.impl;

import java.util.List;
import java.util.ArrayList;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.plugin.config.impl.ClusterConfigDecorator;
import org.hippoecm.frontend.plugin.config.impl.JcrClusterConfig;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrTemplateStore implements IPluginConfigService {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(JcrTemplateStore.class);

    private int count = 0;
    private String serviceId;

    public void setId(String id) {
        serviceId = id;
    }

    public IClusterConfig getCluster(String key) {
        IClusterConfig cluster;
        String type = key;
        final String mode;
        int idx;
        if ((idx = type.indexOf('/')) > 0) {
            mode = type.substring(idx + 1);
            type = type.substring(0, idx);
        } else {
            mode = "edit";
        }
        Node templateNode = getTemplateNode(type);
        if (templateNode != null) {
            cluster = new JcrClusterConfig(new JcrNodeModel(templateNode));
            cluster = new ClusterConfigDecorator(cluster, serviceId + ".clusters." + (count++));
            cluster.put("mode", mode);
        } else {
            cluster = null;
        }
        return cluster;
    }

    public List<String> listClusters(String folder) {
        return new ArrayList();
    }

    public IClusterConfig getDefaultCluster() {
        // TODO Auto-generated method stub
        return null;
    }

    public void detach() {
        // TODO Auto-generated method stub

    }

    private Node getTemplateNode(String type) {
        try {
            Session session = ((UserSession) org.apache.wicket.Session.get()).getJcrSession();
            NamespaceRegistry nsReg = session.getWorkspace().getNamespaceRegistry();

            String prefix = "system";
            String subType = type;
            if (type.indexOf(':') > 0) {
                prefix = type.substring(0, type.indexOf(':'));
                subType = type.substring(type.indexOf(':') + 1);
            }
            String uri = "internal";
            if (!"system".equals(prefix)) {
                uri = nsReg.getURI(prefix);
            }

            String nsVersion = "_" + uri.substring(uri.lastIndexOf("/") + 1);
            if (prefix.length() > nsVersion.length()
                    && nsVersion.equals(prefix.substring(prefix.length() - nsVersion.length()))) {
                prefix = prefix.substring(0, prefix.length() - nsVersion.length());
            } else {
                uri = nsReg.getURI("rep");
            }

            String path = HippoNodeType.NAMESPACES_PATH + "/" + prefix + "/" + subType + "/"
                    + HippoNodeType.HIPPO_TEMPLATE;
            Node node = session.getRootNode().getNode(path);
            if (node != null) {
                NodeIterator nodes = node.getNodes(HippoNodeType.HIPPO_TEMPLATE);
                while (nodes.hasNext()) {
                    Node template = nodes.nextNode();
                    if (template.isNodeType("frontend:plugincluster")) {
                        return template;
                    }
                }
            }
        } catch (PathNotFoundException ex) {
            log.info("Path not found: " + ex.getMessage());
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

}
