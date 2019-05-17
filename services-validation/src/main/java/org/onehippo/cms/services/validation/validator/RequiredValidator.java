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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequiredValidator implements Validator<Object> {

    private static final Logger log = LoggerFactory.getLogger(RequiredValidator.class);

    private final Map<String, Validator> validators;

    // constructor must be public because class is instantiated via reflection
    @SuppressWarnings("WeakerAccess")
    public RequiredValidator(final Node node) {
        try {
            validators = createValidators(node);
        } catch (final RepositoryException e) {
            throw new ValidationContextException("Failed to create required validator", e);
        }
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
        final String type = context.getType();
        final String jcrType= context.getJcrType();
        final Validator requiredValidator = getRequiredValidator(type, jcrType);

        if (requiredValidator == null) {
            log.info("No 'required' validator found for field '{}' of type '{}', assuming all values are valid",
                    context.getJcrName(), type);
            return Optional.empty();
        }

        final RequiredValidationContext requiredValidationContext = new RequiredValidationContext(context);
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
    private Validator getRequiredValidator(final String type, final String jcrType) {
        if (validators.containsKey(type)) {
            return validators.get(type);
        }

        if (!type.equals(jcrType) && validators.containsKey(jcrType)) {
            return validators.get(jcrType);
        }

        return null;
    }
}
