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
package org.hippoecm.editor.template;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.wicket.util.collections.MiniMap;
import org.hippoecm.frontend.editor.plugins.field.NodeFieldPlugin;
import org.hippoecm.frontend.editor.plugins.field.PropertyFieldPlugin;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaClusterConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.render.ListViewPlugin;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.ITypeLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuiltinTemplateConfig extends JavaClusterConfig {

    private static final long serialVersionUID = 1L;
    
    static final Logger log = LoggerFactory.getLogger(BuiltinTemplateConfig.class);

    private ITypeLocator typeLocator;
    private ITemplateLocator locator;
    private ITypeDescriptor type;
    private String name;

    public BuiltinTemplateConfig(ITypeDescriptor type, ITypeLocator typeLocator, ITemplateLocator locator) {
        this.type = type;
        this.locator = locator;
        this.typeLocator = typeLocator;
        super.put("type", type.getName());
        this.name = type.getName().replace(':', '_');
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<String> getServices() {
        List<String> result = new LinkedList<String>();
        result.add("wicket.id");
        return result;
    }

    @Override
    public List<String> getReferences() {
        List<String> result = new LinkedList<String>();
        result.add("wicket.model");
        result.add("model.compareTo");
        result.add("engine");
        return result;
    }

    @Override
    public List<String> getProperties() {
        List<String> result = new LinkedList<String>();
        result.add("mode");
        return result;
    }

    @Override
    public List<IPluginConfig> getPlugins() {
        List<IPluginConfig> list = new LinkedList<IPluginConfig>();
        IPluginConfig config = new JavaPluginConfig("root");
        config.put("plugin.class", ListViewPlugin.class.getName());
        config.put("item", "${cluster.id}.field");
        list.add(config);

        Map<String, ITypeDescriptor> declarations = new TreeMap<String, ITypeDescriptor>();
        Map<String, IClusterConfig> templates = new TreeMap<String, IClusterConfig>();
        for (String superType : type.getSuperTypes()) {
            try {
                ITypeDescriptor type = typeLocator.locate(superType);
                for (Map.Entry<String, IFieldDescriptor> entry : type.getFields().entrySet()) {
                    declarations.put(entry.getKey(), type);
                }

                try {
                    Map<String, Object> criteria = new MiniMap<String, Object>(1);
                    criteria.put("type", type);
                    templates.put(superType, locator.getTemplate(criteria));
                } catch (StoreException ex) {
                    // ignore
                }
            } catch (StoreException e) {
                throw new RuntimeException("Could not locate superType " + superType);
            }
        }

        Map<String, IFieldDescriptor> fields = type.getFields();
        for (Map.Entry<String, IFieldDescriptor> entry : fields.entrySet()) {
            IFieldDescriptor field = entry.getValue();
            ITypeDescriptor type = field.getTypeDescriptor();

            if (declarations.containsKey(entry.getKey())) {
                if (!templates.containsKey(declarations.get(entry.getKey()).getName())) {
                    continue;
                }
                // TODO: extract plugin from super configuration
            } else {
                try {
                    Map<String, Object> criteria = new MiniMap<String, Object>(1);
                    criteria.put("type", type);
                    /* IClusterConfig cluster = */ locator.getTemplate(criteria);
                } catch (StoreException e) {
                    continue;
                }
            }

            config = new JavaPluginConfig(entry.getKey());
            if (type.isNode()) {
                config.put("plugin.class", NodeFieldPlugin.class.getName());
            } else {
                config.put("plugin.class", PropertyFieldPlugin.class.getName());
            }
            config.put("wicket.id", "${cluster.id}.field");
            config.put("caption", entry.getKey());
            config.put("field", entry.getKey());
            list.add(config);
        }
        return list;
    }

}
