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
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.PluginFactory;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.INotificationListener;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.widgets.TextAreaWidget;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.api.HippoNodeType;

public class TemplateEngine extends Form implements INotificationListener {

    private static final long serialVersionUID = 1L;

    private Template template;
    private TemplateConfig config;
    private Plugin plugin;

    public TemplateEngine(String wicketId, TemplateDescriptor descriptor, JcrNodeModel model, TemplateConfig config,
            Plugin plugin) {
        super(wicketId, model);
        this.config = config;
        this.plugin = plugin;

        Channel incoming = plugin.getDescriptor().getIncoming();
        if (incoming != null) {
            incoming.subscribe(this);
        }

        add(template = new Template("template", model, descriptor, this));
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public TemplateConfig getConfig() {
        return config;
    }

    public void receive(Notification notification) {
        if ("select".equals(notification.getOperation())) {
            try {
                JcrNodeModel model = new JcrNodeModel(notification.getData());
                Node node = model.getNode().getCanonicalNode();
                if (node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                    setTemplate(node);
                    setModel(model);
                    notification.getContext().addRefresh(this);
                }
            } catch (RepositoryException ex) {
                // TODO: log error
                ex.printStackTrace();
            }
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
            } else {
                // complain
                System.err.println("child is not a ITemplatePlugin");
            }
            return child;
        } else if (field.getTemplate() != null) {
            // the field specifies a template
            TemplateDescriptor descriptor = getConfig().getTemplate(field.getTemplate());
            if (fieldModel.getObject() instanceof Node) {
                return new Template(wicketId, new JcrNodeModel((Node) fieldModel.getObject()), descriptor, this);
            } else {
                // should not happen
                return new Label(wicketId, "xxx");
            }
        } else {
            if (fieldModel.getObject() instanceof Property) {
                Property prop = (Property) fieldModel.getObject();
                JcrPropertyModel model = new JcrPropertyModel(prop);

                return new ValueTemplate(wicketId, model, field, this);
            } else {
                // should not happen
                return new Label(wicketId, "yyy");
            }
        }
    }

    public Component createWidget(String wicketId, FieldDescriptor descriptor, JcrPropertyValueModel model)
            throws RepositoryException {
        if (descriptor.isBinary()) {
            return new Label("value", "(binary)");
        } else if (descriptor.isProtected()) {
            return new Label("value", model);
        } else if (descriptor.isLarge()) {
            return new TextAreaWidget("value", model);
        } else {
            return new TextFieldWidget("value", model);
        }
    }

    private boolean setTemplate(Node node) throws RepositoryException {
        NodeType type = node.getPrimaryNodeType();
        TemplateDescriptor descriptor = config.getTemplate(type.getName());
        if (descriptor != null) {
            template = new Template("template", new JcrNodeModel(node), descriptor, this);
            replace(template);
            return true;
        }
        return false;
    }
}
