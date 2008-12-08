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
package org.hippoecm.frontend.editor.builder;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.plugins.field.NodeFieldPlugin;
import org.hippoecm.frontend.editor.plugins.field.PropertyFieldPlugin;
import org.hippoecm.frontend.i18n.types.TypeTranslator;
import org.hippoecm.frontend.model.IModelService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IJcrService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.JcrTypeStore;
import org.hippoecm.frontend.widgets.AbstractView;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateListPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(TemplateListPlugin.class);

    private List<ITypeDescriptor> templateList;
    private ITypeDescriptor editedType;
    private JcrNodeModel templateNodeModel;

    public TemplateListPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        JcrNodeModel templateTypeModel = (JcrNodeModel) getModel();
        try {
            Node typeNode = templateTypeModel.getNode();
            if (typeNode.hasNode(HippoNodeType.HIPPO_TEMPLATE)) {
                Node templateHandle = typeNode.getNode(HippoNodeType.HIPPO_TEMPLATE);
                NodeIterator templates = templateHandle.getNodes(HippoNodeType.HIPPO_TEMPLATE);
                while (templates.hasNext()) {
                    Node template = templates.nextNode();
                    if (template.isNodeType("frontend:plugincluster")) {
                        templateNodeModel = new JcrNodeModel(template);
                        JcrTypeStore typeStore = new JcrTypeStore(typeNode.getParent().getName());
                        editedType = typeStore.getTypeDescriptor(typeNode.getParent().getName() + ":"
                                + NodeNameCodec.decode(typeNode.getName()));
                        break;
                    }
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }

        templateList = new LinkedList<ITypeDescriptor>();
        ITemplateEngine engine = context.getService(config.getString(ITemplateEngine.ENGINE), ITemplateEngine.class);
        if (engine != null) {
            String[] templates = config.getStringArray("templates");
            if (templates != null) {
                for (String type : templates) {
                    ITypeDescriptor typeDescriptor = engine.getType(type);
                    if (engine.getTemplate(typeDescriptor, ITemplateEngine.EDIT_MODE) != null) {
                        templateList.add(typeDescriptor);
                    }
                }
            } else {
                log.warn("No templates configured");
            }
        } else {
            log.error("No template engine found under {}", config.getString(ITemplateEngine.ENGINE));
        }

        AbstractView templateView = new AbstractView("templates", new ListDataProvider(templateList)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item item) {
                final ITypeDescriptor type = (ITypeDescriptor) item.getModelObject();
                AjaxLink link = new AjaxLink("template") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        addField(type);
                    }

                    @Override
                    public boolean isEnabled() {
                        return "edit".equals(getPluginConfig().getString("mode"));
                    }
                };
                final String name = type.getName();
                link.add(new Label("template-name", new TypeTranslator(new JcrNodeTypeModel(name)).getTypeName()));
                item.add(link);

                item.add(new AbstractBehavior() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onComponentTag(final Component component, final ComponentTag tag) {
                        Object classes = tag.getUserData("class");
                        if (classes instanceof String) {
                            tag.put("class", (String) classes + " " + name.toLowerCase().replace(':', '_'));
                        } else {
                            tag.put("class", name.toLowerCase().replace(':', '_'));
                        }
                    }
                });
            }

            @Override
            public void destroyItem(Item item) {
                // nothing
            }
        };
        templateView.populate();
        add(templateView);
    }

    @Override
    public void onDetach() {
        for (ITypeDescriptor type : templateList) {
            type.detach();
        }
        if (editedType != null) {
            editedType.detach();
            templateNodeModel.detach();
        }
        super.onDetach();
    }

    protected void addField(ITypeDescriptor typeDescriptor) {
        if(editedType != null) {
            try {
                String name = editedType.addField(typeDescriptor.getName());

                // add item to template
                Node templateNode = templateNodeModel.getNode();

                String pluginName = UUID.randomUUID().toString();
                Node itemNode = templateNode.addNode(pluginName, "frontend:plugin");
                if (typeDescriptor.isNode()) {
                    itemNode.setProperty(IPlugin.CLASSNAME, NodeFieldPlugin.class.getName());
                } else {
                    itemNode.setProperty(IPlugin.CLASSNAME, PropertyFieldPlugin.class.getName());
                }
                itemNode.setProperty("wicket.id", "${cluster.id}.field");
                itemNode.setProperty("wicket.model", "${wicket.model}");
                itemNode.setProperty("mode", "${mode}");
                itemNode.setProperty("engine", "${engine}");
                itemNode.setProperty("field", name);
                itemNode.setProperty("caption", new String[] { typeDescriptor.getName() });

                IJcrService jcrService = getPluginContext().getService(IJcrService.class.getName(), IJcrService.class);
                jcrService.flush((JcrNodeModel) getModel());

                // update helper model
                select(new JcrNodeModel(itemNode));

            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        } else {
            log.error("No type is being edited");
        }
    }

    private void select(JcrNodeModel model) {
        IModelService helperModel = getPluginContext().getService(getPluginConfig().getString("helper.model"),
                IModelService.class);
        if (helperModel != null) {
            helperModel.setModel(model);
        }
    }
}
