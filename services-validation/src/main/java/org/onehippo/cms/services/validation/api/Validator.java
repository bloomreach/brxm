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
package org.onehippo.cms.services.validation.api;

import java.util.Map;

/**
 * Checks whether a value adheres to certain constraints.
 *
 * Implementations should either provide a public no-arguments constructor, or a public constructor that accepts
 * a single argument of type {@link Map <String, String>}. The latter will get all custom configuration properties
 * as key-value pairs.
 *
 * The system reuses a single instance of each validator, so implementations must be thread-safe.
 */
public interface Validator<V> extends AbstractValidator<ValidationContext, V> {
}
