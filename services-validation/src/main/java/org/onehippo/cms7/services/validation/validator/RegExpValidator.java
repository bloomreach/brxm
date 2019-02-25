/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.validation.validator;

import java.util.regex.Pattern;

import org.onehippo.cms7.services.validation.ValidatorConfig;
import org.onehippo.cms7.services.validation.exception.InvalidValidatorException;
import org.onehippo.cms7.services.validation.exception.ValidatorException;
import org.onehippo.cms7.services.validation.field.FieldContext;

/**
 * Validator that validates if the given value matches the configured regular expression.
 * <p>
 * Use property "regexp.pattern" to set the required expression.
 */
public class RegExpValidator extends AbstractFieldValidator {

    private final Pattern pattern;

    private final static String PATTERN_KEY = "regexp.pattern";

    public RegExpValidator(final ValidatorConfig config) throws InvalidValidatorException {
        super(config);

        if (config.hasProperty(PATTERN_KEY)) {
            pattern = Pattern.compile(config.getProperty(PATTERN_KEY));
        } else {
            throw new InvalidValidatorException(
                    "Missing required property 'regexp.pattern' on validator '" + config.getName() + "'");
        }
    }

    @Override
    public void init(final FieldContext context) throws InvalidValidatorException {
        if (!"string".equalsIgnoreCase(context.getType())) {
            throw new InvalidValidatorException("Cannot validate non-string field with a regular expression");
        }
    }

    @Override
    public boolean isValid(final FieldContext context, final String value) throws ValidatorException {
        return pattern.matcher(value).find();
    }
}
