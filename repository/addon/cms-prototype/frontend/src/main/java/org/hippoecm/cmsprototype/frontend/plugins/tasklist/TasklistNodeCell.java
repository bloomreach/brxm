/*
 * Copyright 2007-2008 Hippo
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

package org.hippoecm.cmsprototype.frontend.plugins.tasklist;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.cmsprototype.frontend.plugins.generic.list.NodeCell;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Request;

public class TasklistNodeCell extends NodeCell {

    private static final long serialVersionUID = 1L;

    public TasklistNodeCell(String id, NodeModelWrapper model, Channel channel, String nodePropertyName) {
        super(id, model, channel, nodePropertyName);
    }

    @Override
    protected boolean hasDefaultCustomizedLabels(String nodePropertyName) {
        // [TODO] use a constant
        if("documentname".equals(nodePropertyName)) {
            return true;
        }
        return false; 
    }

    @Override
    protected void sendChannelRequest(NodeModelWrapper model, AjaxRequestTarget target, Channel channel) {
        // create a "select" request with the node path as a parameter

        if(model instanceof Task) {
            JcrNodeModel nodeModel = ((NodeModelWrapper) model).getNodeModel();            
            Request request = channel.createRequest("browse", nodeModel);
            channel.send(request);
            request.getContext().apply(target);
        }
    }
    
    @Override
    protected void addDefaultCustomizedLabel(NodeModelWrapper model, String nodePropertyName, AjaxLink link) {
        if(model instanceof Task) {
            Task task = (Task)model;
            // [TODO] use a constant
            if("documentname".equals(nodePropertyName)) {
                link.add(new Label("label",task.getDocumentname()));
                return;
            }
        }
    }
    
}
