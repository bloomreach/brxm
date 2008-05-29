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
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.frontend.yui.dragdrop.node.DragNodeBehavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListNodesPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ListNodesPlugin.class);

    private final JcrNodeModel parentNodeModel;
    private final WebMarkupContainer listContainer;

    private class Entry implements Serializable {
        private static final long serialVersionUID = 1L;

        boolean newEntry;
        JcrNodeModel nodeModel;

        public Entry(JcrNodeModel nodeModel, boolean newEntry) {
            this.nodeModel = nodeModel;
            this.newEntry = newEntry;
        }
    }

    public ListNodesPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);

        String parentNodePath = pluginDescriptor.getParameter("parentNodePath").getStrings().get(0);
        parentNodeModel = new JcrNodeModel(parentNodePath);

        IModel myModel = new Model() {
            private static final long serialVersionUID = 1L;
            private List<Entry> list;

            @Override
            public Object getObject() {
                if (list == null) {
                    list = new LinkedList<Entry>();
                } else {
                    list.clear();
                }
                try {
                    NodeIterator nodes = parentNodeModel.getNode().getNodes();
                    while (nodes.hasNext()) {
                        Node node = nodes.nextNode();
                        Entry entry = new Entry(new JcrNodeModel(node), node.isNew());
                        list.add(entry);
                    }
                } catch (RepositoryException ex) {
                    log.error(ex.getMessage());
                }
                Collections.sort(list, new Comparator<Entry>() {
                    public int compare(Entry o1, Entry o2) {
                        try {
                            return o1.nodeModel.getNode().getName().compareTo(o2.nodeModel.getNode().getName());
                        } catch (RepositoryException e) {
                            log.error("Error while comparing node names", e);
                        }
                        return 0;
                    }
                });
                return list;
            }
        };

        final ListView listView = new ListView("list", myModel) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem item) {
                final Entry entry = (Entry) item.getModel().getObject();
                String name = "unknown";
                try {
                    name = entry.nodeModel.getNode().getDisplayName();
                } catch (RepositoryException ex) {
                    log.error(ex.getMessage());
                }
                if (entry.newEntry) {
                    name += " *";
                }

                item.add(new AjaxLink("link") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        Channel top = getTopChannel();
                        Request request = top.createRequest("select", entry.nodeModel);
                        top.send(request);
                        request.getContext().apply(target);
                    }

                }.add(new Label("name", name)));
                if (!entry.newEntry)
                    item.add(new DragNodeBehavior(entry.nodeModel));

            }
        };
        listView.setOutputMarkupId(true);

        listContainer = new WebMarkupContainer("listContainer");
        listContainer.setOutputMarkupId(true);
        listContainer.add(listView);
        add(listContainer);

        String label = pluginDescriptor.getParameter("label").getStrings().get(0);
        String nodeType = pluginDescriptor.getParameter("nodeType").getStrings().get(0);
        add(new AddNodeWidget("newNode", new Model(label), parentNodeModel, nodeType) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onAddNode(AjaxRequestTarget target, Node node) {
                Channel top = getTopChannel();
                Request request = top.createRequest("select", new JcrNodeModel(node));
                top.send(request);
                request.getContext().apply(target);
                target.addComponent(listContainer);
            }
        });
    }

    @Override
    public void receive(Notification notification) {
        if (notification.getOperation().equals("flush")) {
            notification.getContext().addRefresh(listContainer);
        }
    }
}
