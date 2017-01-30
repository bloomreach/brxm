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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cm.impl.model.ConfigurationImpl;
import org.onehippo.cm.impl.model.ConfigurationNodeImpl;
import org.onehippo.cm.impl.model.ContentDefinitionImpl;
import org.onehippo.cm.impl.model.ModuleImpl;
import org.onehippo.cm.impl.model.ProjectImpl;
import org.onehippo.cm.impl.model.builder.sorting.DefinitionSorter;

import static org.junit.Assert.assertEquals;

public class ModelBuilderTest {

    private final List<String> modules = new ArrayList<>();
    private final DependencyVerifier<ConfigurationImpl> VERIFIER = new DependencyVerifier<ConfigurationImpl>() {
        public void verifyConfigurationDependencies(final Collection<ConfigurationImpl> configurations) { }
    };
    private final ConfigurationTreeBuilder CONF_BUILDER = new ConfigurationTreeBuilder() {
        public void addDefinition(final ContentDefinitionImpl definition, ConfigurationNodeImpl root) { }
    };
    private final DefinitionSorter DEFINITION_SORTER = new DefinitionSorter() {
        public void sort(final ModuleImpl module, final MergedModel mergedModel) {
            modules.add(module.getName());
        }
    };

    private final ModelBuilder modelBuilder = new ModelBuilder();

    @Before
    public void setup() {
        modules.clear();
        modelBuilder.setDependencyVerifier(VERIFIER);
        modelBuilder.setConfigurationTreeBuilder(CONF_BUILDER);
        modelBuilder.setDefinitionSorter(DEFINITION_SORTER);
    }

    @Test
    public void assert_deep_sorting() throws Exception {
        // TODO: change below data structure into one loaded from test resources

        ConfigurationImpl configuration1 = new ConfigurationImpl("c1").setAfter(ImmutableList.of("c2"));
        ConfigurationImpl configuration2 = new ConfigurationImpl("c2");
        ConfigurationImpl configuration3 = new ConfigurationImpl("c3").setAfter(ImmutableList.of("c2", "c1"));

        ProjectImpl project1a = configuration1.addProject("p1a").setAfter(ImmutableList.of("p1b"));
        ProjectImpl project1b = configuration1.addProject("p1b").setAfter(ImmutableList.of("p1c"));
        ProjectImpl project1c = configuration1.addProject("p1c");
        ProjectImpl project2a = configuration2.addProject("p2a");
        ProjectImpl project3a = configuration3.addProject("p3a");

        project1a.addModule("m1a1").setAfter(ImmutableList.of("m1a2"));
        project1a.addModule("m1a2").setAfter(ImmutableList.of("m1a3"));
        project1a.addModule("m1a3");
        project1b.addModule("m1b1");
        project1c.addModule("m1c1");
        project2a.addModule("m2a1");
        project3a.addModule("m3a1");

        modelBuilder.build(ImmutableList.of(configuration1, configuration2, configuration3));

        assertEquals("[m2a1, m1c1, m1b1, m1a3, m1a2, m1a1, m3a1]", modules.toString());
    }
}
