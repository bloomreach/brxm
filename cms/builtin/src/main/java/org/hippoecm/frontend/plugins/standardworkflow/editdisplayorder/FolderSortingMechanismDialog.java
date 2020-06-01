/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.frontend.plugins.standardworkflow.editdisplayorder;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.IWorkflowInvoker;
import org.hippoecm.addon.workflow.WorkflowDialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.repository.api.WorkflowDescriptor;

/**
 * Creates dialog to edit the display order of subfolders.
 */
public class FolderSortingMechanismDialog extends WorkflowDialog<WorkflowDescriptor> {

    /**
     * Constructs the dialog for editing the display order.
     * @param invoker The {@link org.hippoecm.frontend.plugins.standardworkflow.FolderWorkflowPlugin} instance
     * @param model a {@link org.hippoecm.addon.workflow.WorkflowDescriptorModel}
     * @param titleModel The model used for the title of this dialog
     * @param folderSortingMechanism The model with a pojo that contains a "alphabetically" property.
     */
    public FolderSortingMechanismDialog(final IWorkflowInvoker invoker,
                                        final IModel<WorkflowDescriptor> model,
                                        final IModel<String> titleModel,
                                        final IModel<Boolean> folderSortingMechanism) {
        super(invoker, model, titleModel);

        setSize(DialogConstants.SMALL_AUTO);

        final StringResourceModel setFolderSortingMechanism = new StringResourceModel("order-folder-content");
        add(new Label("order-child-folder-radio-group-label", setFolderSortingMechanism));

        final RadioGroup<Boolean> orderChildFolderRadioGroup = new RadioGroup<>("order-child-folder-radio-group",
                folderSortingMechanism);
        add(orderChildFolderRadioGroup);

        final StringResourceModel alphabetically = new StringResourceModel("alphabetically", this);
        orderChildFolderRadioGroup.add(new Radio<>("alphabetically-radio", Model.of(Boolean.TRUE)));
        orderChildFolderRadioGroup.add(new Label("alphabetically-label", alphabetically));

        final StringResourceModel manually = new StringResourceModel("manually", this);
        orderChildFolderRadioGroup.add(new Radio<>("manually-radio", Model.of(Boolean.FALSE)));
        orderChildFolderRadioGroup.add(new Label("manually-label", manually));
    }

}
