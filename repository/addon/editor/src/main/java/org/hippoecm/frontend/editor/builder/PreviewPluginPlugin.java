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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IJcrService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreviewPluginPlugin extends RenderPlugin {
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
                                pluginNode.getParent().orderBefore(
                                        next.getName() + "[" + next.getIndex() + "]",
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
        add(new AjaxLink("edit") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                PreviewPluginPlugin.this.setModel(pluginNodeModel);
            }
        });
        add(new AjaxLink("remove") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                try {
                    Node node = pluginNodeModel.getNode();
                    if (node != null) {
                        node.remove();
                        flush();
                    }
                } catch (RepositoryException ex) {
                    log.error(ex.getMessage());
                }
            }
        });

        addExtensionPoint("preview");
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
