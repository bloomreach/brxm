/*
 *  Copyright 2008 Hippo.
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
import org.apache.jackrabbit.spi.commons.name.PathBuilder;
import org.hippoecm.repository.dataprovider.ParameterizedNodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HippoHierarchyManager implements HierarchyManager {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

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

    public PropertyId resolvePropertyPath(Path path) throws RepositoryException {
        return hierMgr.resolvePropertyPath(path);
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
        Path.Element[] elements = path.getElements();
        PathBuilder builder = new PathBuilder();
        NodeId smartNodeId = null;
        int count = 0;
        for (Path.Element element : elements) {
            if (element instanceof HippoPathParser.SmartElement) {
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
                        smartNodeId = new ParameterizedNodeId(smartNodeId, ((HippoPathParser.SmartElement)element).argument);
                    } else
                        return null;
                } catch (NoSuchItemStateException ex) {
                    throw new RepositoryException("failed to retrieve state of intermediary node", ex);
                } catch (ItemStateException ex) {
                    throw new RepositoryException("failed to retrieve state of intermediary node", ex);
                }
                builder = new PathBuilder();
                count = 0;
            } else {
                builder.addLast(element);
                ++count;
            }
        }
        if (smartNodeId == null) {
            return hierMgr.resolveNodePath(path);
        } else {
            NodeId id = smartNodeId;
            try {
                if (count > 0) {
                    for (Path.Element element : builder.getPath().getElements()) {
                        NodeState parentState = (NodeState)getItemState(id);
                        Name name = element.getName();
                        int index = element.getIndex();
                        if (index == 0) {
                            index = 1;
                        }
                        ChildNodeEntry nodeEntry = parentState.getChildNodeEntry(name, index);
                        if (nodeEntry != null) {
                            id = nodeEntry.getId();
                        } else {
                            return null;
                        }
                    }
                }
                return id;
            } catch (NoSuchItemStateException ex) {
                throw new RepositoryException("failed to retrieve state of intermediary node", ex);
            } catch (ItemStateException ex) {
                throw new RepositoryException("failed to retrieve state of intermediary node", ex);
            }
        }
    }
}
