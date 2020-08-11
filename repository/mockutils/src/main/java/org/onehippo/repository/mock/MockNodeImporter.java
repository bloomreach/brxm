/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.mock;

import java.io.IOException;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.onehippo.cm.model.impl.tree.DefinitionNodeImpl;
import org.onehippo.cm.model.path.JcrPath;
import org.onehippo.cm.model.path.JcrPaths;
import org.onehippo.cm.model.tree.DefinitionNode;
import org.onehippo.cm.model.tree.DefinitionProperty;
import org.onehippo.cm.model.tree.PropertyKind;
import org.onehippo.cm.model.tree.Value;
import org.onehippo.cm.model.util.SnsUtils;
import org.onehippo.repository.util.JcrConstants;

import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.apache.jackrabbit.JcrConstants.JCR_UUID;

/**
 * Imports definition nodes as mock nodes.
 */
class MockNodeImporter {

    private MockNodeImporter() {
    }

    static void importNode(final DefinitionNodeImpl modelNode, final MockNode parentNode) throws RepositoryException, IOException {
        try {
            if (parentNode.hasNodes()) {
                // Self-imposed limitation to keep the import code relatively simple:
                // only import mock nodes below empty nodes. Avoids checks for duplicate UUIDs and complex merge logic.
                throw new RepositoryException("Cannot import mock nodes at " + parentNode.getPath()
                        + " because that node already has child nodes");
            }
            final JcrPath parentPath = JcrPaths.getPath(parentNode.getPath());
            final JcrPath modelPath = parentPath.resolve(modelNode.getJcrName());
            modelNode.getDefinition().setRootPath(modelPath);
            applyNode(modelNode, parentNode);
        } finally {
            modelNode.getDefinition().setRootPath(null);
        }
    }

    private static void applyNode(final DefinitionNode definitionNode, final MockNode parentNode) throws RepositoryException, IOException {
        final MockNode jcrNode = addNode(parentNode, definitionNode);
        applyProperties(definitionNode, jcrNode);
        applyChildNodes(definitionNode, jcrNode);
    }

    private static void applyChildNodes(final DefinitionNode modelNode, final MockNode jcrNode) throws RepositoryException, IOException {
        for (final DefinitionNode modelChild : modelNode.getNodes()) {
            applyNode(modelChild, jcrNode);
        }
    }

    private static MockNode addNode(final MockNode parentNode, final DefinitionNode modelNode) throws RepositoryException {
        final String name = SnsUtils.getUnindexedName(modelNode.getName());
        final String primaryType = getPrimaryType(modelNode);
        final MockNode node = parentNode.addNode(name, primaryType);

        final DefinitionProperty uuidProperty = modelNode.getProperty(JCR_UUID);
        if (uuidProperty != null) {
            final String uuid = uuidProperty.getValue().getString();
            node.setIdentifier(uuid);
        }

        return node;
    }

    private static String getPrimaryType(final DefinitionNode modelNode) {
        final DefinitionProperty primaryType = modelNode.getProperty(JCR_PRIMARYTYPE);
        if (primaryType == null) {
            return JcrConstants.NT_UNSTRUCTURED; // for convenience, use a default primary type for mock nodes
        }
        return primaryType.getValue().getString();
    }

    private static void applyProperties(final DefinitionNode source, final Node targetNode) throws RepositoryException {
        targetNode.setPrimaryType(getPrimaryType(source));

        final DefinitionProperty mixinTypes = source.getProperty(JCR_MIXINTYPES);
        if (mixinTypes != null) {
            for (final Value mixinType : mixinTypes.getValues()) {
                targetNode.addMixin(mixinType.getString());
            }
        }

        for (final DefinitionProperty modelProperty : source.getProperties()) {
            applyProperty(modelProperty, targetNode);
        }
    }

    private static void applyProperty(final DefinitionProperty modelProperty, final Node jcrNode) throws RepositoryException {
        final int jcrType = modelProperty.getValueType().ordinal();
        if (modelProperty.getKind() == PropertyKind.SINGLE) {
            jcrNode.setProperty(modelProperty.getName(), createMockValue(modelProperty.getValue(), jcrType));
        } else {
            jcrNode.setProperty(modelProperty.getName(), createMockValues(modelProperty.getValues(), jcrType));
        }
    }

    private static MockValue createMockValue(final Value value, int jcrType) {
        return new MockValue(jcrType, value.getString());
    }

    private static MockValue[] createMockValues(final List<? extends Value> values, int jcrType) {
        final MockValue[] mockValues = new MockValue[values.size()];

        int index = 0;
        for (Value value: values) {
            mockValues[index++] = createMockValue(value, jcrType);
        }

        return mockValues;
    }
}
