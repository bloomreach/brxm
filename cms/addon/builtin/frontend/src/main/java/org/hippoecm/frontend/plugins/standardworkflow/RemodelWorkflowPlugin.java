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
package org.hippoecm.frontend.plugins.standardworkflow;

import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogAction;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IServiceReference;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.workflow.AbstractWorkflowPlugin;
import org.hippoecm.frontend.plugins.standardworkflow.dialogs.CreateTypeDialog;
import org.hippoecm.frontend.plugins.standardworkflow.remodel.RemodelDialog;
import org.hippoecm.frontend.service.IJcrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemodelWorkflowPlugin extends AbstractWorkflowPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final Logger log = LoggerFactory.getLogger(RemodelWorkflowPlugin.class);
    private static final long serialVersionUID = 1L;

    public RemodelWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        IJcrService jcrService = context.getService(IJcrService.class.getName(), IJcrService.class);
        final IServiceReference<IJcrService> jcrRef = context.getReference(jcrService);

        List<WorkflowActionComponent> actions = new LinkedList<WorkflowActionComponent>();
        DialogAction action;
        WorkflowActionComponent choice;

        action = new DialogAction(new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog() {
                return new RemodelDialog(RemodelWorkflowPlugin.this, jcrRef);
            }
        }, getDialogService());
        choice = new WorkflowActionComponent("remodelRequest-dialog", new StringResourceModel("update-content", this, null), (String) null, action);
        actions.add(choice);

        action = new DialogAction(new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog() {
                return new CreateTypeDialog(RemodelWorkflowPlugin.this, jcrRef);
            }
        }, getDialogService());
        choice = new WorkflowActionComponent("createTypeRequest-dialog", new StringResourceModel("create-type", this, null), (String) null, action);
        actions.add(choice);

        add(new WorkflowActionComponentDropDownChoice("actions", actions));
    }
}
