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
import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.impl.model.ConfigurationImpl;
import org.onehippo.cm.impl.model.ProjectImpl;
import org.onehippo.cm.impl.model.builder.exceptions.CircularDependencyException;
import org.onehippo.cm.impl.model.builder.exceptions.MissingDependencyException;

public class DependencyVerifierTest {

    private final DependencyVerifier verifier = new DependencyVerifier();

    /*
     * test circular dependency detection
     */

    @Test(expected = CircularDependencyException.class)
    public void configurations_self_circular_dependency() {
        Configuration c1 = new ConfigurationImpl("c1").setAfter(ImmutableList.of("c1"));
        Configuration c2 = new ConfigurationImpl("c2");

        verifier.verifyConfigurationDependencies(ImmutableList.of(c1, c2));
    }

    @Test(expected = CircularDependencyException.class)
    public void configurations_two_wise_circular_dependency() {
        Configuration c1 = new ConfigurationImpl("c1").setAfter(ImmutableList.of("c2"));
        Configuration c2 = new ConfigurationImpl("c2").setAfter(ImmutableList.of("c1"));

        verifier.verifyConfigurationDependencies(ImmutableList.of(c1, c2));
    }

    @Test(expected = CircularDependencyException.class)
    public void configurations_three_wise_circular_dependency() {
        Configuration c1 = new ConfigurationImpl("c1").setAfter(ImmutableList.of("c2"));
        Configuration c2 = new ConfigurationImpl("c2").setAfter(ImmutableList.of("c3"));
        Configuration c3 = new ConfigurationImpl("c3").setAfter(ImmutableList.of("c1"));

        verifier.verifyConfigurationDependencies(ImmutableList.of(c1, c2, c3));
    }

    @Test(expected = CircularDependencyException.class)
    public void configurations_complex_multiple_circular_dependencies() {
        // c2 is part of 2 circular dependencies
        Configuration c1 = new ConfigurationImpl("c1").setAfter(ImmutableList.of("c3"));
        Configuration c2 = new ConfigurationImpl("c2").setAfter(ImmutableList.of("c1", "c2a"));
        Configuration c3 = new ConfigurationImpl("c3").setAfter(ImmutableList.of("c2"));

        Configuration c2a = new ConfigurationImpl("c2a").setAfter(ImmutableList.of("c2b"));
        Configuration c2b = new ConfigurationImpl("c2b").setAfter(ImmutableList.of("c2"));

        verifier.verifyConfigurationDependencies(ImmutableList.of(c1, c2, c3, c2a, c2b));
    }

    @Test(expected = CircularDependencyException.class)
    public void projects_self_circular_dependency() {
        ConfigurationImpl c1 = new ConfigurationImpl("c1");

        c1.addProject("p1").setAfter(ImmutableList.of("p1"));
        c1.addProject("p2");

        verifier.verifyConfigurationDependencies(ImmutableList.of(c1));
    }

    @Test(expected = CircularDependencyException.class)
    public void projects_two_wise_circular_dependency() {
        ConfigurationImpl c1 = new ConfigurationImpl("c1");

        c1.addProject("p1").setAfter(ImmutableList.of("p2"));
        c1.addProject("p2").setAfter(ImmutableList.of("p1"));

        verifier.verifyConfigurationDependencies(ImmutableList.of(c1));
    }

    @Test(expected = CircularDependencyException.class)
    public void projects_three_wise_circular_dependency() {
        ConfigurationImpl c1 = new ConfigurationImpl("c1");

        c1.addProject("p1").setAfter(ImmutableList.of("p2"));
        c1.addProject("p2").setAfter(ImmutableList.of("p3"));
        c1.addProject("p3").setAfter(ImmutableList.of("p1"));

        verifier.verifyConfigurationDependencies(ImmutableList.of(c1));
    }

    @Test(expected = CircularDependencyException.class)
    public void modules_self_circular_dependency() {
        ConfigurationImpl c1 = new ConfigurationImpl("c1");
        ProjectImpl p1 = c1.addProject("p1");

        p1.addModule("m1").setAfter(ImmutableList.of("m1"));
        p1.addModule("m2");

        verifier.verifyConfigurationDependencies(ImmutableList.of(c1));
    }

    @Test(expected = CircularDependencyException.class)
    public void modules_two_wise_circular_dependency() {
        ConfigurationImpl c1 = new ConfigurationImpl("c1");
        ProjectImpl p1 = c1.addProject("p1");

        p1.addModule("m1").setAfter(ImmutableList.of("m2"));
        p1.addModule("m2").setAfter(ImmutableList.of("m1"));

        verifier.verifyConfigurationDependencies(ImmutableList.of(c1));
    }

    @Test(expected = CircularDependencyException.class)
    public void modules_three_wise_circular_dependency() {
        ConfigurationImpl c1 = new ConfigurationImpl("c1");
        ProjectImpl p1 = c1.addProject("p1");

        p1.addModule("m1").setAfter(ImmutableList.of("m2"));
        p1.addModule("m2").setAfter(ImmutableList.of("m3"));
        p1.addModule("m3").setAfter(ImmutableList.of("m1"));

        verifier.verifyConfigurationDependencies(ImmutableList.of(c1));
    }

    /*
     * test missing dependency detection
     */

    @Test(expected = MissingDependencyException.class)
    public void configuration_missing_dependency() {
        Configuration c1 = new ConfigurationImpl("c1").setAfter(ImmutableList.of("foo"));

        verifier.verifyConfigurationDependencies(ImmutableList.of(c1));
    }

    @Test(expected = MissingDependencyException.class)
    public void configuration_missing_dependency_again() {
        Configuration c1 = new ConfigurationImpl("c1").setAfter(ImmutableList.of("c1"));
        Configuration c2 = new ConfigurationImpl("c1").setAfter(ImmutableList.of("foo"));

        verifier.verifyConfigurationDependencies(ImmutableList.of(c1, c2));
    }

    @Test(expected = MissingDependencyException.class)
    public void project_missing_dependency() {
        ConfigurationImpl c1 = new ConfigurationImpl("c1");
        c1.addProject("p1").setAfter(ImmutableList.of("foo"));

        verifier.verifyConfigurationDependencies(ImmutableList.of(c1));
    }

    @Test(expected = MissingDependencyException.class)
    public void project_missing_dependency_again() {
        ConfigurationImpl c1 = new ConfigurationImpl("c1");
        c1.addProject("p1").setAfter(ImmutableList.of("p2"));
        c1.addProject("p2").setAfter(ImmutableList.of("foo"));

        verifier.verifyConfigurationDependencies(ImmutableList.of(c1));
    }

    @Test(expected = MissingDependencyException.class)
    public void module_missing_dependency() {
        ConfigurationImpl c1 = new ConfigurationImpl("c1");
        ProjectImpl p1 = c1.addProject("p1");
        p1.addModule("m1").setAfter(ImmutableList.of("foo"));

        verifier.verifyConfigurationDependencies(ImmutableList.of(c1));
    }

    @Test(expected = MissingDependencyException.class)
    public void module_missing_dependency_again() {
        ConfigurationImpl c1 = new ConfigurationImpl("c1");
        ProjectImpl p1 = c1.addProject("p1");
        p1.addModule("m1").setAfter(ImmutableList.of("m2"));
        p1.addModule("m2").setAfter(ImmutableList.of("foo"));

        verifier.verifyConfigurationDependencies(ImmutableList.of(c1));
    }
}
