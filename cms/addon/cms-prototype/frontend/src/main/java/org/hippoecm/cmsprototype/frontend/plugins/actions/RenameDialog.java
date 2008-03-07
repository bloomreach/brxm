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
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RenameDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(RenameDialog.class);

    private String name;

    public RenameDialog(DialogWindow dialogWindow, Channel channel) {
        super(dialogWindow, channel);
        String title;
        try {
            Node node = dialogWindow.getNodeModel().getNode();
            node = Utils.findHandle(node);
            name = node.getName();
            if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                title = "Rename document " + node.getName();
            } else {
                title = "Rename folder " + node.getName();
            }
        } catch (RepositoryException e) {
            title = e.getMessage();
        }
        add(new TextFieldWidget("name", new PropertyModel(this, "name")));
        dialogWindow.setTitle(title);
    }

    @Override
    public void ok() throws RepositoryException {
        Node node = dialogWindow.getNodeModel().getNode();
        if (node != null) {
            node = Utils.findHandle(node);
            Node renamedNode;
            if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                NodeIterator iterator = node.getNodes();
                while (iterator.hasNext()) {
                    Node child = iterator.nextNode();
                    if (child.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                        rename(child);
                    }
                }
            }
            renamedNode = rename(node);
            renamedNode.getParent().save();
            if (channel != null && renamedNode != null) {
                Request request = channel.createRequest("flush", new JcrNodeModel("/"));
                channel.send(request);

                request = channel.createRequest("select", new JcrNodeModel(renamedNode));
                channel.send(request);
            }
        }
    }

    private Node rename(Node node) throws RepositoryException{
        Node result;
        UserSession wicketSession = (UserSession) getSession();
        HippoSession session = (HippoSession) wicketSession.getJcrSession();
        String srcAbsPath = node.getPath();
        String destAbsPath = srcAbsPath.substring(0, srcAbsPath.lastIndexOf("/") + 1) + name;
        session.move(srcAbsPath, destAbsPath);
        result = (Node) session.getItem(destAbsPath);
        return result;
    }

    @Override
    public void cancel() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
