/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cm.impl.model.builder;

import java.util.LinkedList;
import java.util.List;

import org.onehippo.cm.api.model.ContentDefinition;
import org.onehippo.cm.api.model.Definition;
import org.onehippo.cm.api.model.NamespaceDefinition;
import org.onehippo.cm.api.model.NodeTypeDefinition;

public class DefinitionTriple {
    private final List<NamespaceDefinition> namespaceDefinitions = new LinkedList<>();
    private final List<NodeTypeDefinition> nodeTypeDefinitions = new LinkedList<>();
    private final List<ContentDefinition> contentDefinitions = new LinkedList<>();

    public List<NamespaceDefinition> getNamespaceDefinitions() {
        return namespaceDefinitions;
    }

    public List<NodeTypeDefinition> getNodeTypeDefinitions() {
        return nodeTypeDefinitions;
    }

    public List<ContentDefinition> getContentDefinitions() {
        return contentDefinitions;
    }

    public void addDefinition(final Definition definition) {
        if (definition instanceof NamespaceDefinition) {
            namespaceDefinitions.add((NamespaceDefinition) definition);
        } else if (definition instanceof NodeTypeDefinition) {
            nodeTypeDefinitions.add((NodeTypeDefinition)definition);
        } else {
            contentDefinitions.add((ContentDefinition)definition);
        }
    }
}
