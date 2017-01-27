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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

import org.junit.Test;
import org.onehippo.cm.api.model.ContentDefinition;
import org.onehippo.cm.api.model.NamespaceDefinition;
import org.onehippo.cm.api.model.NodeTypeDefinition;
import org.onehippo.cm.impl.model.ConfigurationImpl;
import org.onehippo.cm.impl.model.ContentDefinitionImpl;
import org.onehippo.cm.impl.model.DefinitionNodeImpl;
import org.onehippo.cm.impl.model.ModuleImpl;
import org.onehippo.cm.impl.model.ProjectImpl;
import org.onehippo.cm.impl.model.SourceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class DeepSortingTest {

    @Test
    public void assert_deep_sorting() throws Exception {
        // TODO: change below data structure into one loaded from test resources

        ConfigurationImpl configuration1 = new ConfigurationImpl("c1").setAfter(ImmutableList.of("c2"));
        ConfigurationImpl configuration2 = new ConfigurationImpl("c2");
        ConfigurationImpl configuration3 = new ConfigurationImpl("c3").setAfter(ImmutableList.of("c2", "c1"));

        ProjectImpl project1a = configuration1.addProject("p1a").setAfter(ImmutableList.of("p1b"));
        ProjectImpl project1b = configuration1.addProject("p1b").setAfter(ImmutableList.of("p1c"));
        ProjectImpl project1c = configuration1.addProject("p1c");
        ProjectImpl project2a = configuration2.addProject("project2a");
        ProjectImpl project3a = configuration3.addProject("project3a");

        ModuleImpl module1a1 = project1a.addModule("m1a1").setAfter(ImmutableList.of("m1a2"));
        ModuleImpl module1a2 = project1a.addModule("m1a2").setAfter(ImmutableList.of("m1a3"));
        ModuleImpl module1a3 = project1a.addModule("m1a3");
        ModuleImpl module1b1 = project1b.addModule("module1b1");
        ModuleImpl module1c1 = project1c.addModule("module1c1");
        ModuleImpl module2a1 = project2a.addModule("module2a1");
        ModuleImpl module3a1 = project3a.addModule("module3a1");

        SourceImpl source1a1a = module1a1.addSource("/foo/bar/lux");
        SourceImpl source1a1b = module1a1.addSource("/bar/foo/lux");
        SourceImpl source1a1c = module1a1.addSource("/lux/bar");
        SourceImpl source1a2a = module1a2.addSource("/1/a/2/a");
        SourceImpl source1a3a = module1a3.addSource("/1/a/3/a");
        SourceImpl source1b1a = module1b1.addSource("/1/b/1/a");
        SourceImpl source1c1a = module1c1.addSource("/1/c/1/a");
        SourceImpl source2a1a = module2a1.addSource("/2/a/1/a");
        SourceImpl source3a1a = module3a1.addSource("/3/a/1/a");

        // add definitions to sources
        addConfigDefinition(source1a1a, "/path/to/nodeX");
        addNodeTypeDefinition(source1a1a, "source1a1a-nodetype");
        addNamespaceDefinition(source1a1a, "source1a1a-namespace");
        addContentDefinition(source1a1a, "/path/to");

        addNamespaceDefinition(source1a1b, "source1a1b-namespace");
        addConfigDefinition(source1a1b, "/path/to/nodeZ");
        addContentDefinition(source1a1b, "/path");

        addNodeTypeDefinition(source1a1c, "a-source1a1c-nodetype2");
        addConfigDefinition(source1a1c, "/path/to/nodeY");
        addNodeTypeDefinition(source1a1c, "a-source1a1c-nodetype1");

        addConfigDefinition(source1a2a, "source1a2a");
        addConfigDefinition(source1a3a, "source1a3a");
        addConfigDefinition(source1b1a, "source1b1a");
        addConfigDefinition(source1c1a, "source1c1a");
        addConfigDefinition(source2a1a, "source2a1a");
        addConfigDefinition(source3a1a, "source3a1a");


        final DefinitionTriple result = new ConfigurationNodeBuilder()
                .extractOrderedDefinitions(ImmutableList.of(configuration1, configuration2, configuration3));


        final List<NamespaceDefinition> nd = result.getNamespaceDefinitions();
        final String sortedNamespaces = nd.stream().map(NamespaceDefinition::getPrefix).collect(Collectors.toList()).toString();
        assertEquals("[source1a1a-namespace, source1a1b-namespace]", sortedNamespaces);

        final List<NodeTypeDefinition> nt = result.getNodeTypeDefinitions();
        final String sortedNodeTypes = nt.stream().map(NodeTypeDefinition::getValue).collect(Collectors.toList()).toString();
        assertEquals("[cnd-a-source1a1c-nodetype1, cnd-a-source1a1c-nodetype2, cnd-source1a1a-nodetype]", sortedNodeTypes);

        final List<ContentDefinition> cd = result.getContentDefinitions();
        final String sortedContentDefinitions = cd.stream().map(e -> e.getNode().getPath()).collect(Collectors.toList()).toString();
        assertEquals("[source2a1a, source1c1a, source1b1a, source1a3a, source1a2a, /path, /path/to, /path/to/nodeX, /path/to/nodeY, /path/to/nodeZ, source3a1a]", sortedContentDefinitions);
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
