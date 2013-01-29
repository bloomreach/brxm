/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.builder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.TemplateEngineException;
import org.hippoecm.frontend.editor.plugins.field.NodeFieldPlugin;
import org.hippoecm.frontend.editor.plugins.field.PropertyFieldPlugin;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.TypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InheritedFieldSection extends Section {

    static final Logger log = LoggerFactory.getLogger(InheritedFieldSection.class);

    class InheritedField implements Serializable {
        final ITypeDescriptor type;
        final IFieldDescriptor field;

        InheritedField(final ITypeDescriptor type, final IFieldDescriptor field) {
            this.type = type;
            this.field = field;
        }

        IPluginConfig getInheritedConfig() throws TemplateEngineException {
            ITemplateEngine templateEngine = getTemplateEngine();
            final IClusterConfig template = templateEngine.getTemplate(type, IEditor.Mode.EDIT);
            final String fieldName = field.getName();
            for (IPluginConfig config : template.getPlugins()) {
                if (config.containsKey("field") && fieldName.equals(config.getString("field"))) {
                    return config;
                }
            }
            return null;
        }

        public IFieldDescriptor getField() {
            return field;
        }

        public String getName() {
            return field.getName();
        }
    }

    private class InheritedFieldsView extends SectionView<InheritedField> {

        public InheritedFieldsView(String id) {
            super(id, new ListDataProvider<InheritedField>() {

                @Override
                public List<InheritedField> getData() {
                    Map<String, InheritedField> fieldList = new LinkedHashMap<String,InheritedField>();

                    try {
                        addEditableFields(fieldList, builder.getTypeDescriptor().getSuperTypes());
                    } catch (TemplateEngineException e) {
                        log.error("Unable to build list of inheritable plugin configurations", e);
                    }
                    return new ArrayList<InheritedField>(fieldList.values());
                }

                private void addEditableFields(Map<String, InheritedField> fieldList, List<String> typesNames) throws TemplateEngineException {
                    // A deliberate check not to start retrieving the template engine and start the steps for real gain
                    if (typesNames.isEmpty()) {
                        return;
                    }

                    final ITemplateEngine templateEngine = getTemplateEngine();

                    for (String typeName : typesNames) {
                        final ITypeDescriptor typeDescriptor = templateEngine.getType(typeName);
                        addEditableFields(fieldList, typeDescriptor.getSuperTypes());

                        try {
                            final IClusterConfig template = templateEngine.getTemplate(typeDescriptor, IEditor.Mode.EDIT);

                            for (Map.Entry<String, IFieldDescriptor> fieldEntry : typeDescriptor.getFields().entrySet()) {
                                final String fieldName = fieldEntry.getKey();
                                final IFieldDescriptor fieldDescriptor = fieldEntry.getValue();

                                for (IPluginConfig config : template.getPlugins()) {
                                    if (config.containsKey("field") && fieldName.equals(config.getString("field"))) {
                                        fieldList.put(fieldName, new InheritedField(typeDescriptor, fieldDescriptor));
                                        break;
                                    }
                                }
                            }
                        } catch (TemplateEngineException e) {
                            log.debug("Could not find template for " + typeDescriptor.getName());
                        }
                    }
                }

                @Override
                public IModel<InheritedField> model(final InheritedField object) {
                    return new Model<InheritedField>(object);
                }

                @Override
                public void detach() {
                }
            });
        }

        @Override
        IModel<String> getNameModel(final InheritedField object) {
            return new Model<String>(object.getField().getName());
        }

        @Override
        void onClickItem(final InheritedField inheritedField) {
            try {
                IClusterConfig clusterConfig = builder.getTemplate();

                IPluginConfig inheritedConfig = getInheritedPluginConfig(inheritedField);

                IFieldDescriptor fieldDescriptor = inheritedField.getField();
                String pluginName = TypeHelper.getFieldName(fieldDescriptor.getPath(),
                                                            fieldDescriptor.getTypeDescriptor().getName());

                JavaPluginConfig pluginConfig = new JavaPluginConfig(pluginName);
                pluginConfig.putAll(inheritedConfig);
                pluginConfig.put("wicket.id", extPtRef.getModel().getObject());

                List<IPluginConfig> plugins = new LinkedList<IPluginConfig>(clusterConfig.getPlugins());
                plugins.add(pluginConfig);
                clusterConfig.setPlugins(plugins);
            } catch (BuilderException be) {
                log.error("unable to add field", be);
            }
        }

        private IPluginConfig getInheritedPluginConfig(final InheritedField inheritedField) {
            final IFieldDescriptor fieldDescriptor = inheritedField.getField();
            IPluginConfig inheritedConfig = null;
            try {
                inheritedConfig = inheritedField.getInheritedConfig();
            } catch (TemplateEngineException e) {
                log.warn("Unable to load inherited plugin configuration for " + fieldDescriptor.getName(), e);
            }
            if (inheritedConfig == null) {
                ITypeDescriptor fieldType = fieldDescriptor.getTypeDescriptor();
                inheritedConfig = new JavaPluginConfig();
                if (fieldType.isNode()) {
                    inheritedConfig.put("plugin.class", NodeFieldPlugin.class.getName());
                } else {
                    inheritedConfig.put("plugin.class", PropertyFieldPlugin.class.getName());
                }
                inheritedConfig.put("field", fieldDescriptor.getName());
                inheritedConfig.put("caption", fieldType.getName());
            }
            return inheritedConfig;
        }
    }

    private final IModelReference<String> extPtRef;
    private final TemplateBuilder builder;
    private final IPluginContext context;
    private final IPluginConfig config;

    public InheritedFieldSection(IPluginContext context, IPluginConfig config) {
        this.context = context;
        this.config = config;
        builder = context.getService(config.getString(TemplateBuilderConstants.MODEL_BUILDER), TemplateBuilder.class);

        extPtRef = getModelReference(TemplateBuilderConstants.MODEL_SELECTED_EXTENSION_POINT);
    }

    @Override
    public IModel<String> getTitleModel() {
        return new ResourceModel("inherited");
    }

    @Override
    public SectionView<?> createView(final String id) {
        return new InheritedFieldsView(id);
    }

    private ITemplateEngine getTemplateEngine() {
        return context.getService(config.getString("engine"), ITemplateEngine.class);
    }

    private IModelReference getModelReference(String key) {
        return context.getService(config.getString(key), IModelReference.class);
    }

}
