/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.PropertyIterable;
import org.onehippo.cm.model.impl.source.ContentSourceImpl;
import org.onehippo.cm.model.impl.GroupImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.ProjectImpl;
import org.onehippo.cm.model.impl.definition.ContentDefinitionImpl;
import org.onehippo.cm.model.impl.tree.DefinitionNodeImpl;
import org.onehippo.cm.model.impl.tree.DefinitionPropertyImpl;
import org.onehippo.cm.model.impl.tree.ValueImpl;
import org.onehippo.cm.model.tree.ValueType;

import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.apache.jackrabbit.JcrConstants.JCR_UUID;
import static org.onehippo.cm.engine.ValueProcessor.isKnownDerivedPropertyName;
import static org.onehippo.cm.engine.ValueProcessor.valueFrom;
import static org.onehippo.cm.engine.ValueProcessor.valuesFrom;
import static org.onehippo.cm.model.Constants.YAML_EXT;

/**
 * Node export
 */
public class JcrContentExporter {

    /**
     * Export specified node
     *
     * @param node
     * @return
     */
    public ModuleImpl exportNode(final Node node) throws RepositoryException {
        final ModuleImpl module = new ModuleImpl("export-module", new ProjectImpl("export-project", new GroupImpl("export-group")));
        module.setConfigResourceInputProvider(new JcrResourceInputProvider(node.getSession()));
        module.setContentResourceInputProvider(module.getConfigResourceInputProvider());
        final String sourceFilename = mapNodeNameToFileName(NodeNameCodec.decode(node.getName()));
        final ContentSourceImpl contentSource = module.addContentSource(sourceFilename + YAML_EXT);
        final ContentDefinitionImpl contentDefinition = contentSource.addContentDefinition();

        exportNode(node, contentDefinition, false, Collections.emptySet());

        return module;
    }

    protected String mapNodeNameToFileName(String part) {
        return part.contains(":") ? part.replace(":", "-") : part;
    }

    public DefinitionNodeImpl exportNode(final Node node, final ContentDefinitionImpl contentDefinition,
                                         final boolean fullPath,
                                         final Set<String> excludedPaths) throws RepositoryException {
        if (isVirtual(node)) {
            throw new ConfigurationRuntimeException("Virtual node cannot be exported: " + node.getPath());
        }

        final DefinitionNodeImpl definitionNode;
        if (fullPath) {
            definitionNode = new DefinitionNodeImpl(node.getPath(), contentDefinition);
        } else {
            // Creating a definition with path 'rooted' at the node itself, without possible SNS index: we're not supporting indexed path elements
            definitionNode = new DefinitionNodeImpl("/" + node.getName(), node.getName(), contentDefinition);
        }
        contentDefinition.setNode(definitionNode);
        contentDefinition.setRootPath(node.getPath());

        exportProperties(node, definitionNode);
        definitionNode.sortProperties();

        for (final Node childNode : new NodeIterable(node.getNodes())) {
            if (!excludedPaths.contains(childNode.getPath())) {
                exportNode(childNode, definitionNode, excludedPaths);
            }
        }
        return definitionNode;
    }

    DefinitionNodeImpl exportNode(final Node sourceNode, final DefinitionNodeImpl parentNode, final Set<String> excludedPaths) throws RepositoryException {

        if (!isVirtual(sourceNode) && !shouldExcludeNode(sourceNode.getPath())) {
            final DefinitionNodeImpl definitionNode = parentNode.addNode(createNodeName(sourceNode));

            exportProperties(sourceNode, definitionNode);

            for (final Node childNode : new NodeIterable(sourceNode.getNodes())) {
                if (!excludedPaths.contains(childNode.getPath())) {
                    exportNode(childNode, definitionNode, excludedPaths);
                }
            }
            return definitionNode;
        }
        return null;
    }

    protected boolean shouldExcludeNode(final String jcrPath) {
        return false;
    }

    protected boolean isVirtual(final Node node) throws RepositoryException {
        return ((HippoNode) node).isVirtual();
    }

    private String createNodeName(final Node sourceNode) throws RepositoryException {
        final String name = sourceNode.getName();
        if (sourceNode.getIndex() > 1) {
            return name + "[" + sourceNode.getIndex() + "]";
        } else {
            if (sourceNode.getDefinition().allowsSameNameSiblings() && sourceNode.getParent().hasNode(name + "[2]")) {
                return name + "[1]";
            }
        }
        return name;
    }

    protected void exportProperties(final Node sourceNode, final DefinitionNodeImpl definitionNode) throws RepositoryException {
        exportPrimaryTypeAndMixins(sourceNode, definitionNode);
        for (final Property property : new PropertyIterable(sourceNode.getProperties())) {
            if (!propertyShouldBeSkipped(property)) {
                exportProperty(property, definitionNode);
            }
        }
        definitionNode.sortProperties();
    }

    protected boolean propertyShouldBeSkipped(Property property) throws RepositoryException {

        final String propName = property.getName();
        if (propName.equals(JCR_PRIMARYTYPE) || propName.equals(JCR_MIXINTYPES)) {
            return true; //Already processed those properties
        }
        if (isKnownDerivedPropertyName(propName)) {
            return true;
        }
        // skip protected properties, which are managed internally by JCR and don't make sense in export
        // (except JCR:UUID, which we need to do references properly)
        return !propName.equals(JCR_UUID) && property.getDefinition().isProtected();
    }

    DefinitionPropertyImpl exportProperty(final Property property, DefinitionNodeImpl definitionNode) throws RepositoryException {
        if (property.isMultiple()) {
            return definitionNode.addProperty(property.getName(), ValueType.fromJcrType(property.getType()),
                    valuesFrom(property, definitionNode));
        } else {
            final ValueImpl value = valueFrom(property, definitionNode);
            final DefinitionPropertyImpl targetProperty = definitionNode.addProperty(property.getName(), value);
            value.setParent(targetProperty);
            return targetProperty;
        }
    }

    void exportPrimaryTypeAndMixins(final Node sourceNode, final DefinitionNodeImpl definitionNode) throws RepositoryException {

        final Property primaryTypeProperty = sourceNode.getProperty(JCR_PRIMARYTYPE);
        final ValueImpl value = valueFrom(primaryTypeProperty, definitionNode);
        definitionNode.addProperty(primaryTypeProperty.getName(), value);

        final NodeType[] mixinNodeTypes = sourceNode.getMixinNodeTypes();
        if (mixinNodeTypes.length > 0) {
            final List<ValueImpl> values = new ArrayList<>();
            for (final NodeType mixinNodeType : mixinNodeTypes) {
                values.add(new ValueImpl(mixinNodeType.getName()));
            }
            values.sort(Comparator.comparing(ValueImpl::getString));
            definitionNode.addProperty(JCR_MIXINTYPES, ValueType.STRING, values.toArray(new ValueImpl[values.size()]));
        }
    }

}