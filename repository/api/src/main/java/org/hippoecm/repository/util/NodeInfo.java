/*
 *  Copyright 2012-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.util;

import java.util.Arrays;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;

public final class NodeInfo {

    private final String name;
    private final NodeType nodeType;
    private final NodeType[] mixinTypes;
    private final int index;

    public NodeInfo(String name, int index, NodeType nodeType, NodeType[] mixinTypes) {
        this.name = name;
        this.nodeType = nodeType;
        this.index = index;
        this.mixinTypes = mixinTypes;
    }

    public NodeInfo(Node child) throws RepositoryException {
        this(child.getName(), child.getIndex(), JcrUtils.getPrimaryNodeType(child), JcrUtils.getMixinNodeTypes(child));
    }

    public String getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }

    public NodeType getNodeType() { return nodeType; }

    public String getNodeTypeName() {
        return nodeType.getName();
    }

    public NodeType[] getMixinTypes() {
        return mixinTypes;
    }

    public String[] getMixinNames() {
        final String[] mixinNames = new String[mixinTypes.length];
        for (int i = 0; i < mixinTypes.length; i++) {
            mixinNames[i] = mixinTypes[i].getName();
        }
        return mixinNames;
    }

    public NodeDefinition getApplicableChildNodeDef(NodeType[] parentTypes) {
        NodeDefinition residualDefinition = null;
        for (NodeType parentType : parentTypes) {
            for (NodeDefinition nodeDef : parentType.getChildNodeDefinitions()) {
                if (nodeDef.getName().equals(getName())) {
                    if (!hasRequiredPrimaryNodeType(nodeDef)) {
                        continue;
                    }
                    return nodeDef;
                } else if ("*".equals(nodeDef.getName())) {
                    if (!hasRequiredPrimaryNodeType(nodeDef)) {
                        continue;
                    }
                    residualDefinition = nodeDef;
                }
            }
        }
        if (residualDefinition != null) {
            return residualDefinition;
        }
        return null;
    }

    public boolean hasApplicableChildNodeDef(NodeType[] parentTypes) {
        return getApplicableChildNodeDef(parentTypes) != null;
    }

    private boolean hasRequiredPrimaryNodeType(final NodeDefinition definition) {
        for (String primaryNodeTypeName : definition.getRequiredPrimaryTypeNames()) {
            if (!nodeType.isNodeType(primaryNodeTypeName)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "NodeInfo[" + getName() + '[' + getIndex() + "](type=" + getNodeTypeName() + ", mixins=" + Arrays.toString(getMixinNames()) +")]";
    }
}