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

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.cmsprototype.frontend.model.tree.FolderTreeNode;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.dialog.lookup.LookupDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.tree.AbstractTreeNode;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MoveDialog extends LookupDialog {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(MoveDialog.class);

    public MoveDialog(DialogWindow dialogWindow, Channel channel) {
        super("Move", new FolderTreeNode(dialogWindow.getNodeModel().findRootModel()), dialogWindow, channel);
    }

    @Override
    public void ok() throws RepositoryException {
        JcrNodeModel sourceNodeModel = dialogWindow.getNodeModel();
        if (sourceNodeModel.getParentModel() != null) {
            String nodeName = sourceNodeModel.getNode().getName();
            String sourcePath = sourceNodeModel.getNode().getPath();

            AbstractTreeNode targetNodeModel = getSelectedNode();
            String targetPath = targetNodeModel.getNodeModel().getNode().getPath();
            if (!targetPath.endsWith("/")) {
                targetPath += "/";
            }
            targetPath += nodeName;

            // The actual move
            Session jcrSession = ((UserSession) getSession()).getJcrSession();
            jcrSession.move(sourcePath, targetPath);

            if (channel != null) {
                Request request = channel.createRequest("select", targetNodeModel.getNodeModel());
                channel.send(request);

                //TODO: lookup common ancestor iso root
                request = channel.createRequest("flush", targetNodeModel.getNodeModel().findRootModel());
                channel.send(request);
            }
        }
    }

    @Override
    public void cancel() {
    }

}
