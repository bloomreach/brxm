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

import java.util.Map;

import org.onehippo.cms.services.validation.api.Violation;
import org.onehippo.cms.services.validation.api.ViolationFactory;

public class TestViolationFactory implements ViolationFactory {

    private boolean called = false;

    public boolean isCalled() {
        return called;
    }

    @Override
    public Violation createViolation() {
        called = true;
        return new TestViolation();
    }

    @Override
    public Violation createViolation(final Map<String, String> parameters) {
        return null;
    }

    @Override
    public Violation createViolation(final String subKey) {
        return new TestViolation();
    }

    @Override
    public Violation createViolation(final String subKey, final Map<String, String> parameters) {
        return null;
    }
}
