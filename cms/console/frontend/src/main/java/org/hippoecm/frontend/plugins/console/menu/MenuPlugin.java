/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.console.NodeModelReference;
import org.hippoecm.frontend.plugins.console.Shortcuts;
import org.hippoecm.frontend.plugins.console.editor.EditorUpdate;
import org.hippoecm.frontend.plugins.console.menu.copy.CopyDialog;
import org.hippoecm.frontend.plugins.console.menu.delete.DeleteDialog;
import org.hippoecm.frontend.plugins.console.menu.deletemultiple.DeleteMultipleDialog;
import org.hippoecm.frontend.plugins.console.menu.help.HelpDialog;
import org.hippoecm.frontend.plugins.console.menu.move.MoveDialog;
import org.hippoecm.frontend.plugins.console.menu.node.NodeDialog;
import org.hippoecm.frontend.plugins.console.menu.open.OpenDialog;
import org.hippoecm.frontend.plugins.console.menu.property.PropertyDialog;
import org.hippoecm.frontend.plugins.console.menu.rename.RenameDialog;
import org.hippoecm.frontend.plugins.console.menu.reset.ResetDialog;
import org.hippoecm.frontend.plugins.console.menu.save.SaveDialog;
import org.hippoecm.frontend.plugins.console.menu.save.SaveDialogLink;
import org.hippoecm.frontend.plugins.standards.sort.NodeSortPanel;
import org.hippoecm.frontend.service.render.ListViewPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MenuPlugin extends ListViewPlugin<Node> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(MenuPlugin.class);

    private SaveDialogLink saveDialogLink;
    private NodeSortPanel sorter;

    public MenuPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        IDialogService dialogService = getDialogService();

        IDialogFactory dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog<Node> createDialog() {
                return new NodeDialog(new NodeModelReference(MenuPlugin.this,  (JcrNodeModel) getDefaultModel()));
            }
        };
        add(new DialogLink("node-dialog", new Model<String>("Add"), dialogFactory, dialogService));

        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog<Node> createDialog() {
                return new DeleteDialog(new NodeModelReference(MenuPlugin.this,  (JcrNodeModel) getDefaultModel()));
            }
        };
        add(new DialogLink("delete-dialog", new Model<String>("Delete"), dialogFactory, dialogService));

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
                return new ResetDialog() {
                    @Override
                    public void onOk() {
                        super.onOk();
                        AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
                        if (target != null) {
                            send(getPage(), Broadcast.DEPTH, new EditorUpdate(target));
                        }
                    }
                };
            }
        };
        add(new DialogLink("reset-dialog", new Model<String>("Reset"), dialogFactory, dialogService));

        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog<Node> createDialog() {
                return new PropertyDialog(new NodeModelReference(MenuPlugin.this,  (JcrNodeModel) getDefaultModel())) {
                    @Override
                    public void onOk() {
                        super.onOk();
                        AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
                        if (target != null) {
                            send(getPage(), Broadcast.DEPTH, new EditorUpdate(target));
                        }
                    }
                };
            }
        };
        add(new DialogLink("property-dialog", new Model<String>("Add Property"), dialogFactory, dialogService, Shortcuts.CTRL_P));

        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog<Node> createDialog() {
                return new RenameDialog(new NodeModelReference(MenuPlugin.this,  (JcrNodeModel) getDefaultModel()));
            }
        };
        add(new DialogLink("rename-dialog", new Model<String>("Rename"), dialogFactory, dialogService));

        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog<Node> createDialog() {
                return new MoveDialog(new NodeModelReference(MenuPlugin.this,  (JcrNodeModel) getDefaultModel()));
            }
        };
        add(new DialogLink("move-dialog", new Model<String>("Move"), dialogFactory, dialogService));

        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog<Node> createDialog() {
                return new CopyDialog(new NodeModelReference(MenuPlugin.this,  (JcrNodeModel) getDefaultModel()));
            }
        };
        add(new DialogLink("copy-dialog", new Model<String>("Copy"), dialogFactory, dialogService));

        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog<Node> createDialog() {
                return new OpenDialog(new NodeModelReference(MenuPlugin.this,  (JcrNodeModel) getDefaultModel()));
            }
        };
        add(new DialogLink("open-dialog", new Model<String>("Open"), dialogFactory, dialogService, Shortcuts.CTRL_O));

        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog<Node> createDialog() {
                return new DeleteMultipleDialog(new NodeModelReference(MenuPlugin.this, (JcrNodeModel) getDefaultModel()));
            }
        };
        add(new DialogLink("delete-multiple-dialog", new Model<String>("Delete *"), dialogFactory, dialogService, Shortcuts.CTRL_M));

        dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;
            public AbstractDialog<Void> createDialog() {
                return new HelpDialog();
            }
        };
        add(new DialogLink("help-dialog", new Model<String>("?"), dialogFactory, dialogService, Shortcuts.CTRL_H));

        add(sorter = new NodeSortPanel("sorter-panel"));
        sorter.setDefaultModel(getDefaultModel());

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
    protected void onModelChanged() {
        sorter.setDefaultModel(getDefaultModel());
    }

    @Override
    public void render(PluginRequestTarget target) {
        super.render(target);
        if (target != null) {
            saveDialogLink.update(target);
            if (sorter.isDirty()) {
                target.add(sorter);
            }
        }
    }

}
