/*
 * Copyright 2019-2023 Bloomreach
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

package org.onehippo.cms.services.validation.api.internal;

import javax.jcr.Node;

/**
 * Wrapper around the configuration of a validator.
 */
public interface ValidatorConfig {

    /**
     * @return the name of a validator
     */
    String getName();

    /**
     * @return the fully qualified Java class name of a validator.
     */
    String getClassName();

    /**
     * @return the configuration node of a validator.
     */
    Node getNode();
}
