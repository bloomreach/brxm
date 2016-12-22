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
import org.onehippo.cm.impl.model.SourceImpl;

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

    protected SourceImpl source1a;
    protected SourceImpl source1b;
    protected SourceImpl source1c;

    protected DependencyVerifier verifier = new DependencyVerifier();

    @Before
    public void setup() {

        configuration1 = new ConfigurationImpl("configuration1");
        configuration2 = new ConfigurationImpl("configuration2");
        configuration3 = new ConfigurationImpl("configuration3");

        project1a = new ProjectImpl("project1a", configuration1);
        project1b = new ProjectImpl("project1b", configuration1);
        project1c = new ProjectImpl("project1c", configuration1);

        module1a = new ModuleImpl("module1a", project1a);
        module1b = new ModuleImpl("module1b", project1a);
        module1c = new ModuleImpl("module1c", project1a);

        source1a = new SourceImpl();
        source1b = new SourceImpl();
        source1c = new SourceImpl();

        source1a.setPath("/foo/bar/lux");
        source1b.setPath("/bar/foo/lux");
        source1c.setPath("/lux/bar");

        module1a.setSources(ImmutableMap.of(source1a.getPath(),source1a,
                source1b.getPath(), source1b,
                source1c.getPath(), source1c));
    }
}
