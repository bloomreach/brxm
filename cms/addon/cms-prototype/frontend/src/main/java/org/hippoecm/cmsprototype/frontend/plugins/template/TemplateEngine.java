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
package org.hippoecm.cmsprototype.frontend.plugins.template;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.JcrEvent;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginEvent;
import org.hippoecm.repository.api.HippoNodeType;

public class TemplateEngine extends Form {

    private static final long serialVersionUID = 1L;

    private Template template;
    private TemplateConfig config;
    private Plugin plugin;

    public TemplateEngine(String wicketId, TemplateDescriptor descriptor, JcrNodeModel model, TemplateConfig config,
            Plugin plugin) {
        super(wicketId, model);
        this.config = config;
        this.plugin = plugin;

        add(template = new Template("template", model, descriptor, this));
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public TemplateConfig getConfig() {
        return config;
    }

    public void update(AjaxRequestTarget target, PluginEvent event) {
        JcrNodeModel model = event.getNodeModel(JcrEvent.NEW_MODEL);
        if (model != null) {
            try {
                // FIXME: currently, the first document under the hippo:handle is
                // used.  This should be under the control of the workflow.
                Node node = model.getNode().getCanonicalNode();
                if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                    NodeIterator children = node.getNodes();
                    while (children.hasNext()) {
                        Node child = children.nextNode();
                        NodeType type = child.getPrimaryNodeType();
                        TemplateDescriptor descriptor = config.getTemplate(type.getName());
                        if (descriptor != null) {
                            template = new Template("template", new JcrNodeModel(null, child), descriptor, this);
                            replace(template);
                            break;
                        }
                    }
                }
            } catch (RepositoryException ex) {
                ex.printStackTrace();
            }
            setModel(model);
        }
        if (target != null && findPage() != null) {
            target.addComponent(this);
        }
    }

    public void onChange(AjaxRequestTarget target) {
        if (target != null && findPage() != null) {
            target.addComponent(this);
        }
    }
}
