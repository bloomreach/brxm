/*
 *  Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.services.validation.validator;

import java.util.Optional;

import org.onehippo.cms7.services.validation.Validator;
import org.onehippo.cms7.services.validation.ValidatorConfig;
import org.onehippo.cms7.services.validation.ValidatorContext;
import org.onehippo.cms7.services.validation.Violation;
import org.onehippo.cms7.services.validation.exception.InvalidValidatorException;
import org.onehippo.cms.services.validation.util.TranslationUtils;

public abstract class AbstractFieldValidator implements Validator {

    private final ValidatorConfig config;

    public AbstractFieldValidator(final ValidatorConfig config) {
        this.config = config;
    }

    @Override
    public void init(final ValidatorContext context) throws InvalidValidatorException {
    }

    public String getName() {
        return config.getName();
    }

    @Override
    public Optional<Violation> validate(final ValidatorContext context, final String value) {
        return !isValid(context, value) ? getViolation(context) : Optional.empty();
    }

    protected abstract boolean isValid(final ValidatorContext context, final String value);

    protected Optional<Violation> getViolation(final ValidatorContext context) {
        return Optional.of(() -> getViolationMessage(context));
    }

    protected String getViolationMessage(final ValidatorContext context) {
        return TranslationUtils.getTranslatedMessage(getName(), context.getLocale());
    }

}
