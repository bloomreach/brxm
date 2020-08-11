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

import java.util.Date;
import java.util.Optional;

import org.apache.commons.lang.time.DateUtils;
import org.onehippo.cms.services.validation.api.ValidationContext;
import org.onehippo.cms.services.validation.api.Validator;
import org.onehippo.cms.services.validation.api.Violation;
import org.onehippo.repository.util.DateConstants;

/**
 * Validates that a Date value is not null and not the default {@link DateConstants#EMPTY_DATE}.
 */
public class RequiredDateValidator implements Validator<Date> {

    @Override
    public Optional<Violation> validate(final ValidationContext context, final Date date) {
        if (date == null || DateUtils.isSameInstant(date, DateConstants.EMPTY_DATE)) {
            return Optional.of(context.createViolation());
        }
        return Optional.empty();
    }
}
