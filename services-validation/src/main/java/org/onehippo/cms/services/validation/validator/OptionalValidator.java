/*
 * Copyright 2021-2023 Bloomreach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.onehippo.cms.services.validation.validator;

import java.util.Optional;

import org.onehippo.cms.services.validation.api.ValidationContext;
import org.onehippo.cms.services.validation.api.Validator;
import org.onehippo.cms.services.validation.api.Violation;

/**
 * Represents a validator for optional values.
 * At the moment it does not validate anything.
 */
public class OptionalValidator implements Validator<Object> {

    @Override
    public Optional<Violation> validate(final ValidationContext context, final Object value) {
        // TODO (meggermont): what to do if value is not null?
        return Optional.empty();
    }
}
