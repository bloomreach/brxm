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

import java.nio.file.FileSystem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.onehippo.cm.api.model.ConfigurationModel;
import org.onehippo.cm.api.model.ContentDefinition;
import org.onehippo.cm.api.model.Group;
import org.onehippo.cm.impl.model.ConfigurationModelImpl;
import org.onehippo.cm.impl.model.ConfigurationNodeImpl;
import org.onehippo.cm.impl.model.GroupImpl;
import org.onehippo.cm.impl.model.ModelUtils;
import org.onehippo.cm.impl.model.ModuleImpl;
import org.onehippo.cm.impl.model.ProjectImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ConfigurationModelBuilder accumulates {@link GroupImpl}s into a map of groups and, when building
 * the model, sorts the involved objects into processing order (based on "after" dependencies) to construct
 * the tree of {@link ConfigurationNodeImpl}s.
 */
public class ConfigurationModelBuilder {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationModelBuilder.class);
    private static final OrderableListSorter<GroupImpl> groupSorter = new OrderableListSorter<>(Group.class.getSimpleName());

    private final List<GroupImpl> groups = new ArrayList<>();
    private final Map<String, GroupImpl> groupMap = new HashMap<>();

    private final Set<ModuleImpl> replacements = new HashSet<>();

    // Used for cleanup when done with this ConfigurationModel
    private final Set<FileSystem> filesystems = new HashSet<>();

    public ConfigurationModel build() {
        sort();

        final ConfigurationModelImpl mergedModel = new ConfigurationModelImpl();
        mergedModel.setSortedGroups(groups);

        final ConfigurationTreeBuilder configurationTreeBuilder = new ConfigurationTreeBuilder();
        groups.forEach(group ->
                group.getModifiableProjects().forEach(project ->
                        project.getModifiableModules().forEach(module -> {
                            logger.info("Merging module {}", ModelUtils.formatModule(module));
                            mergedModel.addNamespaceDefinitions(module.getNamespaceDefinitions());
                            mergedModel.addNodeTypeDefinitions(module.getNodeTypeDefinitions());
                            module.getConfigDefinitions().forEach(configurationTreeBuilder::push);
                            mergedModel.addConfigDefinitions(module.getConfigDefinitions());
                            mergedModel.addContentDefinitions(module.getContentDefinitions().stream().map(x -> (ContentDefinition)x).collect(Collectors.toSet()));
                            mergedModel.addWebFileBundleDefinitions(module.getWebFileBundleDefinitions());
                        })
                )
        );
        mergedModel.setConfigurationRootNode(configurationTreeBuilder.build());

        mergedModel.setFileSystems(filesystems);

        return mergedModel;
    }

    public ConfigurationModelBuilder push(final Map<String, GroupImpl> groups) {
        for (GroupImpl group : groups.values()) {
            push(group);
        }
        return this;
    }

    public ConfigurationModelBuilder push(final GroupImpl group) {
        final String name = group.getName();
        final GroupImpl consolidated = groupMap.containsKey(name)
                ? groupMap.get(name) : createGroup(name);

        consolidated.addAfter(group.getAfter());
        for (ProjectImpl project : group.getModifiableProjects()) {
            consolidated.pushProject(project, Collections.unmodifiableSet(replacements));
        }
        return this;
    }

    public ConfigurationModelBuilder pushReplacements(final Set<ModuleImpl> modules) {
        modules.forEach(this::pushReplacement);
        return this;
    }

    public ConfigurationModelBuilder pushReplacement(final ModuleImpl module) {
        push((GroupImpl) module.getProject().getGroup());
        replacements.add(module);
        return this;
    }

    private GroupImpl createGroup(final String name) {
        final GroupImpl group = new GroupImpl(name);

        groupMap.put(name, group);
        groups.add(group);

        return group;
    }

    private void sort() {
        groupSorter.sort(groups);
        groups.forEach(GroupImpl::sortProjects);
    }

    /**
     * Track a FileSystem for later cleanup in ConfigurationModel#close().
     * @param fs
     * @see ConfigurationModel#close()
     */
    public void addFileSystem(FileSystem fs) {
        filesystems.add(fs);
    }
}
