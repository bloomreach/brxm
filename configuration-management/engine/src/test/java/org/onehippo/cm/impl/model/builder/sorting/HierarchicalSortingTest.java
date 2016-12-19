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
package org.onehippo.cm.impl.model.builder.sorting;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.Orderable;
import org.onehippo.cm.api.model.Project;
import org.onehippo.cm.impl.model.builder.AbstractBaseTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.onehippo.cm.impl.model.builder.sorting.DependencySorter.Sorter.getConfigurationSorter;

public class HierarchicalSortingTest extends AbstractBaseTest {

    @Ignore
    @Test
    public void assert_deep_sorting() throws Exception {

        configuration1.setAfter(ImmutableList.of(configuration2.getName()));
        configuration3.setAfter(ImmutableList.of(configuration2.getName(), configuration1.getName()));
        project1a.setAfter(ImmutableList.of(project1b.getName()));
        project1b.setAfter(ImmutableList.of(project1c.getName()));
        module1a.setAfter(ImmutableList.of(module1b.getName()));
        module1b.setAfter(ImmutableList.of(module1c.getName()));

        List<Configuration> sorted = new DependencySorter().sort(ImmutableList.of(configuration1, configuration2, configuration3));

        final String sortedConfigurationNames = sorted.stream().map((Function<Orderable, Object>)Orderable::getName).collect(Collectors.toList()).toString();
        assertEquals("[configuration2, configuration1, configuration3]", sortedConfigurationNames);

        // get configuration1
        Configuration sortedConfiguration1 = sorted.get(1);
        assertFalse("Expected new object for configuration", sortedConfiguration1 == configuration1);

        Map<String, Project> sortedProjects = sortedConfiguration1.getProjects();

        assertTrue(sortedProjects instanceof LinkedHashMap);
        assertTrue("Sorted Map should not be recreated on the fly", sortedProjects == sortedConfiguration1.getProjects());

        final String sortedProjectNames = sortedProjects.values().stream().map((Function<Orderable, Object>)Orderable::getName).collect(Collectors.toList()).toString();

        assertEquals("[project1c, project1b, project1a]", sortedProjectNames);

        Project sortedProject1a = sortedProjects.get("project1a");

        assertFalse(sortedProject1a.getConfiguration() == configuration1);
        assertTrue(sortedProject1a.getConfiguration() == sortedConfiguration1);

        Map<String, Module> sortedModules = sortedProject1a.getModules();

        assertTrue(sortedModules instanceof LinkedHashMap);

        final String sortedModuleNames = sortedModules.values().stream().map((Function<Orderable, Object>)Orderable::getName).collect(Collectors.toList()).toString();

        assertEquals("[module1c, module1b, module1a]", sortedModuleNames);

        assertFalse(sortedModules.get("module1a").getProject() == project1a);
        assertTrue(sortedModules.get("module1a").getProject() == sortedProject1a);
    }


}
