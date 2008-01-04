/*
 * Copyright 2007 Hippo
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
package org.hippoecm.frontend.plugins.template;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.PluginFactory;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.INotificationListener;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.api.HippoNodeType;

public class TemplateEngine extends Form implements INotificationListener {

    private static final long serialVersionUID = 1L;

    private TemplateConfig config;
    private Plugin plugin;

    public TemplateEngine(String wicketId, TemplateDescriptor descriptor, JcrNodeModel model, TemplateConfig config,
            Plugin plugin) {
        super(wicketId, model);
        this.config = config;
        this.plugin = plugin;

        setOutputMarkupId(true);

        Channel incoming = plugin.getDescriptor().getIncoming();
        if (incoming != null) {
            incoming.subscribe(this);
        }

        add(new Template("template", model, descriptor, this));
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public TemplateConfig getConfig() {
        return config;
    }

    @Override
    public Component setModel(IModel model) {
        JcrNodeModel nodeModel = (JcrNodeModel) model;
        try {
            // FIXME: currently, the first document under the hippo:handle is
            // used.  This should be under the control of the workflow.
            Node node = nodeModel.getNode().getCanonicalNode();
            if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                NodeIterator children = node.getNodes();
                while (children.hasNext()) {
                    Node child = children.nextNode();
                    if (setTemplate(child)) {
                        break;
                    }
                }
            } else {
                setTemplate(node);
            }
        } catch (RepositoryException ex) {
            ex.printStackTrace();
        }
        super.setModel(model);
        return this;
    }

    public void receive(Notification notification) {
        if ("select".equals(notification.getOperation())) {
            JcrNodeModel model = new JcrNodeModel(notification.getData());
            setModel(model);
            notification.getContext().addRefresh(this);
        }
    }

    public void onChange(AjaxRequestTarget target) {

        // FIXME: send a request up to notify other components

        if (target != null && findPage() != null) {
            target.addComponent(this);
        }
    }

    public Component createTemplate(String wicketId, FieldModel fieldModel) {
        FieldDescriptor field = fieldModel.getDescriptor();
        if (field.getRenderer() != null) {
            // the field specifies a renderer, let it handle the item
            JcrNodeModel model = null;
            try {
                Property property = (Property) fieldModel.getObject();
                model = new JcrNodeModel((Node) property.getParent());
            } catch (RepositoryException ex) {
                ex.printStackTrace();
            }

            // create a new channel
            // FIXME: should the outgoing channel be shared between plugins?
            Channel outgoing = getPlugin().getPluginManager().getChannelFactory().createChannel();

            // instantiate the plugin that should handle the field
            String className = field.getRenderer();
            PluginDescriptor pluginDescriptor = new PluginDescriptor(wicketId, className, outgoing);
            PluginFactory pluginFactory = new PluginFactory(getPlugin().getPluginManager());
            Plugin child = pluginFactory.createPlugin(pluginDescriptor, model, getPlugin());

            if (child instanceof ITemplatePlugin) {
                ((ITemplatePlugin) child).initTemplatePlugin(field, this);
            }
            return child;
        } else {
            // if no renderer is specified, fall back to default behaviour;
            // for nodes, a template must be defined for the node type.
            TemplateDescriptor descriptor = getConfig().getTemplate(field.getType());
            if (descriptor != null) {
                // the field specifies a template
                JcrNodeModel nodeModel = new JcrNodeModel((Node) fieldModel.getObject());
                return new Template(wicketId, nodeModel, descriptor, this);
            } else {
                Property prop = (Property) fieldModel.getObject();
                JcrPropertyModel model = new JcrPropertyModel(prop);
                return new ValueTemplate(wicketId, model, field, this);
            }
        }
    }

    public Component createWidget(String wicketId, FieldDescriptor descriptor, JcrPropertyValueModel model)
            throws RepositoryException {
        if (descriptor.isBinary()) {
            return new Label("value", "(binary)");
        } else if (descriptor.isProtected()) {
            return new Label("value", model);
        } else {
            return new TextFieldWidget("value", model);
        }
    }

    private boolean setTemplate(Node node) throws RepositoryException {
        NodeType type = node.getPrimaryNodeType();
        TemplateDescriptor descriptor = config.getTemplate(type.getName());
        if (descriptor != null) {
            replace(new Template("template", new JcrNodeModel(node), descriptor, this));
        }
        return false;
    }
}
