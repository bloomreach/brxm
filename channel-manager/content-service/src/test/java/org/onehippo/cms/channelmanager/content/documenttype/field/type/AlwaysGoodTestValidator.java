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
package org.onehippo.cms.channelmanager.content.documenttype.field.type;

import java.util.Optional;

import org.onehippo.cms.services.validation.api.ValidationContext;
import org.onehippo.cms.services.validation.api.ValidationContextException;
import org.onehippo.cms.services.validation.api.ValidatorConfig;
import org.onehippo.cms.services.validation.api.ValidatorInstance;
import org.onehippo.cms.services.validation.api.Violation;

public class AlwaysGoodTestValidator implements ValidatorInstance {

    @Override
    public ValidatorConfig getConfig() {
        return null;
    }

    @Override
    public Optional<Violation> validate(final ValidationContext context, final Object value) throws ValidationContextException {
        // everything is OK
        return Optional.empty();
    }

    @Override
    public Violation createViolation() {
        return null;
    }

    @Override
    public Violation createViolation(final String subKey) {
        return null;
    }
}
