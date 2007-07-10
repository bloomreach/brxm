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
package org.hippocms.repository.webapp.menu.delete;

import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.PropertyModel;
import org.hippocms.repository.webapp.menu.AbstractDialog;
import org.hippocms.repository.webapp.menu.DialogWindow;
import org.hippocms.repository.webapp.model.JcrNodeModel;

public class DeleteDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    public DeleteDialog(final DialogWindow dialogWindow, JcrNodeModel model) {
        super(dialogWindow, model);
        add(new Label("message", new PropertyModel(this, "message")));
        if (model.getNode() == null) {
            ok.setVisible(false);
        }
    }

    public void ok() {
        try {
            if (model.getNode() != null) {
                model.getNode().remove();
            }
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void cancel() {
    }

    public String getMessage() {
        String msg;
        try {
            if (model.getNode() == null) {
                msg = "Allready deleted";
            } else {
                msg = "Delete " + model.getNode().getPath();
            }
        } catch (RepositoryException e) {
            msg = e.getMessage();
        }
        return msg;
    }

    public void setMessage(String message) {
    }

}
