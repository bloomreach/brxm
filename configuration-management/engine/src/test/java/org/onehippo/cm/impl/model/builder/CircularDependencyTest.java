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

import com.google.common.collect.ImmutableList;

import org.junit.Test;
import org.onehippo.cm.impl.model.ConfigurationImpl;
import org.onehippo.cm.impl.model.builder.exceptions.CircularDependencyException;

public class CircularDependencyTest extends AbstractBaseTest {


    @Test(expected = CircularDependencyException.class)
    public void configurations_self_circular_dependency() {
        // config 1 depends on config 1
        configuration1.setAfter(ImmutableList.of(configuration1.getName()));

        ConfigurationNodeBuilder builder = new ConfigurationNodeBuilder();
        builder.verifyConfigurationDependencies(ImmutableList.of(configuration1, configuration2));
    }

    @Test(expected = CircularDependencyException.class)
    public void configurations_two_wise_circular_dependency() {
        // config 1 depends on config 2
        configuration1.setAfter(ImmutableList.of(configuration2.getName()));
        // config 2 depends on config 1
        configuration2.setAfter(ImmutableList.of(configuration1.getName()));

        ConfigurationNodeBuilder builder = new ConfigurationNodeBuilder();
        builder.verifyConfigurationDependencies(ImmutableList.of(configuration1, configuration2));
    }

    @Test(expected = CircularDependencyException.class)
    public void configurations_three_wise_circular_dependency() {
        // config 1 depends on config 2
        configuration1.setAfter(ImmutableList.of(configuration2.getName()));
        // config 2 depends on config 3
        configuration2.setAfter(ImmutableList.of(configuration3.getName()));
        // config 3 depends on config 1
        configuration3.setAfter(ImmutableList.of(configuration1.getName()));

        ConfigurationNodeBuilder builder = new ConfigurationNodeBuilder();
        builder.verifyConfigurationDependencies(ImmutableList.of(configuration1, configuration2, configuration3));
    }

    @Test(expected = CircularDependencyException.class)
    public void configurations_complex_multiple_circular_dependencies() {
        // this test is to assure the verifyDependencies does not by accident loops forever

        // config 2 depends on config 1
        configuration2.setAfter(ImmutableList.of(configuration1.getName()));
        // config 3 depends on config 2
        configuration3.setAfter(ImmutableList.of(configuration2.getName()));
        // config 1 depends on config 3
        configuration1.setAfter(ImmutableList.of(configuration3.getName()));

        // extra circle
        ConfigurationImpl configuration2a = new ConfigurationImpl();
        ConfigurationImpl configuration2b = new ConfigurationImpl();

        configuration2a.setName("configuration2a");
        configuration2b.setName("configuration2b");

        configuration2.setAfter(ImmutableList.of(configuration2a.getName()));
        configuration2a.setAfter(ImmutableList.of(configuration2b.getName()));
        configuration2b.setAfter(ImmutableList.of(configuration2.getName()));

        ConfigurationNodeBuilder builder = new ConfigurationNodeBuilder();
        builder.verifyConfigurationDependencies(ImmutableList.of(configuration1, configuration2, configuration3, configuration2a, configuration2b));
    }

    @Test(expected = CircularDependencyException.class)
    public void projects_self_circular_dependency() {
        project1a.setAfter(ImmutableList.of(project1a.getName()));

        ConfigurationNodeBuilder builder = new ConfigurationNodeBuilder();
        builder.verifyProjectDependencies(ImmutableList.of(project1a, project1b));
    }

    @Test(expected = CircularDependencyException.class)
    public void projects_two_wise_circular_dependency() {
        project1a.setAfter(ImmutableList.of(project1b.getName()));
        project1b.setAfter(ImmutableList.of(project1a.getName()));

        ConfigurationNodeBuilder builder = new ConfigurationNodeBuilder();
        builder.verifyProjectDependencies(ImmutableList.of(project1a, project1b));
    }

    @Test(expected = CircularDependencyException.class)
    public void projects_three_wise_circular_dependency() {
        project1a.setAfter(ImmutableList.of(project1b.getName()));
        project1b.setAfter(ImmutableList.of(project1c.getName()));
        project1c.setAfter(ImmutableList.of(project1a.getName()));

        ConfigurationNodeBuilder builder = new ConfigurationNodeBuilder();
        builder.verifyProjectDependencies(ImmutableList.of(project1a, project1b, project1c));
    }
}
