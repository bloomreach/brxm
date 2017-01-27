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

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

import org.junit.Test;
import org.onehippo.cm.api.model.ContentDefinition;
import org.onehippo.cm.api.model.NamespaceDefinition;
import org.onehippo.cm.api.model.NodeTypeDefinition;
import org.onehippo.cm.impl.model.ModuleImpl;
import org.onehippo.cm.impl.model.ProjectImpl;
import org.onehippo.cm.impl.model.SourceImpl;

import static org.junit.Assert.assertEquals;

public class DeepSortingTest extends AbstractBaseTest {

    @Test
    public void assert_deep_sorting() throws Exception {

        configuration1.setAfter(ImmutableList.of(configuration2.getName()));
        configuration3.setAfter(ImmutableList.of(configuration2.getName(), configuration1.getName()));
        project1a.setAfter(ImmutableList.of(project1b.getName()));
        project1b.setAfter(ImmutableList.of(project1c.getName()));
        module1a1.setAfter(ImmutableList.of(module1a2.getName()));
        module1a2.setAfter(ImmutableList.of(module1a3.getName()));

        final ProjectImpl project2a = configuration2.addProject("project2a");
        final ProjectImpl project3a = configuration3.addProject("project3a");

        final ModuleImpl module1b1 = project1b.addModule("module1b1");
        final ModuleImpl module1c1 = project1c.addModule("module1c1");
        final ModuleImpl module2a1 = project2a.addModule("module2a1");
        final ModuleImpl module3a1 = project3a.addModule("module3a1");

        final SourceImpl source1a2a = module1a2.addSource("/1/a/2/a");
        final SourceImpl source1a3a = module1a3.addSource("/1/a/3/a");
        final SourceImpl source1b1a = module1b1.addSource("/1/b/1/a");
        final SourceImpl source1c1a = module1c1.addSource("/1/c/1/a");
        final SourceImpl source2a1a = module2a1.addSource("/2/a/1/a");
        final SourceImpl source3a1a = module3a1.addSource("/3/a/1/a");

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
}
