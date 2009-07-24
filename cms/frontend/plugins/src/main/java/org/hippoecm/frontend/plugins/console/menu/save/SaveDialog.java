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
package org.hippoecm.frontend.plugins.console.menu.save;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.console.menu.MenuPlugin;
import org.hippoecm.frontend.plugins.console.menu.content.ContentImportDialog;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SaveDialog extends AbstractDialog {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ContentImportDialog.class);

    protected boolean hasPendingChanges;
    protected MenuPlugin plugin;

    public SaveDialog(MenuPlugin plugin) {
        this.plugin = plugin;

        Component message;
        JcrNodeModel nodeModel = (JcrNodeModel) plugin.getModel();
        try {
            HippoNode rootNode = (HippoNode) nodeModel.getNode().getSession().getRootNode();
            if (rootNode.getSession().hasPendingChanges()) {
                hasPendingChanges = true;
                StringBuffer buf;
                buf = new StringBuffer("Pending changes:\n");

                NodeIterator it = rootNode.pendingChanges();
                if (it.hasNext()) {
                    while (it.hasNext()) {
                        Node node = it.nextNode();
                        buf.append(node.getPath()).append("\n");
                    }
                }
                message = new MultiLineLabel("message", buf.toString());
            } else {
                message = new Label("message", "There are no pending changes");
                setOkVisible(false);
            }
        } catch (RepositoryException e) {
            log.error("Error while rendering save dialog", e);
            message = new Label("message", "exception: " + e.getMessage());
            setOkVisible(false);
        }
        add(message);
    }

    @Override
    public void onOk() {
        try {
            ((UserSession) Session.get()).getJcrSession().save();
        } catch (AccessDeniedException e) {
            error(e.getClass().getName() + ": " + e.getMessage());
        } catch (RepositoryException e) {
            log.error("Error while saving content from the console", e);
            error(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public IModel getTitle() {
        return new Model("Save Session");
    }

}
