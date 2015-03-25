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

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.AbstractWorkflowDialog;
import org.hippoecm.addon.workflow.IWorkflowInvoker;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.widgets.NameUriField;
import org.hippoecm.frontend.plugins.standardworkflow.validators.RenameDocumentValidator;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.StringCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RenameDocumentDialog extends AbstractWorkflowDialog<RenameDocumentArguments> {
    private static Logger log = LoggerFactory.getLogger(RenameDocumentDialog.class);

    private final IModel<String> title;
    private final NameUriField nameUriContainer;
    private final IModel<StringCodec> nodeNameCodecModel;

    public RenameDocumentDialog(RenameDocumentArguments renameDocumentModel, IModel<String> title,
                                IWorkflowInvoker invoker, IModel<StringCodec> nodeNameCodec, final WorkflowDescriptorModel workflowDescriptorModel) {
        super(Model.of(renameDocumentModel), invoker);
        this.title = title;
        this.nodeNameCodecModel = nodeNameCodec;

        final String originalUriName = renameDocumentModel.getUriName();
        final String originalTargetName = renameDocumentModel.getTargetName();

        final boolean uriModified = !StringUtils.equals(originalTargetName, originalUriName);
        add(nameUriContainer = new NameUriField("name-url", this.nodeNameCodecModel, originalUriName, originalTargetName, uriModified));

        final Locale cmsLocale = UserSession.get().getLocale();
        final RenameMessage message = new RenameMessage(cmsLocale, renameDocumentModel.getLocalizedNames());
        if (message.shouldShow()) {
            warn(message.forFolder());
        }

        add(new RenameDocumentValidator(nameUriContainer, workflowDescriptorModel) {
            @Override
            protected void showError(final String key, final Object... parameters) {
                error(new StringResourceModel(key, RenameDocumentDialog.this, null, parameters).getObject());
            }
        });
    }

    @Override
    public IModel<String> getTitle() {
        return title;
    }

    @Override
    public IValueMap getProperties() {
        return DialogConstants.MEDIUM;
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
