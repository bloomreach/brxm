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
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.standardworkflow.TemplateEditorWorkflow;

public class NamespaceWorkflowPlugin extends CompatibilityWorkflowPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public NamespaceWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        List<WorkflowActionComponent> actions = new LinkedList<WorkflowActionComponent>();

        DialogAction action = new DialogAction(new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog() {
                return new NamespaceDialog();
            }
        }, getDialogService());
        WorkflowActionComponent choice = new WorkflowActionComponent("createNamespaceRequest-dialog",
                new StringResourceModel("create-namespace", this, null), (String) null, action);
        actions.add(choice);

        add(new WorkflowActionComponentDropDownChoice("actions", actions));
    }

public class NamespaceDialog extends CompatibilityWorkflowPlugin.Dialog {
    private static final long serialVersionUID = 1L;

    private String prefix;

    private String url;

    public NamespaceDialog() {
        super();

        add(new TextFieldWidget("prefix", new PropertyModel(this, "prefix")));

        add(new TextFieldWidget("url", new PropertyModel(this, "url")));
    }

    @Override
    protected String execute() {
        try {
        TemplateEditorWorkflow workflow = (TemplateEditorWorkflow) getWorkflow();
        workflow.createNamespace(NodeNameCodec.encode(prefix, true), url);
        return null;
        } catch(Exception ex) {
            return ex.getClass().getName()+": "+ex.getMessage();
        }
    }

    public IModel getTitle() {
        return new StringResourceModel("create-namespace", this, null);
    }
}

}
