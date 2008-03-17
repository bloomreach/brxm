/*
 * Copyright 2007 Hippo
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
package org.hippoecm.frontend.template.config;

import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.config.PluginRepositoryConfig;
import org.hippoecm.frontend.template.ItemDescriptor;
import org.hippoecm.frontend.template.TemplateDescriptor;
import org.hippoecm.frontend.template.TypeDescriptor;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryTemplateConfig extends PluginRepositoryConfig implements TemplateConfig {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(RepositoryTemplateConfig.class);

    public RepositoryTemplateConfig() {
        super(getTemplateBasePath());
    }

    public TemplateDescriptor getTemplate(TypeDescriptor type) {
        if (type != null) {
            String typeName = type.getName();
            PluginDescriptor plugin;
            if (typeName.indexOf(':') > 0) {
                String prefix = typeName.substring(0, typeName.indexOf(':'));
                plugin = getPlugin(prefix + "/" + typeName + "/" + typeName);
            } else {
                plugin = getPlugin("system/" + typeName + "/" + typeName);
            }
            if (plugin != null) {
                return new RepositoryTemplateDescriptor(type, plugin);
            }
        }
        return null;
    }

    public Node getTemplateNode(TypeDescriptor type) {
        try {
            String typeName = type.getName();
            String prefix;
            if (typeName.indexOf(':') > 0) {
                prefix = typeName.substring(0, typeName.indexOf(':'));
            } else {
                prefix = "system";
            }
            return getJcrSession().getRootNode().getNode(getBasePath() + "/" + prefix + "/" + typeName + "/" + typeName);
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    @Override
    public PluginDescriptor getPlugin(String pluginId) {
        try {
            Node pluginNode = getJcrSession().getRootNode().getNode(getBasePath() + "/" + pluginId);
            if (pluginNode != null) {
                return nodeToDescriptor(pluginNode);
            }
            log.error("No plugin node found for " + pluginId);
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public TemplateDescriptor createTemplate(Node node, TypeDescriptor type) throws RepositoryException {
        return new RepositoryTemplateDescriptor(type, nodeToDescriptor(node));
    }

    private static String getTemplateBasePath() {
        return HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.TEMPLATES_PATH;
    }

    static List<ItemDescriptor> getTemplateItems(PluginDescriptor plugin, TemplateDescriptor parent) {
        List<ItemDescriptor> items = new LinkedList<ItemDescriptor>();
        int itemId = 0;
        for (PluginDescriptor child : plugin.getChildren()) {
            RepositoryItemDescriptor item = new RepositoryItemDescriptor(itemId++, child, parent);
            String name = ((Descriptor) child).field;
            if (name != null) {
                item.field = name;
            }
            items.add(item);
        }
        return items;
    }

    @Override
    protected PluginDescriptor createDescriptor(Node node, String pluginId, String className) {
        return new Descriptor(node, pluginId, className);
    }

    private class Descriptor extends PluginRepositoryConfig.Descriptor {
        private static final long serialVersionUID = 1L;

        String field;

        Descriptor(Node node, String pluginId, String className) {
            super(node, pluginId, className);

            field = null;
            try {
                if (node.hasProperty(HippoNodeType.HIPPO_FIELD)) {
                    field = node.getProperty(HippoNodeType.HIPPO_FIELD).getString();
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
    }

    private static class RepositoryItemDescriptor extends ItemDescriptor {
        private static final long serialVersionUID = 1L;

        TemplateDescriptor template;
        String field;

        RepositoryItemDescriptor(int id, PluginDescriptor plugin, TemplateDescriptor template) {
            super(id, plugin);
            this.template = template;
            this.field = null;
        }

        @Override
        public String getField() {
            return field;
        }

        @Override
        public String getType() {
            return template.getType();
        }

        @Override
        public List<ItemDescriptor> getItems() {
            return getTemplateItems(getPlugin(), template);
        }
    }

    private static class RepositoryTemplateDescriptor extends TemplateDescriptor {
        private static final long serialVersionUID = 1L;

        RepositoryTemplateDescriptor(TypeDescriptor type, PluginDescriptor plugin) {
            super(type, plugin);
        }

        @Override
        public List<ItemDescriptor> getItems() {
            return getTemplateItems(getPlugin(), this);
        }
    }
}
