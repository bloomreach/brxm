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
package org.hippoecm.frontend.model;

import java.io.Serializable;

/**
 * A JcrNodeModelState is used to indicate the state of a JcrNodeModel object.
 */
public class JcrNodeModelState implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int UNCHANGED = 0;
    public static final int NEW = 1;
    public static final int CHILD_ADDED = 2;
    public static final int CHILD_REMOVED = 3;
    public static final int MOVED = 4;
    public static final int PROPERTY_UPDATED = 5;
    public static final int DELETED = 6;

    private boolean changed = true;
    private boolean newNode = false;
    private boolean childAdded = false;
    private boolean childRemoved = false;
    private boolean moved = false;
    private boolean propertyUpdated = false;
    private boolean deleted = false;
    
    private JcrNodeModel relatedNode;
    
    public JcrNodeModelState(int state) {
        this.relatedNode = null;
        mark(state);
    }

    public JcrNodeModelState(int state, JcrNodeModel relatedNode) {
        this.relatedNode = relatedNode;
        mark(state);
    }

    public void mark(int state) {
        if (state == UNCHANGED) {
            this.changed = false;
            this.newNode = false;
            this.childAdded = false;
            this.childRemoved = false;
            this.moved = false;
            this.propertyUpdated = false;
            this.deleted = false;
        }
        else if (state == NEW) {
            this.changed = true;
            this.newNode = true;
        }
        else if (state == CHILD_ADDED) {
            this.changed = true;
            this.childAdded = true;
        }
        else if (state == CHILD_REMOVED) {
            this.changed = true;
            this.childRemoved = true;
        }
        else if (state == MOVED) {
            this.changed = true;
            this.moved = true;
        }
        else if (state == PROPERTY_UPDATED) {
            this.changed = true;
            this.propertyUpdated = true;
        }
        else if (state == DELETED) {
            this.changed = true;
            this.deleted = true;
        }
    }
    
    public JcrNodeModel getRelatedNode() {
        return relatedNode;
    }
    
    public void setRelatedNode(JcrNodeModel node) {
        this.relatedNode = node;
    }

    public boolean isChanged() {
        return changed;
    }

    public boolean isChildAdded() {
        return childAdded;
    }

    public boolean isChildRemoved() {
        return childRemoved;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public boolean isMoved() {
        return moved;
    }

    public boolean isNewNode() {
        return newNode;
    }

    public boolean isPropertyUpdated() {
        return propertyUpdated;
    }
    
    public String toString() {
        return "[changed:" + isChanged()
                + ", child added:" + isChildAdded()
                + ", child removed:" + isChildRemoved()
                + ", deleted:" + isDeleted()
                + ", moved:" + isMoved()
                + ", property changed:" + isPropertyUpdated()
                + "]";
                
    }

}
