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
package org.onehippo.cm.impl.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.onehippo.cm.api.model.Definition;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.Project;
import org.onehippo.cm.api.model.Source;

public class ModuleImpl implements Module {

    private String name;
    private Project project;
    private Set<String> modifiableAfter = new LinkedHashSet<>();
    private Set<String> after = Collections.unmodifiableSet(modifiableAfter);
    private Map<String, SourceImpl> modifiableSources = new LinkedHashMap<>();
    private Map<String, Source> sources = Collections.unmodifiableMap(modifiableSources);

    private List<NamespaceDefinitionImpl> namespaces = new ArrayList<>();
    private List<NodeTypeDefinitionImpl> nodeTypes = new ArrayList<>();
    private List<ContentDefinitionImpl> contentDefinitions = new ArrayList<>();

    public ModuleImpl(final String name, final ProjectImpl project) {
        if (name == null) {
            throw new IllegalArgumentException("Parameter 'name' cannot be null");
        }
        this.name = name;

        if (project == null) {
            throw new IllegalArgumentException("Parameter 'project' cannot be null");
        }
        this.project = project;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public Set<String> getAfter() {
        return after;
    }

    public Set<String> getModifiableAfter() {
        return modifiableAfter;
    }

    public ModuleImpl addAfter(final Set<String> after) {
        modifiableAfter.addAll(after);
        return this;
    }

    @Override
    public Map<String, Source> getSources() {
        return sources;
    }

    public Map<String, SourceImpl> getModifiableSources() {
        return modifiableSources;
    }

    public SourceImpl addSource(final String path) {
        final SourceImpl source = new SourceImpl(path, this);
        modifiableSources.put(path, source);
        return source;
    }

    public List<NamespaceDefinitionImpl> getNamespaces() {
        return namespaces;
    }

    public List<NodeTypeDefinitionImpl> getNodeTypes() {
        return nodeTypes;
    }

    public List<ContentDefinitionImpl> getContentDefinitions() {
        return contentDefinitions;
    }

    void pushDefinitions(final ModuleImpl module) {
        // sort sources to provide consistent error reporting on conflicting definition paths.
        final Set<Source> sortedSources = new TreeSet<>(Comparator.comparing(Source::getPath));
        sortedSources.addAll(module.getSources().values());

        // sort definitions into namespaces, node types and content
        sortedSources.forEach(source ->
                source.getDefinitions().forEach(definition -> {
                    if (definition instanceof NamespaceDefinitionImpl) {
                        namespaces.add((NamespaceDefinitionImpl) definition);
                    } else if (definition instanceof NodeTypeDefinitionImpl) {
                        ensureSingleSourceForNodeTypes(definition);
                        nodeTypes.add((NodeTypeDefinitionImpl) definition);
                    } else if (definition instanceof ContentDefinitionImpl) {
                        contentDefinitions.add((ContentDefinitionImpl) definition);
                    } else {
                        throw new IllegalStateException("Failed to sort unsupported definition class '"
                                + definition.getClass().getName() + "'.");
                    }
                })
        );
    }

    public void sortDefinitions() {
        namespaces.sort(Comparator.comparing(NamespaceDefinitionImpl::getPrefix));
        // node types stay sorted in insertion order
        contentDefinitions.sort(new ContentDefinitionComparator());
    }

    private void ensureSingleSourceForNodeTypes(final Definition nodeTypeDefinition) {
        if (!nodeTypes.isEmpty()
                && !nodeTypeDefinition.getSource().getPath().equals(nodeTypes.get(0).getSource().getPath())) {
            final String msg = String.format("CNDs are specified in multiple sources of a module: %s and %s. "
                    + "For proper ordering, they must be specified in a single source.",
                    ModelUtils.formatDefinitionOrigin(nodeTypeDefinition),
                    ModelUtils.formatDefinitionOrigin(nodeTypes.get(0)));
            throw new IllegalStateException(msg);
        }
    }

    private class ContentDefinitionComparator implements Comparator<ContentDefinitionImpl> {
        public int compare(final ContentDefinitionImpl def1, final ContentDefinitionImpl def2) {
            final String rootPath1 = def1.getNode().getPath();
            final String rootPath2 = def2.getNode().getPath();

            if (def1 != def2 && rootPath1.equals(rootPath2)) {
                throw new IllegalStateException("Duplicate content root paths '" + rootPath1 + "' in module '"
                        + getName() + "'.");
            }
            return rootPath1.compareTo(rootPath2);
        }
    }
}
