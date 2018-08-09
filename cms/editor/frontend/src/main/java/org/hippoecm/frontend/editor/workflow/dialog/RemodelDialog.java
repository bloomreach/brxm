/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.workflow.dialog;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.dialog.IDialogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemodelDialog extends CompatibilityWorkflowPlugin.WorkflowAction.WorkflowDialog {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(RemodelDialog.class);

    private RemodelWizard wizard;

    WorkflowDescriptorModel model;

    public RemodelDialog(CompatibilityWorkflowPlugin.WorkflowAction action, WorkflowDescriptorModel model) {
        action.super();
        this.model = model;

        if (model == null) {
            add(new Label("wizard"));
        } else {
            wizard = new RemodelWizard("wizard");
            add(wizard);
        }

        setOkVisible(false);
        setCancelVisible(false);
    }

    @Override
    public void setDialogService(IDialogService dialogService) {
        super.setDialogService(dialogService);
        wizard.setDialogService(dialogService);
    }

    @Override
    public IModel getTitle() {
        return new StringResourceModel("update-content", this);
    }

    void remodel() throws Exception {
        execute();
    }

    @Override
    public IValueMap getProperties() {
        return DialogConstants.SMALL;
    }
}
