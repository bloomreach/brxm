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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Repository;
import org.apache.jackrabbit.core.id.NodeId;

import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;

/**
 *  The {@link StateCollection} class is used to collect all {@link ItemState} changes in a {@link ChangeLog} with the 
 *  same {@link NodeId} of {@link NodeState}s or the parent {@link NodeId} of {@link PropertyState}s. This helps in 
 *  applying all the changes to a node in a remote {@link Repository} at once.
 */
public class StateCollection {

    /** The node and the properties are newly added */
    public static final int ADD_NODE = 0;

    /** The node exists, but the properties have changed */
    public static final int MODIFY_NODE = 1;

    /** The node and all it's properties and children are deleted */
    public static final int DELETE_NODE = 2;

    /**
     * Set the default node operation type to {@link #MODIFY_NODE}.
     */
    private int nodeOperation = MODIFY_NODE;

    /**
     * The {@link NodeId} to which the changes apply.
     */
    private final NodeId id;

    /**
     * The newly added states for the node.
     */
    private List<ItemState> addedStates = new ArrayList<ItemState>();

    /**
     * The modified states for the node.
     */
    private List<ItemState> modifiedStates = new ArrayList<ItemState>();

    /**
     * The removed states for the node.
     */
    private List<ItemState> deletedStates = new ArrayList<ItemState>();

    /**
     * Create a new {@link StateCollection} for a node with the specified {@link NodeId}.
     * 
     * @param id the {@link NodeId}.
     */
    public StateCollection(NodeId id) {
        this.id = id;
    }

    /**
     * Get {@link NodeId} of the node on which the changes are made.
     * 
     * @return the {@link NodeId}
     */
    public NodeId getId() {
        return id;
    }

    /**
     * Get type of operation that is performed on the node.
     * 
     * @see StateCollection#ADD_NODE
     * @see StateCollection#MODIFY_NODE
     * @see StateCollection#DELETE_NODE
     * @return the type of node operation
     */
    public int getNodeOperation() {
        return nodeOperation;
    }

    /**
     * Set type of operation that is performed on the node. 
     * 
     * @param nodeOperation This must be one of the following:
     * <ul>
     * <li> {@link StateCollection#ADD_NODE}
     * <li> {@link StateCollection#MODIFY_NODE}
     * <li> {@link StateCollection#DELETE_NODE}
     * </ul>
     */
    public void setNodeOperation(int nodeOperation) {
        this.nodeOperation = nodeOperation;
    }

    /**
     * Add an added {@link ItemState}.
     * 
     * @param state the new {@link ItemState}
     */
    void addAddedState(ItemState state) {
        addedStates.add(state);
    }

    /**
     * Add a modified {@link ItemState}
     * 
     * @param state the modified {@link ItemState}
     */
    void addModifiedState(ItemState state) {
        modifiedStates.add(state);
    }

    /**
     * Add a deleted {@link ItemState}
     * 
     * @param state the deleted {@link ItemState}
     */
    void addDeletedState(ItemState state) {
        deletedStates.add(state);
    }

    /**
     * Return an iterator over all added states.
     *
     * @return iterator over all added states.
     */
    Iterator<ItemState> addedStates() {
        return addedStates.iterator();
    }

    /**
     * Return an iterator over all modified states.
     *
     * @return iterator over all modified states.
     */
    Iterator<ItemState> modifiedStates() {
        return modifiedStates.iterator();
    }

    /**
     * Return an iterator over all deleted states.
     *
     * @return iterator over all deleted states.
     */
    Iterator<ItemState> deletedStates() {
        return deletedStates.iterator();
    }

    void addState(ItemState state, int operation) {
        if (state.isNode()) {
            setNodeOperation(operation);
        } else {
            if (operation == StateCollection.ADD_NODE) {
                addAddedState(state);
            }
            if (operation == StateCollection.MODIFY_NODE) {
                addModifiedState(state);
            }
            if (operation == StateCollection.DELETE_NODE) {
                addDeletedState(state);
            }
        }
    }
}
