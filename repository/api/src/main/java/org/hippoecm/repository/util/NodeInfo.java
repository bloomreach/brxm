/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;

public final class NodeInfo {

    private final String name;

    private final String nodeTypeName;

    private final String[] mixinNames;

    private final int index;

    public NodeInfo(String name, int index, String nodeTypeName, String[] mixinNames) {
        this.name = name;
        this.nodeTypeName = nodeTypeName;
        this.mixinNames = mixinNames;
        this.index = index;
    }

    public NodeInfo(String name, int index, NodeType nodeType, NodeType[] mixins) {
        this.name = name;
        this.nodeTypeName = nodeType.getName();
        this.mixinNames = new String[mixins.length];
        for (int i = 0; i < mixins.length; i++) {
            mixinNames[i] = mixins[i].getName();
        }
        this.index = index;
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

    public String getNodeTypeName() {
        return nodeTypeName;
    }

    public String[] getMixinNames() {
        return mixinNames;
    }

    public NodeDefinition getApplicableChildNodeDef(NodeType[] nodeTypes)
            throws ConstraintViolationException {
        NodeDefinition residualDefinition = null;
        for (NodeType nodeType : nodeTypes) {
            NodeDefinition[] nodeDefs = nodeType.getChildNodeDefinitions();
            for (NodeDefinition nodeDef : nodeDefs) {
                if (nodeDef.getName().equals(getName())) {
                    return nodeDef;
                } else if ("*".equals(nodeDef.getName())) {
                    residualDefinition = nodeDef;
                }
            }
        }
        if (residualDefinition != null) {
            return residualDefinition;
        }
        throw new ConstraintViolationException("Cannot set property " + this.getName());
    }

    @Override
    public String toString() {
        return "NodeInfo[" + getName() + '[' + getIndex() + "](type=" + getNodeTypeName() + ", mixins=" + Arrays.toString(getMixinNames()) +")]";
    }
}