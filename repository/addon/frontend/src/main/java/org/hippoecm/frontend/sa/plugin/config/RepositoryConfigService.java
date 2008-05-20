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
package org.hippoecm.frontend.sa.plugin.config;

import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.parameters.RepositoryParameterValue;
import org.hippoecm.frontend.sa.core.IPluginConfig;
import org.hippoecm.frontend.sa.core.impl.PluginConfig;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryConfigService implements IPluginConfigService, IDetachable {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(RepositoryConfigService.class);

    private JcrNodeModel model;

    public RepositoryConfigService(JcrNodeModel model) {
        this.model = model;
    }

    public List<IPluginConfig> getPlugins() {
        List<IPluginConfig> result = new LinkedList<IPluginConfig>();
        try {
            NodeIterator nodes = model.getNode().getNodes();
            while (nodes.hasNext()) {
                Node node = nodes.nextNode();
                if (node.isNodeType(HippoNodeType.NT_FRONTENDPLUGIN)) {
                    result.add(new RepositoryPluginConfig(node));
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return result;
    }

    public void detach() {
        model.detach();
    }

    static class RepositoryPluginConfig extends PluginConfig {
        private static final long serialVersionUID = 1L;

        RepositoryPluginConfig(Node node) {
            putAll(new RepositoryParameterValue(node).getMap());
        }
    }

}
