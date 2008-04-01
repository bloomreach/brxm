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
package org.hippoecm.frontend.plugins.template.builder;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.PluginModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.PluginFactory;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugin.empty.EmptyPlugin;
import org.hippoecm.frontend.plugin.error.ErrorPlugin;
import org.hippoecm.frontend.plugins.template.BuiltinTemplateConfig;
import org.hippoecm.frontend.template.FieldDescriptor;
import org.hippoecm.frontend.template.TemplateDescriptor;
import org.hippoecm.frontend.template.TemplateEngine;
import org.hippoecm.frontend.template.TypeDescriptor;
import org.hippoecm.frontend.template.config.RepositoryTemplateConfig;
import org.hippoecm.frontend.template.config.RepositoryTypeConfig;
import org.hippoecm.frontend.template.config.TemplateConfig;
import org.hippoecm.frontend.template.model.TemplateModel;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemEditorPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ItemEditorPlugin.class);

    private Plugin fieldParams;
    private Plugin field;
    private Plugin template;
    private TemplateConfig templateConfig;

    public ItemEditorPlugin(PluginDescriptor descriptor, IPluginModel model, Plugin parentPlugin) {
        super(descriptor, new JcrNodeModel(model), parentPlugin);

        templateConfig = new BuiltinTemplateConfig(getPluginManager().getTemplateEngine().getTypeConfig());

        field = createFieldPlugin("field");
        add(field);

        fieldParams = createFieldParamsPlugin("field-parameters");
        add(fieldParams);

        template = createTemplatePlugin("template");
        add(template);
    }

    @Override
    public void receive(Notification notification) {
        if ("template.select".equals(notification.getOperation())) {
            JcrNodeModel nodeModel = new JcrNodeModel(notification.getModel());
            if (!nodeModel.equals(getPluginModel())) {
                setPluginModel(nodeModel);

                field.destroy();
                replace(field = createFieldPlugin("field"));

                fieldParams.destroy();
                replace(fieldParams = createFieldParamsPlugin("field-parameters"));

                template.destroy();
                replace(template = createTemplatePlugin("template"));

                notification.getContext().addRefresh(this);
            }
        }
        super.receive(notification);
    }

    private Plugin createFieldPlugin(String wicketId) {
        try {
            Node fieldNode = getFieldNode();
            if (fieldNode != null) {
                JcrNodeModel fieldModel = new JcrNodeModel(fieldNode);

                TemplateEngine engine = getPluginManager().getTemplateEngine();
                TypeDescriptor typeDescriptor = engine.getTypeConfig().getTypeDescriptor(HippoNodeType.NT_FIELD);
                TemplateDescriptor templateDescriptor = engine.getTemplateConfig().getTemplate(typeDescriptor);
                TemplateModel templateModel = new TemplateModel(templateDescriptor, fieldModel.getParentModel(),
                        fieldModel.getNode().getName(), fieldModel.getNode().getIndex());
                return engine.createTemplate(wicketId, templateModel, this, null);
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
            return createErrorPlugin(wicketId, ex.getMessage());
        }
        return createEmptyPlugin(wicketId);
    }

    private Plugin createFieldParamsPlugin(String wicketId) {
        try {
            JcrNodeModel itemNodeModel = (JcrNodeModel) getModel();
            Node itemNode = itemNodeModel.getNode();
            if (itemNode != null && getFieldNode() != null) {
                TypeDescriptor typeDescriptor = new TypeDescriptor("internal", HippoNodeType.NT_PARAMETERS) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Map<String, FieldDescriptor> getFields() {
                        Map<String, FieldDescriptor> result = new HashMap<String, FieldDescriptor>();
                        FieldDescriptor caption = new FieldDescriptor("caption");
                        caption.setIsMultiple(true);
                        caption.setType("String");
                        result.put("caption", caption);
                        FieldDescriptor css = new FieldDescriptor("css");
                        css.setIsMultiple(true);
                        css.setType("String");
                        result.put("css", css);
                        return result;
                    }
                };
                TemplateDescriptor templateDescriptor = templateConfig.getTemplate(typeDescriptor);
                if (!itemNode.hasNode(HippoNodeType.HIPPO_PARAMETERS)) {
                    itemNode.addNode(HippoNodeType.HIPPO_PARAMETERS, HippoNodeType.NT_PARAMETERS);
                }
                TemplateModel model = new TemplateModel(templateDescriptor, itemNodeModel,
                        HippoNodeType.HIPPO_PARAMETERS, 1);

                return getPluginManager().getTemplateEngine().createTemplate(wicketId, model, this, null);
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
            return createErrorPlugin(wicketId, ex.getMessage());
        }
        return createEmptyPlugin(wicketId);
    }

    private Plugin createTemplatePlugin(String wicketId) {
        try {
            JcrNodeModel itemNodeModel = (JcrNodeModel) getModel();
            Node fieldNode = getFieldNode();
            if (fieldNode != null) {
                String typeName = fieldNode.getProperty(HippoNodeType.HIPPO_TYPE).getString();
                Node subTemplateNode = new RepositoryTemplateConfig().getTemplateNode(typeName);
                if (subTemplateNode != null && subTemplateNode.hasNode("hippo:options")) {
                    Node configNode = subTemplateNode.getNodes("hippo:options").nextNode();
                    TypeDescriptor typeDescriptor = new RepositoryTypeConfig().createTypeDescriptor(configNode);

                    Node itemNode = itemNodeModel.getNode();
                    if (!itemNode.hasNode(HippoNodeType.HIPPO_PARAMETERS)) {
                        itemNode.addNode(HippoNodeType.HIPPO_PARAMETERS, HippoNodeType.NT_PARAMETERS);
                    }
                    TemplateDescriptor templateDescriptor = templateConfig.getTemplate(typeDescriptor);
                    TemplateModel model = new TemplateModel(templateDescriptor, itemNodeModel,
                            HippoNodeType.HIPPO_PARAMETERS, 1);

                    return getPluginManager().getTemplateEngine().createTemplate(wicketId, model, this, null);
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
            return createErrorPlugin(wicketId, ex.getMessage());
        }
        return createEmptyPlugin(wicketId);
    }

    private Plugin createErrorPlugin(String wicketId, String message) {
        PluginModel error = new PluginModel();
        error.put("error", message);
        PluginDescriptor plugin = new PluginDescriptor(wicketId, ErrorPlugin.class.getName());
        return new PluginFactory(getPluginManager()).createPlugin(plugin, error, this);
    }

    private Plugin createEmptyPlugin(String wicketId) {
        PluginDescriptor plugin = new PluginDescriptor(wicketId, EmptyPlugin.class.getName());
        return new PluginFactory(getPluginManager()).createPlugin(plugin, null, this);
    }

    private Node getTypeNode() throws RepositoryException {
        JcrNodeModel itemNodeModel = (JcrNodeModel) getModel();
        Node itemNode = itemNodeModel.getNode();
        Node templateNode = itemNode;
        while (!templateNode.isNodeType(HippoNodeType.NT_TEMPLATETYPE)) {
            templateNode = templateNode.getParent();
        }
        String typeName = templateNode.getName();
        RepositoryTypeConfig repoTypeConfig = new RepositoryTypeConfig();
        return repoTypeConfig.getTypeNode(typeName);
    }

    private Node getFieldNode() throws RepositoryException {
        JcrNodeModel itemNodeModel = (JcrNodeModel) getModel();
        Node itemNode = itemNodeModel.getNode();
        if (itemNode.hasProperty(HippoNodeType.HIPPO_FIELD)) {
            Node typeNode = getTypeNode();

            String fieldName = itemNode.getProperty(HippoNodeType.HIPPO_FIELD).getString();
            NodeIterator fieldIter = typeNode.getNodes(HippoNodeType.HIPPO_FIELD);
            while (fieldIter.hasNext()) {
                Node fieldNode = fieldIter.nextNode();
                String name = fieldNode.getProperty(HippoNodeType.HIPPO_NAME).getString();
                if (name.equals(fieldName)) {
                    return fieldNode;
                }
            }
        }
        return null;
    }
}
