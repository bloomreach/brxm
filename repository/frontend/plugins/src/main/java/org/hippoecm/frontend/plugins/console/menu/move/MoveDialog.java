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
package org.hippoecm.frontend.plugins.console.menu.move;

import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.panel.Panel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.tree.AbstractTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeNode;
import org.hippoecm.frontend.plugins.console.menu.MenuPlugin;
import org.hippoecm.frontend.sa.dialog.IDialogService;
import org.hippoecm.frontend.sa.dialog.lookup.LookupDialog;
import org.hippoecm.frontend.sa.plugin.IPluginContext;
import org.hippoecm.frontend.sa.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MoveDialog extends LookupDialog {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(MoveDialog.class);

    public MoveDialog(MenuPlugin plugin, IPluginContext context, IDialogService dialogWindow) {
        super(plugin, context, dialogWindow);
    }

    public String getTitle() {
        return "Move Node";
    }

    @Override
    protected Panel getInfoPanel() {
        RenderPlugin plugin = pluginRef.getService();
        JcrNodeModel nodeModel = (JcrNodeModel) plugin.getModel();
        return new MoveDialogInfoPanel("info", nodeModel);
    }

    @Override
    protected boolean isValidSelection(AbstractTreeNode targetModel) {
        return true;
    }

    @Override
    protected AbstractTreeNode getRootNode() {
        return new JcrTreeNode(new JcrNodeModel("/"));
    }

    @Override
    public void ok() throws RepositoryException {

        MenuPlugin plugin = (MenuPlugin) pluginRef.getService();
        JcrNodeModel sourceNodeModel = (JcrNodeModel) plugin.getModel();
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
            UserSession wicketSession = (UserSession) getSession();
            HippoSession jcrSession = (HippoSession) wicketSession.getJcrSession();
            jcrSession.move(sourcePath, targetPath);

            plugin.setModel(targetNodeModel.getNodeModel());
            plugin.flushNodeModel(targetNodeModel.getNodeModel());
            plugin.flushNodeModel(sourceNodeModel.getParentModel());
        }
    }

    @Override
    public void cancel() {
    }

}
