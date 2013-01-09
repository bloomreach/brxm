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
package org.hippoecm.repository.jackrabbit;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.HierarchyManagerImpl;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.commons.name.CargoNamePath;
import org.apache.jackrabbit.spi.commons.name.PathBuilder;
import org.hippoecm.repository.dataprovider.ParameterizedNodeId;

public class HippoHierarchyManager implements HierarchyManager {

    protected HierarchyManager hierMgr;
    protected HippoSessionItemStateManager itemStateMgr;

    static final int RETURN_NODE = 1;
    static final int RETURN_PROPERTY = 2;
    static final int RETURN_ANY = (RETURN_NODE | RETURN_PROPERTY);

    public HippoHierarchyManager(HippoSessionItemStateManager itemStateMgr, HierarchyManager hierMgr) {
        this.hierMgr = hierMgr;
        this.itemStateMgr = itemStateMgr;
    }

    public ItemState getItemState(ItemId id) throws NoSuchItemStateException, ItemStateException {
        return itemStateMgr.getItemState(id);
    }

    public Path getPath(ItemId id) throws ItemNotFoundException, RepositoryException {
        return hierMgr.getPath(id);
    }

    public Name getName(ItemId id) throws ItemNotFoundException, RepositoryException {
        return hierMgr.getName(id);
    }

    public Name getName(NodeId id, NodeId parentId) throws ItemNotFoundException, RepositoryException {
        return hierMgr.getName(id, parentId);
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

    public boolean isShareAncestor(NodeId ancestor, NodeId descendant)
            throws ItemNotFoundException, RepositoryException {
        return hierMgr.isShareAncestor(ancestor, descendant);
    }

    public int getShareRelativeDepth(NodeId ancestorId, ItemId descendantId) throws ItemNotFoundException, RepositoryException {
        return hierMgr.getShareRelativeDepth(ancestorId, descendantId);
    }

    public NodeId resolveNodePath(Path path) throws RepositoryException {
        try {
            return (NodeId) resolvePath(path, 1, ((HierarchyManagerImpl)hierMgr).getRootNodeId(), RETURN_NODE);
        } catch (ItemStateException e) {
            throw new RepositoryException("failed to retrieve state of intermediary node", e);
        }
    }

    public PropertyId resolvePropertyPath(Path path) throws RepositoryException {
        try {
            return (PropertyId) resolvePath(path, 1, ((HierarchyManagerImpl)hierMgr).getRootNodeId(), RETURN_PROPERTY);
        } catch (ItemStateException e) {
            throw new RepositoryException("failed to retrieve state of intermediary node", e);
        }
    }

    public ItemId resolvePath(Path path) throws RepositoryException {
        try {
            return resolvePath(path, 1, ((HierarchyManagerImpl)hierMgr).getRootNodeId(), RETURN_ANY);
        } catch (ItemStateException e) {
            throw new RepositoryException("failed to retrieve state of intermediary node", e);
        }
    }

    protected ItemId resolvePath(Path path, int next,
                                 ItemId id, int typesAllowed)
            throws ItemStateException, MalformedPathException, RepositoryException {
        PathBuilder builder = new PathBuilder();
        NodeId smartNodeId = null;
        int count = 0;
        for (Path.Element element : path.getElements()) {
            if (element instanceof CargoNamePath) {
                Name name = element.getName();
                int index = element.getIndex();
                if (index == 0) {
                    index = 1;
                }
                NodeId parentId = hierMgr.resolveNodePath(builder.getPath());
                try {
                    NodeState parentState = (NodeState)getItemState(parentId);
                    ChildNodeEntry nodeEntry = parentState.getChildNodeEntry(name, index);
                    if (nodeEntry != null) {
                        smartNodeId = nodeEntry.getId();
                        smartNodeId = new ParameterizedNodeId(smartNodeId, element.getIdentifier());
                    } else
                        return null;
                } catch (NoSuchItemStateException ex) {
                    throw new ItemStateException("failed to retrieve state of intermediary node", ex);
                } catch (ItemStateException ex) {
                    throw new ItemStateException("failed to retrieve state of intermediary node", ex);
                }
                builder = new PathBuilder();
                count = 0;
            } else {
                builder.addLast(element);
                ++count;
            }
        }
        if (smartNodeId == null) {
            if(typesAllowed == RETURN_PROPERTY) {
                return hierMgr.resolvePropertyPath(path);
            } else if(typesAllowed == RETURN_NODE) {
                return hierMgr.resolveNodePath(path);
            } else {
                return hierMgr.resolvePath(path);
            }
        } else {
            ItemId itemId = smartNodeId;
            try {
                if (count > 0) {
                    Path.Element[] remainingElts = builder.getPath().getElements();
                    for (int i = 0; i < remainingElts.length; i++) {
                        Path.Element element = remainingElts[i];
                        NodeState parentState = (NodeState)getItemState(itemId);
                        Name name = element.getName();
                        int index = element.getIndex();
                        if (index == 0) {
                            index = 1;
                        }
                        int typeExpected = (i+1 < remainingElts.length ? RETURN_NODE : typesAllowed);
                        if ((typeExpected & RETURN_NODE) != 0) {
                            ChildNodeEntry nodeEntry = parentState.getChildNodeEntry(name, index);
                            if (nodeEntry != null) {
                                itemId = nodeEntry.getId();
                            } else if ((typeExpected & RETURN_PROPERTY) == 0) {
                                return null;
                            }
                        }
                        if ((typeExpected & RETURN_PROPERTY) != 0) {
                            if (parentState.hasPropertyName(name) && (index <= 1)) {
                                itemId = new PropertyId(parentState.getNodeId(), name);
                            }
                        }               
                    }
                }
                return itemId;
            } catch (NoSuchItemStateException ex) {
                throw new ItemStateException("failed to retrieve state of intermediary node", ex);
            } catch (ItemStateException ex) {
                throw new ItemStateException("failed to retrieve state of intermediary node", ex);
            }
        }
    }
}
