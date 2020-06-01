/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.mapper;

import org.junit.Test;
import org.onehippo.cm.model.impl.GroupImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.ProjectImpl;
import org.onehippo.cm.model.impl.definition.ConfigDefinitionImpl;
import org.onehippo.cm.model.impl.source.ConfigSourceImpl;
import org.onehippo.cm.model.impl.tree.DefinitionNodeImpl;
import org.onehippo.cm.model.impl.tree.DefinitionPropertyImpl;
import org.onehippo.cm.model.impl.tree.ValueImpl;
import org.onehippo.cm.model.path.JcrPaths;

import static org.junit.Assert.assertEquals;

public class DefaultFileMapperTest {

    @Test
    public void testApply() {
        final DefaultFileMapper defaultFileMapper = new DefaultFileMapper();

        final ProjectImpl project = new ProjectImpl("project", new GroupImpl("group"));
        final ModuleImpl module = new ModuleImpl("module", project);
        final ConfigSourceImpl source = new ConfigSourceImpl("path/to/content.yaml", module);
        final ConfigDefinitionImpl configDefinition = new ConfigDefinitionImpl(source);

        final DefinitionNodeImpl definitionNode = new DefinitionNodeImpl(JcrPaths.getPath("/path/to"),
                JcrPaths.getSegment("node"), configDefinition);

        final ValueImpl valueImpl = new ValueImpl("SomeStringValue");
        valueImpl.makeStringResourceValue("/path/to/resource.yaml");

        new DefinitionPropertyImpl("propertyName", valueImpl, definitionNode);

        final String fileName = defaultFileMapper.apply(valueImpl);
        assertEquals("/path/to/propertyName.yaml", fileName);
    }
}