/*
 * Copyright 2008 Hippo
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
package org.hippoecm.frontend.plugins.standardworkflow;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.AbstractWorkflowDialog;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.workflow.AbstractWorkflowPlugin;
import org.hippoecm.frontend.plugin.workflow.EmbedWorkflowPlugin;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FolderEmbeddedWorkflowPlugin extends AbstractWorkflowPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    transient Logger log = LoggerFactory.getLogger(FolderWorkflowPlugin.class);

    String item;

    public FolderEmbeddedWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        item = (String) config.get(EmbedWorkflowPlugin.ITEM_ID);

        add(new DialogLink("delete-dialog", new Model("Delete"), new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog(IDialogService dialogService) {
                return new DeleteDialog(FolderEmbeddedWorkflowPlugin.this, dialogService);
            }
        }, getDialogService()));


        //add(new Label("move-dialog", "move"));
        //add(new Label("rename-dialog", "rename"));
        //add(new Label("duplicate-dialog", "duplicate"));
    }

    private void writeObject(ObjectOutputStream out)
            throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        log = LoggerFactory.getLogger(FolderEmbeddedWorkflowPlugin.class);
    }

    class DeleteDialog extends AbstractWorkflowDialog {
        private static final long serialVersionUID = 1L;

        public DeleteDialog(FolderEmbeddedWorkflowPlugin plugin, IDialogService dialogWindow) {
            super(plugin, dialogWindow, "Delete item");

            WorkflowsModel wflModel = (WorkflowsModel) plugin.getModel();
            if (wflModel.getNodeModel().getNode() == null) {
                ok.setVisible(false);
            }
        }

        @Override
        protected void execute() throws Exception {
            FolderWorkflow workflow = (FolderWorkflow) getWorkflow();
            workflow.delete(item);
        }

        @Override
        public void cancel() {
        }
    }
}

