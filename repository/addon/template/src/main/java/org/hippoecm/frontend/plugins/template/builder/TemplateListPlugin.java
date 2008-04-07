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

import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.jackrabbit.value.StringValue;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.frontend.plugin.parameters.ParameterValue;
import org.hippoecm.frontend.plugins.template.field.NodeFieldPlugin;
import org.hippoecm.frontend.plugins.template.field.PropertyFieldPlugin;
import org.hippoecm.frontend.template.TemplateDescriptor;
import org.hippoecm.frontend.template.TemplateEngine;
import org.hippoecm.frontend.template.TypeDescriptor;
import org.hippoecm.frontend.template.config.JcrTypeModel;
import org.hippoecm.frontend.template.config.RepositoryTypeConfig;
import org.hippoecm.frontend.template.config.TemplateConfig;
import org.hippoecm.frontend.template.config.TypeConfig;
import org.hippoecm.frontend.template.model.ItemModel;
import org.hippoecm.frontend.widgets.AbstractView;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.standardworkflow.RemodelWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateListPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(TemplateListPlugin.class);

    private JcrTypeModel typeModel;
    private JcrNodeModel templateNodeModel;

    public TemplateListPlugin(PluginDescriptor pluginDescriptor, final IPluginModel pluginModel, Plugin parentPlugin) {
        super(pluginDescriptor, new ItemModel(pluginModel), parentPlugin);

        ItemModel itemModel = (ItemModel) getPluginModel();
        templateNodeModel = itemModel.getNodeModel();
        try {
            Node typeNode = templateNodeModel.getNode();
            while (!typeNode.isNodeType(HippoNodeType.NT_TEMPLATETYPE)) {
                typeNode = typeNode.getParent();
            }
            RepositoryTypeConfig typeConfig = new RepositoryTypeConfig(RemodelWorkflow.VERSION_DRAFT);
            typeModel = typeConfig.getTypeModel(typeNode.getName());
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }

        List<TemplateDescriptor> templateList = new LinkedList<TemplateDescriptor>();
        ParameterValue templateValue = pluginDescriptor.getParameter("templates");
        if (templateValue != null) {
            TemplateEngine engine = parentPlugin.getPluginManager().getTemplateEngine();
            TemplateConfig templateConfig = engine.getTemplateConfig();
            TypeConfig defaultTypeConfig = engine.getTypeConfig();
            for (String type : templateValue.getStrings()) {
                TypeDescriptor typeDescriptor = defaultTypeConfig.getTypeDescriptor(type);
                TemplateDescriptor template = templateConfig.getTemplate(typeDescriptor);
                if (template != null) {
                    templateList.add(template);
                }
            }
        }

        AbstractView templates = new AbstractView("templates", new ListDataProvider(templateList), this) {
            private static final long serialVersionUID = 1L;

            public void populateItem(Item item) {
                final TemplateDescriptor template = (TemplateDescriptor) item.getModelObject();
                AjaxLink link = new AjaxLink("template") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        addField(template);

                        Channel channel = getTopChannel();
                        Request request = channel.createRequest("flush", templateNodeModel);
                        channel.send(request);
                        request.getContext().apply(target);
                    }
                };
                link.add(new Label("template-name", new Model(template.getTypeDescriptor().getName())));
                item.add(link);
            }

            public void destroyItem(Item item) {
                // nothing
            }
        };
        templates.populate();
        add(templates);
    }

    @Override
    public void onDetach() {
        typeModel.detach();
        templateNodeModel.detach();
        super.onDetach();
    }

    protected void addField(TemplateDescriptor template) {
        try {
            String name = typeModel.addField(template.getTypeDescriptor().getName());

            // add item to template
            Node templateNode = templateNodeModel.getNode();
            Node itemNode = templateNode.addNode(HippoNodeType.HIPPO_ITEM, HippoNodeType.NT_TEMPLATEITEM);
            itemNode.setProperty(HippoNodeType.HIPPO_FIELD, name);
            if (template.getTypeDescriptor().isNode()) {
                itemNode.setProperty(HippoNodeType.HIPPO_RENDERER, NodeFieldPlugin.class.getName());
            } else {
                itemNode.setProperty(HippoNodeType.HIPPO_RENDERER, PropertyFieldPlugin.class.getName());
            }
            Node paramNode = itemNode.addNode(HippoNodeType.HIPPO_PARAMETERS, HippoNodeType.NT_PARAMETERS);
            paramNode.setProperty("caption", new Value[] { new StringValue(template.getTypeDescriptor().getName()) });
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }
}
