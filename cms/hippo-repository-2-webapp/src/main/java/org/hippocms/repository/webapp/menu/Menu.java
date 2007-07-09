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

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.panel.Panel;
import org.hippocms.repository.webapp.menu.node.NodeDialog;
import org.hippocms.repository.webapp.menu.node.NodeDialogPage;
import org.hippocms.repository.webapp.menu.property.PropertyDialog;
import org.hippocms.repository.webapp.menu.property.PropertyDialogPage;
import org.hippocms.repository.webapp.menu.reset.ResetDialog;
import org.hippocms.repository.webapp.menu.reset.ResetDialogPage;
import org.hippocms.repository.webapp.menu.save.SaveDialog;
import org.hippocms.repository.webapp.menu.save.SaveDialogPage;
import org.hippocms.repository.webapp.model.JcrNodeModel;

public class Menu extends Panel {
    private static final long serialVersionUID = 1L;

    public Menu(String id, Component target, final JcrNodeModel model) {
        super(id);
        setOutputMarkupId(true);

        final NodeDialog nodeDialog = new NodeDialog("node-dialog", target);
        nodeDialog.setPageCreator(new ModalWindow.PageCreator() {
            private static final long serialVersionUID = 1L;
            public Page createPage() {
                return new NodeDialogPage(nodeDialog, model);
            }
        });
        add(nodeDialog);
        add(nodeDialog.dialogLink("node-dialog-link"));

        final PropertyDialog propertyDialog = new PropertyDialog("property-dialog", target);
        propertyDialog.setPageCreator(new ModalWindow.PageCreator() {
            private static final long serialVersionUID = 1L;
            public Page createPage() {
                return new PropertyDialogPage(propertyDialog, model);
            }
        });
        add(propertyDialog);
        add(propertyDialog.dialogLink("property-dialog-link"));

        final SaveDialog saveDialog = new SaveDialog("save-dialog", target);
        saveDialog.setPageCreator(new ModalWindow.PageCreator() {
            private static final long serialVersionUID = 1L;
            public Page createPage() {
                return new SaveDialogPage(saveDialog);
            }
        });
        add(saveDialog);
        add(saveDialog.dialogLink("save-dialog-link"));

        final ResetDialog resetDialog = new ResetDialog("reset-dialog", target);
        resetDialog.setPageCreator(new ModalWindow.PageCreator() {
            private static final long serialVersionUID = 1L;

            public Page createPage() {
                return new ResetDialogPage(resetDialog);
            }
        });
        add(resetDialog);
        add(resetDialog.dialogLink("reset-dialog-link"));
    }
}
