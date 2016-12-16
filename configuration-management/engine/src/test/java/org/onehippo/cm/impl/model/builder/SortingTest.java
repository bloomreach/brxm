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
package org.onehippo.cm.impl.model.builder;

import java.util.SortedSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

import org.junit.Test;
import org.onehippo.cm.api.model.Configuration;

import static org.junit.Assert.assertEquals;

public class SortingTest extends AbstractBaseTest {

    @Test
    public void sort_two_configurations() throws Exception {

        // config 1 depends on config 2
        configuration1.setDependsOn(ImmutableList.of(configuration2.getName()));

        ConfigurationNodeBuilder builder = new ConfigurationNodeBuilder();
        SortedSet<Configuration> sorted = builder.sort(ImmutableList.of(configuration1, configuration2));

        String sortedNames = sorted.stream().map((Function<Configuration, Object>)Configuration::getName).collect(Collectors.toList()).toString();
        assertEquals("[configuration2, configuration1]", sortedNames);

        SortedSet<Configuration> sorted2 = builder.sort(ImmutableList.of(configuration2, configuration1));
        assertEquals(sorted, sorted2);
    }

    @Test
    public void sort_three_configurations() throws Exception {

        // config 1 depends on config 2
        configuration1.setDependsOn(ImmutableList.of(configuration2.getName()));
        // config 3 depends on config 1
        configuration3.setDependsOn(ImmutableList.of(configuration1.getName()));

        ConfigurationNodeBuilder builder = new ConfigurationNodeBuilder();
        SortedSet<Configuration> sorted = builder.sort(ImmutableList.of(configuration1, configuration2, configuration3));

        String sortedNames = sorted.stream().map((Function<Configuration, Object>)Configuration::getName).collect(Collectors.toList()).toString();
        assertEquals("[configuration2, configuration1, configuration3]", sortedNames);

        SortedSet<Configuration> sorted2 = builder.sort(ImmutableList.of(configuration2, configuration3, configuration1));
        assertEquals(sorted, sorted2);
    }

    @Test
    public void sort_three_configurations_multiple_dependencies() throws Exception {

        // config 1 depends on config 2
        configuration1.setDependsOn(ImmutableList.of(configuration2.getName()));
        // config 3 depends on config 1 and config 2
        configuration3.setDependsOn(ImmutableList.of(configuration1.getName(), configuration2.getName()));

        ConfigurationNodeBuilder builder = new ConfigurationNodeBuilder();
        SortedSet<Configuration> sorted = builder.sort(ImmutableList.of(configuration1, configuration2, configuration3));

        String sortedNames = sorted.stream().map((Function<Configuration, Object>)Configuration::getName).collect(Collectors.toList()).toString();
        assertEquals("[configuration2, configuration1, configuration3]", sortedNames);

        SortedSet<Configuration> sorted2 = builder.sort(ImmutableList.of(configuration2, configuration3, configuration1));
        assertEquals(sorted, sorted2);

    }

    @Test
    public void sort_three_undeterministic_depends_sorts_on_name() throws Exception {

        // config 1 depends on config 2
        configuration1.setDependsOn(ImmutableList.of(configuration2.getName()));
        // config 3 depends on config 2
        configuration3.setDependsOn(ImmutableList.of(configuration2.getName()));

        ConfigurationNodeBuilder builder = new ConfigurationNodeBuilder();
        SortedSet<Configuration> sorted = builder.sort(ImmutableList.of(configuration1, configuration2, configuration3));

        String sortedNames = sorted.stream().map((Function<Configuration, Object>)Configuration::getName).collect(Collectors.toList()).toString();
        assertEquals("[configuration2, configuration1, configuration3]", sortedNames);

        SortedSet<Configuration> sorted2 = builder.sort(ImmutableList.of(configuration2, configuration3, configuration1));
        assertEquals(sorted, sorted2);
    }

}
