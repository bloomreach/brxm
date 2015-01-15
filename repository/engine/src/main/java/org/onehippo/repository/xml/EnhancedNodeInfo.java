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
package org.onehippo.repository.xml;

import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.xml.NodeInfo;
import org.apache.jackrabbit.spi.Name;

import static org.onehippo.repository.xml.EnhancedSystemViewConstants.COMBINE;
import static org.onehippo.repository.xml.EnhancedSystemViewConstants.INSERT;
import static org.onehippo.repository.xml.EnhancedSystemViewConstants.OVERLAY;
import static org.onehippo.repository.xml.EnhancedSystemViewConstants.SKIP;

class EnhancedNodeInfo extends NodeInfo {

    private String mergeBehavior;
    private NodeImpl originItem;
    private String location;
    private int index;

    EnhancedNodeInfo(Name name, Name nodeTypeName, Name[] mixinNames,
                            NodeId id, String mergeBehavior, String location, int index) {
        super(name, nodeTypeName, mixinNames, id);
        this.mergeBehavior = mergeBehavior;
        this.location = location;
        this.index = index;
    }

    boolean mergeSkip() {
        return SKIP.equalsIgnoreCase(mergeBehavior);
    }

    boolean mergeOverlay() {
        return OVERLAY.equalsIgnoreCase(mergeBehavior);
    }

    boolean mergeCombine() {
        return COMBINE.equalsIgnoreCase(mergeBehavior);
    }

    String mergeInsertBefore() {
        if (INSERT.equalsIgnoreCase(mergeBehavior)) {
            return (location != null ? location : "");
        } else {
            return null;
        }
    }

    NodeImpl getOrigin() {
        return originItem;
    }

    void setOrigin(NodeImpl originItem) {
        this.originItem = originItem;
    }

    int getIndex() {
        return index;
    }
}
