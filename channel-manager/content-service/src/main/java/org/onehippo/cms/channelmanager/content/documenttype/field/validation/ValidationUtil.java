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

package org.onehippo.cms.channelmanager.content.documenttype.field.validation;

import java.util.Optional;

import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.services.validation.api.FieldContext;
import org.onehippo.cms.services.validation.api.ValidatorInstance;
import org.onehippo.cms.services.validation.api.Violation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidationUtil {

    private static final Logger log = LoggerFactory.getLogger(ValidationUtil.class);

    private ValidationUtil() {
    }

    /**
     * Validates the value of a field with a validator.
     *
     * @param value the field value wrapper
     * @param validatorName the name of the validator to use
     *
     * @return whether the validator deemed the value valid
     */
    public static boolean validateValue(final FieldValue value,
                                         final FieldContext context,
                                         final String validatorName,
                                         final Object validatedValue) {
        final ValidatorInstance validator = FieldTypeUtils.getValidator(validatorName);
        if (validator == null) {
            log.warn("Failed to find validator '{}', ignoring it", validatorName);
            return true;
        }

        final Optional<Violation> violation = validator.validate(context, validatedValue);

        violation.ifPresent((error) -> {
            ValidationErrorInfo errorInfo = new ValidationErrorInfo(validatorName, error.getMessage());
            value.setErrorInfo(errorInfo);
        });

        return !violation.isPresent();
    }
}
