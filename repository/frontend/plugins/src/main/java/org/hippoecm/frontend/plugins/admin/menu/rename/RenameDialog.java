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
package org.hippoecm.frontend.plugins.admin.menu.rename;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RenameDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(RenameDialog.class);

    /**
     * The name of the current node represented in the dialog
     */
    private String name;

    public RenameDialog(DialogWindow dialogWindow, Channel channel) {
        super(dialogWindow, channel);
        dialogWindow.setTitle("Rename Node");

        JcrNodeModel nodeModel = dialogWindow.getNodeModel();
        try {
            // get name of current node
            name = nodeModel.getNode().getName();
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }

        add(new TextFieldWidget("name", new PropertyModel(this, "name")));
        if (nodeModel.getNode() == null) {
            ok.setVisible(false);
        }
    }

    @Override
    protected void ok() throws RepositoryException {
        JcrNodeModel nodeModel = dialogWindow.getNodeModel();

        if (nodeModel.getParentModel() != null) {
            JcrNodeModel parentModel = nodeModel.getParentModel();

            //The actual JCR move
            String oldPath = nodeModel.getNode().getPath();
            String newPath = parentModel.getNode().getPath();
            if (!newPath.endsWith("/")) {
                newPath += "/";
            }
            newPath += getName();
            Session jcrSession = ((UserSession) getSession()).getJcrSession();
            jcrSession.move(oldPath, newPath);

            Channel channel = getIncoming();
            if(channel != null) {
                JcrNodeModel newNodeModel = new JcrNodeModel(parentModel.getNode().getNode(getName()));
                Request request = channel.createRequest("flush", parentModel.getMapRepresentation());
                channel.send(request);

                request = channel.createRequest("select", newNodeModel.getMapRepresentation());
                channel.send(request);
            }
        }
    }

    @Override
    protected void cancel() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
