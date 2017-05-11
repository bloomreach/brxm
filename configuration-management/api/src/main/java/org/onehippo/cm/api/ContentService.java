/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.api;

import org.onehippo.cm.api.model.DefinitionType;
import org.onehippo.cms7.services.SingletonService;

import java.util.EnumSet;

@SingletonService
public interface ContentService {

    /**
     * Apply the whole or a part of a merged configuration model to the JCR as the new active configuration.
     * @param mergedModel the configuration model to apply
     * @param includeDefinitionTypes the set of definition types to apply -- may be a subset
     */
    void apply(final MergedModel mergedModel, final EnumSet<DefinitionType> includeDefinitionTypes) throws Exception;

}
