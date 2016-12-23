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

        project1a = configuration1.addProject("project1a");
        project1b = configuration1.addProject("project1b");
        project1c = configuration1.addProject("project1c");

        module1a = project1a.addModule("module1a");
        module1b = project1a.addModule("module1b");
        module1c = project1a.addModule("module1c");

        source1a = module1a.addSource("/foo/bar/lux");
        source1b = module1a.addSource("/bar/foo/lux");
        source1c = module1a.addSource("/lux/bar");
    }

}
