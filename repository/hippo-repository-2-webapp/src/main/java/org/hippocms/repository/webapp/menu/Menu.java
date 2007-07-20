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
package org.hippocms.repository.webapp.menu;

import org.apache.wicket.Page;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.hippocms.repository.webapp.menu.delete.DeleteDialog;
import org.hippocms.repository.webapp.menu.node.NodeDialog;
import org.hippocms.repository.webapp.menu.property.PropertyDialog;
import org.hippocms.repository.webapp.menu.reset.ResetDialog;
import org.hippocms.repository.webapp.menu.save.SaveDialog;
import org.hippocms.repository.webapp.model.JcrNodeModel;

public class Menu extends Panel {
    private static final long serialVersionUID = 1L;

    public Menu(String id, final JcrNodeModel model) {
        super(id);
        setOutputMarkupId(true);

        final DialogWindow nodeDialog = new DialogWindow("node-dialog");
        nodeDialog.setPageCreator(new ModalWindow.PageCreator() {
            private static final long serialVersionUID = 1L;
            public Page createPage() {
                return new NodeDialog(nodeDialog, model);
            }
        });
        add(nodeDialog);
        add(nodeDialog.dialogLink("node-dialog-link"));

        final DialogWindow deleteDialog = new DialogWindow("delete-dialog");
        deleteDialog.setPageCreator(new ModalWindow.PageCreator() {
            private static final long serialVersionUID = 1L;
            public Page createPage() {
                return new DeleteDialog(deleteDialog, model);
            }
        });
        add(deleteDialog);
        add(deleteDialog.dialogLink("delete-dialog-link"));

        final DialogWindow propertyDialog = new DialogWindow("property-dialog");
        propertyDialog.setPageCreator(new ModalWindow.PageCreator() {
            private static final long serialVersionUID = 1L;
            public Page createPage() {
                return new PropertyDialog(propertyDialog, model);
            }
        });
        add(propertyDialog);
        add(propertyDialog.dialogLink("property-dialog-link"));

        final DialogWindow saveDialog = new DialogWindow("save-dialog");
        saveDialog.setPageCreator(new ModalWindow.PageCreator() {
            private static final long serialVersionUID = 1L;
            public Page createPage() {
                return new SaveDialog(saveDialog, model);
            }
        });
        add(saveDialog);
        add(saveDialog.dialogLink("save-dialog-link"));

        final DialogWindow resetDialog = new DialogWindow("reset-dialog");
        resetDialog.setPageCreator(new ModalWindow.PageCreator() {
            private static final long serialVersionUID = 1L;
            public Page createPage() {
                return new ResetDialog(resetDialog, model);
            }
        });
        add(resetDialog);
        add(resetDialog.dialogLink("reset-dialog-link"));
        
        
        add(new Label("path", new PropertyModel(model, "path")));
    }

}
