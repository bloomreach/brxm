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
package org.hippoecm.cmsprototype.frontend.plugins.todo;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.frontend.session.UserSession;

public class NodeCell extends Panel {
    private static final long serialVersionUID = 1L;

    public NodeCell(String id, NodeModelWrapper model, final Channel channel) {
        super(id, model);
        AjaxLink link = new AjaxLink("link", model) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                // create a "select" request with the node path as a parameter
                JcrNodeModel nodeModel = ((NodeModelWrapper)this.getModel()).getNodeModel(); 
                Request request = channel.createRequest("select", nodeModel.getMapRepresentation());
                channel.send(request);
                request.getContext().apply(target);
            }
        
        };
        add(link);
        
        String type = "";
        String username ="";
        String doc = "";
        String reason = "";
        String path ="";
        
        try {
                
                type = model.getNodeModel().getNode().getProperty("type").getString();
                        username = model.getNodeModel().getNode().getProperty("username").getString();

                        UserSession session = (UserSession) Session.get();
                Node node = session.getJcrSession().getNodeByUUID(model.getNodeModel().getNode().getProperty("document").getString());
                        
                if(node != null) {
                        doc = node.getName();
                        path = node.getPath();
                }else {
                        doc ="unknown";
                }
                
                
                        reason = model.getNodeModel().getNode().getProperty("reason").getString();
                        
                } catch (ValueFormatException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                } catch (PathNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                } catch (RepositoryException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                }

                add(new Label("type", type));
        add(new Label("username", username));
        add(new Label("path", path));
        add(new Label("reason", reason));
        
        link.add(new Label("label", doc));
        
    }


}
