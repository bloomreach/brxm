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

/**
 * An instance of a {@link Validator}.
 *
 * Provides access to the configuration of a validator and to translated violations.
 */
public interface ValidatorInstance extends AbstractValidator<FieldContext, Object>, ValidationContext {

    /**
     * @return the configuration of this validator instance.
     */
    ValidatorConfig getConfig();

}
