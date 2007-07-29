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
package org.hippocms.repository.plugins.admin.menu.reset;

import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.basic.Label;
import org.hippocms.repository.frontend.dialog.AbstractDialog;
import org.hippocms.repository.frontend.dialog.DialogWindow;
import org.hippocms.repository.frontend.model.JcrNodeModel;

public class ResetDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    public ResetDialog(final DialogWindow dialogWindow, JcrNodeModel model) {
        super(dialogWindow, model);
        dialogWindow.setTitle("Refresh Session (undo changes)");
        
        Label label;
        try {
            boolean changes = model.getNode().getSession().hasPendingChanges();
            if (changes) {
                label = new Label("message", "There are pending changes");
            } else {
                label = new Label("message", "There are no pending changes");
            }
        } catch (RepositoryException e) {
            label = new Label("message", "exception: " + e.getMessage());
        }
        add(label);
    }

    public void ok() throws RepositoryException {
        model.getNode().getSession().refresh(false);
    }

    public void cancel() {
    }

}
