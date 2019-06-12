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

package org.onehippo.cms.services.validation;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import javax.jcr.Node;

import org.onehippo.cms.services.validation.api.ValidationContextException;
import org.onehippo.cms.services.validation.api.Validator;
import org.onehippo.cms.services.validation.api.ValueContext;
import org.onehippo.cms.services.validation.api.Violation;
import org.onehippo.cms.services.validation.api.internal.ValidatorConfig;
import org.onehippo.cms.services.validation.api.internal.ValidatorInstance;

class ValidatorInstanceImpl implements ValidatorInstance {

    private final Validator validator;
    private final ValidatorConfig config;
    private final ThreadLocal<ValueContext> validatorContext;

    ValidatorInstanceImpl(final Validator validator, final ValidatorConfig config) {
        this.validator = validator;
        this.config = config;
        validatorContext = new ThreadLocal<>();
    }

    @Override
    public ValidatorConfig getConfig() {
        return config;
    }

    @Override
    public Optional<Violation> validate(final ValueContext context, final Object value) {
        try {
            validatorContext.set(context);
            return runValidator(value);
        } catch (ClassCastException e) {
            throw new ValidationContextException("Validator '" + validator.getClass().getName() + "'"
                    + " with name '" + config.getName() + "' is used in field '" + context.getJcrName() + "'"
                    + " of type '" + context.getJcrType() + "'."
                    + " The value of that field is of type '" + value.getClass().getName() + "',"
                    + " which is not compatible with the value type expected by the validator", e);
        } finally {
            validatorContext.remove();
        }
    }

    @SuppressWarnings("unchecked")
    private Optional<Violation> runValidator(final Object value) {
        return validator.validate(this, value);
    }

    @Override
    public String getJcrName() {
        return validatorContext.get().getJcrName();
    }

    @Override
    public String getJcrType() {
        return validatorContext.get().getJcrType();
    }

    @Override
    public String getType() {
        return validatorContext.get().getType();
    }

    @Override
    public Locale getLocale() {
        return validatorContext.get().getLocale();
    }

    @Override
    public TimeZone getTimeZone() {
        return validatorContext.get().getTimeZone();
    }

    @Override
    public Node getParentNode() {
        return validatorContext.get().getParentNode();
    }

    @Override
    public Node getDocumentNode() {
        return validatorContext.get().getDocumentNode();
    }

    @Override
    public Violation createViolation() {
        return createViolation((Map<String, String>) null);
    }

    @Override
    public Violation createViolation(final Map<String,String> parameters) {
        final TranslatedViolation violation = new TranslatedViolation(getLocale(), config.getName());
        violation.setParameters(parameters);
        return violation;
    }

    @Override
    public Violation createViolation(final String subKey) {
        return createViolation(subKey, null);
    }

    @Override
    public Violation createViolation(final String subKey, final Map<String,String> parameters) {
        final String key = config.getName() + "#" + subKey;
        final TranslatedViolation violation = new TranslatedViolation(getLocale(), key);
        violation.setParameters(parameters);
        return violation;
    }
}
