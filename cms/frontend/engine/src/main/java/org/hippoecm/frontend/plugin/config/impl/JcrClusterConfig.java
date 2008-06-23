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
import javax.jcr.Value;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrClusterConfig extends JcrPluginConfig implements IClusterConfig {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(JcrClusterConfig.class);

    private transient List<IPluginConfig> configs = null;

    public JcrClusterConfig(JcrNodeModel nodeModel) {
        super(nodeModel);
    }

    private void load() {
        if (configs == null) {
            configs = new LinkedList<IPluginConfig>();
            try {
                Node node = nodeModel.getNode();
                NodeIterator children = node.getNodes();
                while (children.hasNext()) {
                    Node child = children.nextNode();
                    configs.add(new JcrPluginConfig(new JcrNodeModel(child)));
                }
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
        }
    }

    @Override
    public void detach() {
        configs = null;
        super.detach();
    }

    public List<IPluginConfig> getPlugins() {
        load();
        return configs;
    }

    public List<String> getOverrides() {
        List<String> result = new LinkedList<String>();
        try {
            Node node = getNodeModel().getNode();
            if (node.hasProperty("frontend:overrides")) {
                for (Value value : node.getProperty("frontend:overrides").getValues()) {
                    result.add(value.getString());
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return result;
    }

}
