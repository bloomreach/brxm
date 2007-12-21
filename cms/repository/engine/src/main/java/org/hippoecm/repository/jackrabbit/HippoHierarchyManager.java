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

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HippoHierarchyManager implements HierarchyManager {
    private static Logger log = LoggerFactory.getLogger(HippoHierarchyManager.class);
    
    protected HierarchyManager hierMgr;
    protected HippoSessionItemStateManager itemStateMgr;

    public HippoHierarchyManager(HippoSessionItemStateManager itemStateMgr, HierarchyManager hierMgr) {
        this.hierMgr = hierMgr;
        this.itemStateMgr = itemStateMgr;
    }

    public ItemState getItemState(ItemId id) throws NoSuchItemStateException, ItemStateException {
        return itemStateMgr.getItemState(id);
    }

    public ItemId resolvePath(Path path) throws RepositoryException {
        return hierMgr.resolvePath(path);
    }

    public Path getPath(ItemId id) throws ItemNotFoundException, RepositoryException {
        return hierMgr.getPath(id);
    }

    public Name getName(ItemId id) throws ItemNotFoundException, RepositoryException {
        return hierMgr.getName(id);
    }

    public int getDepth(ItemId id) throws ItemNotFoundException, RepositoryException {
        return hierMgr.getDepth(id);
    }

    public int getRelativeDepth(NodeId ancestorId, ItemId descendantId) throws ItemNotFoundException, RepositoryException {
        return hierMgr.getRelativeDepth(ancestorId, descendantId);
    }

    public boolean isAncestor(NodeId nodeId, ItemId itemId) throws ItemNotFoundException, RepositoryException {
        return hierMgr.isAncestor(nodeId, itemId);        
    }
}
