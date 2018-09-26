/*
 *  Copyright 2012-2018 Hippo B.V. (http://www.onehippo.com)
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
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;

import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CopyHandler that adds the first node as a child.
 */
public class DefaultCopyHandler implements CopyHandler {

    private static final Set<String> PROTECTED_PROPERTY_NAMES = Stream.of(
            JcrConstants.JCR_UUID,
            JcrConstants.JCR_BASE_VERSION,
            JcrConstants.JCR_PREDECESSORS,
            JcrConstants.JCR_VERSION_HISTORY,
            JcrConstants.JCR_FROZEN_MIXIN_TYPES,
            JcrConstants.JCR_FROZEN_PRIMARY_TYPE,
            JcrConstants.JCR_FROZEN_UUID,
            JcrConstants.JCR_IS_CHECKED_OUT,
            JcrConstants.JCR_LOCK_OWNER,
            JcrConstants.JCR_LOCK_IS_DEEP
    ).collect(Collectors.toSet());

    private static final Logger log = LoggerFactory.getLogger(DefaultCopyHandler.class);

    private final Stack<Node> nodes = new Stack<>();
    private final Stack<NodeType[]> nodeTypes = new Stack<>();
    private final Set<String> protectedMixinNames = new HashSet<>();
    private final Set<String> protectedMixinPropertyNames = new HashSet<>();
    protected final NodeTypeManager nodeTypeManager;

    public DefaultCopyHandler(Node node) throws RepositoryException {
        this(node, Collections.emptySet());
    }

    public DefaultCopyHandler(Node node, Set<String> protectedMixinNames) throws RepositoryException {
        JcrUtils.ensureIsCheckedOut(node);
        setCurrent(node);
        nodeTypeManager = node.getSession().getWorkspace().getNodeTypeManager();
        for (String protectedMixinName : protectedMixinNames) {
            if (!nodeTypeManager.hasNodeType(protectedMixinName)) {
                log.warn("Node type {} is unknown, skipping it", protectedMixinName);
            } else {
                final NodeType mixinNodeType = nodeTypeManager.getNodeType(protectedMixinName);
                if (!mixinNodeType.isMixin()) {
                    log.warn("Node type {} is not a mixin, skipping it", protectedMixinName);
                } else {
                    this.protectedMixinNames.add(mixinNodeType.getName());
                    for (PropertyDefinition propertyDefinition : mixinNodeType.getPropertyDefinitions()) {
                        this.protectedMixinPropertyNames.add(propertyDefinition.getName());
                    }
                }
            }
        }
    }

    protected DefaultCopyHandler setCurrent(Node node) throws RepositoryException {
        nodes.push(node);
        if (node != null) {
            NodeType[] mixinNodeTypes = JcrUtils.getMixinNodeTypes(node);
            NodeType[] nodeTypes = new NodeType[mixinNodeTypes.length + 1];
            nodeTypes[0] = JcrUtils.getPrimaryNodeType(node);
            if (mixinNodeTypes.length > 0) {
                System.arraycopy(mixinNodeTypes, 0, nodeTypes, 1, mixinNodeTypes.length);
            }
            this.nodeTypes.push(nodeTypes);
        } else {
            nodeTypes.push(null);
        }
        return this;
    }

    @Override
    public void startNode(final NodeInfo nodeInfo) throws RepositoryException {
        if (getCurrent() != null) {
            final NodeDefinition definition = nodeInfo.getApplicableChildNodeDef(getCurrentNodeTypes());
            if (definition != null && !definition.isProtected()) {
                final Node childDest;
                if (definition.isAutoCreated() && nodeInfo.getIndex() == 1 && getCurrent().hasNode(nodeInfo.getName())) {
                    childDest = getCurrent().getNode(nodeInfo.getName());
                } else {
                    childDest = getCurrent().addNode(nodeInfo.getName(), nodeInfo.getNodeTypeName());
                }
                for (String nodeTypeName : nodeInfo.getMixinNames()) {
                    if (!protectedMixinNames.contains(nodeTypeName)) {
                        childDest.addMixin(nodeTypeName);
                    }
                }
                setCurrent(childDest);
                return;
            }
            throw new IllegalArgumentException("No applicable child node definition");
        }
        throw new IllegalStateException("No current copy target node");
    }

    @Override
    public void endNode() throws RepositoryException {
        nodes.pop();
        nodeTypes.pop();
    }

    @Override
    public void setProperty(final PropInfo propInfo) throws RepositoryException {

        if (propInfo == null) {
            return;
        }

        final Node current = getCurrent();
        if (current == null) {
            return;
        }

        if (isProtected(propInfo)) {
            return;
        }

        final PropertyDefinition definition = propInfo.getApplicablePropertyDef(getCurrentNodeTypes());
        if (definition == null) {
            log.error("Unable to create property from PropInfo {} : No applicable property definition", propInfo);
            return;
        }

        if (definition.isProtected()) {
            return;
        }

        if (propInfo.isMultiple()) {
            current.setProperty(propInfo.getName(), propInfo.getValues(), propInfo.getType());
        } else {
            current.setProperty(propInfo.getName(), propInfo.getValue(), propInfo.getType());
        }
    }

    @Override
    public Node getCurrent() {
        return nodes.peek();
    }

    protected NodeType[] getCurrentNodeTypes() {
        return nodeTypes.peek();
    }

    final Set<String> getProtectedMixinNames() {
        return protectedMixinNames;
    }

    private boolean isProtected(final PropInfo propInfo) {
        return PROTECTED_PROPERTY_NAMES.contains(propInfo.getName()) || protectedMixinPropertyNames.contains(propInfo.getName());
    }
}