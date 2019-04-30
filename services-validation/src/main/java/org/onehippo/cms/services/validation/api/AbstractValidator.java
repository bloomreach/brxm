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
package org.onehippo.cms.services.validation.api;

import java.util.Optional;

/**
 * Checks whether a value adheres to certain constraints.
 */
public interface AbstractValidator<C, V> {

    /**
     * Validates that a value adheres to certain constraints.
     *
     * @param context the context in which the value is validated
     * @param value the value to validate
     *
     * @return a violation that explains what to do with the invalid value,
     * or an empty {@link Optional} when the value is valid.
     *
     * @throws ValidationContextException when this validator is used in a context that does not make sense.
     */
    Optional<Violation> validate(C context, V value) throws ValidationContextException;

}
