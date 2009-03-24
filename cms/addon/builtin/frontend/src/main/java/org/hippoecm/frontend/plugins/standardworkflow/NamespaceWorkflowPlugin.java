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


import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.standardworkflow.TemplateEditorWorkflow;

public class NamespaceWorkflowPlugin extends CompatibilityWorkflowPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public NamespaceWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        add(new WorkflowAction("create", new StringResourceModel("create-namespace", this, null)) {
            public String url;
            public String prefix;
            @Override
            protected Dialog createRequestDialog() {
                return new NamespaceDialog(this);
            }
            @Override
            protected String execute(Workflow wf) throws Exception {
                TemplateEditorWorkflow workflow = (TemplateEditorWorkflow) wf;
                workflow.createNamespace(NodeNameCodec.encode(prefix, true), url);
                return null;
            }
        });
    }

    public class NamespaceDialog extends CompatibilityWorkflowPlugin.WorkflowAction.WorkflowDialog {
        public NamespaceDialog(CompatibilityWorkflowPlugin.WorkflowAction action) {
            action.super( );
            add(new TextFieldWidget("prefix", new PropertyModel(action, "prefix")));
            add(new TextFieldWidget("url", new PropertyModel(action, "url")));
        }
        public IModel getTitle() {
            return new StringResourceModel("create-namespace", this, null);
        }
    }
}
