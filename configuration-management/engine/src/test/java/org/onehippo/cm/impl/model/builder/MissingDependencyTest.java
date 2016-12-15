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

public class MissingDependencyTest extends AbstractBaseTest {

    @Test(expected = MissingDependencyException.class)
    public void missing_dependency() {
        // config 1 depends on non existing foo
        configuration1.setDependsOn(ImmutableList.of("foo"));

        ConfigurationNodeBuilder builder = new ConfigurationNodeBuilder();
        builder.verifyDependencies(configuration1);
    }

    @Test(expected = MissingDependencyException.class)
    public void missing_dependency_again() {
        // config 1 depends on non existing foo
        configuration1.setDependsOn(ImmutableList.of(configuration2.getName()));
        configuration2.setDependsOn(ImmutableList.of("foo"));

        ConfigurationNodeBuilder builder = new ConfigurationNodeBuilder();
        builder.verifyDependencies(configuration1, configuration2);
    }
}
