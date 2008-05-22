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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class RepositoryConfigService implements IPluginConfigService {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(RepositoryConfigService.class);

    private JcrNodeModel model;

    RepositoryConfigService(JcrNodeModel model) {
        this.model = model;
    }

    public List<IPluginConfig> getPlugins(String key) {
        try {
            Node node = model.getNode();
            if (node != null) {
                NodeIterator children = node.getNodes(key);
                if (children.getSize() < Integer.MAX_VALUE) {
                    List<IPluginConfig> result = new ArrayList<IPluginConfig>();
                    while (children.hasNext()) {
                        Node child = children.nextNode();
                        result.add(new JcrPluginConfig(new JcrNodeModel(child)));
                    }
                    return result;
                } else {
                    log.warn("Too many children");
                }
            } else {
                log.warn("Node model is not valid");
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

}
