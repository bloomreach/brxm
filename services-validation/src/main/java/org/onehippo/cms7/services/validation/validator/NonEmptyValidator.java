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
package org.onehippo.cms7.services.validation.validator;

import org.apache.commons.lang.StringUtils;
import org.htmlcleaner.HtmlCleaner;
import org.onehippo.cms7.services.validation.ValidatorConfig;
import org.onehippo.cms7.services.validation.ValidatorContext;
import org.onehippo.cms7.services.validation.exception.InvalidValidatorException;
import org.onehippo.cms7.services.validation.exception.ValidatorException;
import org.onehippo.cms7.services.validation.util.HtmlUtils;

/**
 * Validator that validates that a String value is non-empty.
 * <p>
 * When the type of the value is the builtin "Html" type, an {@link HtmlCleaner} is used to verify this. Such a field
 * therefore does not require the html validator to be declared separately.
 */
public class NonEmptyValidator extends AbstractFieldValidator {

    public NonEmptyValidator(final ValidatorConfig config) {
        super(config);
    }

    @Override
    public void init(final ValidatorContext context) throws InvalidValidatorException {
        if (!"String".equals(context.getType())) {
            throw new InvalidValidatorException("Cannot validate non-string field for emptiness");
        }
    }

    @Override
    public boolean isValid(final ValidatorContext context, final String value) throws ValidatorException {
        return "Html".equals(context.getName())
            ? !HtmlUtils.isEmpty(value)
            : !StringUtils.isBlank(value);
    }

}
