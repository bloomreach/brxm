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
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;

public class DeleteDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    public DeleteDialog(DialogWindow dialogWindow, Channel channel) {
        super(dialogWindow, channel);
        String message;
        try {
            Node node = dialogWindow.getNodeModel().getNode();
            Node source = Utils.findHandle(node);
            if (source.isNodeType(HippoNodeType.NT_HANDLE)) {
                message = "Delete document " + node.getName();
            } else {
                message = "Delete folder " + node.getName();
            }
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
            Node toBeDeleted = null;
            try {
                toBeDeleted = Utils.findHandle(node);

                if (toBeDeleted != null) {
                    Node parent = Utils.findHandle(toBeDeleted.getParent());

                    UserSession wicketSession = (UserSession) getSession();
                    HippoSession jcrSession = (HippoSession) wicketSession.getJcrSession();
                    toBeDeleted.remove();
                    jcrSession.save();

                    if (channel != null) {
                        Request request = channel.createRequest("select", new JcrNodeModel(parent));
                        channel.send(request);

                        request = channel.createRequest("flush", new JcrNodeModel("/"));
                        channel.send(request);
                    }
                }
            } catch (RepositoryException e) {
                //TODO
            }
        }
    }

    @Override
    public void cancel() {
    }

}
