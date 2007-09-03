/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.lucene;

import org.apache.jackrabbit.core.query.lucene.IndexingConfiguration;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.name.QName;

public interface ServicingIndexingConfiguration extends IndexingConfiguration{
   
    /**
     * Returns <code>true</code> if the property with the given name is a facet
     * according to this configuration.
     *
     * @param state        the node state.
     * @param propertyName the name of a property.
     * @return <code>true</code> if the property is indexed; <code>false</code>
     *         otherwise.
     */
    boolean isFacet(NodeState state, QName propertyName);
}
