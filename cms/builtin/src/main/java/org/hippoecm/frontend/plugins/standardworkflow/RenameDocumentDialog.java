/*
 *  Copyright 2012-2015 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Locale;

import org.apache.wicket.ajax.AjaxChannel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.addon.workflow.IWorkflowInvoker;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.addon.workflow.WorkflowDialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.plugins.standardworkflow.validators.RenameDocumentValidator;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.NameUriField;
import org.hippoecm.repository.api.StringCodec;

public class RenameDocumentDialog extends WorkflowDialog<RenameDocumentArguments> {

    private final NameUriField nameUriContainer;
    private final IModel<StringCodec> nodeNameCodecModel;

    public RenameDocumentDialog(final RenameDocumentArguments renameDocumentArguments,
                                final IModel<String> title,
                                final IWorkflowInvoker invoker,
                                final IModel<StringCodec> nodeNameCodec,
                                final WorkflowDescriptorModel workflowDescriptorModel) {
        super(invoker, Model.of(renameDocumentArguments), title);

        this.nodeNameCodecModel = nodeNameCodec;

        setSize(DialogConstants.MEDIUM_AUTO);

        final String originalUriName = renameDocumentArguments.getUriName();
        final String originalTargetName = renameDocumentArguments.getTargetName();

        add(nameUriContainer = new NameUriField("name-url", nodeNameCodecModel, originalUriName, originalTargetName, true));

        // The dialog produces ajax requests in NameUriField and OK/Cancel dialog buttons, which may cause Wicket
        // exceptions when typing very fast. Thus it needs to use a dedicated ajax channel with ACTIVE behavior when
        // some AJAX requests may be sent after dialog is closed.
        final AjaxChannel activeAjaxChannel = new AjaxChannel(getMarkupId(), AjaxChannel.Type.ACTIVE);
        setAjaxChannel(activeAjaxChannel);
        nameUriContainer.setAjaxChannel(activeAjaxChannel);

        add(new RenameDocumentValidator(this, nameUriContainer, workflowDescriptorModel));
    }

    @Override
    protected void onOk() {
        RenameDocumentArguments renameDocumentArguments = getModel().getObject();
        renameDocumentArguments.setUriName(nameUriContainer.getUrl());
        renameDocumentArguments.setTargetName(nameUriContainer.getName());
        super.onOk();
    }

    @Override
    protected void onDetach() {
        nodeNameCodecModel.detach();
        super.onDetach();
    }
}
