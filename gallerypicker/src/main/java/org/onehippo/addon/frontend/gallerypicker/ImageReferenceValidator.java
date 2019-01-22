/*
 *  Copyright 2013-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.addon.frontend.gallerypicker;

import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.editor.validator.plugins.AbstractCmsValidator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.validation.IFieldValidator;
import org.hippoecm.frontend.validation.ValidationException;
import org.hippoecm.frontend.validation.Violation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validator for checking mandatory links to images.
 * <p>
 * It checks the link's reference (a node identifier) for not being set at all or being set to root node '/' or to node
 * '/content/gallery'.
 */
public class ImageReferenceValidator extends AbstractCmsValidator {

    private static final Logger log = LoggerFactory.getLogger(ImageReferenceValidator.class);

    public ImageReferenceValidator(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    public void preValidation(final IFieldValidator type) throws ValidationException {
        // do nothing
    }

    @Override
    public Set<Violation> validate(final IFieldValidator fieldValidator, final JcrNodeModel model,
                                   final IModel childModel) throws ValidationException {

        final Set<Violation> violations = new HashSet<>();
        try {
            final Object object = childModel.getObject();
            final String ref;
            if (object instanceof Node && ((Node) object).hasProperty("hippo:docbase")) {
                ref = ((Node) object).getProperty("hippo:docbase").getString();
            } else if (object instanceof String) {
                ref = (String) object;
            } else {
                return violations;
            }

            final String contentGalleryIdentifier = model.getNode().getSession().nodeExists("/content/gallery") 
                    ? model.getNode().getSession().getNode("/content/gallery").getIdentifier() 
                    : null;
            if (ref == null || ref.equals("")
                    || ref.equals("cafebabe-cafe-babe-cafe-babecafebabe")
                    || ref.equals(contentGalleryIdentifier)) {
                final Violation violation = fieldValidator.newValueViolation(childModel, "reference-is-empty");
                violation.setValidationScope(getValidationScope());
                violations.add(violation);
            }
        } catch (RepositoryException repositoryException) {
            log.error("Error validating image reference field: " + fieldValidator.getFieldDescriptor(),
                    repositoryException);
        }
        return violations;
    }

}
