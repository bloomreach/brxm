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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.plugins.field.FieldPlugin;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IJcrService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreviewPluginPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(PreviewPluginPlugin.class);

    private JcrNodeModel pluginNodeModel;

    public PreviewPluginPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        pluginNodeModel = new JcrNodeModel(config.getString("plugin.node.path"));

        add(new AjaxLink("up") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                try {
                    Node pluginNode = pluginNodeModel.getNode();
                    NodeIterator siblings = pluginNode.getParent().getNodes();
                    Node previous = null;
                    while (siblings.hasNext()) {
                        Node next = siblings.nextNode();
                        if (next.isNodeType("frontend:plugin")) {
                            if (next.isSame(pluginNode)) {
                                if (previous != null) {
                                    pluginNode.getParent().orderBefore(
                                            pluginNode.getName() + "[" + pluginNode.getIndex() + "]",
                                            previous.getName() + "[" + previous.getIndex() + "]");
                                    flush();
                                } else {
                                    log.warn("Unable to move the plugin up, as it is the first.");
                                }
                                break;
                            }
                            previous = next;
                        }
                    }
                } catch (RepositoryException ex) {
                    log.error(ex.getMessage());
                }
            }
        });
        add(new AjaxLink("down") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                try {
                    Node pluginNode = pluginNodeModel.getNode();
                    NodeIterator siblings = pluginNode.getParent().getNodes();
                    Node previous = null;
                    while (siblings.hasNext()) {
                        Node next = siblings.nextNode();
                        if (next.isNodeType("frontend:plugin")) {
                            if (previous != null && previous.isSame(pluginNode)) {
                                pluginNode.getParent().orderBefore(next.getName() + "[" + next.getIndex() + "]",
                                        previous.getName() + "[" + previous.getIndex() + "]");
                                flush();
                                break;
                            }
                            previous = next;
                        }
                    }
                } catch (RepositoryException ex) {
                    log.error(ex.getMessage());
                }
            }
        });
        add(new AjaxLink("remove") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                try {
                    Node node = pluginNodeModel.getNode();
                    if (node != null) {
                        JcrTypeHelper helper = new JcrTypeHelper(new JcrNodeModel(node.getParent().getParent()
                                .getParent()), "edit");

                        // clean up prototype
                        String fieldName = node.getProperty(FieldPlugin.FIELD).getString();
                        ITypeDescriptor type = helper.getTypeDescriptor();
                        IFieldDescriptor field = type.getField(fieldName);
                        if (!field.getPath().equals("*")) {
                            ITypeDescriptor subType = getTemplateEngine().getType(field.getType());
                            JcrNodeModel prototype = helper.getPrototype();
                            if (subType.isNode()) {
                                NodeIterator children = prototype.getNode().getNodes(field.getPath());
                                while (children.hasNext()) {
                                    Node child = children.nextNode();
                                    child.remove();
                                }
                            } else {
                                Node prototypeNode = prototype.getNode();
                                if (prototypeNode.hasProperty(field.getPath())) {
                                    prototypeNode.getProperty(field.getPath()).remove();
                                }
                            }
                        } else {
                            log.error("removing wildcard fields is not supported");
                        }

                        // clean up type
                        helper.getTypeDescriptor().removeField(fieldName);

                        // clean up template
                        node.remove();
                        flush();
                    }
                } catch (RepositoryException ex) {
                    log.error(ex.getMessage());
                }
            }
        });

        add(new AjaxEventBehavior("onclick") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                PreviewPluginPlugin.this.setModel(pluginNodeModel);
            }
            
        });
        addExtensionPoint("preview");
    }

    protected ITemplateEngine getTemplateEngine() {
        IPluginContext context = getPluginContext();
        return context.getService(getPluginConfig().getString("engine"), ITemplateEngine.class);
    }

    @Override
    public void onDetach() {
        pluginNodeModel.detach();
        super.onDetach();
    }

    private void flush() {
        IPluginContext context = getPluginContext();
        IJcrService jcrService = context.getService(IJcrService.class.getName(), IJcrService.class);
        if (jcrService != null) {
            jcrService.flush(pluginNodeModel.getParentModel());
        }
    }

}
