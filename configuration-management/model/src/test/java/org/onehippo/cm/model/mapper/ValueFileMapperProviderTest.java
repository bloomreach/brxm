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
package org.onehippo.cm.model.mapper;

import java.util.List;

import org.junit.Test;
import org.onehippo.cm.model.AbstractBaseTest;
import org.onehippo.cm.model.impl.GroupImpl;
import org.onehippo.cm.model.impl.ModelTestUtils;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.ProjectImpl;
import org.onehippo.cm.model.impl.definition.AbstractDefinitionImpl;
import org.onehippo.cm.model.impl.definition.ConfigDefinitionImpl;
import org.onehippo.cm.model.impl.source.ConfigSourceImpl;
import org.onehippo.cm.model.impl.source.SourceImpl;
import org.onehippo.cm.model.impl.tree.DefinitionNodeImpl;
import org.onehippo.cm.model.impl.tree.DefinitionPropertyImpl;
import org.onehippo.cm.model.impl.tree.ValueImpl;
import org.onehippo.cm.model.tree.Value;
import org.onehippo.cm.model.tree.ValueType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ValueFileMapperProviderTest extends AbstractBaseTest {


    private static final String DUMMY_VALUE = "Dummy value";

    @Test
    public void testLoadPlugins() {
        ValueFileMapperProvider provider = ValueFileMapperProvider.getInstance();
        List<ValueFileMapper> list = provider.valueFileMappers;
        assertEquals(5, list.size());
        assertTrue(list.stream().anyMatch(x -> x.getClass().equals(NtFileMapper.class)));
        assertTrue(list.stream().anyMatch(x -> x.getClass().equals(DummyValueFileMapper.class)));
        assertTrue(list.stream().anyMatch(x -> x.getClass().equals(OtherValueFileMapper.class)));

        GroupImpl group = new GroupImpl("dummyGroup");
        ProjectImpl project = new ProjectImpl("dummyProject", group);
        ModuleImpl module = new ModuleImpl("dummyModule", project);
        ConfigSourceImpl source = new ConfigSourceImpl("somePath", module);
        ConfigDefinitionImpl definition = new ConfigDefinitionImpl(source);
        DefinitionNodeImpl definitionNode = new DefinitionNodeImpl("/path/to/", "dummyNode", definition);

        ValueImpl value = new ValueImpl(DUMMY_VALUE);
        definitionNode.addProperty("dummyProperty", value);

        String smartName = provider.generateName(value);
        assertEquals(DUMMY_VALUE, smartName);
    }

    @Test
    public void testNtFile() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /css:\n" +
                    "      jcr:primaryType: nt:folder\n" +
                    "      /bootstrap.css:\n" +
                    "        jcr:primaryType: nt:file\n" +
                    "        /jcr:content:\n" +
                    "          jcr:primaryType: nt:resource\n" +
                    "          jcr:data:\n" +
                    "            type: binary\n" +
                    "            value: !!binary xxxx\n" +
                    "          jcr:mimeType: text/css";
        final List<AbstractDefinitionImpl> defs = ModelTestUtils.parseNoSort(yaml);
        final DefinitionNodeImpl node = ((ConfigDefinitionImpl) defs.get(0)).getNode()
                .resolveNode("bootstrap.css/jcr:content");
        final DefinitionPropertyImpl property = node.getProperty("jcr:data");
        assertEquals("/css/bootstrap.css",
                ValueFileMapperProvider.getInstance().generateName(property.getValue()));
    }
}

class DummyValueFileMapper implements ValueFileMapper{

    @Override
    public String apply(Value value) {
        return (value.getType() != ValueType.STRING)? null : value.getString();
    }
}

class OtherValueFileMapper implements ValueFileMapper{

    @Override
    public String apply(Value value) {
        return null;
    }
}