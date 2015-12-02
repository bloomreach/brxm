/*
 *  Copyright 2015-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.autoexport;

import java.util.Stack;

import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.PropertyIterable;

import net.sf.json.JSONObject;
import static org.onehippo.repository.util.JcrConstants.JCR_MIXIN_TYPES;
import static org.onehippo.repository.util.JcrConstants.JCR_PRIMARY_TYPE;

class ResourceBundlesJSONSerializer implements ItemVisitor {

    private final JSONObject root = new JSONObject();
    private final Stack<JSONObject> current = new Stack<>();
    private final Stack<DeltaInstruction> currentInstruction = new Stack<>();

    private ResourceBundlesJSONSerializer(final DeltaInstruction rootInstruction) {
        current.push(root);
        currentInstruction.push(rootInstruction);
    }

    @Override
    public void visit(final Property property) throws RepositoryException {
    }

    @Override
    public void visit(final Node node) throws RepositoryException {
        final String nodeName = node.getName();
        if (node.isNodeType(HippoNodeType.NT_RESOURCEBUNDLES)) {
            final DeltaInstruction instruction = currentInstruction.peek().getInstruction(nodeName, true);
            if (instruction != null) {
                current.peek().put(nodeName, new JSONObject());
                JSONObject bundles = (JSONObject) current.peek().get(nodeName);
                current.push(bundles);
                currentInstruction.push(instruction);
                for (Node child : new NodeIterable(node.getNodes())) {
                    visit(child);
                }
                current.pop();
                currentInstruction.pop();
            }
        } else if (node.isNodeType(HippoNodeType.NT_RESOURCEBUNDLE)) {
            final JSONObject bundle = new JSONObject();
            final DeltaInstruction instruction = currentInstruction.peek().getInstruction(nodeName, true);
            if (instruction != null) {
                for (Property property : new PropertyIterable(node.getProperties())) {
                    if (!property.getName().equals(JCR_PRIMARY_TYPE) && !property.getName().equals(JCR_MIXIN_TYPES)) {
                        if (instruction.getInstruction(property.getName(), false) != null) {
                            bundle.put(property.getName(), property.getString());
                        }
                    }
                }
                current.peek().put(nodeName, bundle);
            }
        }
    }

    static JSONObject resourceBundlesToJSON(Session session, final DeltaInstruction rootInstruction) throws RepositoryException {
        final Node translations = session.getNode("/hippo:configuration/hippo:translations");
        final ResourceBundlesJSONSerializer visitor = new ResourceBundlesJSONSerializer(rootInstruction);
        for (Node node : new NodeIterable(translations.getNodes())) {
            node.accept(visitor);
        }
        return visitor.root;
    }

}
