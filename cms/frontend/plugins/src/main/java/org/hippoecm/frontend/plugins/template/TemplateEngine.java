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
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.PluginFactory;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.INotificationListener;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugins.template.config.FieldDescriptor;
import org.hippoecm.frontend.plugins.template.config.TemplateConfig;
import org.hippoecm.frontend.plugins.template.config.TemplateDescriptor;
import org.hippoecm.frontend.plugins.template.model.FieldModel;
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

        setModel(model);
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
        JcrItemModel itemModel = fieldModel.getItemModel();

        if(field.isNode()) {
            if (field.isMultiple()) {
                // wrap multi-valued fields (i.e. same-name siblings or optional fields)
                // in a MultiTemplate.  Fields can thus be added, removed and ordered.
                return new MultiTemplate(wicketId, fieldModel, this);

            } else {
                // for nodes, a template must be defined for the node type.
                // the field specifies a template
                TemplateDescriptor descriptor = getConfig().getTemplate(field.getType());
                return new Template(wicketId, new JcrNodeModel(itemModel), descriptor, this);
            }
        } else {
            if (field.getRenderer() != null) {
                // the field specifies a renderer, instantiate the plugin with the parent of the
                // node.  The field description is passed with initTemplatePlugin call.
    
                // template does not apply to parent of root => parent exists
                JcrNodeModel model = new JcrNodeModel(itemModel);
    
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
                JcrPropertyModel model = new JcrPropertyModel(itemModel.getPath() + "/" + field.getPath());
                return new ValueTemplate(wicketId, model, field, this);
            }
        }
    }

    public Component createWidget(String wicketId, FieldDescriptor descriptor, JcrPropertyValueModel model) {
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
