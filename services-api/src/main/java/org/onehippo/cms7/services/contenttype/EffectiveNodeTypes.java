/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.services.contenttype;

import java.util.Map;
import java.util.Set;

/**
 * An lightweight and immutable representation of the aggregated or effective JCR Repository NodeType definitions.
 * If the model is still 'current' its version property can be compared against the current version provided by the {@link ContentTypeService}.
 */
public interface EffectiveNodeTypes {

    /**
     * @return The immutable instance version which is automatically incremented for every new (changed) EffectiveNodeTypes instance created by the ContentTypeService
     */
    long version();

    /**
     * @param name Qualified Name for a registered NodeType (see JCR-2.0 3.2.5.2)
     * @return The immutable and aggregated or effective NodeType definition representation
     */
    EffectiveNodeType getType(String name);

    /**
     * @return The immutable map of EffectiveNodeTypes grouped by their namespace prefix as key
     */
    Map<String, Set<EffectiveNodeType>> getTypesByPrefix();
}
