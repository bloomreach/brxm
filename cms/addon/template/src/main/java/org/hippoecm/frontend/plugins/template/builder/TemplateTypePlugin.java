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

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.PluginFactory;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugin.empty.EmptyPlugin;
import org.hippoecm.frontend.plugins.template.BuiltinTemplateConfig;
import org.hippoecm.frontend.template.FieldDescriptor;
import org.hippoecm.frontend.template.TemplateDescriptor;
import org.hippoecm.frontend.template.TemplateEngine;
import org.hippoecm.frontend.template.TypeDescriptor;
import org.hippoecm.frontend.template.config.RepositoryTemplateConfig;
import org.hippoecm.frontend.template.config.RepositoryTypeConfig;
import org.hippoecm.frontend.template.config.TemplateConfig;
import org.hippoecm.frontend.template.config.TypeConfig;
import org.hippoecm.frontend.template.model.TemplateModel;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.standardworkflow.RemodelWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateTypePlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(TemplateTypePlugin.class);

    private TypeConfig typeConfig;
    private TypeDescriptor typeVersion;
    private String typeName;

    public TemplateTypePlugin(PluginDescriptor pluginDescriptor, final IPluginModel pluginModel, Plugin parentPlugin) {
        super(pluginDescriptor, new TemplateModel(pluginModel), parentPlugin);

        typeConfig = new RepositoryTypeConfig(RemodelWorkflow.VERSION_DRAFT);

        TemplateModel model = (TemplateModel) getPluginModel();
        Node typeNode = model.getJcrNodeModel().getNode();
        if (typeNode != null) {
            add(createTemplate(model.getJcrNodeModel()));
        } else {
            add(new EmptyPanel("template"));
        }

        if (typeNode != null) {
            try {
                typeName = typeNode.getName();
                typeVersion = new TypeDescriptor(typeConfig.getTypeDescriptor(typeName).getMapRepresentation());
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
    }

    @Override
    public void receive(Notification notification) {
        if ("flush".equals(notification.getOperation())) {
            JcrNodeModel typeModel = ((TemplateModel) getPluginModel()).getJcrNodeModel();
            JcrNodeModel flushedModel = (JcrNodeModel) notification.getModel();
            if (typeModel.equals(flushedModel)) {
                // update prototype
                try {
                    Node prototype = null;
                    NodeIterator iter = typeModel.getNode().getNode(HippoNodeType.HIPPO_PROTOTYPE).getNodes(
                            HippoNodeType.HIPPO_PROTOTYPE);
                    while (iter.hasNext()) {
                        Node node = iter.nextNode();
                        if (node.isNodeType(HippoNodeType.NT_REMODEL)) {
                            if (node.getProperty(HippoNodeType.HIPPO_REMODEL).getString().equals("draft")) {
                                prototype = node;
                                break;
                            }
                        }
                    }

                    TemplateEngine engine = getPluginManager().getTemplateEngine();
                    TypeDescriptor type = typeConfig.getTypeDescriptor(typeName);
                    if (prototype != null && typeVersion != null) {
                        Map<String, FieldDescriptor> oldFields = typeVersion.getFields();
                        for (Map.Entry<String, FieldDescriptor> entry : oldFields.entrySet()) {
                            FieldDescriptor oldField = entry.getValue();
                            TypeDescriptor fieldType = engine.getTypeConfig().getTypeDescriptor(oldField.getType());

                            FieldDescriptor newField = type.getField(entry.getKey());
                            if (newField != null) {
                                if (!newField.getPath().equals(oldField.getPath()) && !newField.getPath().equals("*")
                                        && !oldField.getPath().equals("*")) {
                                    assert (newField.getType().equals(oldField.getType()));
                                    if (fieldType.isNode()) {
                                        if (prototype.hasNode(oldField.getPath())) {
                                            Node child = prototype.getNode(oldField.getPath());
                                            child.getSession().move(child.getPath(),
                                                    prototype.getPath() + "/" + newField.getPath());
                                        }
                                    } else {
                                        if (prototype.hasProperty(oldField.getPath())) {
                                            Property property = prototype.getProperty(oldField.getPath());
                                            if (property.getDefinition().isMultiple()) {
                                                Value[] values = property.getValues();
                                                property.remove();
                                                if (newField.isMultiple()) {
                                                    prototype.setProperty(newField.getPath(), values);
                                                } else if (values.length > 0) {
                                                    prototype.setProperty(newField.getPath(), values[0]);
                                                }
                                            } else {
                                                Value value = property.getValue();
                                                property.remove();
                                                if (newField.isMultiple()) {
                                                    prototype.setProperty(newField.getPath(), new Value[] { value });
                                                } else {
                                                    prototype.setProperty(newField.getPath(), value);
                                                }
                                            }
                                        }
                                    }
                                } else if (oldField.getPath().equals("*") || newField.getPath().equals("*")) {
                                    log.warn("Wildcard fields are not supported");
                                }
                            } else {
                                if (oldField.getPath().equals("*")) {
                                    log
                                            .warn("Removing wildcard fields is unsupported.  Items that fall under the definition will not be removed.");
                                } else {
                                    if (fieldType.isNode()) {
                                        if (prototype.hasNode(oldField.getPath())) {
                                            prototype.getNode(oldField.getPath()).remove();
                                        }
                                    } else {
                                        if (prototype.hasProperty(oldField.getPath())) {
                                            prototype.getProperty(oldField.getPath()).remove();
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (RepositoryException ex) {
                    log.error(ex.getMessage());
                } finally {
                    typeVersion = new TypeDescriptor(typeConfig.getTypeDescriptor(typeName).getMapRepresentation());
                }
            }
        }
        super.receive(notification);
    }

    protected Panel createTemplate(JcrNodeModel model) {
        try {
            Node node = model.getNode();
            if (!node.isNodeType(HippoNodeType.NT_TEMPLATETYPE)) {
                log.error("Invalid node type");
                return new EmptyPanel("template");
            }

            TemplateEngine engine = getPluginManager().getTemplateEngine();
            if (!node.hasNode(HippoNodeType.HIPPO_TEMPLATE)) {
                BuiltinTemplateConfig builtinConfig = new BuiltinTemplateConfig(typeConfig);
                TemplateDescriptor descriptor = builtinConfig.getTemplate(typeConfig.getTypeDescriptor(node.getName()),
                        TemplateConfig.EDIT_MODE);

                Node template = node.addNode(HippoNodeType.HIPPO_TEMPLATE, HippoNodeType.NT_HANDLE);
                template = template.addNode(HippoNodeType.HIPPO_TEMPLATE, HippoNodeType.NT_TEMPLATE);
                template.addMixin("mix:referenceable");

                RepositoryTemplateConfig repoConfig = new RepositoryTemplateConfig();
                repoConfig.save(template, descriptor);
            }

            node = node.getNode(HippoNodeType.HIPPO_TEMPLATE).getNode(HippoNodeType.HIPPO_TEMPLATE);

            TypeDescriptor typeDescriptor = typeConfig.getTypeDescriptor(HippoNodeType.NT_TEMPLATE);
            TemplateDescriptor templateDescriptor = engine.getTemplateConfig().getTemplate(typeDescriptor,
                    TemplateConfig.EDIT_MODE);

            if (templateDescriptor != null) {
                TemplateModel templateModel = new TemplateModel(templateDescriptor, new JcrNodeModel(node.getParent()),
                        node.getName(), node.getIndex());

                return engine.createTemplate("template", templateModel, this, null);
            } else {
                PluginDescriptor descriptor = new PluginDescriptor("template", EmptyPlugin.class.getName());
                return new PluginFactory(getPluginManager()).createPlugin(descriptor, null, this);
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return new EmptyPanel("template");
    }

}
