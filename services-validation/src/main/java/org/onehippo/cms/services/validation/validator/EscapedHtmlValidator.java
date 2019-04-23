/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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

import org.onehippo.cms.services.validation.api.ValidationContextException;
import org.onehippo.cms.services.validation.api.Validator;
import org.onehippo.cms.services.validation.api.ValidatorConfig;
import org.onehippo.cms.services.validation.api.ValidationContext;
import org.onehippo.cms.services.validation.api.Violation;

/**
 * Validator that validates if the value is properly HTML escaped using a regular expression.
 */
public class EscapedHtmlValidator implements Validator {

    private static final Pattern INVALID_CHARS = Pattern.compile(".*[<>&\"'].*");

    @Override
    public Optional<Violation> validate(final ValidationContext context, final String value) {
        if (INVALID_CHARS.matcher(value).matches()) {
            return Optional.of(context.createViolation(this));
        }
        return Optional.empty();
    }
}
