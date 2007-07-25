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
package org.hippocms.repository.frontend.dialog;

import java.lang.reflect.Constructor;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow.PageCreator;
import org.hippocms.repository.frontend.dialog.error.ErrorDialog;
import org.hippocms.repository.frontend.model.JcrNodeModel;
import org.hippocms.repository.frontend.update.IUpdatable;

public class DynamicDialogCreator implements PageCreator, IUpdatable {
    private static final long serialVersionUID = 1L;

    private DialogWindow window;
    private JcrNodeModel model;

    private String classname;

    public DynamicDialogCreator(DialogWindow window, JcrNodeModel model) {
        this.window = window;
        this.model = model;
        Node node = model.getNode();
        try {
            if (node.hasProperty("renderer")) {
                classname = node.getProperty("renderer").getString();
            } else {
                classname = null;
            }
        } catch (RepositoryException e) {
            classname = "org.hippocms.repository.frontend.dialog.error.ErrorDialog";
        }

    }

    public Page createPage() {
        Page result = null;
        if (classname != null) {
            try {
                Class dialogClass = Class.forName(classname);
                Class[] formalArgs = new Class[] { window.getClass(), model.getClass() };
                Constructor constructor = dialogClass.getConstructor(formalArgs);
                Object[] actualArgs = new Object[] { window, model };
                result = (AbstractDialog) constructor.newInstance(actualArgs);
            } catch (Exception e) {
                String msg = e.getClass().getName() + ": " + e.getMessage();
                result = new ErrorDialog(window, model, msg);
            }
        } else {
           String msg = "Node has no associated renderer"; 
           result = new ErrorDialog(window, model, msg); 
        }
        return result;
    }

    public void update(AjaxRequestTarget target, JcrNodeModel model) {
        Node node = model.getNode();
        try {
            if (node.hasProperty("renderer")) {
                classname = node.getProperty("renderer").getString();
            } else {
                classname = null;
            }
        } catch (RepositoryException e) {
            classname = "org.hippocms.repository.frontend.dialog.error.ErrorDialog";
        }
    }

}
