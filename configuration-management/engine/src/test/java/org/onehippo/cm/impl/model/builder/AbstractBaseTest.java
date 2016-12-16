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

import com.google.common.collect.ImmutableMap;

import org.junit.Before;
import org.onehippo.cm.impl.model.ConfigurationImpl;
import org.onehippo.cm.impl.model.ModuleImpl;
import org.onehippo.cm.impl.model.ProjectImpl;

public abstract class AbstractBaseTest {

    protected ConfigurationImpl configuration1;
    protected ConfigurationImpl configuration2;
    protected ConfigurationImpl configuration3;

    protected ProjectImpl project1a;
    protected ProjectImpl project1b;
    protected ProjectImpl project1c;


    protected ModuleImpl module1a;
    protected ModuleImpl module1b;
    protected ModuleImpl module1c;


    protected DependencyVerifier verifier = new DependencyVerifier();
    protected DependencySorter sorter = new DependencySorter();

    @Before
    public void setup() {

        configuration1 = new ConfigurationImpl();
        configuration2 = new ConfigurationImpl();
        configuration3 = new ConfigurationImpl();

        configuration1.setName("configuration1");
        configuration2.setName("configuration2");
        configuration3.setName("configuration3");

        project1a = new ProjectImpl();
        project1b = new ProjectImpl();
        project1c = new ProjectImpl();

        project1a.setName("project1a");
        project1b.setName("project1b");
        project1c.setName("project1c");

        configuration1.setProjects(ImmutableMap.of(project1a.getName(), project1a, project1b.getName(), project1b, project1c.getName(), project1c));

        module1a = new ModuleImpl();
        module1b = new ModuleImpl();
        module1c = new ModuleImpl();

        module1a.setName("module1a");
        module1b.setName("module1b");
        module1c.setName("module1c");

        project1a.setModules(ImmutableMap.of(module1a.getName(), module1a, module1b.getName(), module1b, module1c.getName(), module1c));
    }
}
