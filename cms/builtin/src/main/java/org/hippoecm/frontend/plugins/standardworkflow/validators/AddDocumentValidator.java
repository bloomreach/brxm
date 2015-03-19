/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.frontend.plugins.standardworkflow.validators;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.plugins.standardworkflow.components.NameUriField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validate localized name and url name upon adding a new document/folder
 */
public abstract class AddDocumentValidator extends DocumentFormValidator{
    /**
     * Error key messages. Component uses this validator must have these keys in its resource bundle
     */
    public static final String ERROR_SNS_NODE_EXISTS = "error-sns-node-exists";
    public static final String ERROR_LOCALIZED_NAME_EXISTS = "error-localized-name-exists";
    public static final String ERROR_VALIDATION_NAMES = "error-validation-names";

    static Logger log = LoggerFactory.getLogger(AddDocumentValidator.class);

    private final NameUriField nameUriContainer;
    private final WorkflowDescriptorModel workflowDescriptorModel;

    public AddDocumentValidator(NameUriField nameUriContainer, final WorkflowDescriptorModel workflowDescriptorModel) {
        this.nameUriContainer = nameUriContainer;
        this.workflowDescriptorModel = workflowDescriptorModel;
    }

    @Override
    public FormComponent<?>[] getDependentFormComponents() {
        return nameUriContainer.getComponents();
    }

    @Override
    public void validate(final Form<?> form) {
        String newNodeName = nameUriContainer.getUrlComponent().getValue().toLowerCase();
        String newLocalizedName = nameUriContainer.getNameComponent().getValue();
        try {
            final Node parentNode = workflowDescriptorModel.getNode();
            if (parentNode.hasNode(newNodeName)) {
                showError(ERROR_SNS_NODE_EXISTS, new Object[]{newNodeName});
                return;
            }
            if (existedLocalizedName(parentNode, newLocalizedName)) {
                showError(ERROR_LOCALIZED_NAME_EXISTS, new Object[]{newLocalizedName});
            }
        } catch (RepositoryException e) {
            log.error("validation error", e);
            showError(ERROR_VALIDATION_NAMES, null);
        }
    }
}