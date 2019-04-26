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

import org.apache.commons.lang.StringUtils;
import org.htmlcleaner.HtmlCleaner;
import org.onehippo.cms.services.validation.api.ValidationContext;
import org.onehippo.cms.services.validation.api.Validator;
import org.onehippo.cms.services.validation.api.Violation;
import org.onehippo.cms.services.validation.util.HtmlUtils;

/**
 * Validator that validates that a String value is non-empty.
 * <p>
 * When the type of the value is the builtin "Html" type, an {@link HtmlCleaner} is used to verify this. Such a field
 * therefore does not require the html validator to be declared separately.
 */
public class NonEmptyValidator implements Validator<String> {

    @Override
    public Optional<Violation> validate(final ValidationContext context, final String value) {
        final boolean isEmpty = "Html".equals(context.getType())
                ? HtmlUtils.isEmpty(value)
                : StringUtils.isBlank(value);

        if (isEmpty) {
            return Optional.of(context.createViolation());
        }

        return Optional.empty();
    }
}
