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
package org.hippoecm.frontend.plugins.template;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.hippoecm.frontend.legacy.plugin.PluginDescriptor;
import org.hippoecm.frontend.legacy.plugin.parameters.ParameterValue;
import org.hippoecm.frontend.legacy.template.FieldDescriptor;
import org.hippoecm.frontend.legacy.template.ItemDescriptor;
import org.hippoecm.frontend.legacy.template.TemplateDescriptor;
import org.hippoecm.frontend.legacy.template.TypeDescriptor;
import org.hippoecm.frontend.legacy.template.config.TemplateConfig;
import org.hippoecm.frontend.legacy.template.config.TypeConfig;
import org.hippoecm.frontend.plugins.template.field.NodeFieldPlugin;
import org.hippoecm.frontend.plugins.template.field.PropertyFieldPlugin;

public class BuiltinTemplateConfig implements TemplateConfig {
    private static final long serialVersionUID = 1L;

    private TypeConfig typeConfig;

    public BuiltinTemplateConfig(TypeConfig typeConfig) {
        this.typeConfig = typeConfig;
    }

    public TemplateDescriptor getTemplate(TypeDescriptor type, String mode) {
        return new BuiltinTemplateDescriptor(type, mode);
    }

    class BuiltinTemplateDescriptor extends TemplateDescriptor {
        private static final long serialVersionUID = 1L;

        public BuiltinTemplateDescriptor(TypeDescriptor type, String mode) {
            super(type, new PluginDescriptor("template", NodeTemplatePlugin.class.getName()), mode);
        }

        @Override
        public List<ItemDescriptor> getItems() {
            List<ItemDescriptor> items = new LinkedList<ItemDescriptor>();
            Map<String, FieldDescriptor> fields = getTypeDescriptor().getFields();
            int id = 0;
            for (Map.Entry<String, FieldDescriptor> entry : fields.entrySet()) {
                FieldDescriptor field = entry.getValue();
                TypeDescriptor type = typeConfig.getTypeDescriptor(field.getType());
                PluginDescriptor pluginDescriptor;
                if (type.isNode()) {
                    pluginDescriptor = new PluginDescriptor("hippo:item", NodeFieldPlugin.class.getName());
                } else {
                    pluginDescriptor = new PluginDescriptor("hippo:item", PropertyFieldPlugin.class.getName());
                }

                Map<String, ParameterValue> parameters = new HashMap<String, ParameterValue>();
                List<String> captionList = new LinkedList<String>();
                captionList.add(entry.getKey());
                parameters.put("caption", new ParameterValue(captionList));
                pluginDescriptor.setParameters(parameters);

                ItemDescriptor itemDescriptor = new ItemDescriptor(id++, pluginDescriptor, getMode());
                itemDescriptor.setField(entry.getKey());
                itemDescriptor.setTemplate(this);
                items.add(itemDescriptor);
            }
            return items;
        }
    }
}
