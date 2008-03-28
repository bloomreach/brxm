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

package org.hippoecm.frontend.plugins.cms.action;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.ChannelFactory;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

public class ActionPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ActionPlugin.class);

    private AjaxLink edit;

    public ActionPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, new JcrNodeModel(model), parentPlugin);

        edit = new AjaxLink("edit-link", getPluginModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                Channel channel = getTopChannel();
                if (channel != null) {
                    Request request = channel.createRequest("edit", getPluginModel());
                    channel.send(request);
                    request.getContext().apply(target);
                }
            }

        };
        add(edit);

        JcrNodeModel jcrModel = (JcrNodeModel) getModel();
        Channel channel = getTopChannel();
        ChannelFactory factory = getPluginManager().getChannelFactory();

        // setVisibilities();
        edit.setVisible(true);
    }

    @Override
    public void receive(Notification notification) {
        if ("select".equals(notification.getOperation())) {
            JcrNodeModel model = new JcrNodeModel(notification.getModel());

            try {
                HippoNode n = model.getNode();
                System.err.println("received "+n.getPath());
                if(n.isNodeType("hippo:handle")) {
                    Node best = null;
                    for(NodeIterator iter = n.getNodes(); iter.hasNext(); ) {
                        Node sub = iter.nextNode();
                        if(sub.isNodeType("hippo:document")) {
                            if(sub.hasProperty("hippostd:state")) {
                                if(sub.getProperty("hippostd:state").getString().equals("draft")) {
                                    best = sub;
                                } else if(sub.getProperty("hippostd:state").getString().equals("unpublished") && (best == null || !best.getProperty("hippostd:state").getString().equals("draft"))) {
                                    best = sub;
                                } else if(sub.getProperty("hippostd:state").getString().equals("unpublished") && best == null) {
                                    best = sub;
                                }
                            }
                        }
                    }
                    if(best != null) {
                        model = new JcrNodeModel(best);
                        Channel channel = getTopChannel();
                        if (channel != null) {
                            Request request = channel.createRequest("select", model);
                            channel.send(request);
                            // request.getContext().apply(model);
                        }
                    }
                }
            } catch(RepositoryException ex) {
                log.error(ex.getMessage());
            }
            // populateView(new JcrNodeModel(notification.getModel()));
            setPluginModel(model);

            try {
                Node node = new JcrNodeModel(getPluginModel()).getNode();
                Node canonical = ((HippoNode)node).getCanonicalNode();
                boolean isRoot = node.isNodeType("rep:root");            
                boolean isVirtual = canonical == null || !canonical.isSame(node);
                if(isRoot || isVirtual) {
                    edit.setVisible(false);
                } else if(canonical != null && canonical.hasProperty("hippostd:state")) {
                    System.err.println("BERRY "+canonical.getProperty("hippostd:state").getString());
                    edit.setVisible(canonical.getProperty("hippostd:state").getString().equals("draft"));
                }
            } catch(RepositoryException ex) {
                log.error(ex.getMessage());
                edit.setVisible(false);
            }

            notification.getContext().addRefresh(this);
        }
        super.receive(notification);
    }
}
