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

import java.lang.reflect.InvocationTargetException;

import javax.jcr.Node;

import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.console.menu.copy.CopyDialog;
import org.hippoecm.frontend.plugins.console.menu.delete.DeleteDialog;
import org.hippoecm.frontend.plugins.console.menu.move.MoveDialog;
import org.hippoecm.frontend.plugins.console.menu.node.NodeDialog;
import org.hippoecm.frontend.plugins.console.menu.property.PropertyDialog;
import org.hippoecm.frontend.plugins.console.menu.rename.RenameDialog;
import org.hippoecm.frontend.plugins.console.menu.reset.ResetDialog;
import org.hippoecm.frontend.plugins.console.menu.save.SaveDialog;
import org.hippoecm.frontend.service.render.RenderPlugin;

public class MenuPlugin extends RenderPlugin<Node> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(MenuPlugin.class);

    public MenuPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        IDialogService dialogService = getDialogService();

        IDialogFactory dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog<Node> createDialog() {
                return new NodeDialog(MenuPlugin.this);
            }
        };
        add(new DialogLink("node-dialog", new Model("Add Node"), dialogFactory, dialogService));

        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog<Node> createDialog() {
                return new DeleteDialog(MenuPlugin.this);
            }
        };
        add(new DialogLink("delete-dialog", new Model("Delete Node"), dialogFactory, dialogService));

        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog<Node> createDialog() {
                return new SaveDialog(MenuPlugin.this);
            }
        };
        add(new DialogLink("save-dialog", new Model("Write changes to repository"), dialogFactory, dialogService));

        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog<Node> createDialog() {
                return new ResetDialog(MenuPlugin.this);
            }
        };
        add(new DialogLink("reset-dialog", new Model("Reset"), dialogFactory, dialogService));

        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog<Node> createDialog() {
                return new PropertyDialog(MenuPlugin.this);
            }
        };
        add(new DialogLink("property-dialog", new Model("Add Property"), dialogFactory, dialogService));

        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog<Node> createDialog() {
                return new RenameDialog(MenuPlugin.this);
            }
        };
        add(new DialogLink("rename-dialog", new Model("Rename Node"), dialogFactory, dialogService));

        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog<Node> createDialog() {
                return new MoveDialog(MenuPlugin.this);
            }
        };
        add(new DialogLink("move-dialog", new Model("Move Node"), dialogFactory, dialogService));

        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog<Node> createDialog() {
                return new CopyDialog(MenuPlugin.this);
            }
        };
        add(new DialogLink("copy-dialog", new Model("Copy Node"), dialogFactory, dialogService));

        RepeatingView menuItems = new RepeatingView("items");
        for (IPluginConfig item : config.getPluginConfig("items").getPluginConfigSet()) {
            String itemLabel = item.getString("label");
            final String itemClass = item.getString("class");
            dialogFactory = new IDialogFactory() {
                private static final long serialVersionUID = 1L;
                public AbstractDialog<Void> createDialog() {
                    try {
                        return (AbstractDialog)Class.forName(itemClass).getConstructor(new Class[] {MenuPlugin.class}).newInstance(MenuPlugin.this);
                    } catch (ClassNotFoundException ex) {
                        log.error(ex.getMessage(), ex);
                    } catch (NoSuchMethodException ex) {
                        log.error(ex.getMessage(), ex);
                    } catch (InstantiationException ex) {
                        log.error(ex.getMessage(), ex);
                    } catch (IllegalAccessException ex) {
                        log.error(ex.getMessage(), ex);
                    } catch (IllegalArgumentException ex) {
                        log.error(ex.getMessage(), ex);
                    } catch (InvocationTargetException ex) {
                        log.error(ex.getMessage(), ex);
                    }
                    return null;
                }
            };
            System.err.println("BERRY \""+itemLabel+"\"");
            menuItems.add(new DialogLink(menuItems.newChildId(), new Model(itemLabel), dialogFactory, dialogService));
        }
        add(menuItems);
    }

    @Deprecated
    public void flushNodeModel(JcrNodeModel nodeModel) {
    }
}
