/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.api;

import java.util.Map;

/**
 * Map representation of a JCR Node.
 * <p>
 * Properties and sub-nodes of the node representing the map are available via the #get() method.
 * This method returns three types of values depending on whether the requested object is a node,
 * a single-valued property, or a multi-valued property: a {@code RepositoryMap}, a primitive {@code Object}
 * representing the value of the property (a {@code String}, {@code Boolean}, {@code Calendar}, {@code Long},
 * or a {@code RepositoryMap} in case of a reference property), or an {@code Object[]} containing such values.
 * </p>
 * <p>
 * In the case of properties and sub-nodes with the same name, properties have precedence.
 * In the case of same name sibling nodes, only the first node is returned in the form a {@code RepositoryMap}.
 * </p>
 */
public interface RepositoryMap extends Map {

    public boolean exists();

}
