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

import java.util.Optional;

import org.onehippo.cms.services.validation.api.ValidationContext;
import org.onehippo.cms.services.validation.api.ValidationContextException;
import org.onehippo.cms.services.validation.api.Validator;
import org.onehippo.cms.services.validation.api.Violation;
import org.onehippo.cms.services.validation.api.internal.ValidationService;
import org.onehippo.cms.services.validation.api.internal.ValidatorInstance;
import org.onehippo.cms7.services.HippoServiceRegistry;

public class RequiredValidator implements Validator<Object> {

    private final ValidationService validationService;

    public RequiredValidator() {
        validationService = HippoServiceRegistry.getService(ValidationService.class);
    }

    @Override
    public Optional<Violation> validate(final ValidationContext context, final Object value) {
        final String fieldType = context.getType();
        final ValidatorInstance requiredValidator = validationService.getRequiredValidator(fieldType);

        if (requiredValidator == null) {
            throw new ValidationContextException("No 'required' validator found for type '" + fieldType + "'"
                    + ", cannot validate required field '" + context.getJcrName() + "'");
        }

        return requiredValidator.validate(context, value);
    }
}
