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
package org.hippoecm.frontend.editor.config;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.ClusterConfigDecorator;
import org.hippoecm.frontend.plugin.config.impl.JavaClusterConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugins.standardworkflow.types.IFieldDescriptor;
import org.hippoecm.frontend.plugins.standardworkflow.types.ITypeDescriptor;
import org.hippoecm.frontend.plugins.standardworkflow.types.ITypeStore;
import org.hippoecm.frontend.editor.plugins.field.NodeFieldPlugin;
import org.hippoecm.frontend.editor.plugins.field.PropertyFieldPlugin;
import org.hippoecm.frontend.service.render.ListViewPlugin;

public class BuiltinTemplateStore implements IClusterable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    private static int instanceCount = 0;

    private ITypeStore typeConfig;

    public BuiltinTemplateStore(ITypeStore typeConfig) {
        this.typeConfig = typeConfig;
    }

    public IClusterConfig getTemplate(ITypeDescriptor type, String mode) {
        return new ClusterConfigDecorator(new BuiltinTemplateConfig(type, mode), newId());
    }

    class BuiltinTemplateConfig extends JavaClusterConfig {
        private static final long serialVersionUID = 1L;

        private ITypeDescriptor type;

        public BuiltinTemplateConfig(ITypeDescriptor type, String mode) {
            this.type = type;
            put("mode", mode);
            put("wicket.model", "{cluster}.model");
        }

        @Override
        public List<String> getOverrides() {
            List<String> result = new LinkedList<String>();
            result.add("wicket.id");
            result.add("wicket.dialog");
            result.add("engine");
            return result;
        }

        @Override
        public List<IPluginConfig> getPlugins() {
            List<IPluginConfig> list = new LinkedList<IPluginConfig>();
            IPluginConfig config = new JavaPluginConfig();
            config.put("plugin.class", ListViewPlugin.class.getName());
            config.put("wicket.id", "cluster:wicket.id");
            config.put("item", "{cluster}.item");
            list.add(config);

            Map<String, IFieldDescriptor> fields = type.getFields();
            for (Map.Entry<String, IFieldDescriptor> entry : fields.entrySet()) {
                IFieldDescriptor field = entry.getValue();
                ITypeDescriptor type = typeConfig.getTypeDescriptor(field.getType());

                config = new JavaPluginConfig();
                if (type.isNode()) {
                    config.put("plugin.class", NodeFieldPlugin.class.getName());
                } else {
                    config.put("plugin.class", PropertyFieldPlugin.class.getName());
                }
                config.put("wicket.id", "{cluster}.item");
                config.put("wicket.model", "cluster:wicket.model");
                config.put("engine", "cluster:engine");
                config.put("mode", "cluster:mode");
                config.put("caption", entry.getKey());
                config.put("field", entry.getKey());
                config.put("item", "{cluster}.fields." + entry.getKey());
                config.put("template.wicket.id", "{cluster}.fields." + entry.getKey());
                list.add(config);
            }
            return list;
        }

        @Override
        public void detach() {
            type.detach();
        }
    }

    private static String newId() {
        synchronized (BuiltinTemplateStore.class) {
            return BuiltinTemplateStore.class.getName() + "." + (instanceCount++);
        }
    }
}
