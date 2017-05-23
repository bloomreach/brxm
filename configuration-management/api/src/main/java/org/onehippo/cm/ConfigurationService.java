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

import java.util.List;

import org.onehippo.cm.model.ConfigurationModel;
import org.onehippo.cms7.services.SingletonService;
import org.onehippo.repository.bootstrap.PostStartupTask;

/**
 * Operations related to repository configuration, including those related to bootstrap and the config baseline.
 */
@SingletonService
public interface ConfigurationService {

    /**
     * Perform initial repository configuration, including first-start bootstrap or configuration updates as necessary.
     */
    void configureRepository();

    /**
     * Setup tasks for the post-startup bootstrap phase.
     * @return a List of PostStartupTasks to be executed after normal repository startup has completed
     */
    List<PostStartupTask> contentBootstrap();

    /**
     * Apply the whole or a part of a merged configuration model to the JCR as the new active configuration.
     * @param model the configuration model to apply
     */
    void apply(final ConfigurationModel model) throws Exception;

    /**
     * Store a merged configuration model as a baseline configuration in the JCR.
     * The provided ConfigurationModel is assumed to be fully formed and validated.
     * @param model the configuration model to store as the new baseline
     */
    void storeBaseline(final ConfigurationModel model) throws Exception;

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
