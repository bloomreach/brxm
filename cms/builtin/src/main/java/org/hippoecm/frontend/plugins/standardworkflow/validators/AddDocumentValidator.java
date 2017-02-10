/*
 * Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.frontend.widgets.NameUriField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validate localized name and url name upon adding a new document/folder
 */
public class AddDocumentValidator extends DocumentFormValidator {

    private static final Logger log = LoggerFactory.getLogger(AddDocumentValidator.class);

    private final NameUriField nameUriField;
    private final WorkflowDescriptorModel workflowDescriptor;

    public AddDocumentValidator(final Form form, final NameUriField nameUriField,
                                final WorkflowDescriptorModel workflowDescriptor) {
        super(form);

        this.nameUriField = nameUriField;
        this.workflowDescriptor = workflowDescriptor;
    }

    @Override
    public FormComponent<?>[] getDependentFormComponents() {
        return nameUriField.getComponents();
    }

    @Override
    public void validate(final Form<?> form) {
        final String newNodeName = nameUriField.getUrlValue();
        final String newDisplayName = nameUriField.getNameValue();

        try {
            final Node parentNode = workflowDescriptor.getNode();
            final boolean hasNodeWithSameName = parentNode.hasNode(newNodeName);
            final boolean hasNodeWithSameLocalizedName = hasChildWithDisplayName(parentNode, newDisplayName);

            if (hasNodeWithSameName && hasNodeWithSameLocalizedName) {
                showError(ERROR_SNS_NAMES_EXIST, newNodeName, newDisplayName);
            } else if (hasNodeWithSameName) {
                showError(ERROR_SNS_NODE_EXISTS, newNodeName);
            } else if (hasNodeWithSameLocalizedName) {
                showError(ERROR_LOCALIZED_NAME_EXISTS, newDisplayName);
            }
        } catch (RepositoryException e) {
            log.error("validation error: {}", e.getMessage());
            showError(ERROR_VALIDATION_NAMES);
        }
    }
}
