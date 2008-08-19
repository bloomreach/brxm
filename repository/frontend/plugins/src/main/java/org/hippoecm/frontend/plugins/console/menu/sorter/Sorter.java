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
package org.hippoecm.frontend.plugins.console.menu.sorter;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.console.menu.MenuPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sorter extends Panel {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(Sorter.class);

    private AjaxLink up;
    private AjaxLink down;

    public Sorter(String id) {
        super(id);

        up = new AjaxLink("up") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                IModel model = Sorter.this.getModel();
                if (model instanceof JcrNodeModel) {
                    JcrNodeModel nodeModel = (JcrNodeModel) model;
                    Node node = nodeModel.getNode();
                    try {
                        Node parentNode = node.getParent();
                        NodeIterator siblings = parentNode.getNodes();
                        long position = -1;
                        while (siblings.hasNext()) {
                            Node sibling = siblings.nextNode();
                            if (sibling.getName().equals(nodeModel.getNode().getName())) {
                                position = siblings.getPosition();
                                break;
                            }
                        }
                        String placedBefore = null;
                        siblings = parentNode.getNodes();
                        for (int i = 0; i < position - 1; i++) {
                            placedBefore = siblings.nextNode().getName();
                        }
                        parentNode.orderBefore(node.getName(), placedBefore);

                        up.setEnabled(position > 2);
                        down.setEnabled(true);
                        
                        MenuPlugin menu = (MenuPlugin) findParent(MenuPlugin.class);
                        menu.redraw();
                        menu.flushNodeModel(nodeModel.getParentModel());
                    } catch (RepositoryException e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }
        };
        add(up);
        up.setEnabled(false);

        down = new AjaxLink("down") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                IModel model = Sorter.this.getModel();
                if (model instanceof JcrNodeModel) {
                    JcrNodeModel nodeModel = (JcrNodeModel) model;
                    Node node = nodeModel.getNode();
                    try {
                        Node parentNode = node.getParent();
                        NodeIterator siblings = parentNode.getNodes();
                        String placedBefore = null;
                        while (siblings.hasNext()) {
                            Node sibling = siblings.nextNode();
                            if (sibling.getName().equals(node.getName())) {
                                siblings.nextNode();
                                if (siblings.hasNext()) {
                                    placedBefore = siblings.nextNode().getName();
                                } else {
                                    placedBefore = null;
                                }
                                break;
                            }
                        }
                        parentNode.orderBefore(node.getName(), placedBefore);

                        up.setEnabled(true);
                        down.setEnabled(placedBefore != null);

                        MenuPlugin menu = (MenuPlugin) findParent(MenuPlugin.class);
                        menu.redraw();
                        menu.flushNodeModel(nodeModel.getParentModel());
                    } catch (RepositoryException e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }
        };
        add(down);
        down.setEnabled(false);
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();
        IModel model = getModel();
        if (model instanceof JcrNodeModel) {
            try {
                JcrNodeModel newModel = (JcrNodeModel) model;
                if (newModel.getNode().getDepth() == 0) {
                    up.setEnabled(false);
                    down.setEnabled(false);
                } else {
                    Node parent = newModel.getNode().getParent();
                    if (!parent.getPrimaryNodeType().hasOrderableChildNodes()) {
                        up.setEnabled(false);
                        down.setEnabled(false);
                    } else {
                        NodeIterator siblings = parent.getNodes();
                        long size = siblings.getSize();
                        long position = -1;
                        while (siblings.hasNext()) {
                            Node sibling = siblings.nextNode();
                            if (sibling.getName().equals(newModel.getNode().getName())) {
                                position = siblings.getPosition();
                                break;
                            }
                        }
                        if (position != -1) {
                            up.setEnabled(position > 1);
                            down.setEnabled(position < size);
                        }
                    }
                }
            } catch (RepositoryException e) {
                log.error(e.getMessage(), e);
            }
        }
        MenuPlugin plugin = (MenuPlugin) findParent(MenuPlugin.class);
        plugin.redraw();
    }

}
