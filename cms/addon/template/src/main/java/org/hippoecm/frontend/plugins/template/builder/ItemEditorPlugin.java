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
package org.hippoecm.frontend.plugins.template.builder;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.legacy.model.IPluginModel;
import org.hippoecm.frontend.legacy.model.PluginModel;
import org.hippoecm.frontend.legacy.plugin.Plugin;
import org.hippoecm.frontend.legacy.plugin.PluginDescriptor;
import org.hippoecm.frontend.legacy.plugin.PluginFactory;
import org.hippoecm.frontend.legacy.plugin.channel.Notification;
import org.hippoecm.frontend.legacy.plugin.empty.EmptyPlugin;
import org.hippoecm.frontend.legacy.plugin.error.ErrorPlugin;
import org.hippoecm.frontend.legacy.template.FieldDescriptor;
import org.hippoecm.frontend.legacy.template.TemplateDescriptor;
import org.hippoecm.frontend.legacy.template.TemplateEngine;
import org.hippoecm.frontend.legacy.template.TypeDescriptor;
import org.hippoecm.frontend.legacy.template.config.JcrFieldModel;
import org.hippoecm.frontend.legacy.template.config.JcrTypeModel;
import org.hippoecm.frontend.legacy.template.config.RepositoryTemplateConfig;
import org.hippoecm.frontend.legacy.template.config.RepositoryTypeConfig;
import org.hippoecm.frontend.legacy.template.config.TemplateConfig;
import org.hippoecm.frontend.legacy.template.model.TemplateModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.template.BuiltinTemplateConfig;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.standardworkflow.RemodelWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemEditorPlugin extends Plugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ItemEditorPlugin.class);

    private Plugin fieldParams;
    private Plugin field;
    private Plugin template;
    private TemplateConfig templateConfig;
    private JcrTypeModel typeModel;

    public ItemEditorPlugin(PluginDescriptor descriptor, IPluginModel model, Plugin parentPlugin) {
        super(descriptor, new JcrNodeModel(model), parentPlugin);

        updateModel();

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

                updateModel();

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

    @Override
    public void onDetach() {
        if (typeModel != null) {
            typeModel.detach();
        }
        super.onDetach();
    }

    private Plugin createFieldPlugin(String wicketId) {
        try {
            JcrFieldModel fieldModel = getFieldModel();
            if (fieldModel != null) {
                JcrNodeModel nodeModel = fieldModel.getNodeModel();

                TemplateEngine engine = getPluginManager().getTemplateEngine();
                TypeDescriptor typeDescriptor = engine.getTypeConfig().getTypeDescriptor(HippoNodeType.NT_FIELD);
                TemplateDescriptor templateDescriptor = engine.getTemplateConfig().getTemplate(typeDescriptor, TemplateConfig.EDIT_MODE);
                TemplateModel templateModel = new TemplateModel(templateDescriptor, nodeModel.getParentModel(),
                        nodeModel.getNode().getName(), nodeModel.getNode().getIndex());
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
            if (itemNode != null && getFieldModel() != null) {
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
                TemplateDescriptor templateDescriptor = templateConfig.getTemplate(typeDescriptor, TemplateConfig.EDIT_MODE);
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
            JcrFieldModel fieldModel = getFieldModel();
            if (fieldModel != null) {
                String typeName = fieldModel.getTypeName();

                Node subTemplateNode = new RepositoryTemplateConfig().getTemplateNode(typeName);
                if (subTemplateNode != null && subTemplateNode.hasNode("hippo:options")) {
                    Node configNode = subTemplateNode.getNodes("hippo:options").nextNode();
                    TypeDescriptor typeDescriptor = new RepositoryTypeConfig(RemodelWorkflow.VERSION_CURRENT)
                            .createTypeDescriptor(configNode, typeName);

                    Node itemNode = itemNodeModel.getNode();
                    if (!itemNode.hasNode(HippoNodeType.HIPPO_PARAMETERS)) {
                        itemNode.addNode(HippoNodeType.HIPPO_PARAMETERS, HippoNodeType.NT_PARAMETERS);
                    }
                    TemplateDescriptor templateDescriptor = templateConfig.getTemplate(typeDescriptor, TemplateConfig.EDIT_MODE);
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

    private void updateModel() {
        try {
            JcrNodeModel itemNodeModel = (JcrNodeModel) getModel();
            Node itemNode = itemNodeModel.getNode();
            Node templateNode = itemNode;
            while (!templateNode.isNodeType(HippoNodeType.NT_TEMPLATETYPE)) {
                templateNode = templateNode.getParent();
            }
            String typeName = templateNode.getName();
            RepositoryTypeConfig repoTypeConfig = new RepositoryTypeConfig(RemodelWorkflow.VERSION_DRAFT);
            typeModel = repoTypeConfig.getTypeModel(typeName);
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    private JcrFieldModel getFieldModel() throws RepositoryException {
        JcrNodeModel itemNodeModel = (JcrNodeModel) getModel();
        Node itemNode = itemNodeModel.getNode();
        if (itemNode.hasProperty(HippoNodeType.HIPPO_FIELD)) {
            return typeModel.getField(itemNode.getProperty(HippoNodeType.HIPPO_FIELD).getString());
        }
        return null;
    }
}
