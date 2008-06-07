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
package org.hippoecm.frontend.legacy.template.config;

import java.util.LinkedList;
import java.util.List;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.legacy.plugin.PluginDescriptor;
import org.hippoecm.frontend.legacy.plugin.config.PluginRepositoryConfig;
import org.hippoecm.frontend.legacy.template.ItemDescriptor;
import org.hippoecm.frontend.legacy.template.TemplateDescriptor;
import org.hippoecm.frontend.legacy.template.TypeDescriptor;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class RepositoryTemplateConfig extends PluginRepositoryConfig implements TemplateConfig {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(RepositoryTemplateConfig.class);

    public RepositoryTemplateConfig() {
        super(HippoNodeType.NAMESPACES_PATH);
    }

    public TemplateDescriptor getTemplate(TypeDescriptor type, String mode) {
        if (type != null) {
            String typeName = type.getName();
            PluginDescriptor plugin = getPlugin(typeName);
            if (plugin != null) {
                return new RepositoryTemplateDescriptor(type, plugin, mode);
            }
        }
        return null;
    }

    public Node getTemplateNode(String type) {
        try {
            HippoSession session = (HippoSession) getJcrSession();
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
                    if (template.isNodeType(HippoNodeType.NT_TEMPLATE)) {
                        return template;
                    }
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    @Override
    public PluginDescriptor getPlugin(String type) {
        try {
            Node pluginNode = getTemplateNode(type);
            if (pluginNode != null) {
                return nodeToDescriptor(pluginNode);
            }
            log.error("No plugin node found for " + type);
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public TemplateDescriptor createTemplate(Node node, TypeDescriptor type) throws RepositoryException {
        String mode = TemplateConfig.EDIT_MODE;
        if (node.hasProperty(HippoNodeType.HIPPO_TEMPLATEMODE)) {
            mode = node.getProperty(HippoNodeType.HIPPO_TEMPLATEMODE).getString();
        }
        return new RepositoryTemplateDescriptor(type, nodeToDescriptor(node), mode);
    }

    public void save(Node node, ItemDescriptor descriptor) {
        try {
            super.save(node, descriptor.getPlugin());
            if (descriptor.getField() != null) {
                node.setProperty(HippoNodeType.HIPPO_FIELD, descriptor.getField());
            }
            for (ItemDescriptor item : descriptor.getItems()) {
                Node itemNode = node.addNode(HippoNodeType.HIPPO_ITEM, HippoNodeType.NT_TEMPLATEITEM);
                save(itemNode, item);
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    static List<ItemDescriptor> getTemplateItems(PluginDescriptor plugin, String mode,
            RepositoryTemplateDescriptor parent) {
        List<ItemDescriptor> items = new LinkedList<ItemDescriptor>();
        int itemId = 0;
        for (PluginDescriptor child : plugin.getChildren()) {
            Descriptor descriptor = (Descriptor) child;
            String childMode = mode;
            if (descriptor.mode != null) {
                childMode = descriptor.mode;
            }
            RepositoryItemDescriptor item = new RepositoryItemDescriptor(itemId++, child, childMode, parent);
            if (descriptor.field != null) {
                item.field = descriptor.field;
            }
            items.add(item);
        }
        return items;
    }

    @Override
    protected PluginDescriptor createDescriptor(Node node, String pluginId, String className) {
        return new Descriptor(node, pluginId, className);
    }

    @Deprecated
    private class Descriptor extends PluginRepositoryConfig.Descriptor {
        private static final long serialVersionUID = 1L;

        String field;
        String mode;

        Descriptor(Node node, String pluginId, String className) {
            super(node, pluginId, className);

            field = null;
            mode = null;
            try {
                if (node.hasProperty(HippoNodeType.HIPPO_FIELD)) {
                    field = node.getProperty(HippoNodeType.HIPPO_FIELD).getString();
                }
                if (node.hasProperty(HippoNodeType.HIPPO_TEMPLATEMODE)) {
                    mode = node.getProperty(HippoNodeType.HIPPO_TEMPLATEMODE).getString();
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
    }

    @Deprecated
    private static class RepositoryItemDescriptor extends ItemDescriptor {
        private static final long serialVersionUID = 1L;

        RepositoryTemplateDescriptor template;
        String field;

        RepositoryItemDescriptor(int id, PluginDescriptor plugin, String mode, RepositoryTemplateDescriptor template) {
            super(id, plugin, mode);
            setTemplate(template);
            this.template = template;
            this.field = null;
        }

        @Override
        public String getField() {
            return field;
        }

        @Override
        public List<ItemDescriptor> getItems() {
            return getTemplateItems(getPlugin(), getMode(), template);
        }
    }

    @Deprecated
    private static class RepositoryTemplateDescriptor extends TemplateDescriptor {
        private static final long serialVersionUID = 1L;

        RepositoryTemplateDescriptor(TypeDescriptor type, PluginDescriptor plugin, String mode) {
            super(type, plugin, mode);
        }

        @Override
        public List<ItemDescriptor> getItems() {
            return getTemplateItems(getPlugin(), getMode(), this);
        }
    }
}
