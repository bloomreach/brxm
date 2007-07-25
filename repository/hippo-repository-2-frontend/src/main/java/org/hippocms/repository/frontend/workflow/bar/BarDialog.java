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
package org.hippocms.repository.frontend.workflow.bar;

import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.PropertyModel;
import org.hippocms.repository.frontend.dialog.AbstractDialog;
import org.hippocms.repository.frontend.dialog.DialogWindow;
import org.hippocms.repository.frontend.model.JcrNodeModel;

public class BarDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;
            
    public BarDialog(DialogWindow dialogWindow, JcrNodeModel model) {
        super(dialogWindow, model);
        add(new Label("message", new PropertyModel(this, "message")));              
        dialogWindow.setTitle("bar");
    }
 

    public void ok() throws RepositoryException {
    }

    public void cancel() {
    }

    public String getMessage()  {
        try {
            return "Barring: " + model.getNode().getPath();
        } catch (RepositoryException e) {
            return e.getMessage();
        }
    }

    public void setMessage(String message) {
    }

}
