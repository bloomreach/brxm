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
package org.hippoecm.frontend.plugins.console.menu;

import javax.jcr.Node;

import org.apache.wicket.model.Model;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.console.NodeModelReference;
import org.hippoecm.frontend.plugins.console.menu.copy.CopyDialog;
import org.hippoecm.frontend.plugins.console.menu.delete.DeleteDialog;
import org.hippoecm.frontend.plugins.console.menu.move.MoveDialog;
import org.hippoecm.frontend.plugins.console.menu.node.NodeDialog;
import org.hippoecm.frontend.plugins.console.menu.property.PropertyDialog;
import org.hippoecm.frontend.plugins.console.menu.rename.RenameDialog;
import org.hippoecm.frontend.plugins.console.menu.reset.ResetDialog;
import org.hippoecm.frontend.plugins.console.menu.save.SaveDialog;
import org.hippoecm.frontend.plugins.console.menu.save.SaveDialogLink;
import org.hippoecm.frontend.service.render.ListViewPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MenuPlugin extends ListViewPlugin<Node> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(MenuPlugin.class);

    private DialogLink saveDialogLink;

    public MenuPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        IDialogService dialogService = getDialogService();

        IDialogFactory dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog<Node> createDialog() {
                return new NodeDialog(new NodeModelReference(MenuPlugin.this,  (JcrNodeModel) getDefaultModel()));
            }
        };
        add(new DialogLink("node-dialog", new Model<String>("Add Node"), dialogFactory, dialogService));

        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog<Node> createDialog() {
                return new DeleteDialog(new NodeModelReference(MenuPlugin.this,  (JcrNodeModel) getDefaultModel()));
            }
        };
        add(new DialogLink("delete-dialog", new Model<String>("Delete Node"), dialogFactory, dialogService));

        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog<Node> createDialog() {
                return new SaveDialog();
            }
        };
        saveDialogLink = new SaveDialogLink("save-dialog", new Model<String>("Write changes to repository"), dialogFactory, dialogService);
        add(saveDialogLink);

        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog<Node> createDialog() {
                return new ResetDialog();
            }
        };
        add(new DialogLink("reset-dialog", new Model<String>("Reset"), dialogFactory, dialogService));

        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog<Node> createDialog() {
                return new PropertyDialog(MenuPlugin.this);
            }
        };
        add(new DialogLink("property-dialog", new Model<String>("Add Property"), dialogFactory, dialogService));

        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog<Node> createDialog() {
                return new RenameDialog(new NodeModelReference(MenuPlugin.this,  (JcrNodeModel) getDefaultModel()));
            }
        };
        add(new DialogLink("rename-dialog", new Model<String>("Rename Node"), dialogFactory, dialogService));

        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog<Node> createDialog() {
                return new MoveDialog(new NodeModelReference(MenuPlugin.this,  (JcrNodeModel) getDefaultModel()));
            }
        };
        add(new DialogLink("move-dialog", new Model<String>("Move Node"), dialogFactory, dialogService));

        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog<Node> createDialog() {
                return new CopyDialog(new NodeModelReference(MenuPlugin.this,  (JcrNodeModel) getDefaultModel()));
            }
        };
        add(new DialogLink("copy-dialog", new Model<String>("Copy Node"), dialogFactory, dialogService));

// Doesn't fit in current design :/
//        dialogFactory = new IDialogFactory() {
//            private static final long serialVersionUID = 1L;
//            @Override public Dialog createDialog() {
//                return new T9idsDialog(new NodeModelReference(MenuPlugin.this, (JcrNodeModel)getDefaultModel()));
//            }
//        };
//        add(new DialogLink("t9ids-dialog", new Model<String>("Generate new t9 ids"), dialogFactory, dialogService));
    }

    @Override
    public void render(PluginRequestTarget target) {
        super.render(target);
        if (target != null) {
            target.addComponent(saveDialogLink.get("dialog-link:dialog-link-text-extended"));
        }
    }

}
