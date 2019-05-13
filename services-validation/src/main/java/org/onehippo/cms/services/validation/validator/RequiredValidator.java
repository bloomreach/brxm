/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms.services.validation.validator;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.onehippo.cms.services.validation.JcrValidatorConfig;
import org.onehippo.cms.services.validation.RequiredValidationContext;
import org.onehippo.cms.services.validation.ValidatorFactory;
import org.onehippo.cms.services.validation.api.ValidationContext;
import org.onehippo.cms.services.validation.api.ValidationContextException;
import org.onehippo.cms.services.validation.api.Validator;
import org.onehippo.cms.services.validation.api.Violation;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.contenttype.ContentTypeService;
import org.onehippo.cms7.services.contenttype.EffectiveNodeType;

import static org.onehippo.cms.services.validation.validator.NonEmptyHtmlValidator.log;

public class RequiredValidator implements Validator<Object> {

    private final Map<String, Validator> validators;
    private final ContentTypeService contentTypeService;

    public RequiredValidator(final Node node) {
        try {
            validators = createValidators(node);
        } catch (final RepositoryException e) {
            throw new ValidationContextException("Failed to create required validator", e);
        }

        contentTypeService = HippoServiceRegistry.getService(ContentTypeService.class);
    }

    private static Map<String, Validator> createValidators(final Node config) throws RepositoryException {
        final Map<String, Validator> validators = new HashMap<>();
        final NodeIterator iterator = config.getNodes();
        while (iterator.hasNext()) {
            final Node validatorConfigNode = iterator.nextNode();
            final JcrValidatorConfig validatorConfig = new JcrValidatorConfig(validatorConfigNode);
            final String validatorName = validatorConfig.getName();
            validators.computeIfAbsent(validatorName,
                    name -> ValidatorFactory.createValidator(validatorConfig));
        }

        return validators;
    }

    @Override
    public Optional<Violation> validate(final ValidationContext context, final Object value) {
        final String fieldType = context.getType();
        final Validator requiredValidator = getRequiredValidator(fieldType);

        if (requiredValidator == null) {
            throw new ValidationContextException("No 'required' validator found for type '" + fieldType + "'"
                    + ", cannot validate required field '" + context.getJcrName() + "'");
        }

        final RequiredValidationContext requiredValidationContext = new RequiredValidationContext(context, fieldType);
        return runValidator(requiredValidator, requiredValidationContext, value);
    }

    @SuppressWarnings("unchecked")
    private Optional<Violation> runValidator(final Validator validator, final ValidationContext context, final Object value) {
        return validator.validate(context, value);
    }

    /**
     * Returns an instance of a required {@link Validator}, or null if the configuration cannot be found.
     * @param type The type of the field
     * @return Instance of a {@link Validator}
     */
    private Validator getRequiredValidator(final String type) {
        final Validator requiredValidator = validators.get(type);

        if (requiredValidator != null) {
            return requiredValidator;
        }

        try {
            return getRequiredValidatorForSuperType(type);
        } catch (RepositoryException e) {
            log.warn("Could not find required validator for type '{}'", type, e);
            return null;
        }
    }

    private Validator getRequiredValidatorForSuperType(final String type) throws RepositoryException {
        final EffectiveNodeType effectiveNodeType = contentTypeService.getEffectiveNodeTypes().getType(type);

        for (final String superType : effectiveNodeType.getSuperTypes()) {
            final Validator requiredValidator = getRequiredValidator(superType);
            if (requiredValidator != null) {
                return requiredValidator;
            }
        }

        return null;
    }

}
