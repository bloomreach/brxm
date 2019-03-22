/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNode;

public class ResetDialog extends Dialog<Node> {
    private static final long serialVersionUID = 1L;

    protected boolean hasPendingChanges;

    public ResetDialog() {
        setTitle(Model.of("Refresh Session"));
        setSize(DialogConstants.MEDIUM);

        Component message;
        try {
            HippoNode rootNode = (HippoNode) UserSession.get().getJcrSession().getRootNode();
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
                message = new Label("message", "There are no pending changes.");
            }
        } catch (RepositoryException e) {
            message = new Label("message", "exception: " + e.getMessage());
            e.printStackTrace();
        }
        add(message);

        Label resetText = new Label("resetText", Model.of("Reset session?"));
        resetText.setVisible(hasPendingChanges);
        queue(resetText);

        setFocusOnOk();
    }

    @Override
    public void onOk() {
        try {
            Node rootNode = UserSession.get().getJcrSession().getRootNode();
            // always refresh regardless of the local changes, so external changes
            // can also be exposed.
            rootNode.refresh(false);
        } catch (RepositoryException ex) {
            error(ex.getMessage());
        }
    }
}
