/*
 *  Copyright 2014-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.reviewedactions;

import org.apache.wicket.Component;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.editor.workflow.dialog.DocumentMetadataDialog;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.repository.api.Workflow;

public class DocMetaDataPlugin extends AbstractDocumentWorkflowPlugin {

    public DocMetaDataPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        add(new StdWorkflow("docMetaData", new StringResourceModel("docmetadata-label", this, null), context, getModel()) {

            @Override
            public String getSubMenu() {
                return "document";
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.INFO_CIRCLE);
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                final WorkflowDescriptorModel wdm = getModel();
                return new DocumentMetadataDialog(wdm);
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                return null;
            }
        });

    }
}
