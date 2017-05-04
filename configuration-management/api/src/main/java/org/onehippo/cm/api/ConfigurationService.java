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

package org.onehippo.cm.api;

import java.util.EnumSet;

import org.onehippo.cm.api.model.DefinitionType;
import org.onehippo.cms7.services.SingletonService;

@SingletonService
public interface ConfigurationService {

    /**
     * Apply the whole or a part of a merged configuration model to the JCR as the new active configuration.
     * @param mergedModel the configuration model to apply
     * @param includeDefinitionTypes the set of definition types to apply -- may be a subset
     */
    void apply(final MergedModel mergedModel, final EnumSet<DefinitionType> includeDefinitionTypes) throws Exception;

    /**
     * Store a merged configuration model as a baseline configuration in the JCR.
     * The provided MergedModel is assumed to be fully formed and validated.
     * @param model the configuration model to store as the new baseline
     */
    void storeBaseline(final MergedModel model) throws Exception;

    /**
     * Load a (partial) MergedModel from the stored configuration baseline in the JCR. This model will not contain
     * content definitions, which are not stored in the baseline.
     * @throws Exception
     */
    MergedModel loadBaseline() throws Exception;

    /**
     * Compare a MergedModel against the baseline by comparing manifests produced by model.compileManifest()
     * @param model the model to compare against the baseline
     * @return true iff the manifest compiled by model matches the manifest of the stored configuration baseline
     */
    boolean matchesBaselineManifest(MergedModel model) throws Exception;

}
