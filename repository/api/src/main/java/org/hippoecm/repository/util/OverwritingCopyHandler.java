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

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

/**
 * CopyHandler that overwrites the first started node.
 */
public class OverwritingCopyHandler extends DefaultCopyHandler {

    private int depth;

    public OverwritingCopyHandler(final Node destNode) throws RepositoryException {
        super(destNode);
    }

    @Override
    public void startNode(final NodeInfo nodeInfo) throws RepositoryException {
        if (depth == 0) {
            Node node = getCurrent();

            removeProperties(node);
            removeChildNodes(node);

            setPrimaryType(node, nodeInfo);
            replaceMixins(node, nodeInfo);

            // set the node again, so that super class can update it's cached values
            setCurrent(node);
        } else {
            super.startNode(nodeInfo);
        }
        depth++;
    }

    protected void removeProperties(final Node node) throws RepositoryException {
        for (Property property : new PropertyIterable(node.getProperties())) {
            if (!property.getDefinition().isProtected()) {
                property.remove();
            }
        }
    }

    protected void removeChildNodes(final Node node) throws RepositoryException {
        for (Node child : new NodeIterable(node.getNodes())) {
            if (!child.getDefinition().isProtected()) {
                child.remove();
            }
        }
    }

    protected void setPrimaryType(final Node node, final NodeInfo nodeInfo) throws RepositoryException {
        if (!nodeInfo.getNodeTypeName().equals(node.getPrimaryNodeType().getName())) {
            node.setPrimaryType(nodeInfo.getNodeTypeName());
        }
    }

    protected void replaceMixins(final Node node, final NodeInfo nodeInfo) throws RepositoryException {
        Set<String> mixinSet = new TreeSet<>();
        Collections.addAll(mixinSet, nodeInfo.getMixinNames());
        for (NodeType nodeType : node.getMixinNodeTypes()) {
            final String mixinName = nodeType.getName();
            if (!mixinSet.contains(mixinName)) {
                node.removeMixin(mixinName);
            } else {
                mixinSet.remove(mixinName);
            }
        }
        for (String mixinName : mixinSet) {
            node.addMixin(mixinName);
        }
    }

    @Override
    public void endNode() throws RepositoryException {
        depth--;
        if (depth > 0) {
            super.endNode();
        }
    }
}
