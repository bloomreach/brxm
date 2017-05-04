/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine.parser;

import org.onehippo.cm.api.model.ContentDefinition;
import org.onehippo.cm.api.model.Definition;
import org.onehippo.cm.api.model.DefinitionNode;
import org.onehippo.cm.api.model.DefinitionProperty;
import org.onehippo.cm.api.model.NodeTypeDefinition;
import org.onehippo.cm.api.model.PropertyType;
import org.onehippo.cm.api.model.Source;
import org.onehippo.cm.api.model.Value;
import org.onehippo.cm.api.model.ValueType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Source node crawler. Searches for predefined resources
 */
public class SourceResourceCrawler {

    /**
     * Returns all external resources from source
     * @param source resource to look from
     * @return external resources from current source
     */
    public Set<String> collect(final Source source) {

        final Set<String> resources = new HashSet<>();

        for (Definition definition : source.getDefinitions()) {
            switch (definition.getType()) {
                case CONFIG:
                case CONTENT:
                    representDefinitionNode(((ContentDefinition)definition).getNode(), resources);
                    break;
                case CND:
                    representNodetypeDefinition((NodeTypeDefinition) definition, resources);
                    break;
            }
        }

        return resources;
    }

    private void representDefinitionNode(final DefinitionNode node, final Set<String> resources) {
        for (DefinitionProperty childProperty : node.getProperties().values()) {
            representProperty(childProperty, resources);
        }

        for (DefinitionNode childNode : node.getNodes().values()) {
            representDefinitionNode(childNode, resources);
        }
    }

    private void representProperty(final DefinitionProperty property, final Set<String> resources) {
        if (requiresValueMap(property)) {
            representPropertyUsingMap(property, resources);
        }
    }

    private void representPropertyUsingMap(final DefinitionProperty property, final Set<String> resources) {
            final boolean exposeAsResource = hasResourceValues(property);

            if (property.getType() == PropertyType.SINGLE) {
                final Value value = property.getValue();
                if (exposeAsResource) {
                    processSingleResource(resources, value);
                }
            } else {
                for (Value value : property.getValues()) {
                    if (exposeAsResource) {
                        resources.add(value.getString());
                    }
                }
            }
    }

    private void processSingleResource(final Set<String> resources, final Value value) {
        if (!isBinaryEmbedded(value))  {
            resources.add(value.getString());
        }
    }

    private boolean isBinaryEmbedded(final Value value) {
        return value.getType() == ValueType.BINARY && !value.isResource();
    }

    private boolean requiresValueMap(final DefinitionProperty property) {
        if (hasResourceValues(property)) {
            return true;
        }

        final ValueType valueType = property.getValueType();
        return valueType == ValueType.BINARY;
    }

    private boolean hasResourceValues(final DefinitionProperty property) {
        if (property.getType() == PropertyType.SINGLE) {
            return property.getValue().isResource();
        }

        return Arrays.stream(property.getValues()).anyMatch(Value::isResource);
    }

    private void representNodetypeDefinition(final NodeTypeDefinition definition, final Set<String> resources) {
        if (definition.isResource()) {
            resources.add(definition.getValue());
        }
    }
}

