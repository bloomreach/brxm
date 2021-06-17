/*
 * Copyright 2021 Hippo B.V. (http://www.onehippo.com)
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

import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.services.validation.api.ValidationContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

@RunWith(EasyMockRunner.class)
public class OptionalValidatorTest {

    // System Under Test
    private OptionalValidator optionalValidator;

    @Mock
    private ValidationContext validationContext;

    @Before
    public void initializeSystemUnderTest() {
        optionalValidator = new OptionalValidator();
    }

    @Test
    public void context_is_not_required() {
        assertEquals(Optional.empty(), optionalValidator.validate(null, null));
        assertEquals(Optional.empty(), optionalValidator.validate(null, "foo"));
    }

    @Test
    public void null_value_is_valid() {
        assertEquals(Optional.empty(), optionalValidator.validate(validationContext, null));
    }

    @Test
    public void non_null_value_is_valid() {
        assertEquals(Optional.empty(), optionalValidator.validate(validationContext, "foo"));
    }
}