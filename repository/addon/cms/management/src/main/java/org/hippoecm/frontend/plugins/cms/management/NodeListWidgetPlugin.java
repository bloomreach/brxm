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
package org.hippoecm.frontend.plugins.cms.management;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.YuiPluginHelper;
import org.hippoecm.frontend.plugins.yui.dragdrop.DragSettings;
import org.hippoecm.frontend.plugins.yui.dragdrop.NodeDragBehavior;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.api.HippoNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeListWidgetPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(NodeListWidgetPlugin.class);

    private final JcrNodeModel parentNodeModel;
    private final WebMarkupContainer listContainer;
    private FlushableListModel listModel;

    private class Entry implements Serializable {
        private static final long serialVersionUID = 1L;

        boolean newEntry;
        String path;
        String name;
        String displayName;
        boolean selected = false;

        public Entry(HippoNode node) {
            this.newEntry = node.isNew();
            try {
                this.path = node.getPath();
            } catch (RepositoryException e) {
                throw new IllegalArgumentException("Node path is not available", e);
            }
            try {
                this.displayName = node.getName();
            } catch (RepositoryException e) {
                throw new IllegalArgumentException("Node displayname is not available", e);
            }

            try {
                this.name = node.getName();
            } catch (RepositoryException e) {
                throw new IllegalArgumentException("Node name is not available", e);
            }
        }
    }

    public NodeListWidgetPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        String parentNodePath = config.getString("parentNodePath");
        parentNodeModel = new JcrNodeModel(parentNodePath);
        listModel = new FlushableListModel();

        final ListView listView = new ListView("list", listModel) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem item) {
                final Entry entry = (Entry) item.getModel().getObject();

                String name = entry.name;
                if (entry.newEntry) {
                    name += " *";
                }

                final JcrNodeModel nodeModel = new JcrNodeModel(entry.path);
                item.add(new AjaxLink("link") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        NodeListWidgetPlugin.this.setModel(nodeModel);
                    }

                }.add(new Label("name", name)));
                if (!entry.newEntry)
                    item.add(new NodeDragBehavior(YuiPluginHelper.getManager(context), new DragSettings(YuiPluginHelper.getConfig(config)), nodeModel) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected String getLabel() {
                            return entry.displayName;
                        }
                    });
                if (entry.selected)
                    item.add(new SimpleAttributeModifier("class", "highlight"));

            }
        };
        listView.setOutputMarkupId(true);

        listContainer = new WebMarkupContainer("listContainer");
        listContainer.setOutputMarkupId(true);
        listContainer.add(listView);
        add(listContainer);

        IModel label = new StringResourceModel(config.getString("label"), this, null);
        String nodeType = config.getString("nodeType");
        add(new AddNodeWidget("newNode", label, parentNodeModel, nodeType) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onAddNode(AjaxRequestTarget target, Node node) {
                try {
                    refreshListModel(node.getName());
                } catch (RepositoryException e) {
                    log.error("Error getting name from nodes", e);
                }
                JcrNodeModel nodeModel = new JcrNodeModel(node);
                NodeListWidgetPlugin.this.setModel(nodeModel);
                target.addComponent(listContainer);
            }
        });

        //onModelChanged();
    }

    @Override
    protected void onModelChanged() {
        JcrNodeModel serviceModel = (JcrNodeModel) getModel();
        String nodePath = serviceModel.getItemModel().getPath();
        String parentNodePath = getPluginConfig().getString("parentNodePath");
        if (nodePath.indexOf(parentNodePath) == 0) {
            String name = nodePath.substring(parentNodePath.length() + 1);
            refreshListModel(name);
            redraw();
        } else {
            for (Entry entry : listModel.list) {
                if (entry.selected) {
                    entry.selected = false;
                    redraw();
                    break;
                }
            }
        }
        super.onModelChanged();
    }

    private void refreshListModel(String name) {
        listModel.flush(name);
    }

    class FlushableListModel extends Model {
        private static final long serialVersionUID = 1L;
        private List<Entry> list;
        private boolean flush = true; //first run fills list
        private String selectedPath = null;

        @Override
        public Object getObject() {
            if (list == null)
                list = new LinkedList<Entry>();

            if (flush) {
                if (selectedPath == null) {
                    for (Entry entry : list) {
                        if (entry.selected) {
                            selectedPath = entry.path;
                            break;
                        }
                    }
                }
                list.clear();
                try {
                    NodeIterator nodes = parentNodeModel.getNode().getNodes();
                    while (nodes.hasNext()) {
                        Entry entry = new Entry((HippoNode) nodes.nextNode());
                        if (selectedPath != null && entry.path.equals(selectedPath))
                            entry.selected = true;
                        list.add(entry);
                    }
                } catch (RepositoryException ex) {
                    log.error("An error occurred while trying to get nodes from parentNode["
                            + parentNodeModel.getItemModel().getPath() + "]", ex);
                }
                Collections.sort(list, new Comparator<Entry>() {
                    public int compare(Entry o1, Entry o2) {
                        return o1.name.compareTo(o2.name);
                    }
                });
                flush = false;
                selectedPath = null;
            }
            return list;
        }

        public void flush(String selectedNodeName) {
            flush = true;
            this.selectedPath = parentNodeModel.getItemModel().getPath() + "/" + selectedNodeName;
        }

    }

}
