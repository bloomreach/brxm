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
package org.hippoecm.frontend.editor.compare;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeComparer extends TypedComparer<Node> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(NodeComparer.class);

    public NodeComparer(ITypeDescriptor type) {
        super(type);
        if (!type.isNode()) {
            throw new RuntimeException("type does not correspond to a node type");
        }
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
                    NodeComparer comparer = new NodeComparer(field.getTypeDescriptor());
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
        HashCodeBuilder hcb = new HashCodeBuilder();
        try {
            for (Map.Entry<String, IFieldDescriptor> entry : getType().getFields().entrySet()) {
                IFieldDescriptor field = entry.getValue();
                String path = field.getPath();
                if ("*".equals(path)) {
                    log.debug("Path * not supported");
                    continue;
                }
                if (field.getTypeDescriptor().isNode()) {
                    NodeComparer comparer = new NodeComparer(field.getTypeDescriptor());
                    if (field.isMultiple()) {
                        NodeIterator childIter = node.getNodes(path);
                        while (childIter.hasNext()) {
                            Node child = childIter.nextNode();
                            hcb.append(child.getName());
                            hcb.append(comparer.getHashCode(child));
                        }
                    } else {
                        if (node.hasNode(path)) {
                            Node child = node.getNode(path);
                            hcb.append(child.getName());
                            hcb.append(comparer.getHashCode(child));
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

}
