/*
 *  Copyright 2009-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.workflow;

import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.editor.repository.NamespaceWorkflow;
import org.hippoecm.frontend.editor.layout.ILayoutProvider;
import org.hippoecm.frontend.editor.workflow.action.NewCompoundTypeAction;
import org.hippoecm.frontend.editor.workflow.action.NewDocumentTypeAction;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IEditorManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NamespaceWorkflowPlugin extends CompatibilityWorkflowPlugin<NamespaceWorkflow> {

    private static final Logger log = LoggerFactory.getLogger(NamespaceWorkflowPlugin.class);

    private static final long serialVersionUID = 1L;

    public NamespaceWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        final ILayoutProvider layouts = context.getService(ILayoutProvider.class.getName(), ILayoutProvider.class);
        add(new NewDocumentTypeAction(this, "new-document-type", new StringResourceModel("new-document-type", this), layouts));
        add(new NewCompoundTypeAction(this, layouts));
    }

    public IEditorManager getEditorManager() {
        return getPluginContext().getService(getPluginConfig().getString("editor.id", IEditorManager.class.getName()), IEditorManager.class);
    }
}
