/*
 *  Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.AbstractWorkflowDialog;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.plugins.standardworkflow.RenameMessage;
import org.hippoecm.frontend.plugins.standardworkflow.components.NameUriField;
import org.hippoecm.frontend.plugins.standardworkflow.validators.RenameDocumentValidator;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.Localized;
import org.hippoecm.repository.api.StringCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public  class RenameDocumentDialog extends AbstractWorkflowDialog<Void> {
    private static Logger log = LoggerFactory.getLogger(RenameDocumentDialog.class);

    private final IModel<String> title;
    private final NameUriField nameUriContainer;
    private final PropertyModel<String> nameModel;
    private final PropertyModel<String> uriModel;

    public RenameDocumentDialog(StdWorkflow action, IModel<String> title, IModel<StringCodec> codecModel) {
        super(null, action);

        this.title = title;

        nameModel = new PropertyModel<>(action, "targetName");
        uriModel = new PropertyModel<>(action, "uriName");
        final PropertyModel<Map<Localized, String>> localizedNamesModel = new PropertyModel<>(action, "localizedNames");

        String originalName = nameModel.getObject();
        String originalUriName = uriModel.getObject();
        final boolean uriModified = !StringUtils.equals(originalName, originalUriName);
        add(nameUriContainer = new NameUriField("name-url", codecModel, originalUriName, originalName, uriModified));

        add(new RenameDocumentValidator(nameUriContainer, action.getModel()) {
            @Override
            protected void showError(final String key, final Object... parameters) {
                error(new StringResourceModel(key, RenameDocumentDialog.this, null, parameters).getObject());
            }
        });

        final Locale cmsLocale = UserSession.get().getLocale();
        final RenameMessage message = new RenameMessage(cmsLocale, localizedNamesModel.getObject());
        if (message.shouldShow()) {
            warn(message.forDocument());
        }
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
        this.uriModel.setObject(nameUriContainer.getUrl());
        this.nameModel.setObject(nameUriContainer.getName());
        super.onOk();
    }
}