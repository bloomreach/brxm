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
package org.hippoecm.cmsprototype.frontend.plugins.actions;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.model.ExceptionModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(DeleteDialog.class);
    
    public DeleteDialog(DialogWindow dialogWindow, Channel channel) {
        super(dialogWindow, channel);
        String message;
        try {
            Node node = dialogWindow.getNodeModel().getNode();
            message = "Delete node " + node.getName();
        } catch (RepositoryException e) {
            message = e.getMessage();
            ok.setEnabled(false);
        }
        dialogWindow.setTitle("Delete");
        add(new Label("message", message));
    }

    @Override
    public void ok() throws RepositoryException {
        Node node = dialogWindow.getNodeModel().getNode();
        if (node != null) {
            Node toBeDeleted = node;
            Node parent = node.getParent();
            Node parentHandle = Utils.findHandle(node);
            toBeDeleted.save();
            parent.save();
            if (channel != null) {
                Request request = channel.createRequest("select", new JcrNodeModel(parentHandle));
                channel.send(request);
                request = channel.createRequest("flush", new JcrNodeModel(parentHandle));
                channel.send(request);
            }
        }
    }

    @Override
    public void cancel() {
    }

}
