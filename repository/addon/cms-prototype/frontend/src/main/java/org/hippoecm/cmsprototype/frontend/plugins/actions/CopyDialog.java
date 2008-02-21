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

import org.hippoecm.cmsprototype.frontend.plugins.foldertree.FolderTreeNode;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.dialog.lookup.LookupDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CopyDialog extends LookupDialog {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(CopyDialog.class);

    public CopyDialog(DialogWindow dialogWindow, Channel channel) {
        super("Copy", new FolderTreeNode(dialogWindow.getNodeModel().findRootModel()), dialogWindow, channel);
    }

    @Override
    public void ok() throws RepositoryException {
        if (dialogWindow.getNodeModel().getParentModel() != null) {
            Node source = Utils.findHandle(dialogWindow.getNodeModel().getNode());
            Node target = getSelectedNode().getNodeModel().getNode();
            if (target != null) {
                UserSession wicketSession = (UserSession) getSession();
                HippoSession jcrSession = (HippoSession) wicketSession.getJcrSession();

                String targetPath = target.getPath() + "/" + source.getName();

                jcrSession.copy(source, targetPath);
                jcrSession.save();

                if (channel != null) {
                    Request request = channel.createRequest("select", new JcrNodeModel(target));
                    channel.send(request);

                    request = channel.createRequest("flush", new JcrNodeModel(target));
                    channel.send(request);
                }
            }
        }
    }

    @Override
    public void cancel() {
    }

}
