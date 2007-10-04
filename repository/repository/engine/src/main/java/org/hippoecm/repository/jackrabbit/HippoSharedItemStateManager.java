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

import org.apache.jackrabbit.core.state.SharedItemStateManager;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.core.state.ItemStateCacheFactory;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.SharedItemStateManager;

public class HippoSharedItemStateManager extends SharedItemStateManager {
    public RepositoryImpl repository;

    public HippoSharedItemStateManager(RepositoryImpl repository, PersistenceManager persistMgr, NodeId rootNodeId,
            NodeTypeRegistry ntReg, boolean usesReferences, ItemStateCacheFactory cacheFactory)
            throws ItemStateException {
        super(persistMgr, rootNodeId, ntReg, usesReferences, cacheFactory);
        this.repository = repository;
    }
}
