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
import javax.jcr.nodetype.PropertyDefinition;

import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CopyHandler that adds the first node as a child.
 */
public class DefaultCopyHandler implements CopyHandler {

    private final static String[] PROTECTED = new String[] {
            JcrConstants.JCR_UUID,
            JcrConstants.JCR_BASE_VERSION,
            JcrConstants.JCR_PREDECESSORS,
            JcrConstants.JCR_VERSION_HISTORY,
            JcrConstants.JCR_FROZEN_MIXIN_TYPES,
            JcrConstants.JCR_FROZEN_PRIMARY_TYPE,
            JcrConstants.JCR_FROZEN_UUID,
            JcrConstants.JCR_IS_CHECKED_OUT,
            JcrConstants.JCR_LOCK_OWNER,
            JcrConstants.JCR_LOCK_IS_DEEP,
    };

    static {
        Arrays.sort(PROTECTED);
    }

    static final Logger log = LoggerFactory.getLogger(DefaultCopyHandler.class);

    private Node current;
    private NodeType[] nodeTypes;

    public DefaultCopyHandler(Node node) throws RepositoryException {
        JcrUtils.ensureIsCheckedOut(node, true);
        setCurrent(node);
    }

    protected DefaultCopyHandler setCurrent(Node node) throws RepositoryException {
        this.current = node;
        NodeType[] mixinNodeTypes = JcrUtils.getMixinNodeTypes(current);
        nodeTypes = new NodeType[mixinNodeTypes.length + 1];
        nodeTypes[0] = JcrUtils.getPrimaryNodeType(current);
        if (mixinNodeTypes.length > 0) {
            System.arraycopy(mixinNodeTypes, 0, nodeTypes, 1, mixinNodeTypes.length);
        }
        return this;
    }

    @Override
    public void startNode(final NodeInfo nodeInfo) throws RepositoryException {
        try {
            NodeDefinition definition = nodeInfo.getApplicableChildNodeDef(nodeTypes);
            if (!definition.isProtected()) {
                final Node childDest;
                if (definition.isAutoCreated() && nodeInfo.getIndex() == 1 && current.hasNode(nodeInfo.getName())) {
                    childDest = current.getNode(nodeInfo.getName());
                } else {
                    childDest = current.addNode(nodeInfo.getName(), nodeInfo.getNodeTypeName());
                }
                for (String nodeTypeName : nodeInfo.getMixinNames()) {
                    childDest.addMixin(nodeTypeName);
                }
                setCurrent(childDest);
            }
        } catch (ConstraintViolationException cve) {
            log.error("Unable to create node from NodeInfo " + nodeInfo + ": " + cve.toString());
        }
    }

    @Override
    public void endNode() throws RepositoryException {
        setCurrent(current.getParent());
    }

    @Override
    public void setProperty(final PropInfo propInfo) throws RepositoryException {
        if (propInfo != null && Arrays.binarySearch(PROTECTED, propInfo.getName()) < 0) {
            try {
                PropertyDefinition definition = propInfo.getApplicablePropertyDef(nodeTypes);
                if (!definition.isProtected()) {
                    if (propInfo.isMultiple()) {
                        current.setProperty(propInfo.getName(), propInfo.getValues(), propInfo.getType());
                    } else {
                        current.setProperty(propInfo.getName(), propInfo.getValue(), propInfo.getType());
                    }
                }
            } catch (ConstraintViolationException cve) {
                log.error("Unable to create property from PropInfo " + propInfo);
            }
        }
    }

    @Override
    public Node getCurrent() {
        return current;
    }
}