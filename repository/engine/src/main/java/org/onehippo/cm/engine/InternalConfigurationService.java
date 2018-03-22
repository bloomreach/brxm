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

package org.onehippo.cm.engine;

import javax.jcr.RepositoryException;

import org.onehippo.cm.ConfigurationService;
import org.onehippo.cm.model.ConfigurationModel;

/** INTERNAL USAGE ONLY **/
public interface InternalConfigurationService extends ConfigurationService {

    /**
     * Used for test purposes only: perform a full config apply (verifyOnly, no save)
     * @return true if no failures occurred verifying the config
     * @throws RepositoryException
     */
    boolean verifyConfigurationModel() throws RepositoryException;

    /**
     * Used for test purposes only: return baseline model
     */
    ConfigurationModel getBaselineModel();


    /**
     * Used for test purposes only: perform a single run of AutoExport
     * @throws RepositoryException
     */
    void runSingleAutoExportCycle() throws RepositoryException;
}
