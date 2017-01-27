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

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Before;
import org.onehippo.cm.impl.model.ConfigurationImpl;
import org.onehippo.cm.impl.model.ContentDefinitionImpl;
import org.onehippo.cm.impl.model.DefinitionNodeImpl;
import org.onehippo.cm.impl.model.ModuleImpl;
import org.onehippo.cm.impl.model.ProjectImpl;
import org.onehippo.cm.impl.model.SourceImpl;

import static org.junit.Assert.fail;

public abstract class AbstractBaseTest {

    protected ConfigurationImpl configuration1;
    protected ConfigurationImpl configuration2;
    protected ConfigurationImpl configuration3;

    protected ProjectImpl project1a;
    protected ProjectImpl project1b;
    protected ProjectImpl project1c;

    protected ModuleImpl module1a1;
    protected ModuleImpl module1a2;
    protected ModuleImpl module1a3;

    protected SourceImpl source1a1a;
    protected SourceImpl source1a1b;
    protected SourceImpl source1a1c;

    protected DependencyVerifier verifier = new DependencyVerifier();

    @Before
    public void setup() {
        configuration1 = new ConfigurationImpl("configuration1");
        configuration2 = new ConfigurationImpl("configuration2");
        configuration3 = new ConfigurationImpl("configuration3");

        project1a = configuration1.addProject("project1a");
        project1b = configuration1.addProject("project1b");
        project1c = configuration1.addProject("project1c");

        module1a1 = project1a.addModule("module1a1");
        module1a2 = project1a.addModule("module1a2");
        module1a3 = project1a.addModule("module1a3");

        source1a1a = module1a1.addSource("/foo/bar/lux");
        source1a1b = module1a1.addSource("/bar/foo/lux");
        source1a1c = module1a1.addSource("/lux/bar");
    }

    protected void addNamespaceDefinition(final SourceImpl source, final String id) {
        try {
            source.addNamespaceDefinition(id, new URI("http://www." + id + ".com"));
        } catch (URISyntaxException e) {
            fail("Unexpected exception");
        }
    }

    protected void addNodeTypeDefinition(final SourceImpl source, final String id) {
        source.addNodeTypeDefinition("cnd-" + id, false);
    }

    protected void addContentDefinition(final SourceImpl source, final String path) {
        setNode(source.addContentDefinition(), path);
    }

    protected void addConfigDefinition(final SourceImpl source, final String path) {
        setNode(source.addConfigDefinition(), path);
    }

    private void setNode(final ContentDefinitionImpl contentDefinition, final String path) {
        contentDefinition.setNode(new DefinitionNodeImpl(path, path.substring(path.lastIndexOf('/') + 1), contentDefinition));
    }
}
