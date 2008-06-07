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
package org.hippoecm.frontend.plugins.admin.menu.copy;

import javax.jcr.RepositoryException;

import org.hippoecm.frontend.legacy.dialog.DialogWindow;
import org.hippoecm.frontend.legacy.dialog.lookup.InfoPanel;
import org.hippoecm.frontend.legacy.dialog.lookup.LookupDialog;
import org.hippoecm.frontend.legacy.plugin.channel.Channel;
import org.hippoecm.frontend.legacy.plugin.channel.Request;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.tree.JcrTreeNode;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated use org.hippoecm.frontend.plugins.console.menu.* instead
 */
@Deprecated
public class CopyDialog extends LookupDialog {
    private static final long serialVersionUID = 1L;

    private CopyDialogInfoPanel infoPanel;

    static final Logger log = LoggerFactory.getLogger(CopyDialog.class);

    public CopyDialog(DialogWindow dialogWindow) {
        super("Copy", new JcrTreeNode(dialogWindow.getNodeModel().findRootModel()), dialogWindow);
    }

    @Override
    protected InfoPanel getInfoPanel(DialogWindow dialogWindow) {
        JcrNodeModel nodeModel = dialogWindow.getNodeModel();
        infoPanel = new CopyDialogInfoPanel("info", nodeModel);
        add(infoPanel);
        if (nodeModel.getNode() == null) {
            ok.setVisible(false);
        }
        super.setInfoPanel(infoPanel);
        return infoPanel;
    }

    @Override
    public void ok() throws RepositoryException {
        JcrNodeModel sourceNodeModel = getDialogWindow().getNodeModel();
        String name = infoPanel.getName();

        if (sourceNodeModel.getParentModel() != null && getSelectedNode() != null && name != null && !"".equals(name)) {

            JcrNodeModel targetNodeModel = getSelectedNode().getNodeModel();
            String targetPath = targetNodeModel.getNode().getPath();
            if (!targetPath.endsWith("/")) {
                targetPath += "/";
            }
            //targetPath += sourceNodeModel.getNode().getName();
            targetPath += name;

            // The actual copy
            UserSession wicketSession = (UserSession) getSession();
            HippoSession jcrSession = (HippoSession) wicketSession.getJcrSession();
            jcrSession.copy(sourceNodeModel.getNode(), targetPath);

            Channel channel = getChannel();
            if (channel != null) {
                Request request = channel.createRequest("select", targetNodeModel);
                channel.send(request);

                //TODO: lookup common ancestor iso root
                request = channel.createRequest("flush", targetNodeModel.findRootModel());
                channel.send(request);
            }
        }
    }

    @Override
    public void cancel() {
    }

}
