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
package org.hippoecm.frontend.dialog;

import java.lang.reflect.Constructor;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow.PageCreator;
import org.hippoecm.frontend.dialog.error.ErrorDialog;
import org.hippoecm.frontend.model.JcrNodeModel;

public class ContextDialogFactory implements PageCreator {
    private static final long serialVersionUID = 1L;

    public static final String ERROR = "org.hippoecm.frontend.dialog.error.ErrorDialog";

    private DialogWindow window;
    private JcrNodeModel model;

    private String classname;

    public ContextDialogFactory(DialogWindow window, JcrNodeModel model) {
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
            classname = ERROR;
        }
    }

    public Page createPage() {
        Page result = null;
        if (classname == null || ERROR.equals(classname)) {
            String msg = "No dialog renderer found";
            result = new ErrorDialog(window, msg);
        } else {
            try {
                Class clazz = Class.forName(classname);
                Class[] formalArgs = new Class[] { window.getClass(), model.getClass() };
                Constructor constructor = clazz.getConstructor(formalArgs);
                Object[] actualArgs = new Object[] { window, model };
                result = (AbstractDialog) constructor.newInstance(actualArgs);
            } catch (Exception e) {
                String msg = e.getClass().getName() + ": " + e.getMessage();
                result = new ErrorDialog(window, msg);
            }
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
            classname = ERROR;
        }
    }

}
