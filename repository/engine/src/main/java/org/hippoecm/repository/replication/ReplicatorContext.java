/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.replication;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;

/**
 * The ReplicatorContext provides a context for {@link Replicator}s and {@link ReplicatorNode}s.
 */
public class ReplicatorContext {

    /**
     * The replicator journal
     */
    private final ReplicationJournal journal;

    /**
     * NamePathResolver.
     */
    private final NamePathResolver npRes;

    /**
     * Node type registry.
     */
    private final NodeTypeRegistry ntReg;

    /**
     * Shared item state provider.
     */
    private final ItemStateManager itemMgr;

    /**
     * Hierarchy manager.
     */
    private final HierarchyManager hierMgr;

    /**
     * Create an immutable context for a <code>ReplicatorNode</code> and a {@link Replicator}.
     * @param journal
     * @param itemMgr
     * @param hierMgr
     * @param nsRes
     */
    public ReplicatorContext(ReplicationJournal journal, ItemStateManager itemMgr, NodeTypeRegistry ntReg,
            HierarchyManager hierMgr, NamePathResolver npRes) {
        this.journal = journal;
        this.itemMgr = itemMgr;
        this.ntReg = ntReg;
        this.hierMgr = hierMgr;
        this.npRes = npRes;
    }

    /**
     * Return the journal
     *
     * @return journal
     */
    public ReplicationJournal getJournal() {
        return journal;
    }

    /**
     * Get the replication home directory which is used for storing the
     * replication node revision versions and the journal files.
     * @return the replication home directory
     */
    public String getReplicationHomeDir() {
        return journal.getDirectory();
    }

    /**
     * Get the shared item state manager.
     * @return the item state manager
     */
    public ItemStateManager getItemStateManager() {
        return itemMgr;
    }

    /**
     * Get the node type registry.
     * @return the node type registry
     */
    public NodeTypeRegistry getNodeTypeRegistry() {
        return ntReg;
    }

    /**
     * Get the hierarchy manager.
     * @return the hierarchy manager
     */
    public HierarchyManager getHierarchyManager() {
        return hierMgr;
    }

    /**
     * Return a namespace resolver to map prefixes to URIs and vice-versa.
     * @return namespace resolver
     */
    public NamePathResolver getNamePathResolver() {
        return npRes;
    }

}
