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
package org.hippoecm.frontend.plugins.admin.menu;

import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.model.JcrEvent;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugins.admin.menu.delete.DeleteDialog;
import org.hippoecm.frontend.plugins.admin.menu.export.ExportDialog;
import org.hippoecm.frontend.plugins.admin.menu.move.MoveDialog;
import org.hippoecm.frontend.plugins.admin.menu.node.NodeDialog;
import org.hippoecm.frontend.plugins.admin.menu.property.PropertyDialog;
import org.hippoecm.frontend.plugins.admin.menu.rename.RenameDialog;
import org.hippoecm.frontend.plugins.admin.menu.reset.ResetDialog;
import org.hippoecm.frontend.plugins.admin.menu.save.SaveDialog;

public class MenuPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    private DialogWindow nodeDialog;
    private DialogWindow deleteDialog;
    private DialogWindow moveDialog;
    private DialogWindow renameDialog;
    private DialogWindow exportDialog;
    private DialogWindow propertyDialog;
    private DialogWindow saveDialog;
    private DialogWindow resetDialog;
    
    public MenuPlugin(String id, final JcrNodeModel model) {
        super(id, model);

        nodeDialog = new DialogWindow("node-dialog", model, false);
        nodeDialog.setPageCreator(new ModalWindow.PageCreator() {
            private static final long serialVersionUID = 1L;
            public Page createPage() {
                return new NodeDialog(nodeDialog);
            }
        });
        add(nodeDialog);
        add(nodeDialog.dialogLink("node-dialog-link"));

        deleteDialog = new DialogWindow("delete-dialog", model, false);
        deleteDialog.setPageCreator(new ModalWindow.PageCreator() {
            private static final long serialVersionUID = 1L;
            public Page createPage() {
                return new DeleteDialog(deleteDialog);
            }
        });
        add(deleteDialog);
        add(deleteDialog.dialogLink("delete-dialog-link"));
        
        moveDialog = new DialogWindow("move-dialog", model, true);
        moveDialog.setPageCreator(new ModalWindow.PageCreator() {
            private static final long serialVersionUID = 1L;
            public Page createPage() {
                return new MoveDialog(moveDialog);
            }
        });
        add(moveDialog);
        add(moveDialog.dialogLink("move-dialog-link"));

        renameDialog = new DialogWindow("rename-dialog", model, false);
        renameDialog.setPageCreator(new ModalWindow.PageCreator() {
            private static final long serialVersionUID = 1L;
            public Page createPage() {
                return new RenameDialog(renameDialog);
            }
        });
        add(renameDialog);
        add(renameDialog.dialogLink("rename-dialog-link"));
        
        exportDialog = new DialogWindow("export-dialog", model, false);
        exportDialog.setPageCreator(new ModalWindow.PageCreator() {
            private static final long serialVersionUID = 1L;
            public Page createPage() {
                return new ExportDialog(exportDialog);
            }
        });
        add(exportDialog);
        add(exportDialog.dialogLink("export-dialog-link"));

        propertyDialog = new DialogWindow("property-dialog", model, false);
        propertyDialog.setPageCreator(new ModalWindow.PageCreator() {
            private static final long serialVersionUID = 1L;
            public Page createPage() {
                return new PropertyDialog(propertyDialog);
            }
        });
        add(propertyDialog);
        add(propertyDialog.dialogLink("property-dialog-link"));

        saveDialog = new DialogWindow("save-dialog", model, true);
        saveDialog.setPageCreator(new ModalWindow.PageCreator() {
            private static final long serialVersionUID = 1L;
            public Page createPage() {
                return new SaveDialog(saveDialog);
            }
        });
        add(saveDialog);
        add(saveDialog.dialogLink("save-dialog-link"));

        resetDialog = new DialogWindow("reset-dialog", model, true);
        resetDialog.setPageCreator(new ModalWindow.PageCreator() {
            private static final long serialVersionUID = 1L;
            public Page createPage() {
                return new ResetDialog(resetDialog);
            }
        });
        add(resetDialog);
        add(resetDialog.dialogLink("reset-dialog-link"));

        add(new Label("path", new PropertyModel(model, "path")));
    }
    
    public void update(AjaxRequestTarget target, JcrEvent jcrEvent) {
        if (jcrEvent.getModel() != null) {
            setModel(jcrEvent.getModel());
        }
        if (target != null) {
            target.addComponent(this);
        }
    }

}
