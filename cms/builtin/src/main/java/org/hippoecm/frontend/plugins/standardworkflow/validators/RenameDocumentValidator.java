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

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.widgets.NameUriField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RenameDocumentValidator extends DocumentFormValidator {

    static Logger log = LoggerFactory.getLogger(RenameDocumentValidator.class);

    private final WorkflowDescriptorModel workflowDescriptorModel;
    private final NameUriField nameUriContainer;
    private final String originalName;
    private final String originalUrl;

    public RenameDocumentValidator(NameUriField nameUriContainer, WorkflowDescriptorModel workflowDescriptorModel) {
        super(nameUriContainer);

        this.nameUriContainer = nameUriContainer;
        this.workflowDescriptorModel = workflowDescriptorModel;
        this.originalName = nameUriContainer.getName();
        this.originalUrl = nameUriContainer.getUrl();
    }

    @Override
    public FormComponent<?>[] getDependentFormComponents() {
        return nameUriContainer.getComponents();
    }

    @Override
    public void validate(final Form<?> form) {
        final String newUrlName = nameUriContainer.getUrlComponent().getValue().toLowerCase();
        final String newLocalizedName = nameUriContainer.getNameComponent().getValue();
        try {
            final Node parentNode = workflowDescriptorModel.getNode().getParent();

            if (StringUtils.equals(newUrlName, originalUrl)) {
                if (StringUtils.equals(newLocalizedName, originalName)) {
                    showError(NameUriField.ERROR_SAME_NAMES);
                } else if (hasChildWithLocalizedName(parentNode, newLocalizedName)) {
                    showError(NameUriField.ERROR_LOCALIZED_NAME_EXISTS, newLocalizedName);
                }
            } else {
                final boolean hasNodeWithSameName = parentNode.hasNode(newUrlName);
                final boolean hasOtherNodeWithSameLocalizedName = !StringUtils.equals(newLocalizedName, originalName) &&
                        hasChildWithLocalizedName(parentNode, newLocalizedName);

                if (hasNodeWithSameName && hasOtherNodeWithSameLocalizedName) {
                    showError(NameUriField.ERROR_SNS_NAMES_EXIST, newUrlName, newLocalizedName);
                } else if (hasNodeWithSameName) {
                    showError(NameUriField.ERROR_SNS_NODE_EXISTS, newUrlName);
                } else if (hasOtherNodeWithSameLocalizedName) {
                    showError(NameUriField.ERROR_LOCALIZED_NAME_EXISTS, newLocalizedName);
                }
            }
        } catch (RepositoryException e) {
            log.error("validation error: {}", e.getMessage());
            showError(NameUriField.ERROR_VALIDATION_NAMES);
        }
    }
}
