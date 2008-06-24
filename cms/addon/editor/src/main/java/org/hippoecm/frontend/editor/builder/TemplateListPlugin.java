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
import javax.jcr.Value;

import org.apache.jackrabbit.value.StringValue;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.plugins.field.NodeFieldPlugin;
import org.hippoecm.frontend.editor.plugins.field.PropertyFieldPlugin;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standardworkflow.types.ITypeDescriptor;
import org.hippoecm.frontend.plugins.standardworkflow.types.JcrTypeStore;
import org.hippoecm.frontend.service.IJcrService;
import org.hippoecm.frontend.service.render.ListViewService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.widgets.AbstractView;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.standardworkflow.RemodelWorkflow;
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
            Node templateHandle = typeNode.getNode(HippoNodeType.HIPPO_TEMPLATE);
            NodeIterator templates = templateHandle.getNodes(HippoNodeType.HIPPO_TEMPLATE);
            while (templates.hasNext()) {
                Node template = templates.nextNode();
                if (template.isNodeType("frontend:plugincluster")) {
                    templateNodeModel = new JcrNodeModel(template);
                    JcrTypeStore typeStore = new JcrTypeStore(RemodelWorkflow.VERSION_DRAFT);
                    editedType = typeStore.getTypeDescriptor(typeNode.getName());
                    break;
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
                };
                final String name = type.getName();
                link.add(new Label("template-name", new Model(name)));
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
        editedType.detach();
        templateNodeModel.detach();
        super.onDetach();
    }

    protected void addField(ITypeDescriptor typeDescriptor) {
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
            itemNode.setProperty("wicket.id", "{cluster}.field");
            itemNode.setProperty("wicket.model", "{cluster}.model");
            itemNode.setProperty("mode", "cluster:mode");
            itemNode.setProperty("engine", "cluster:engine");
            itemNode.setProperty("field", name);
            itemNode.setProperty(ListViewService.ITEM, "{cluster}." + pluginName);
            itemNode.setProperty("template.wicket.id", "{cluster}." + pluginName);
            itemNode.setProperty("caption", new Value[] { new StringValue(typeDescriptor.getName()) });

            IJcrService jcrService = getPluginContext().getService(IJcrService.class.getName(), IJcrService.class);
            jcrService.flush((JcrNodeModel) getModel());
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }
}
