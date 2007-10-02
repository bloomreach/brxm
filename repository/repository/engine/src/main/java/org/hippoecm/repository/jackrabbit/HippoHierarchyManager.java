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
package org.hippoecm.repository.jackrabbit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.CachingHierarchyManager;
import org.apache.jackrabbit.name.PathResolver;

public class HippoHierarchyManager extends CachingHierarchyManager {
    private static Logger log = LoggerFactory.getLogger(HippoHierarchyManager.class);

    /**
     * Create a new instance of this class.
     *
     * @param rootNodeId   root node id
     * @param provider     item state manager
     * @param resolver   namespace resolver
     */
    public HippoHierarchyManager(NodeId rootNodeId, ItemStateManager provider, PathResolver resolver) {
        super(rootNodeId, provider, resolver);
    }
}
