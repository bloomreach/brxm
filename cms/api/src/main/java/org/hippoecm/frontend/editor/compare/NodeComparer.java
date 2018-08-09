/*
 *  Copyright 2010-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.compare;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.TemplateEngineException;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeComparer extends TypedComparer<Node> {
    static final Logger log = LoggerFactory.getLogger(NodeComparer.class);

    private final ITemplateEngine templateEngine;

    public NodeComparer(ITypeDescriptor type, ITemplateEngine templateEngine) {
        super(type);
        if (!type.isNode()) {
            throw new RuntimeException("type " + type.getName() + " does not correspond to a node type");
        }
        this.templateEngine = templateEngine;
    }

    public boolean areEqual(Node baseNode, Node targetNode) {
        if (baseNode == null && targetNode == null) {
            return true;
        } else if (baseNode == null || targetNode == null) {
            return false;
        }
        try {
            for (Map.Entry<String, IFieldDescriptor> entry : getType().getFields().entrySet()) {
                IFieldDescriptor field = entry.getValue();
                String path = field.getPath();
                if ("*".equals(path)) {
                    log.debug("Path * not supported");
                    continue;
                }
                if (field.getTypeDescriptor().isNode()) {
                    NodeComparer comparer = new NodeComparer(field.getTypeDescriptor(), templateEngine);
                    if (field.isMultiple()) {
                        NodeIterator baseIter = baseNode.getNodes(path);
                        NodeIterator targetIter = targetNode.getNodes(path);
                        if (baseIter.getSize() != targetIter.getSize()) {
                            return false;
                        }
                        while (baseIter.hasNext()) {
                            if (!targetIter.hasNext()) {
                                return false;
                            }
                            if (!comparer.areEqual(baseIter.nextNode(), targetIter
                                    .nextNode())) {
                                return false;
                            }
                        }
                        if (targetIter.hasNext()) {
                            return false;
                        }
                    } else {
                        if (!baseNode.hasNode(path) && !targetNode.hasNode(path)) {
                            continue;
                        }
                        if (baseNode.hasNode(path) && targetNode.hasNode(path)) {
                            if (!comparer.areEqual(baseNode.getNode(path),
                                    targetNode.getNode(path))) {
                                return false;
                            }
                        } else {
                            return false;
                        }
                    }
                } else {
                    if (!baseNode.hasProperty(path) && !targetNode.hasProperty(path)) {
                        continue;
                    }
                    if (baseNode.hasProperty(path) && targetNode.hasProperty(path)) {
                        Property baseProp = baseNode.getProperty(path);
                        ValueComparer comparer = new ValueComparer(field.getTypeDescriptor());
                        Property targetProp = targetNode.getProperty(path);
                        if (field.isMultiple()) {
                            Value[] baseValues = baseProp.getValues();
                            Value[] targetValues = targetProp.getValues();
                            if (baseValues.length != targetValues.length) {
                                return false;
                            }
                            for (int i = 0; i < baseValues.length; i++) {
                                if (!comparer.areEqual(baseValues[i], targetValues[i])) {
                                    return false;
                                }
                            }
                        } else {
                            if (!comparer.areEqual(baseProp.getValue(), targetProp.getValue())) {
                                return false;
                            }
                        }
                    } else {
                        return false;
                    }
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage(), ex);
            return false;
        }
        return true;
    }

    public int getHashCode(Node node) {
        if (node == null) {
            return 0;
        }
        final HashCodeBuilder hcb = new HashCodeBuilder();
        try {
            ITypeDescriptor nodeTypeDescriptor = getNodeTypeDescriptor(node);
            for (Map.Entry<String, IFieldDescriptor> entry : nodeTypeDescriptor.getFields().entrySet()) {
                IFieldDescriptor field = entry.getValue();
                String path = field.getPath();
                if ("*".equals(path)) {
                    log.debug("Path * not supported");
                    continue;
                }
                if (field.getTypeDescriptor().isNode()) {
                    if (field.isMultiple()) {
                        NodeIterator childIter = node.getNodes(path);
                        while (childIter.hasNext()) {
                            Node child = childIter.nextNode();
                            hcb.append(child.getName());
                            hcb.append(getHashCode(child));
                        }
                    } else {
                        if (node.hasNode(path)) {
                            Node child = node.getNode(path);
                            hcb.append(child.getName());
                            hcb.append(getHashCode(child));
                        }
                    }
                } else {
                    if (node.hasProperty(path)) {
                        Property prop = node.getProperty(path);
                        ValueComparer comparer = new ValueComparer(field.getTypeDescriptor());
                        hcb.append(prop.getName());
                        if (field.isMultiple()) {
                            for (Value value : prop.getValues()) {
                                hcb.append(comparer.getHashCode(value));
                            }
                        } else {
                            hcb.append(comparer.getHashCode(prop.getValue()));
                        }
                    }
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage(), ex);
        }
        return hcb.toHashCode();
    }
    private ITypeDescriptor getNodeTypeDescriptor(final Node node) throws RepositoryException {
        final ITypeDescriptor configuredType = getType();
        String nodeTypeName = node.getPrimaryNodeType().getName();
        if (nodeTypeName.equals(JcrConstants.NT_FROZEN_NODE)) {
            nodeTypeName = node.getProperty(JcrConstants.JCR_FROZEN_PRIMARY_TYPE).getString();
        }
        if (StringUtils.equals(configuredType.getName(), nodeTypeName)) {
            return configuredType;
        } else {
            // configured type and node type are different, try to lookup node type descriptor from template engine
            if (templateEngine != null) {
                try {
                    return templateEngine.getType(nodeTypeName);
                } catch (TemplateEngineException e) {
                    log.error("Cannot obtain node type descriptor of '{}' from the template engine", nodeTypeName, e);
                }
            } else {
                log.warn("Cannot obtain the type descriptor of the node type '{}' because the template engine is not set for the field at '{}'. Using the preconfigured type descriptor '{}'",
                        nodeTypeName, node.getPath(), configuredType.getName());
            }
            return configuredType;
        }

    }
}
