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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.onehippo.cm.model.impl.definition.AbstractDefinitionImpl;
import org.onehippo.cm.model.impl.definition.ContentDefinitionImpl;
import org.onehippo.cm.model.impl.definition.NamespaceDefinitionImpl;
import org.onehippo.cm.model.impl.source.SourceImpl;
import org.onehippo.cm.model.impl.tree.DefinitionNodeImpl;
import org.onehippo.cm.model.impl.tree.DefinitionPropertyImpl;
import org.onehippo.cm.model.impl.tree.ValueImpl;
import org.onehippo.cm.model.source.Source;
import org.onehippo.cm.model.tree.PropertyType;

/**
 * Collect a list of (YAML-)external resource files referred to by a {@link Source}.
 * The resources files are represented by their file path, relative to the directory of the Source.
 */
public class SourceResourceCrawler {

    /**
     * Build list of resources referred to by a {@link Source}
     */
    public List<Pair<ValueImpl, String>> collect(final SourceImpl source) {

        final List<Pair<ValueImpl, String>> resources = new ArrayList<>();

        for (AbstractDefinitionImpl definition : source.getDefinitions()) {
            switch (definition.getType()) {
                case CONFIG:
                case CONTENT:
                    collectResourcesForNode(((ContentDefinitionImpl)definition).getNode(), resources);
                    break;
                case NAMESPACE:
                    collectResourcesForNamespace((NamespaceDefinitionImpl) definition, resources);
                    break;
            }
        }

        return resources;
    }

    protected void collectResourcesForNode(final DefinitionNodeImpl node, final List<Pair<ValueImpl, String>> resources) {
        for (DefinitionPropertyImpl childProperty : node.getProperties().values()) {
            collectResourcesForProperty(childProperty, resources);
        }

        for (DefinitionNodeImpl childNode : node.getNodes().values()) {
            collectResourcesForNode(childNode, resources);
        }
    }

    protected void collectResourcesForProperty(final DefinitionPropertyImpl property, final List<Pair<ValueImpl, String>> resources) {
        if (property.getType() == PropertyType.SINGLE) {
            final ValueImpl value = property.getValue();
            if (value.isResource()) {
                resources.add(new MutablePair<>(value, value.getString()));
            }
        } else {
            for (ValueImpl value : property.getValues()) {
                if (value.isResource()) {
                    resources.add(new MutablePair<>(value, value.getString()));
                }
            }
        }
    }

    protected void collectResourcesForNamespace(final NamespaceDefinitionImpl definition, List<Pair<ValueImpl, String>> resources) {
        if (definition.getCndPath() != null) {
            resources.add(new MutablePair<>(definition.getCndPath(), definition.getCndPath().getString()));
        }
    }
}

