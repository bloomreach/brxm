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

import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.addon.workflow.AbstractWorkflowDialogRestyling;
import org.hippoecm.addon.workflow.IWorkflowInvoker;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.plugins.standardworkflow.validators.RenameDocumentValidator;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.NameUriField;
import org.hippoecm.repository.api.StringCodec;

public class RenameDocumentDialog extends AbstractWorkflowDialogRestyling<RenameDocumentArguments> {

    private final NameUriField nameUriContainer;
    private final IModel<StringCodec> nodeNameCodecModel;

    public RenameDocumentDialog(RenameDocumentArguments renameDocumentModel, IModel<String> title,
                                IWorkflowInvoker invoker, IModel<StringCodec> nodeNameCodec, final WorkflowDescriptorModel workflowDescriptorModel) {
        super(invoker, Model.of(renameDocumentModel), title);

        this.nodeNameCodecModel = nodeNameCodec;

        setSize(DialogConstants.MEDIUM_AUTO);

        final String originalUriName = renameDocumentModel.getUriName();
        final String originalTargetName = renameDocumentModel.getTargetName();

        add(nameUriContainer = new NameUriField("name-url", nodeNameCodecModel, originalUriName, originalTargetName, true));

        final Locale cmsLocale = UserSession.get().getLocale();
        final RenameMessage message = new RenameMessage(cmsLocale, renameDocumentModel.getLocalizedNames());
        if (message.shouldShow()) {
            warn(message.forFolder());
        }

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

    //TODO: This override method should be moved to the ancestor class in CMS7-9429
    @Override
    protected FeedbackPanel newFeedbackPanel(String id) {
        return new FeedbackPanel(id);
    }
}
