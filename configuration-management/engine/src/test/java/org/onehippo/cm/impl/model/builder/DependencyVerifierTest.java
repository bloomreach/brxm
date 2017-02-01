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
import com.google.common.collect.ImmutableSet;

import org.junit.Test;
import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.impl.model.ConfigurationImpl;
import org.onehippo.cm.impl.model.builder.exceptions.CircularDependencyException;
import org.onehippo.cm.impl.model.builder.exceptions.MissingDependencyException;

public class DependencyVerifierTest {

    private final DependencyVerifier verifier = new DependencyVerifier();

    /*
     * test circular dependency detection
     */

    @Test(expected = CircularDependencyException.class)
    public void configurations_self_circular_dependency() {
        Configuration c1 = new ConfigurationImpl("c1").addAfter(ImmutableSet.of("c1"));
        Configuration c2 = new ConfigurationImpl("c2");

        verifier.verify(ImmutableList.of(c1, c2));
    }

    @Test(expected = CircularDependencyException.class)
    public void configurations_two_wise_circular_dependency() {
        Configuration c1 = new ConfigurationImpl("c1").addAfter(ImmutableSet.of("c2"));
        Configuration c2 = new ConfigurationImpl("c2").addAfter(ImmutableSet.of("c1"));

        verifier.verify(ImmutableList.of(c1, c2));
    }

    @Test(expected = CircularDependencyException.class)
    public void configurations_three_wise_circular_dependency() {
        Configuration c1 = new ConfigurationImpl("c1").addAfter(ImmutableSet.of("c2"));
        Configuration c2 = new ConfigurationImpl("c2").addAfter(ImmutableSet.of("c3"));
        Configuration c3 = new ConfigurationImpl("c3").addAfter(ImmutableSet.of("c1"));

        verifier.verify(ImmutableList.of(c1, c2, c3));
    }

    @Test(expected = CircularDependencyException.class)
    public void configurations_complex_multiple_circular_dependencies() {
        // c2 is part of 2 circular dependencies
        Configuration c1 = new ConfigurationImpl("c1").addAfter(ImmutableSet.of("c3"));
        Configuration c2 = new ConfigurationImpl("c2").addAfter(ImmutableSet.of("c1", "c2a"));
        Configuration c3 = new ConfigurationImpl("c3").addAfter(ImmutableSet.of("c2"));

        Configuration c2a = new ConfigurationImpl("c2a").addAfter(ImmutableSet.of("c2b"));
        Configuration c2b = new ConfigurationImpl("c2b").addAfter(ImmutableSet.of("c2"));

        verifier.verify(ImmutableList.of(c1, c2, c3, c2a, c2b));
    }

    /*
     * test missing dependency detection
     */

    @Test(expected = MissingDependencyException.class)
    public void configuration_missing_dependency() {
        Configuration c1 = new ConfigurationImpl("c1").addAfter(ImmutableSet.of("foo"));

        verifier.verify(ImmutableList.of(c1));
    }

    @Test(expected = MissingDependencyException.class)
    public void configuration_missing_dependency_again() {
        Configuration c1 = new ConfigurationImpl("c1").addAfter(ImmutableSet.of("c1"));
        Configuration c2 = new ConfigurationImpl("c1").addAfter(ImmutableSet.of("foo"));

        verifier.verify(ImmutableList.of(c1, c2));
    }
}
