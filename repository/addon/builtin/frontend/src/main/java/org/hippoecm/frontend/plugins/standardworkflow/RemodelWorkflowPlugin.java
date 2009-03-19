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

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogAction;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standardworkflow.remodel.RemodelDialog;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.standardworkflow.TemplateEditorWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemodelWorkflowPlugin extends CompatibilityWorkflowPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final Logger log = LoggerFactory.getLogger(RemodelWorkflowPlugin.class);

    private static final long serialVersionUID = 1L;

    public RemodelWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        List<WorkflowActionComponent> actions = new LinkedList<WorkflowActionComponent>();
        DialogAction action;
        WorkflowActionComponent choice;

        action = new DialogAction(new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog() {
                return new RemodelDialog(RemodelWorkflowPlugin.this);
            }
        }, getDialogService());
        choice = new WorkflowActionComponent("remodelRequest-dialog", new StringResourceModel("update-content", this, null), (String) null, action);
        actions.add(choice);

        action = new DialogAction(new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog() {
                return new CreateTypeDialog();
            }
        }, getDialogService());
        choice = new WorkflowActionComponent("createTypeRequest-dialog", new StringResourceModel("create-type", this, null), (String) null, action);
        actions.add(choice);

        add(new WorkflowActionComponentDropDownChoice("actions", actions));
    }

public class CreateTypeDialog extends CompatibilityWorkflowPlugin.Dialog {
    private static final long serialVersionUID = 1L;

    private String name;

    public CreateTypeDialog() {
        super();
        add(new TextFieldWidget("name", new PropertyModel(this, "name")));
    }

    @Override
    protected String execute() {
        try {
        TemplateEditorWorkflow workflow = (TemplateEditorWorkflow) getWorkflow();
        workflow.createType(name);
        return null;
        } catch(Exception ex) {
            return ex.getClass().getName()+": "+ex.getMessage();
        }
    }

    public IModel getTitle() {
        return new StringResourceModel("create-type", this, null);
    }
}
}
