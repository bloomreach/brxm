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

package org.onehippo.cm.impl.model.builder.sorting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.onehippo.cm.api.model.Definition;
import org.onehippo.cm.api.model.NamespaceDefinition;
import org.onehippo.cm.api.model.NodeTypeDefinition;
import org.onehippo.cm.api.model.Source;
import org.onehippo.cm.impl.model.ContentDefinitionImpl;
import org.onehippo.cm.impl.model.ModuleImpl;
import org.onehippo.cm.impl.model.builder.MergedModel;

public class DefinitionSorter {
    public void sort(final ModuleImpl module, final MergedModel mergedModel) {
        final List<Definition> definitions = new ArrayList<>();
        final SortedSet<Source> sortedSources = new TreeSet<>(Comparator.comparing(Source::getPath));
        sortedSources.addAll(module.getSources().values());
        sortedSources.forEach(source -> definitions.addAll(source.getDefinitions()));

        final List<ContentDefinitionImpl> sortedContentDefinitions = new LinkedList<>();
        definitions.sort(new DefinitionComparator(module));
        definitions.forEach(definition -> {
            if (definition instanceof ContentDefinitionImpl) {
                sortedContentDefinitions.add((ContentDefinitionImpl) definition);
            } else if (definition instanceof NodeTypeDefinition) {
                mergedModel.addNodeTypeDefinition((NodeTypeDefinition) definition);
            } else if (definition instanceof NamespaceDefinition) {
                mergedModel.addNamespaceDefinition((NamespaceDefinition) definition);
            } else {
                throw new IllegalStateException("DefinitionSorter doesn't support sorting '"
                        + definition.getClass().getName() + "'.");
            }
        });
        module.setSortedContentDefinitions(sortedContentDefinitions);
    }
}
