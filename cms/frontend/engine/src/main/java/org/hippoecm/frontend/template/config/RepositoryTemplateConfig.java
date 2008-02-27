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

import org.apache.wicket.Application;
import org.hippoecm.frontend.Main;
import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.config.PluginRepositoryConfig;
import org.hippoecm.frontend.template.FieldDescriptor;
import org.hippoecm.frontend.template.ItemDescriptor;
import org.hippoecm.frontend.template.TemplateDescriptor;
import org.hippoecm.frontend.template.TypeDescriptor;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryTemplateConfig extends PluginRepositoryConfig implements TemplateConfig {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(RepositoryTemplateConfig.class);

    public RepositoryTemplateConfig(JcrSessionModel session) {
        super(session, getTemplateBasePath());
    }

    public TemplateDescriptor getTemplate(TypeDescriptor type) {
        if (type != null) {
            String typeName = type.getName();
            PluginDescriptor plugin;
            if (typeName.indexOf(':') > 0) {
                String prefix = typeName.substring(0, typeName.indexOf(':'));
                plugin = getPlugin(prefix + "/" + typeName);
            } else {
                plugin = getPlugin("system/" + typeName);
            }
            return new RepositoryTemplateDescriptor(type, plugin);
        }
        return null;
    }

    private static String getTemplateBasePath() {
        Main main = (Main) Application.get();
        return HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.FRONTEND_PATH + "/" + main.getHippoApplication()
                + "/hippo:templates";
    }

    List<ItemDescriptor> getTemplateItems(PluginDescriptor plugin, TemplateDescriptor parent) {
        List<ItemDescriptor> items = new LinkedList<ItemDescriptor>();
        for (PluginDescriptor child : plugin.getChildren()) {
            String name = ((Descriptor) child).field;
            if (name != "") {
                boolean found = false;
                List<FieldDescriptor> fields = parent.getTypeDescriptor().getFields();
                for (FieldDescriptor field : fields) {
                    if (field.getName().equals(name)) {
                        items.add(new RepositoryFieldDescriptor(field, child));
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    log.warn("Field " + name + " specified in plugin does not exist");
                }
            } else {
                items.add(new RepositoryItemDescriptor(child.getPluginId(), child, parent));
            }
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

    private class RepositoryItemDescriptor extends ItemDescriptor {
        private static final long serialVersionUID = 1L;

        TemplateDescriptor template;

        RepositoryItemDescriptor(String name, PluginDescriptor plugin, TemplateDescriptor template) {
            super(name, plugin);
            this.template = template;
        }

        @Override
        public List<ItemDescriptor> getItems() {
            return getTemplateItems(getPlugin(), template);
        }
    }

    private class RepositoryTemplateDescriptor extends TemplateDescriptor {
        private static final long serialVersionUID = 1L;

        RepositoryTemplateDescriptor(TypeDescriptor type, PluginDescriptor plugin) {
            super(type, plugin);
        }

        @Override
        public List<ItemDescriptor> getItems() {
            return getTemplateItems(getPlugin(), this);
        }
    }

    private class RepositoryFieldDescriptor extends FieldDescriptor {
        private static final long serialVersionUID = 1L;

        RepositoryFieldDescriptor(FieldDescriptor original, PluginDescriptor plugin) {
            super(original.getMapRepresentation());
            setPlugin(plugin);
        }
    }
}
