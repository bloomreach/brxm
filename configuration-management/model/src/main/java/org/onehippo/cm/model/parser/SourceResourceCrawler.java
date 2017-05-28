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
package org.onehippo.cm.model.parser;

import java.util.HashSet;
import java.util.Set;

import org.onehippo.cm.model.ContentDefinition;
import org.onehippo.cm.model.Definition;
import org.onehippo.cm.model.DefinitionNode;
import org.onehippo.cm.model.DefinitionProperty;
import org.onehippo.cm.model.NamespaceDefinition;
import org.onehippo.cm.model.PropertyType;
import org.onehippo.cm.model.Source;
import org.onehippo.cm.model.Value;

/**
 * Collect a list of (YAML-)external resource files referred to by a {@link Source}.
 * The resources files are represented by their file path, relative to the directory of the Source.
 */
public class SourceResourceCrawler {

    /**
     * Build list of resources referred to by a {@link Source}
     */
    public Set<String> collect(final Source source) {

        final Set<String> resources = new HashSet<>();

        for (Definition definition : source.getDefinitions()) {
            switch (definition.getType()) {
                case CONFIG:
                case CONTENT:
                    collectResourcesForNode(((ContentDefinition)definition).getNode(), resources);
                    break;
                case NAMESPACE:
                    collectResourcesForNamespace((NamespaceDefinition) definition, resources);
                    break;
            }
        }

        return resources;
    }

    private void collectResourcesForNode(final DefinitionNode node, final Set<String> resources) {
        for (DefinitionProperty childProperty : node.getProperties().values()) {
            collectResourcesForProperty(childProperty, resources);
        }

        for (DefinitionNode childNode : node.getNodes().values()) {
            collectResourcesForNode(childNode, resources);
        }
    }

    private void collectResourcesForProperty(final DefinitionProperty property, final Set<String> resources) {
        if (property.getType() == PropertyType.SINGLE) {
            final Value value = property.getValue();
            if (value.isResource()) {
                resources.add(value.getString());
            }
        } else {
            for (Value value : property.getValues()) {
                if (value.isResource()) {
                    resources.add(value.getString());
                }
            }
        }
    }

    private void collectResourcesForNamespace(final NamespaceDefinition definition, final Set<String> resources) {
        if (definition.getCndPath() != null) {
            resources.add(definition.getCndPath());
        }
    }
}

