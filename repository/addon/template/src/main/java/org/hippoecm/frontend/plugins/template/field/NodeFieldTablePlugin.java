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
package org.hippoecm.frontend.plugins.template.field;

import java.util.LinkedList;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.template.FieldDescriptor;
import org.hippoecm.frontend.template.ItemDescriptor;
import org.hippoecm.frontend.template.config.TemplateConfig;
import org.hippoecm.frontend.template.model.ItemModel;
import org.hippoecm.frontend.template.model.NodeTemplateProvider;
import org.hippoecm.frontend.template.model.TemplateModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeFieldTablePlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(NodeFieldTablePlugin.class);

    private FieldDescriptor field;
    private NodeTemplateProvider provider;
    private TemplateTableView view;
    private String mode;
    private List<TemplateModel> selected;

    public NodeFieldTablePlugin(PluginDescriptor pluginDescriptor, IPluginModel pluginModel, Plugin parentPlugin) {
        super(pluginDescriptor, new ItemModel(pluginModel), parentPlugin);

        selected = new LinkedList<TemplateModel>();

        ItemModel model = (ItemModel) getPluginModel();
        ItemDescriptor descriptor = (ItemDescriptor) model.getDescriptor();

        field = descriptor.getTemplate().getTypeDescriptor().getField(descriptor.getField());
        mode = descriptor.getTemplate().getMode();

        List<String> captions = pluginDescriptor.getParameter("caption").getStrings();
        if (captions != null && captions.size() > 0) {
            add(new Label("name", captions.get(0)));
        } else {
            add(new Label("name", ""));
        }

        provider = new NodeTemplateProvider(field, getPluginManager().getTemplateEngine(), model.getNodeModel(), descriptor.getMode());
        view = new TemplateTableView("field", provider, this, pluginDescriptor.getParameters());
        add(view);

        add(createAddLink());
        add(createDeleteLink());

        setOutputMarkupId(true);
    }

    @Override
    public void receive(Notification notification) {
        if ("flush".equals(notification.getOperation())) {
            ItemModel fieldModel = (ItemModel) getPluginModel();
            String currentPath = fieldModel.getNodeModel().getItemModel().getPath();

            // refresh the provider if the sent node is a subnode
            JcrNodeModel model = new JcrNodeModel(notification.getModel());
            String path = model.getItemModel().getPath();
            if (path.length() >= currentPath.length()) {
                if (currentPath.equals(path.substring(0, path.length()))) {
                    provider.refresh();
                    notification.getContext().addRefresh(this);

                    replace(createAddLink());
                    view.populate();
                }
            }
        }
        super.receive(notification);
    }

    public void onAddNode(AjaxRequestTarget target) {
        provider.addNew();

        // refresh
        replace(createAddLink());

        if (target != null) {
            target.addComponent(this);
        }
    }

    public void onSelectNode(TemplateModel childModel, AjaxRequestTarget target) {
        selected.add(childModel);

        if (target != null) {
            target.addComponent(get("remove"));
        }
    }

    public void onDeselectNode(TemplateModel childModel, AjaxRequestTarget target) {
        selected.remove(childModel);

        if (target != null) {
            target.addComponent(get("remove"));
        }
    }

    protected Component createAddLink() {
        if (TemplateConfig.EDIT_MODE.equals(mode) && (field.isMultiple() || (provider.size() == 0))) {
            return new AjaxLink("add") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    NodeFieldTablePlugin.this.onAddNode(target);
                }
            };
        } else {
            return new Label("add", "");
        }
    }

    protected Component createDeleteLink() {
        if (TemplateConfig.EDIT_MODE.equals(mode)) {
            return new AjaxLink("remove") {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isEnabled() {
                    return (selected.size() > 0);
                }

                @Override
                public void onClick(AjaxRequestTarget target) {
                    try {
                        for (TemplateModel model : selected) {
                            model.getJcrNodeModel().getNode().remove();
                        }
                        selected = new LinkedList<TemplateModel>();
                    } catch (RepositoryException ex) {
                        log.error(ex.getMessage());
                    }
                    provider.refresh();
                    view.populate();
                    NodeFieldTablePlugin.this.replace(createAddLink());
                    target.addComponent(NodeFieldTablePlugin.this);
                }
            }.setOutputMarkupId(true);
        } else {
            return new Label("remove", "");
        }
    }
}
