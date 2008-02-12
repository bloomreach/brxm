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

import org.hippoecm.cmsprototype.frontend.model.content.Document;
import org.hippoecm.cmsprototype.frontend.model.content.Folder;
import org.hippoecm.cmsprototype.frontend.model.exception.ModelWrapException;
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
            JcrNodeModel source = null;
            try {
                source = new Document(dialogWindow.getNodeModel()).getNodeModel();
            } catch (ModelWrapException e) {
                try {
                    source = new Folder(dialogWindow.getNodeModel()).getNodeModel();
                } catch (ModelWrapException e1) {
                    //Node isn't a Document or a Folder
                }
            }
            if (source != null) {
                JcrNodeModel target = null;
                try {
                    Folder targetFolder = new Folder(getSelectedNode().getNodeModel()); 
                    target = targetFolder.getNodeModel();
                } catch (ModelWrapException e) {
                    try {
                        Document targetDocument = new Document(getSelectedNode().getNodeModel());
                        target = targetDocument.getNodeModel();
                    } catch (ModelWrapException e1) {
                        //target isn't a Document or a Folder
                    }
                }
                if (target != null) {
                    UserSession wicketSession = (UserSession) getSession();
                    HippoSession jcrSession = (HippoSession) wicketSession.getJcrSession();

                    String targetPath = target.getNode().getPath() + "/" + source.getNode().getName();
                    
                    jcrSession.copy(source.getNode(), targetPath);
                    jcrSession.save();

                    if (channel != null) {
                        Request request = channel.createRequest("select", target);
                        channel.send(request);

                        request = channel.createRequest("flush", target.findRootModel());
                        channel.send(request);
                    }
                }
            }
        }
    }

    @Override
    public void cancel() {
    }

}
