/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.plugins.console.menu.reset;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IServiceReference;
import org.hippoecm.frontend.plugins.console.menu.MenuPlugin;

public class ResetDialog extends AbstractDialog {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private boolean hasPendingChanges;
    private IServiceReference<MenuPlugin> pluginRef;

    public ResetDialog(MenuPlugin plugin, IPluginContext context, IDialogService dialogWindow) {
        super(context, dialogWindow);
        this.pluginRef = context.getReference(plugin);

        Component message;
        JcrNodeModel nodeModel = (JcrNodeModel)plugin.getModel();
        try {
            NodeIterator it = nodeModel.getNode().pendingChanges();
            hasPendingChanges = it.hasNext();
            if (hasPendingChanges) {
                StringBuffer buf = new StringBuffer("Pending changes:\n");
                while (it.hasNext()) {
                    Node node = it.nextNode();
                    buf.append(node.getPath()).append("\n");
                }
                message = new MultiLineLabel("message", buf.toString());
            } else {
                message = new Label("message", "There are no pending changes");
                ok.setVisible(false);
            }
        } catch (RepositoryException e) {
            message = new Label("message", "exception: " + e.getMessage());
            ok.setVisible(false);
        }
        add(message);
    }

    @Override
    public void ok() throws RepositoryException {
        MenuPlugin plugin = pluginRef.getService();
        JcrNodeModel nodeModel = (JcrNodeModel) plugin.getModel();

        // The actual JCR refresh
        nodeModel.getNode().refresh(false);

        // Notify other plugins
        plugin.flushNodeModel(nodeModel.getParentModel());
    }

    @Override
    public void cancel() {
    }

    public String getTitle() {
        return "Refresh Session (undo changes)";
    }

}
