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

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.addon.workflow.WorkflowDialog;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.plugins.standardworkflow.validators.RenameDocumentValidator;
import org.hippoecm.frontend.widgets.NameUriField;
import org.hippoecm.repository.api.StringCodec;

/**
 * @deprecated replaced by {@link org.hippoecm.frontend.plugins.standardworkflow.RenameDocumentDialog} since version 3.2.0.
 */
@Deprecated
public  class RenameDocumentDialog extends WorkflowDialog<Void> {

    private final NameUriField nameUriField;
    private final PropertyModel<String> nameModel;
    private final PropertyModel<String> uriModel;

    public RenameDocumentDialog(StdWorkflow action, IModel<String> title, IModel<StringCodec> codecModel) {
        super(action, null, title);

        setSize(DialogConstants.MEDIUM_AUTO);

        nameModel = new PropertyModel<>(action, "targetName");
        uriModel = new PropertyModel<>(action, "uriName");

        String originalName = nameModel.getObject();
        String originalUriName = uriModel.getObject();
        add(nameUriField = new NameUriField("name-url", codecModel, originalUriName, originalName, true));

        add(new RenameDocumentValidator(this, nameUriField, action.getModel()));
    }

    @Override
    protected void onOk() {
        this.uriModel.setObject(nameUriField.getUrl());
        this.nameModel.setObject(nameUriField.getName());
        super.onOk();
    }
}
