/*
 *  Copyright 2013-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.standardworkflow;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeType;

import org.hippoecm.repository.util.DefaultCopyHandler;
import org.hippoecm.repository.util.NodeInfo;
import org.hippoecm.repository.util.PropInfo;

class ExpandingCopyHandler extends DefaultCopyHandler {

    private final Map<String, String[]> renames;
    private final ValueFactory factory;
    private Path path;
    private String lastSubstituteName;

    ExpandingCopyHandler(final Node handle, final Map<String, String[]> renames, final ValueFactory factory) throws RepositoryException {
        super(handle);
        this.renames = renames;
        this.factory = factory;
        this.path = new Path();
    }

    @Override
    public void startNode(NodeInfo nodeInfo) throws RepositoryException {
        if (getCurrent() != null) {

            final String origName = nodeInfo.getName();
            String name = origName;

            path.push(origName);

            for (Map.Entry<String, String[]> entry : renames.entrySet()) {
                final String key = entry.getKey();
                final String[] substitutes = entry.getValue();
                if (key.endsWith("/_name") && substitutes != null && substitutes.length > 0) {
                    if (path.matchKey(key, substitutes[0])) {
                        name = substitutes[0];
                        lastSubstituteName = name;
                        break;
                    }
                }
            }

            NodeType primaryType = nodeInfo.getNodeType();
            for (Map.Entry<String, String[]> entry : renames.entrySet()) {
                final String key = entry.getKey();
                final String[] substitutes = entry.getValue();
                if (key.endsWith("/jcr:primaryType") && substitutes != null && substitutes.length > 0) {
                    if (path.matchKey(key, name)) {
                        primaryType = nodeTypeManager.getNodeType(substitutes[0]);
                        break;
                    }
                }
            }

            List<NodeType> mixins = Arrays.asList(nodeInfo.getMixinTypes());
            for (Map.Entry<String, String[]> entry : renames.entrySet()) {
                final String key = entry.getKey();
                final String[] substitutes = entry.getValue();
                if (key.endsWith("/jcr:mixinTypes") && substitutes != null && substitutes.length > 0) {
                    if (path.matchKey(key, name)) {
                        for (String substitute : substitutes) {
                            mixins.add(nodeTypeManager.getNodeType(substitute));
                        }
                        break;
                    }
                }
            }

            nodeInfo = new NodeInfo(name, nodeInfo.getIndex(), primaryType, mixins.toArray(new NodeType[mixins.size()]));
            if (!nodeInfo.hasApplicableChildNodeDef(getCurrentNodeTypes())) {
                // no applicable child node definition
                // this happens for instance when trying to rename hippo:translation nodes which shouldn't be renamed
                if (origName.equals(name)) {
                    throw new ConstraintViolationException("Cannot change types: conflict with parent node type");
                }
                nodeInfo = new NodeInfo(origName, nodeInfo.getIndex(), primaryType, mixins.toArray(new NodeType[mixins.size()]));
            }
        }
        super.startNode(nodeInfo);
    }

    @Override
    public void endNode() throws RepositoryException {
        super.endNode();
        if (getCurrent() != null) {
            path.pop();
        }
    }

    @Override
    public void setProperty(PropInfo propInfo) throws RepositoryException {
        if (getCurrent() != null) {
            String name = propInfo.getName();


            String[] substitutes = null;
            for (Map.Entry<String, String[]> entry : renames.entrySet()) {
                final String key = entry.getKey();
                if (key.endsWith("/" + name) && path.matchKey(key, lastSubstituteName)) {
                    substitutes = entry.getValue();
                    break;
                }
            }

            if (propInfo.isMultiple()) {
                if (substitutes != null) {
                    Value[] values = new Value[substitutes.length];
                    int i = 0;
                    for (String val : substitutes) {
                        values[i++] = factory.createValue(val, propInfo.getType());
                    }
                    propInfo = new PropInfo(name, propInfo.getType(), values);
                } else {
                    Value[] values = propInfo.getValues();
                    List<Value> newValues = new LinkedList<>();
                    for (int i = 0; i < values.length; i++) {
                        substitutes = null;
                        for (Map.Entry<String, String[]> entry : renames.entrySet()) {
                            final String key = entry.getKey();
                            if (key.endsWith("/" + name + "[" + i + "]") && path.matchKey(key, lastSubstituteName)) {
                                substitutes = entry.getValue();
                                break;
                            }
                        }
                        if (substitutes != null) {
                            for (String val : substitutes) {
                                newValues.add(factory.createValue(val, propInfo.getType()));
                            }
                        } else {
                            newValues.add(values[i]);
                        }
                    }
                    propInfo = new PropInfo(name, propInfo.getType(), newValues.toArray(new Value[newValues.size()]));
                }
            } else {
                if (substitutes != null && substitutes.length == 1) {
                    propInfo = new PropInfo(name, propInfo.getType(), factory.createValue(substitutes[0], propInfo.getType()));
                }
            }
        }
        super.setProperty(propInfo);
    }

    private static class Path {
        private final Stack<String> names = new Stack<>();

        private void push(final String name) {
            names.push(name);
        }

        private void pop() {
            names.pop();
        }

        private boolean matchKey(final String keyPath, final String substitute) {
            final String[] elements = keyPath.split("/");
            if (names.size() != elements.length-1) {
                return false;
            }
            for (int i = 0; i < elements.length-1; i++) {
                if (i == 0 && elements[i].equals(".")) {
                    // matches root
                    continue;
                }
                if (elements[i].equals("_node")) {
                    // matches any
                    continue;
                }
                if (!elements[i].equals(names.get(i))) {
                    return false;
                }
            }
            return true;
        }

    }
}
