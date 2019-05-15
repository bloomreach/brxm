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
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.onehippo.cms.services.validation.api.ValidationContext;
import org.onehippo.cms.services.validation.api.ValidationContextException;
import org.onehippo.cms.services.validation.api.Validator;
import org.onehippo.cms.services.validation.api.Violation;

/**
 * Validator that validates if the given value matches the configured regular expression.
 * <p>
 * Use property "regexp.pattern" to set the required expression.
 */
public class RegExpValidator implements Validator<String> {

    private final Pattern pattern;

    private static final String PATTERN_KEY = "regexp.pattern";

    // constructor must be public because class is instantiated via reflection
    @SuppressWarnings("WeakerAccess")
    public RegExpValidator(final Node config) {
        try {
            pattern = Pattern.compile(config.getProperty(PATTERN_KEY).getString());
        } catch (RepositoryException e) {
            throw new ValidationContextException("Cannot read required property '" + PATTERN_KEY + "'", e);
        }
    }

    @Override
    public Optional<Violation> validate(final ValidationContext context, final String value) {
        if (pattern.matcher(value).find()) {
            return Optional.empty();
        }

        return Optional.of(context.createViolation());
    }
}
