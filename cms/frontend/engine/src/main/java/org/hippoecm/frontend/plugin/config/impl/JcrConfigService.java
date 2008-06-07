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
package org.hippoecm.frontend.plugin.config.impl;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
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
            if (key.indexOf('/') > 0) {
                String provider = key.substring(0, key.indexOf('/'));
                if ("template".equals(provider)) {
                    String type = key.substring(key.indexOf('/') + 1);
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
                        cluster = new JcrClusterConfig(new JcrNodeModel(templateNode)) {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public Object get(Object key) {
                                if ("mode".equals(key)) {
                                    return mode;
                                }
                                return super.get(key);
                            }

                            @Override
                            public Object put(Object key, Object value) {
                                if ("mode".equals(key)) {
                                    log.warn("Illegal attempt to persist template mode");
                                    return null;
                                }
                                return super.put(key, value);
                            }
                        };
                    } else {
                        cluster = null;
                    }
                } else {
                    cluster = null;
                    log.warn("Unknown provider " + provider);
                }
            } else if (model.getNode().hasNode(key)) {
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

    public void detach() {
        model.detach();
    }

    private Node getTemplateNode(String type) {
        try {
            HippoSession session = (HippoSession) model.getNode().getSession();
            NamespaceRegistry nsReg = session.getWorkspace().getNamespaceRegistry();

            String prefix = "system";
            String uri = "";
            if (type.indexOf(':') > 0) {
                prefix = type.substring(0, type.indexOf(':'));
                uri = nsReg.getURI(prefix);
            }

            String nsVersion = "_" + uri.substring(uri.lastIndexOf("/") + 1);
            if (prefix.length() > nsVersion.length()
                    && nsVersion.equals(prefix.substring(prefix.length() - nsVersion.length()))) {
                type = type.substring(prefix.length());
                prefix = prefix.substring(0, prefix.length() - nsVersion.length());
                type = prefix + type;
            } else {
                uri = nsReg.getURI("rep");
            }

            String path = HippoNodeType.NAMESPACES_PATH + "/" + prefix + "/" + type + "/"
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
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

}
