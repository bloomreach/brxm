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
 * Service providing access to the current repository ConfigurationModel (baseline) and allowing applying and
 * new/updated ConfigurationModel at runtime.
 */
@SingletonService
public interface ConfigurationService {

    /**
     * Apply the whole or a part of a merged configuration model to the JCR as the new active configuration.
     * @param model the configuration model to apply
     */
    void apply(final ConfigurationModel model) throws Exception;

    /**
     * Load a (partial) ConfigurationModel from the stored configuration baseline in the JCR. This model will not contain
     * content definitions, which are not stored in the baseline.
     * @throws Exception
     */
    ConfigurationModel loadBaseline() throws Exception;

    /**
     * Compare a ConfigurationModel against the baseline by comparing manifests produced by model.getDigest()
     */
    boolean matchesBaselineManifest(ConfigurationModel model) throws Exception;
}
