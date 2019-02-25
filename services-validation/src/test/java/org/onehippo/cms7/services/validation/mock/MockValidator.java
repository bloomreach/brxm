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
package org.onehippo.cms7.services.validation.mock;

import java.util.Optional;

import org.onehippo.cms7.services.validation.Validator;
import org.onehippo.cms7.services.validation.ValidatorConfig;
import org.onehippo.cms7.services.validation.ValidatorContext;
import org.onehippo.cms7.services.validation.Violation;
import org.onehippo.cms7.services.validation.exception.InvalidValidatorException;

public class MockValidator implements Validator {

    public MockValidator(final ValidatorConfig config) {
    }

    @Override
    public String getName() {
        return "mock-validator";
    }

    @Override
    public void init(final ValidatorContext context) throws InvalidValidatorException {
    }

    @Override
    public Optional<Violation> validate(final ValidatorContext context, final String value) {
        return Optional.empty();
    }

}
