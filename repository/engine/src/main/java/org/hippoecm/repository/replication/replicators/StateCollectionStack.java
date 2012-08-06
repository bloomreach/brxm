/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.repository.replication.replicators;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.state.ChangeLog;

/**
 * A kind of stack in which all the {@link StateCollection}s are collected to represent the
 * complete set of changes in the {@link ChangeLog}.
 * <p>
 * Be careful: this class is NOT THREAD SAFE by design.
 *
 */
public class StateCollectionStack {

    /**
     * A map with all the {@link StateCollection}s. Used as a kind of stack.
     */
    private Map<NodeId, StateCollection> collections = new LinkedHashMap<NodeId, StateCollection>();

    /**
     * Clear the complete "stack".
     */
    public void clear() {
        collections.clear();
    }

    /**
     * Check if the {@link StateCollectionStack} contains one or more {@link StateCollection}s.
     * 
     * @return true if the {@link StateCollectionStack} contains on or more {@link StateCollection}s else false
     */
    public boolean hasStateCollections() {
        return collections.size() > 0;
    }

    /**
     * Check if the specified {@link NodeId} is in the {@link StateCollectionStack}.
     * 
     * @param id the {@link NodeId}
     * @return true if the {@link NodeId} is found else false
     */
    public boolean hasStateCollection(NodeId id) {
        return collections.containsKey(id);
    }

    /**
     * Get the {@link StateCollectionStack} from the stack without removing it.
     * 
     * @param id the {@link NodeId}
     * @return the {@link StateCollection}
     */
    public StateCollection peek(NodeId id) {
        return collections.get(id);
    }

    /**
     * Remove the {@link NodeId} from the {@link StateCollectionStack}.
     * 
     * @param id the {@link NodeId}
     */
    public void removeStateCollection(NodeId id) {
        collections.remove(id);
    }

    /**
     * Push the {@link StateCollection} on the stack.
     * 
     * @param collection the {@link StateCollection}
     */
    public void pushStateCollection(StateCollection collection) {
        collections.put(collection.getId(), collection);
    }

    /**
     * Pop a {@link StateCollection} from the stack.
     * 
     * @return a {@link StateCollection} or null when the {@link StateCollectionStack} is empty
     */
    public StateCollection popStateCollection() {
        if (!hasStateCollections()) {
            return null;
        }
        StateCollection collection = collections.values().iterator().next();
        removeStateCollection(collection.getId());
        return collection;
    }
}
