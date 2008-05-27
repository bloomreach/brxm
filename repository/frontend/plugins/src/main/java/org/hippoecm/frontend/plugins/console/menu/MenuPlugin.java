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
package org.hippoecm.frontend.plugins.console.menu;

import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugins.console.menu.delete.DeleteDialog;
import org.hippoecm.frontend.plugins.console.menu.export.ExportDialog;
import org.hippoecm.frontend.plugins.console.menu.node.NodeDialog;
import org.hippoecm.frontend.plugins.console.menu.property.PropertyDialog;
import org.hippoecm.frontend.plugins.console.menu.rename.RenameDialog;
import org.hippoecm.frontend.plugins.console.menu.reset.ResetDialog;
import org.hippoecm.frontend.plugins.console.menu.save.SaveDialog;
import org.hippoecm.frontend.sa.dialog.AbstractDialog;
import org.hippoecm.frontend.sa.dialog.DialogLink;
import org.hippoecm.frontend.sa.dialog.IDialogFactory;
import org.hippoecm.frontend.sa.dialog.IDialogService;
import org.hippoecm.frontend.sa.plugin.IPluginContext;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.hippoecm.frontend.sa.service.render.RenderPlugin;

public class MenuPlugin extends RenderPlugin {
    private static final long serialVersionUID = 1L;

    public MenuPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        IDialogService dialogService = getDialogService();

        IDialogFactory dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog(IDialogService service) {
                return new NodeDialog(MenuPlugin.this, getPluginContext(), service);
            }
        };
        add(new DialogLink("node-dialog", new Model("Add Node"), dialogFactory, dialogService));

        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog(IDialogService service) {
                return new DeleteDialog(MenuPlugin.this, getPluginContext(), service);
            }
        };
        add(new DialogLink("delete-dialog", new Model("Delete Node"), dialogFactory, dialogService));

        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog(IDialogService service) {
                return new SaveDialog(MenuPlugin.this, getPluginContext(), service);
            }
        };
        add(new DialogLink("save-dialog", new Model("Save"), dialogFactory, dialogService));

        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog(IDialogService service) {
                return new ResetDialog(MenuPlugin.this, getPluginContext(), service);
            }
        };
        add(new DialogLink("reset-dialog", new Model("Reset"), dialogFactory, dialogService));

        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog(IDialogService service) {
                return new ExportDialog(MenuPlugin.this, getPluginContext(), service);
            }
        };
        add(new DialogLink("export-dialog", new Model("Export Node"), dialogFactory, dialogService));

        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog(IDialogService service) {
                return new PropertyDialog(MenuPlugin.this, getPluginContext(), service);
            }
        };
        add(new DialogLink("property-dialog", new Model("Add Property"), dialogFactory, dialogService));

        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog(IDialogService service) {
                return new RenameDialog(MenuPlugin.this, getPluginContext(), service);
            }
        };
        add(new DialogLink("rename-dialog", new Model("Rename Node"), dialogFactory, dialogService));

        //  
        //  dialogFactory = new IDialogFactory() {
        //      private static final long serialVersionUID = 1L;
        //      public AbstractDialog createDialog(IDialogService service) {
        //          return null;
        //      }
        //  };
        //  add(new DialogLink("move-dialog", new Model("Move Node"), dialogFactory, dialogService));
        //  
        //  dialogFactory = new IDialogFactory() {
        //      private static final long serialVersionUID = 1L;
        //      public AbstractDialog createDialog(IDialogService service) {
        //          return null;
        //      }
        //  };
        //  add(new DialogLink("copy-dialog", new Model("Copy Node"), dialogFactory, dialogService));
        //  
        //  
        //  
    }

}
