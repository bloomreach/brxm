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

package org.onehippo.cms.services.validation;

import org.onehippo.cms.services.validation.api.Validator;
import org.onehippo.cms.services.validation.api.internal.ValidatorConfig;
import org.onehippo.cms.services.validation.api.internal.ValidatorInstance;

class ValidatorInstanceFactory {

    private ValidatorInstanceFactory() {
    }

    static ValidatorInstance createValidatorInstance(final ValidatorConfig config) {
        final Validator validator = ValidatorFactory.createValidator(config);

        if (validator == null) {
            return null;
        }

        return new ValidatorInstanceImpl(validator, config);
    }
}
