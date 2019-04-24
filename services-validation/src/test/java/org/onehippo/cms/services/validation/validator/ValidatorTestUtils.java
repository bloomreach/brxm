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

import org.onehippo.cms.services.validation.api.Violation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

class ValidatorTestUtils {

    private ValidatorTestUtils() {
    }

    static void assertValid(final Optional<Violation> violation) {
        assertFalse(violation.isPresent());
    }

    static void assertInvalid(final Optional<Violation> violation) {
        assertTrue(violation.isPresent());
    }

}
