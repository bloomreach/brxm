/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
 */

package org.onehippo.cm;

import org.onehippo.cm.model.ConfigurationModel;
import org.onehippo.cms7.services.SingletonService;

/**
 * Service providing access to the current runtime ConfigurationModel
 */
@SingletonService
public interface ConfigurationService {

    /**
     * Retrieve the current (partial) runtime ConfigurationModel This model will not contain
     * content definitions, which are not stored/retained in the runtime ConfigurationModel.
     * @throws Exception
     */
    ConfigurationModel getRuntimeConfigurationModel();
}
