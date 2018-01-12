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
import org.onehippo.cm.model.impl.GroupImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.ProjectImpl;
import org.onehippo.cm.model.impl.definition.ContentDefinitionImpl;
import org.onehippo.cm.model.impl.source.ContentSourceImpl;
import org.onehippo.cm.model.impl.tree.DefinitionNodeImpl;
import org.onehippo.cm.model.impl.tree.DefinitionPropertyImpl;
import org.onehippo.cm.model.impl.tree.ValueImpl;
import org.onehippo.cm.model.path.JcrPaths;
import org.onehippo.cm.model.tree.ValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(JcrContentExporter.class);

    protected final ExportConfig exportConfig;

    // This constructor should be used only as a convenience for testing
    JcrContentExporter() {
        this.exportConfig = new ExportConfig();
    }

    public JcrContentExporter(final ExportConfig exportConfig) {
        this.exportConfig = exportConfig;

        // TODO: should we also exclude system properties and nodes using a ConfigurationModel?
    }

    /**
     * Export specified node
     */
    ModuleImpl exportNode(final Node node) throws RepositoryException {
        final ModuleImpl module = new ModuleImpl("export-module", new ProjectImpl("export-project", new GroupImpl("export-group")));
        module.setConfigResourceInputProvider(new JcrResourceInputProvider(node.getSession()));
        module.setContentResourceInputProvider(module.getConfigResourceInputProvider());
        final String sourceFilename = mapNodeNameToFileName(NodeNameCodec.decode(node.getName()));
        final ContentSourceImpl contentSource = module.addContentSource(sourceFilename + YAML_EXT);
        final ContentDefinitionImpl contentDefinition = contentSource.addContentDefinition();

        exportNode(node, contentDefinition, false, null, Collections.emptySet());

        return module;
    }

    private String mapNodeNameToFileName(String part) {
        return part.contains(":") ? part.replace(":", "-") : part;
    }

    public void exportNode(final Node node,
                           final ContentDefinitionImpl contentDefinition,
                           final boolean fullPath,
                           final String orderBefore,
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
        definitionNode.setOrderBefore(orderBefore);
        contentDefinition.setNode(definitionNode);
        contentDefinition.setRootPath(JcrPaths.getPath(node.getPath()));

        exportProperties(node, definitionNode);
        definitionNode.sortProperties();

        for (final Node childNode : new NodeIterable(node.getNodes())) {
            exportNode(childNode, definitionNode, excludedPaths);
        }
    }

    protected void exportNode(final Node sourceNode, final DefinitionNodeImpl parentNode, final Set<String> excludedPaths) throws RepositoryException {
        final String path = sourceNode.getPath();
        if (!isVirtual(sourceNode) && !shouldExcludeNode(path) && !excludedPaths.contains(path)) {
            final DefinitionNodeImpl definitionNode = parentNode.addNode(createNodeName(sourceNode));

            exportProperties(sourceNode, definitionNode);

            for (final Node childNode : new NodeIterable(sourceNode.getNodes())) {
                exportNode(childNode, definitionNode, excludedPaths);
            }
        }
    }

    protected boolean shouldExcludeProperty(Property property) throws RepositoryException {
        final String propName = property.getName();
        if (propName.equals(JCR_PRIMARYTYPE) || propName.equals(JCR_MIXINTYPES)) {
            return true; //Already processed those properties
        }

        // check ExportConfig.filterUuidPaths for export (suppressing export of jcr:uuid)
        if (propName.equals(JCR_UUID) && exportConfig.shouldFilterUuid(property.getNode().getPath())) {
            return true;
        }

        // suppress common derived Hippo properties
        if (isKnownDerivedPropertyName(propName)) {
            return true;
        }

        // skip protected properties, which are managed internally by JCR and don't make sense in export
        // (except JCR:UUID, which we need to do references properly)
        if (!propName.equals(JCR_UUID) && property.getDefinition().isProtected()) {
            return true;
        }

        // exclude anything on an excluded path (which might specifically address properties)
        // default to exporting anything not explicitly suppressed
        return exportConfig.isExcludedPath(property.getPath());
    }

    protected boolean shouldExcludeNode(final String jcrPath) {
        // exclude anything on an excluded path
        if (exportConfig.isExcludedPath(jcrPath)) {
            log.debug("Ignoring node because of export exclusion:\n\t{}", jcrPath);
            return true;
        }

        // default to exporting anything not explicitly suppressed
        return false;
    }

    protected boolean isVirtual(final Node node) throws RepositoryException {
        return ((HippoNode) node).isVirtual();
    }

    protected String createNodeName(final Node sourceNode) throws RepositoryException {
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
            if (!shouldExcludeProperty(property)) {
                exportProperty(property, definitionNode);
            }
        }
        definitionNode.sortProperties();
    }

    protected DefinitionPropertyImpl exportProperty(final Property property, DefinitionNodeImpl definitionNode) throws RepositoryException {
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

    protected void exportPrimaryTypeAndMixins(final Node sourceNode, final DefinitionNodeImpl definitionNode) throws RepositoryException {

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
            definitionNode.addProperty(JCR_MIXINTYPES, ValueType.NAME, values);
        }
    }
}