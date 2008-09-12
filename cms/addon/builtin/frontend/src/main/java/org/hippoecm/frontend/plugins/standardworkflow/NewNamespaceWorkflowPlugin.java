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

import java.util.Map;
import java.util.TreeMap;

import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogAction;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IServiceReference;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.workflow.AbstractWorkflowPlugin;
import org.hippoecm.frontend.plugins.standardworkflow.dialogs.NewNamespaceDialog;
import org.hippoecm.frontend.service.IJcrService;

public class NewNamespaceWorkflowPlugin extends AbstractWorkflowPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public NewNamespaceWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        IJcrService jcrService = context.getService(IJcrService.class.getName(), IJcrService.class);
        final IServiceReference<IJcrService> jcrRef = context.getReference(jcrService);
        DialogAction action;
        WorkflowActionComponent choice;

        Map<String, WorkflowActionComponent> actions = new TreeMap<String, WorkflowActionComponent>();

        action = new DialogAction(new IDialogFactory() {
            public AbstractDialog createDialog(IDialogService dialogService) {
                return new NewNamespaceDialog(NewNamespaceWorkflowPlugin.this, dialogService);
            }
        }, getDialogService());
        choice = new WorkflowActionComponent("createNamespaceRequest-dialog", "Create namespace", (String)null, action);
        actions.put(choice.getId(), choice);

        add(new WorkflowActionComponentDropDownChoice("actions", actions));
    }
}
