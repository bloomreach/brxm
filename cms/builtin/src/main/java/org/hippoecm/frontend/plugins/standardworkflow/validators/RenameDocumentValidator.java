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

public abstract class RenameDocumentValidator extends DocumentFormValidator {
    static Logger log = LoggerFactory.getLogger(RenameDocumentValidator.class);
    /**
     * Error key messages. Component uses this validator must have these keys in its resource bundle
     */
    public static final String ERROR_SAME_NAMES = "error-same-names";
    public static final String ERROR_SNS_NODE_EXISTS = "error-sns-node-exists";
    public static final String ERROR_LOCALIZED_NAME_EXISTS = "error-localized-name-exists";
    public static final String ERROR_SNS_NAMES_EXISTS = "error-sns-names-exist";
    public static final String ERROR_VALIDATION_NAMES = "error-validation-names";

    private final WorkflowDescriptorModel workflowDescriptorModel;
    private final NameUriField nameUriContainer;
    private final String originalName;
    private final String originalUrl;

    public RenameDocumentValidator(NameUriField nameUriContainer, WorkflowDescriptorModel workflowDescriptorModel) {
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
                    showError(ERROR_SAME_NAMES);
                } else if (hasChildWithLocalizedName(parentNode, newLocalizedName)) {
                    showError(ERROR_LOCALIZED_NAME_EXISTS, newLocalizedName);
                }
            } else {
                final boolean hasNodeWithSameName = parentNode.hasNode(newUrlName);
                final boolean hasOtherNodeWithSameLocalizedName = !StringUtils.equals(newLocalizedName, originalName) &&
                        hasChildWithLocalizedName(parentNode, newLocalizedName);

                if (hasNodeWithSameName && hasOtherNodeWithSameLocalizedName) {
                    showError(ERROR_SNS_NAMES_EXISTS, newUrlName, newLocalizedName);
                } else if (hasNodeWithSameName) {
                    showError(ERROR_SNS_NODE_EXISTS, newUrlName);
                } else if (hasOtherNodeWithSameLocalizedName) {
                    showError(ERROR_LOCALIZED_NAME_EXISTS, newLocalizedName);
                }
            }
        } catch (RepositoryException e) {
            log.error("validation error", e);
            showError(ERROR_VALIDATION_NAMES);
        }
    }
}
